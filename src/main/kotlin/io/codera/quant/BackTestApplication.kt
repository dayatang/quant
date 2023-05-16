package io.codera.quant

import com.google.common.collect.ImmutableList
import com.ib.controller.ApiController
import io.codera.quant.config.IbConnectionHandler
import io.codera.quant.context.TradingContext
import io.codera.quant.strategy.Criterion
import io.codera.quant.strategy.Strategy
import io.codera.quant.strategy.criterion.NoOpenOrdersExistEntryCriterion
import io.codera.quant.strategy.criterion.OpenOrdersExistForAllSymbolsExitCriterion
import io.codera.quant.strategy.kalman.KalmanFilterStrategy
import io.codera.quant.util.Helper
import org.lst.trading.lib.backtest.BackTest
import org.lst.trading.lib.backtest.BackTestTradingContext
import org.lst.trading.main.strategy.kalman.Cointegration
import java.io.IOException
import java.sql.SQLException
import java.util.*
import kotlin.math.abs

/**
 * Back test Kalman filter cointegration strategy against SPY/VOO pair using Interactive Brokers
 * historical data
 */
object BackTestApplication {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_IB_PORT = 7497
    const val DEFAULT_CLIENT_ID = 0
    const val DAYS_OF_HISTORY = 7
    @Throws(IOException::class, SQLException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val controller = ApiController(IbConnectionHandler(), { _: String? -> }) { _: String? -> }
        controller.connect(DEFAULT_HOST, DEFAULT_IB_PORT, DEFAULT_CLIENT_ID, null)
        val contracts: List<String> = ImmutableList.of("SPY", "VOO")
        val priceSeries = Helper.getHistoryForSymbols(controller, DAYS_OF_HISTORY, contracts)
        // initialize the backtesting engine
        val deposit = 30000
        val backTest = BackTest(deposit.toDouble(), priceSeries)
        backTest.leverage = 4.0
        val tradingContext: TradingContext = BackTestTradingContext()
        val strategy: Strategy = KalmanFilterStrategy(
            contracts[0],
            contracts[1],
            tradingContext,
            Cointegration(1e-4, 1e-3)
        )
        val noOpenOrdersExistCriterion: Criterion = NoOpenOrdersExistEntryCriterion(tradingContext, contracts)
        val openOrdersExistForAllSymbolsCriterion: Criterion =
            OpenOrdersExistForAllSymbolsExitCriterion(tradingContext, contracts)
        strategy.addEntryCriterion(noOpenOrdersExistCriterion)
        strategy.addExitCriterion(openOrdersExistForAllSymbolsCriterion)

        // do the backtest
        val result = backTest.run(strategy)

        // show results
        val orders = StringBuilder()
        orders.append("id,amount,side,instrument,from,to,open,close,pl\n")
        for (order in result.orders) {
            orders.append(
                String.format(
                    Locale.US, "%d,%d,%s,%s,%s,%s,%f,%f,%f\n",
                    order.id,
                    abs(order.amount),
                    if (order.isLong) "Buy" else "Sell",
                    order.instrument,
                    order.openInstant,
                    order.closeInstant,
                    order.openPrice,
                    order.closePrice,
                    order.pl
                )
            )
        }
        print(orders)
        println()
        println("Backtest result of " + strategy.javaClass + ": " + strategy)
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
                result.returnRate * 100, result.returnRate / (DAYS_OF_HISTORY / 251.0) * 100, result.sharpe
            )
        )
        // TODO: quick and dirty method to finish the program. Implement a better way
        System.exit(0)
    }
}
