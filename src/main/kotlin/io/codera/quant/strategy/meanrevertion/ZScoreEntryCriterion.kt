package io.codera.quant.strategy.meanrevertion

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.strategy.Criterion

/**
 *
 */
class ZScoreEntryCriterion(
    private val firstSymbol: String, private val secondSymbol: String,
    private val entryZScore: Double, private val zScore: ZScore, private val tradingContext: TradingContext
) : Criterion {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            try {
                val zs = zScore[tradingContext.getLastPrice(firstSymbol), tradingContext.getLastPrice(secondSymbol)]
                if (zs < -entryZScore || zs > entryZScore) {
                    return true
                }
            } catch (e: PriceNotAvailableException) {
                return false
            }
            return false
        }
}
