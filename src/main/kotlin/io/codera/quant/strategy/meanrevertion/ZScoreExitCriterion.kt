package io.codera.quant.strategy.meanrevertion

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.strategy.Criterion

/**
 *
 */
class ZScoreExitCriterion(
    private val firstSymbol: String, private val secondSymbol: String,
    private val zScore: ZScore, private val tradingContext: TradingContext
) : Criterion {
    private var exitZScore = 0.0

    constructor(
        firstSymbol: String, secondSymbol: String,
        exitZScore: Double, zScore: ZScore, tradingContext: TradingContext
    ) : this(firstSymbol, secondSymbol, zScore, tradingContext) {
        this.exitZScore = exitZScore
    }

    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            try {
                val zs = zScore[tradingContext.getLastPrice(firstSymbol), tradingContext.getLastPrice(secondSymbol)]
                if (tradingContext.getLastOrderBySymbol(firstSymbol)!!.isShort && zs < exitZScore ||
                    tradingContext.getLastOrderBySymbol(firstSymbol)!!.isLong && zs > exitZScore
                ) {
                    return true
                }
            } catch (e: PriceNotAvailableException) {
                return false
            } catch (noOrderAvailable: NoOrderAvailableException) {
                noOrderAvailable.printStackTrace()
                return false
            }
            return false
        }
}
