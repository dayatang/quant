package io.codera.quant.strategy.criterion

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.strategy.Criterion

/**
 * Checks that no open orders available for specified symbols
 */
open class NoOpenOrdersExistEntryCriterion(
    protected val tradingContext: TradingContext,
    protected val symbols: List<String>
) : Criterion {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            for (symbol in symbols) {
                try {
                    val order = tradingContext.getLastOrderBySymbol(symbol)
                    if (order != null) {
                        return false
                    }
                } catch (ignored: NoOrderAvailableException) {
                }
            }
            return true
        }
}
