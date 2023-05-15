package io.codera.quant.strategy.criterion.common

import com.ib.client.OrderStatus
import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.strategy.Criterion

/**
 * Test if there are any orders that are in pending (not filled yet) state
 */
class NoPendingOrdersCommonCriterion(private val tradingContext: TradingContext, private val symbols: List<String>) :
    Criterion {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            for (symbol in symbols) {
                try {
                    if (tradingContext.getLastOrderBySymbol(symbol) != null
                        && tradingContext.getLastOrderBySymbol(symbol)!!.orderStatus != OrderStatus.Filled
                    ) {
                        return false
                    }
                } catch (noOrderAvailable: NoOrderAvailableException) {
                } // Do nothing here as there is not order
            }
            return true
        }
}
