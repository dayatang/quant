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

class YahooFinance(
    private val connection: Connection? = null
) : HistoricalPriceService {

    override fun getHistoricalAdjustedPrices(
        symbol: String
    ): Observable<DoubleSeries> {
        return getHistoricalAdjustedPrices(symbol, DEFAULT_FROM.toInstant())
    }

    fun getHistoricalAdjustedPrices(
        symbol: String, from: Instant
    ): Observable<DoubleSeries> {
        return getHistoricalAdjustedPrices(symbol, from, Instant.now())
    }

    fun getHistoricalAdjustedPrices(
        symbol: String, from: Instant, to: Instant
    ): Observable<DoubleSeries> {
        return getHistoricalPricesCsv(symbol, from, to)
            .map { csv: String -> csvToDoubleSeries(csv, symbol) }
    }

    @Throws(IOException::class)
    fun readCsvToDoubleSeries(
        csvFilePath: String, symbol: String
    ): DoubleSeries {
        val lines = Files.lines(Paths.get(csvFilePath))
        var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
        prices.name = symbol
        prices = prices.toAscending()
        return prices
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun readCsvToDoubleSeriesFromResource(
        csvResourcePath: String, symbol: String
    ): DoubleSeries {
        val resourceUrl = getResource(csvResourcePath)
        val lines = Files.lines(Paths.get(resourceUrl.toURI()))
        var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
        prices.name = symbol
        prices = prices.toAscending()
        return prices
    }

    @Throws(SQLException::class)
    fun readSeriesFromDb(symbol: String): DoubleSeries {
        if (connection == null) return DoubleSeries(symbol)
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM quotes WHERE symbol='$symbol'")
        val doubleSeries = DoubleSeries(symbol)
        while (rs.next()) {
            doubleSeries.add(rs.getDouble(3),
                Instant.ofEpochMilli(rs.getTime(4).time))
        }
        rs.close()
        stmt.close()
        return doubleSeries
    }

    private fun getResource(resource: String): URL =
        Thread.currentThread().contextClassLoader?.getResource(resource)?: //Try with the Thread Context Loader.
        System::class.java.classLoader?.getResource(resource)?: //Let's now try with the classloader that loaded this class.
        ClassLoader.getSystemResource(resource) //Last ditch attempt. Get the resource from the classpath.

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
                .flatMap(Http.asString())
        }

        private fun csvToDoubleSeries(csv: String, symbol: String): DoubleSeries {
            val lines = Stream.of(*csv.split("\n".toRegex())
                .dropLastWhile(String::isEmpty)
                .toTypedArray())
            var prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN)
            prices.name = symbol
            prices = prices.toAscending()
            return prices
        }

        private fun createHistoricalPricesUrl(symbol: String, from: Instant, to: Instant): String {
            return "https://ichart.yahoo.com/table.csv?s=$symbol&" +
                    "${toYahooQueryDate(from, "abc")}&" +
                    "${toYahooQueryDate(to, "def")}&g=d&ignore=.csv"
        }

        private fun toYahooQueryDate(instant: Instant, names: String): String {
            val time = instant.atOffset(ZoneOffset.UTC)
            val strings = names.split("".toRegex())
                .dropLastWhile(String::isEmpty)
                .toTypedArray()
            return "${strings[0]}=${time.monthValue - 1}" +
                    "&${strings[1]}=${time.dayOfMonth}" +
                    "&${strings[2]}=${time.year}"
        }
    }
}
