package io.codera.quant.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.ib.client.*
import com.ib.controller.ApiController
import io.codera.quant.config.ContractBuilder
import io.codera.quant.observers.HistoryObserver
import io.codera.quant.observers.IbHistoryObserver
import io.codera.quant.util.Helper.Response.Rates
import org.joda.time.*
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 *
 */
object Helper {
    private val logger = LoggerFactory.getLogger(Helper::class.java)
    @Throws(IOException::class, NoSuchFieldException::class, IllegalAccessException::class)
    fun getFxQuotes(baseSymbol: String, symbol: String) {
        val dt1 = DateTime(2012, 3, 26, 12, 0, 0, 0)
        val dt2 = DateTime(2017, 1, 1, 12, 0, 0, 0)
        val startDate = LocalDate(dt1)
        val endDate = LocalDate(dt2)
        val restOperations = RestTemplate()
        val converters = restOperations.messageConverters
        for (converter in converters) {
            if (converter is MappingJackson2HttpMessageConverter) {
                val jsonConverter = converter
                jsonConverter.objectMapper = ObjectMapper()
                jsonConverter.supportedMediaTypes = ImmutableList.of(
                    MediaType("application", "json", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET),
                    MediaType("text", "javascript", MappingJackson2HttpMessageConverter.DEFAULT_CHARSET)
                )
            }
        }
        val days = Days.daysBetween(startDate, endDate).days
        val lines: MutableList<String> = Lists.newLinkedList()
        lines.add("Date, Adj Close")
        val file = Paths.get(
            String.format(
                "/Users/beastie/Downloads/%s%s_quotes.csv",
                baseSymbol.lowercase(Locale.getDefault()),
                symbol.lowercase(Locale.getDefault())
            )
        )
        for (i in 0 until days) {
            val d = startDate.withFieldAdded(DurationFieldType.days(), i)
            val res = restOperations.getForObject(
                String.format(
                    "http://api.fixer.io/%s?base=%s&symbols=%s",
                    d, baseSymbol, symbol
                ),
                Response::class.java
            )
            val field = Rates::class.java.getField(symbol.lowercase(Locale.getDefault()))
            val resSymbol = field[res.rates] as Double
            lines.add(String.format("%s, %s", d, resSymbol))
            logger.info("{} {}/{} -  {}", res.date, baseSymbol, symbol, resSymbol)
        }
        Files.write(file, lines, Charset.forName("UTF-8"))
    }

    fun getHistoryForSymbols(
        controller: ApiController,
        daysOfHistory: Int,
        symbols: List<String>
    ): MultipleDoubleSeries {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        val date = LocalDateTime.now().format(formatter)
        val doubleSeries: MutableList<DoubleSeries> = Lists.newArrayList()
        for (symbol in symbols) {
            val contract = ContractBuilder.build(symbol)
            val historyObserver: HistoryObserver = IbHistoryObserver(symbol)
            controller.reqHistoricalData(
                contract, date, daysOfHistory, Types.DurationUnit.DAY,
                Types.BarSize._1_min, Types.WhatToShow.TRADES, false, false, historyObserver
            )
            doubleSeries.add(
                (historyObserver as IbHistoryObserver).observableDoubleSeries()
                    .toBlocking()
                    .first()
            )
        }
        return MultipleDoubleSeries(doubleSeries)
    }

    fun getMean(data: DoubleArray): Double {
        var sum = 0.0
        for (a in data) sum += a
        return sum / data.size
    }

    private fun getVariance(data: DoubleArray): Double {
        val mean = getMean(data)
        var temp = 0.0
        for (a in data) temp += (a - mean) * (a - mean)
        return temp / data.size
    }

    fun getStdDev(data: DoubleArray): Double {
        return Math.sqrt(getVariance(data))
    }

    internal class Response {
        var base: String? = null
        var date: String? = null
        var rates: Rates? = null

        internal class Rates {
            @JsonProperty("AUD")
            var aud = 0.0

            @JsonProperty("CAD")
            var cad = 0.0

            @JsonProperty("CHF")
            var chf = 0.0

            @JsonProperty("CYP")
            var cyp = 0.0

            @JsonProperty("CZK")
            var czk = 0.0

            @JsonProperty("DKK")
            var dkk = 0.0

            @JsonProperty("EEK")
            var eek = 0.0

            @JsonProperty("GBP")
            var gbp = 0.0

            @JsonProperty("HKD")
            var hkd = 0.0

            @JsonProperty("HUF")
            var huf = 0.0

            @JsonProperty("ISK")
            var isk = 0.0

            @JsonProperty("JPY")
            var jpy = 0.0

            @JsonProperty("KRW")
            var krw = 0.0

            @JsonProperty("LTL")
            var ltl = 0.0

            @JsonProperty("LVL")
            var lvl = 0.0

            @JsonProperty("MTL")
            var mtl = 0.0

            @JsonProperty("NOK")
            var nok = 0.0

            @JsonProperty("NZD")
            var nzd = 0.0

            @JsonProperty("PLN")
            var pln = 0.0

            @JsonProperty("ROL")
            var rol = 0.0

            @JsonProperty("SEK")
            var sek = 0.0

            @JsonProperty("SGD")
            var sgd = 0.0

            @JsonProperty("SIT")
            var sit = 0.0

            @JsonProperty("SKK")
            var skk = 0.0

            @JsonProperty("TRY")
            var trl = 0.0

            @JsonProperty("ZAR")
            var zar = 0.0

            @JsonProperty("EUR")
            var eur = 0.0

            @JsonProperty("BGN")
            var bgn = 0.0

            @JsonProperty("BRL")
            var brl = 0.0

            @JsonProperty("CNY")
            var cny = 0.0

            @JsonProperty("HRK")
            var hrk = 0.0

            @JsonProperty("IDR")
            var idr = 0.0

            @JsonProperty("ILS")
            var ils = 0.0

            @JsonProperty("INR")
            var inr = 0.0

            @JsonProperty("MXN")
            var mxn = 0.0

            @JsonProperty("MYR")
            var myr = 0.0

            @JsonProperty("PHP")
            var php = 0.0

            @JsonProperty("RON")
            var ron = 0.0

            @JsonProperty("RUB")
            var rub = 0.0

            @JsonProperty("THB")
            var thb = 0.0

            @JsonProperty("USD")
            var usd = 0.0
            override fun toString(): String {
                return "aud=" + aud +
                        ", cad=" + cad +
                        ", chf=" + chf +
                        ", cyp=" + cyp +
                        ", czk=" + czk +
                        ", dkk=" + dkk +
                        ", eek=" + eek +
                        ", gbp=" + gbp +
                        ", hkd=" + hkd +
                        ", huf=" + huf +
                        ", isk=" + isk +
                        ", jpy=" + jpy +
                        ", krw=" + krw +
                        ", ltl=" + ltl +
                        ", lvl=" + lvl +
                        ", mtl=" + mtl +
                        ", nok=" + nok +
                        ", nzd=" + nzd +
                        ", pln=" + pln +
                        ", rol=" + rol +
                        ", sek=" + sek +
                        ", sgd=" + sgd +
                        ", sit=" + sit +
                        ", skk=" + skk +
                        ", trl=" + trl +
                        ", zar=" + zar +
                        ", eur=" + eur +
                        ", bgn=" + bgn +
                        ", brl=" + brl +
                        ", cny=" + cny +
                        ", hrk=" + hrk +
                        ", idr=" + idr +
                        ", ils=" + ils +
                        ", inr=" + inr +
                        ", mxn=" + mxn +
                        ", myr=" + myr +
                        ", php=" + php +
                        ", ron=" + ron +
                        ", rub=" + rub +
                        ", thb=" + thb +
                        ", usd=" + usd
            }
        }
    }
}
