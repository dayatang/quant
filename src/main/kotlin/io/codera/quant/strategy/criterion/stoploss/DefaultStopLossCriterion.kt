package io.codera.quant.strategy.criterion.stoploss

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.strategy.Criterion
import org.slf4j.LoggerFactory

/**
 *
 */
class DefaultStopLossCriterion(
    private val symbols: List<String>, private val thresholdAmount: Double,
    private val tradingContext: TradingContext
) : Criterion {
    @get:Throws(CriterionViolationException::class)
    override val isMet: Boolean
        get() {
            // check if there are open orders
            var totalPl = 0.0
            for (symbol in symbols) {
                totalPl += try {
                    val order = tradingContext.getLastOrderBySymbol(symbol)
                    val symbolPl =
                        tradingContext.getLastPrice(symbol) * order!!.amount + order.openPrice * -order.amount
                    log.debug("Symbol P/L: {}", symbolPl)
                    symbolPl
                } catch (noOrderAvailable: NoOrderAvailableException) {
                    return false
                } catch (noOrderAvailable: PriceNotAvailableException) {
                    return false
                }
            }
            log.debug("Total PL: {}", totalPl)
            return totalPl <= thresholdAmount
        }

    companion object {
        private val log = LoggerFactory.getLogger(DefaultStopLossCriterion::class.java)
    }
}
