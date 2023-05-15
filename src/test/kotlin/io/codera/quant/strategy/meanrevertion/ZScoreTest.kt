package io.codera.quant.strategy.meanrevertion

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import io.codera.quant.config.GuiceJUnit4Runner
import io.codera.quant.context.TradingContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.lst.trading.lib.util.yahoo.YahooFinance
import java.io.IOException
import java.net.URISyntaxException

/**
 * Tests for [ZScore]
 */
@RunWith(GuiceJUnit4Runner::class)
class ZScoreTest {

    @Inject
    private lateinit var tradingContext: TradingContext

    @Test
    fun getTest() {
            for (symbol in SYMBOLS) {
                tradingContext.addContract(symbol)
            }
            val firstSymbolHistory = tradingContext.getHistoryInMinutes(SYMBOLS[0], MINUTES_OF_HISTORY)
            val secondSymbolHistory = tradingContext.getHistoryInMinutes(SYMBOLS[1], MINUTES_OF_HISTORY)
            val zScore = ZScore(firstSymbolHistory.toArray(), secondSymbolHistory.toArray(), LOOKBACK)
            println(zScore[114.7, 10.30])
        }

    @Throws(IOException::class, URISyntaxException::class)
    @Test
    fun getTestUnit() {
            val finance = YahooFinance()
            val gld = finance.readCsvToDoubleSeriesFromResource("GLD.csv", SYMBOLS[0])
            val uso = finance.readCsvToDoubleSeriesFromResource("USO.csv", SYMBOLS[1])
            val zScore = ZScore(gld.toArray(), uso.toArray(), LOOKBACK)
            Assert.assertEquals("Failed", -1.0102216127916113, zScore[58.33, 66.35], 0.0)
            Assert.assertEquals("Failed", -0.9692409006953596, zScore[57.73, 67.0], 0.0)
            Assert.assertEquals("Failed", -0.9618287583543594, zScore[57.99, 66.89], 0.0)
        }

    companion object {
        private val SYMBOLS: List<String> = ImmutableList.of("GLD", "USO")
        private const val LOOKBACK = 20
        private const val MINUTES_OF_HISTORY = LOOKBACK * 2 - 1
    }
}
