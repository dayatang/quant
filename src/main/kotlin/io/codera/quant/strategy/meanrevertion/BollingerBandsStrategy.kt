package io.codera.quant.strategy.meanrevertion

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.strategy.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Bollinger bands strategy
 */
class BollingerBandsStrategy(
    private val firstSymbol: String, private val secondSymbol: String,
    tradingContext: TradingContext, private val zScore: ZScore
) : AbstractStrategy(tradingContext) {
    override fun getLotSize(contract: String, buy: Boolean): Int {
        return 0
    }

    @Throws(PriceNotAvailableException::class)
    override fun openPosition() {
        val hedgeRatio = abs(zScore.hedgeRatio)
        val baseAmount = (tradingContext.netValue * 0.5 * min(4.0, tradingContext.leverage.toDouble())
                / (tradingContext.getLastPrice(secondSymbol) + hedgeRatio * tradingContext.getLastPrice(firstSymbol)))
        tradingContext.placeOrder(
            firstSymbol, zScore.lastCalculatedZScore < 0,
            max((baseAmount * hedgeRatio), 1.0)
        )
        Strategy.log.debug("Order of {} in amount {}", firstSymbol, baseAmount * hedgeRatio)
        tradingContext.placeOrder(secondSymbol, zScore.lastCalculatedZScore > 0, baseAmount)
        Strategy.log.debug("Order of {} in amount {}", secondSymbol, baseAmount)
    }

    @Throws(PriceNotAvailableException::class)
    override fun closePosition() {
        try {
            tradingContext.closeOrder(tradingContext.getLastOrderBySymbol(firstSymbol))
        } catch (noOrderAvailable: NoOrderAvailableException) {
            Strategy.log.error("No order available for {}", firstSymbol)
        }
        try {
            tradingContext.closeOrder(tradingContext.getLastOrderBySymbol(secondSymbol))
        } catch (noOrderAvailable: NoOrderAvailableException) {
            Strategy.log.error("No order available for {}", secondSymbol)
        }
    }
}
