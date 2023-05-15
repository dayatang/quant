package org.lst.trading.lib.util.yahoo

import org.lst.trading.lib.csv.CsvReader
import org.lst.trading.lib.csv.CsvReader.ParseFunction
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.util.HistoricalPriceService
import org.lst.trading.lib.util.Http
import org.slf4j.LoggerFactory
import rx.Observable
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class YahooFinance : HistoricalPriceService {
    private var connection: Connection? = null

    constructor()
    constructor(connection: Connection?) {
        this.connection = connection
    }

    override fun getHistoricalAdjustedPrices(symbol: String): Observable<DoubleSeries?> {
        return getHistoricalAdjustedPrices(symbol, DEFAULT_FROM.toInstant())
    }

    fun getHistoricalAdjustedPrices(symbol: String, from: Instant): Observable<DoubleSeries?> {
        return getHistoricalAdjustedPrices(symbol, from, Instant.now())
    }

    fun getHistoricalAdjustedPrices(symbol: String, from: Instant, to: Instant): Observable<DoubleSeries?> {
        return getHistoricalPricesCsv(symbol, from, to).map { csv: String -> csvToDoubleSeries(csv, symbol) }
    }

    @Throws(IOException::class)
    fun readCsvToDoubleSeries(csvFilePath: String?, symbol: String): DoubleSeries? {
        val lines = Files.lines(Paths.get(csvFilePath))
        var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
        prices.name = symbol
        prices = prices.toAscending()
        return prices
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun readCsvToDoubleSeriesFromResource(csvResourcePath: String, symbol: String): DoubleSeries {
        val resourceUrl = getResource(csvResourcePath)
        val lines = Files.lines(Paths.get(resourceUrl.toURI()))
        var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
        prices.name = symbol
        prices = prices.toAscending()
        return prices
    }

    @Throws(SQLException::class)
    fun readSeriesFromDb(symbol: String): DoubleSeries {
        if (connection == null) {
            return DoubleSeries(symbol)
        }
        val stmt = connection!!.createStatement()
        val rs = stmt.executeQuery(String.format("SELECT * FROM quotes WHERE symbol='%s'", symbol))
        val doubleSeries = DoubleSeries(symbol)
        while (rs.next()) {
            doubleSeries.add(rs.getDouble(3), Instant.ofEpochMilli(rs.getTime(4).time))
        }
        rs.close()
        stmt.close()
        return doubleSeries
    }

    private fun getResource(resource: String): URL {
        var url: URL

        //Try with the Thread Context Loader.
        var classLoader = Thread.currentThread().contextClassLoader
        if (classLoader != null) {
            url = classLoader.getResource(resource)
            if (url != null) {
                return url
            }
        }

        //Let's now try with the classloader that loaded this class.
        classLoader = System::class.java.classLoader
        if (classLoader != null) {
            url = classLoader.getResource(resource)
            if (url != null) {
                return url
            }
        }

        //Last ditch attempt. Get the resource from the classpath.
        return ClassLoader.getSystemResource(resource)
    }

    companion object {
        const val SEP = ","
        val DATE_COLUMN: ParseFunction<Instant> = ParseFunction.ofColumn("Date")
            .map {
                LocalDate.from(DateTimeFormatter.ISO_DATE.parse(it))
                    .atStartOfDay(ZoneOffset.UTC.normalized())
                    .toInstant()
            }
        val CLOSE_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("Close")
        val HIGH_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("High")
        val LOW_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("Low")
        val OPEN_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("Open")
        val ADJ_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("Adj Close")
        val VOLUME_COLUMN: ParseFunction<Double> = ParseFunction.doubleColumn("Volume")
        val DEFAULT_FROM = OffsetDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        private val log = LoggerFactory.getLogger(YahooFinance::class.java)
        private fun getHistoricalPricesCsv(symbol: String, from: Instant, to: Instant): Observable<String> {
            return Http.get(createHistoricalPricesUrl(symbol, from, to))
                .flatMap<String>(Http.asString())
        }

        private fun csvToDoubleSeries(csv: String, symbol: String): DoubleSeries {
            val lines = Stream.of(*csv.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
            var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
            prices.name = symbol
            prices = prices.toAscending()
            return prices
        }

        private fun createHistoricalPricesUrl(symbol: String, from: Instant, to: Instant): String {
            return String.format(
                "https://ichart.yahoo.com/table.csv?s=%s&%s&%s&g=d&ignore=.csv", symbol,
                toYahooQueryDate(from, "abc"), toYahooQueryDate(to, "def")
            )
        }

        private fun toYahooQueryDate(instant: Instant, names: String): String {
            val time = instant.atOffset(ZoneOffset.UTC)
            val strings = names.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return String.format(
                "%s=%d&%s=%d&%s=%d",
                strings[0],
                time.monthValue - 1,
                strings[1],
                time.dayOfMonth,
                strings[2],
                time.year
            )
        }
    }
}
