package io.codera.quant.backtest

import com.google.common.collect.ImmutableList
import com.ib.controller.ApiController
import io.codera.quant.config.IbConnectionHandler
import io.codera.quant.context.TradingContext
import io.codera.quant.strategy.Criterion
import io.codera.quant.strategy.Strategy
import io.codera.quant.strategy.criterion.NoOpenOrdersExistEntryCriterion
import io.codera.quant.strategy.criterion.OpenOrdersExistForAllSymbolsExitCriterion
import io.codera.quant.strategy.meanrevertion.BollingerBandsStrategy
import io.codera.quant.strategy.meanrevertion.ZScore
import io.codera.quant.strategy.meanrevertion.ZScoreEntryCriterion
import io.codera.quant.strategy.meanrevertion.ZScoreExitCriterion
import io.codera.quant.util.Helper
import io.codera.quant.util.MathUtil
import org.lst.trading.lib.backtest.BackTest
import org.lst.trading.lib.backtest.BackTestTradingContext
import java.util.*

/**
 * Back test for [io.codera.quant.strategy.meanrevertion.BollingerBandsStrategy]
 */
object BollingerBandsBackTest {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_IB_PORT = 7497
    const val DEFAULT_CLIENT_ID = 0
    const val DAYS_OF_HISTORY = 7
    @JvmStatic
    fun main(args: Array<String>) {
        val controller = ApiController(IbConnectionHandler(), { valueOf: String? -> }) { valueOf: String? -> }
        controller.connect(DEFAULT_HOST, DEFAULT_IB_PORT, DEFAULT_CLIENT_ID, null)
        val contracts: List<String> = ImmutableList.of("EWA", "EWC")
        val priceSeries = Helper.getHistoryForSymbols(controller, DAYS_OF_HISTORY, contracts)
        // initialize the backtesting engine
        val deposit = 30000
        val backTest = BackTest(deposit.toDouble(), priceSeries)
        backTest.leverage = 4.0
        val tradingContext: TradingContext = BackTestTradingContext()
        val zScore = ZScore(20, MathUtil())
        val bollingerBandsStrategy: Strategy = BollingerBandsStrategy(
            contracts[0],
            contracts[1],
            tradingContext,
            zScore
        )
        val zScoreEntryCriterion: Criterion = ZScoreEntryCriterion(
            contracts[0], contracts[1], 1.0, zScore,
            tradingContext
        )
        val zScoreExitCriterion: Criterion = ZScoreExitCriterion(
            contracts[0], contracts[1], 0.0, zScore,
            tradingContext
        )
        val noOpenOrdersExistCriterion: Criterion = NoOpenOrdersExistEntryCriterion(tradingContext, contracts)
        val openOrdersExistForAllSymbolsCriterion: Criterion =
            OpenOrdersExistForAllSymbolsExitCriterion(tradingContext, contracts)
        bollingerBandsStrategy.addEntryCriterion(noOpenOrdersExistCriterion)
        bollingerBandsStrategy.addEntryCriterion(zScoreEntryCriterion)
        bollingerBandsStrategy.addExitCriterion(openOrdersExistForAllSymbolsCriterion)
        bollingerBandsStrategy.addExitCriterion(zScoreExitCriterion)

        // do the backtest
        val result = backTest.run(bollingerBandsStrategy)

        // show results
        val orders = StringBuilder()
        orders.append("id,amount,side,instrument,from,to,open,close,pl\n")
        for (order in result.orders) {
            orders.append(
                String.format(
                    Locale.US, "%d,%d,%s,%s,%s,%s,%f,%f,%f\n", order.id,
                    Math.abs(order.amount), if (order.isLong) "Buy" else "Sell", order.instrument,
                    order.openInstant,
                    order.closeInstant,
                    order.openPrice,
                    order.closePrice,
                    order.pl
                )
            )
        }
        println(orders)
        println("Prices: $priceSeries")
        println(
            String.format(
                Locale.US, "Simulated %d days, Initial deposit %d, Leverage %f",
                DAYS_OF_HISTORY, deposit, backTest.leverage
            )
        )
        println(String.format(Locale.US, "Commissions = %f", result.commissions))
        println(
            String.format(
                Locale.US,
                "P/L = %.2f, Final value = %.2f, Result = %.2f%%, Annualized = %.2f%%, Sharpe (rf=0%%) = %.2f",
                result.pl,
                result.finalValue,
                result.getReturn() * 100, result.getReturn() / (DAYS_OF_HISTORY / 251.0) * 100, result.sharpe
            )
        )
        // TODO: quick and dirty method to finish the program. Implement a better way
        System.exit(0)
    }
}
