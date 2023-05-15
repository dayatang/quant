package io.codera.quant.strategy.criterion

import com.ib.client.OrderStatus
import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.strategy.Criterion

/**
 * Created by beastie on 1/23/17.
 */
class OpenIbOrdersExistForAllSymbolsExitCriterion(
    protected val tradingContext: TradingContext,
    protected val symbols: List<String>
) : Criterion {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            for (symbol in symbols) {
                try {
                    val order = tradingContext.getLastOrderBySymbol(symbol)
                    if (order!!.orderStatus != OrderStatus.Filled) {
                        return false
                    }
                } catch (e: NoOrderAvailableException) {
                    return false
                }
            }
            return true
        }
}
