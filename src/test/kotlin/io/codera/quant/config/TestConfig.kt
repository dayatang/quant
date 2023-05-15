package io.codera.quant.config

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.ib.client.OrderType
import com.ib.controller.ApiController
import io.codera.quant.context.IbTradingContext
import io.codera.quant.context.TradingContext
import io.codera.quant.strategy.IbPerMinuteStrategyRunner
import io.codera.quant.strategy.StrategyRunner
import java.sql.SQLException

/**
 * Test configuration
 */
class TestConfig : AbstractModule() {
    override fun configure() {
        bind(StrategyRunner::class.java).to(IbPerMinuteStrategyRunner::class.java)
    }

    @Provides
    fun apiController(): ApiController {
        val controller = ApiController(IbConnectionHandler(), { _: String -> }) { _: String -> }
        controller.connect(HOST, PORT, 0, null)
        return controller
    }

    @Provides
    @Throws(SQLException::class, ClassNotFoundException::class)
    fun tradingContext(controller: ApiController?): TradingContext {
        return IbTradingContext(
            controller!!,
            ContractBuilder(),
            OrderType.MKT,
            2
        )
    }

    companion object {
        private const val HOST = "localhost"
        private const val PORT = 4002
    }
}
