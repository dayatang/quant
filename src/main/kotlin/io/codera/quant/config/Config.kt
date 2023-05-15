package io.codera.quant.config

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.ib.client.OrderType
import com.ib.controller.ApiController
import io.codera.quant.context.IbTradingContext
import io.codera.quant.context.TradingContext
import io.codera.quant.strategy.Criterion
import io.codera.quant.strategy.IbPerMinuteStrategyRunner
import io.codera.quant.strategy.Strategy
import io.codera.quant.strategy.StrategyRunner
import io.codera.quant.strategy.criterion.NoOpenOrdersExistEntryCriterion
import io.codera.quant.strategy.criterion.OpenIbOrdersExistForAllSymbolsExitCriterion
import io.codera.quant.strategy.criterion.common.NoPendingOrdersCommonCriterion
import io.codera.quant.strategy.criterion.stoploss.DefaultStopLossCriterion
import io.codera.quant.strategy.meanrevertion.BollingerBandsStrategy
import io.codera.quant.strategy.meanrevertion.ZScore
import io.codera.quant.strategy.meanrevertion.ZScoreEntryCriterion
import io.codera.quant.strategy.meanrevertion.ZScoreExitCriterion
import io.codera.quant.util.MathUtil
import java.sql.SQLException
import java.util.*

/**
 */
class Config(private val host: String, private val port: Int, private val symbolList: String) : AbstractModule() {
    override fun configure() {
        bind(StrategyRunner::class.java).to(IbPerMinuteStrategyRunner::class.java)
    }

    @Provides
    fun apiController(): ApiController {
        val controller = ApiController(IbConnectionHandler(), { valueOf: String? -> }) { valueOf: String? -> }
        controller.connect(host, port, 0, null)
        return controller
    }

    @Provides
    @Throws(SQLException::class, ClassNotFoundException::class)
    fun tradingContext(controller: ApiController): TradingContext {
        return IbTradingContext(
            controller,
            ContractBuilder(),
            OrderType.MKT,  //        DriverManager.getConnection("jdbc:mysql://localhost/fx", "root", "admin"),
            2
        )
    }

    @Provides
    fun strategy(tradingContext: TradingContext): Strategy {
        val contracts = Arrays.asList(*symbolList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        val zScore = ZScore(20, MathUtil())
        val strategy: Strategy = BollingerBandsStrategy(
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
        val noPendingOrdersCommonCriterion: Criterion = NoPendingOrdersCommonCriterion(tradingContext, contracts)
        val noOpenOrdersExistCriterion: Criterion = NoOpenOrdersExistEntryCriterion(tradingContext, contracts)
        val openOrdersExistForAllSymbolsCriterion: Criterion =
            OpenIbOrdersExistForAllSymbolsExitCriterion(tradingContext, contracts)
        val stopLoss: Criterion = DefaultStopLossCriterion(contracts, -100.0, tradingContext)
        strategy.addCommonCriterion(noPendingOrdersCommonCriterion)
        strategy.addEntryCriterion(noOpenOrdersExistCriterion)
        strategy.addEntryCriterion(zScoreEntryCriterion)
        strategy.addExitCriterion(openOrdersExistForAllSymbolsCriterion)
        strategy.addEntryCriterion(zScoreExitCriterion)
        strategy.addStopLossCriterion(stopLoss)
        return strategy
    }
}
