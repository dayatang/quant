package io.codera.quant.strategy.criterion

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException

/**
 * All orders exist for specified
 */
class OpenOrdersExistForAllSymbolsExitCriterion(
    tradingContext: TradingContext,
    symbols: List<String>
) : NoOpenOrdersExistEntryCriterion(tradingContext, symbols) {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            for (symbol in symbols) {
                try {
                    tradingContext.getLastOrderBySymbol(symbol)
                } catch (e: NoOrderAvailableException) {
                    return false
                }
            }
            return true
        }
}
