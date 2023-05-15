package io.codera.quant.strategy.kalman

import io.codera.quant.config.ContractBuilder
import io.codera.quant.context.IbTradingContext
import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.strategy.*
import org.apache.commons.math3.stat.StatUtils
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.main.strategy.kalman.Cointegration
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Kalman filter strategy
 */
class KalmanFilterStrategy(
    private val firstSymbol: String,
    private val secondSymbol: String,
    tradingContext: TradingContext,
    private val cointegration: Cointegration
) : AbstractStrategy(tradingContext) {
    private var beta = 0.0
    private var baseAmount = 0.0
    private var sd = 0.0
    private val mainEntry: ErrorIsMoreThanStandardDeviationEntry
    private val mainExit: KalmanFilterExitCriterion

    init {
        mainEntry = ErrorIsMoreThanStandardDeviationEntry()
        mainExit = KalmanFilterExitCriterion()
        addEntryCriterion(mainEntry)
        addExitCriterion(mainExit)
    }

    fun setErrorQueueSize(size: Int) {
        mainEntry.setErrorQueueSize(size)
    }

    fun setEntrySdMultiplier(multiplier: Double) {
        mainEntry.setSdMultiplier(multiplier)
    }

    fun setExitMultiplier(multiplier: Double) {
        mainExit.setSdMultiplier(multiplier)
    }

    @Throws(PriceNotAvailableException::class)
    override fun openPosition() {
        tradingContext.placeOrder(secondSymbol, cointegration.error < 0, baseAmount)
        Strategy.Companion.log.debug("Order of {} in amount {}", secondSymbol, baseAmount)
        tradingContext.placeOrder(firstSymbol, cointegration.error > 0, baseAmount * beta)
        Strategy.Companion.log.debug("Order of {} in amount {}", firstSymbol, baseAmount * beta)
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

    override fun getLotSize(contract: String?, buy: Boolean): Int {
        throw UnsupportedOperationException()
    }

    inner class ErrorIsMoreThanStandardDeviationEntry : Criterion {
        private val errorQueue: Queue<Double> = ConcurrentLinkedQueue()
        private val ERROR_QUEUE_SIZE_DEFAULT = 30
        fun setSdMultiplier(sdMultiplier: Double) {
            this.sdMultiplier = sdMultiplier
        }

        private var sdMultiplier = 1.0
        fun setErrorQueueSize(errorQueueSize: Int) {
            this.errorQueueSize = errorQueueSize
        }

        private var errorQueueSize: Int = ERROR_QUEUE_SIZE_DEFAULT

        override fun init() {
            if (tradingContext is IbTradingContext) {
                val firstSymbolHistory = tradingContext.getHistory(firstSymbol, 2)
                val secondSymbolHistory = tradingContext.getHistory(secondSymbol, 2)
                val multipleDoubleSeries = MultipleDoubleSeries(
                    firstSymbolHistory,
                    secondSymbolHistory
                )
                for (entry in multipleDoubleSeries) {
                    // TODO (dsinyakov): remove cointegration logic duplication below
                    var x = entry.item[0]
                    var y = entry.item[1]
                    if (firstSymbol.contains("=F") && secondSymbol.contains("=F")) {
                        x = x * ContractBuilder.Companion.getFutureMultiplier(firstSymbol)!!
                        y = y * ContractBuilder.Companion.getFutureMultiplier(secondSymbol)!!
                    }
                    cointegration.step(x, y)
                    val error = cointegration.error
                    errorQueue.add(error)
                    if (errorQueue.size > errorQueueSize + 1) {
                        errorQueue.poll()
                    }
                }
            }
        }

        @get:Throws(CriterionViolationException::class)
        override val isMet: Boolean
            get() {
                Strategy.Companion.log.debug("Evaluating ErrorIsMoreThanStandardDeviationEntry criteria")
                var x: Double
                try {
                    x = tradingContext.getLastPrice(firstSymbol)
                    Strategy.Companion.log.info("Current {} price {}", firstSymbol, x)
                } catch (e: PriceNotAvailableException) {
                    Strategy.Companion.log.error("Price for $firstSymbol is not available.")
                    return false
                }
                var y: Double
                try {
                    y = tradingContext.getLastPrice(secondSymbol)
                    Strategy.Companion.log.info("Current {} price {}", secondSymbol, y)
                } catch (e: PriceNotAvailableException) {
                    Strategy.Companion.log.error("Price for $secondSymbol is not available.")
                    return false
                }
                beta = cointegration.beta
                if (firstSymbol.contains("=F") && secondSymbol.contains("=F")) {
                    x = x * ContractBuilder.Companion.getFutureMultiplier(firstSymbol)!!
                    y = y * ContractBuilder.Companion.getFutureMultiplier(secondSymbol)!!
                }
                cointegration.step(x, y)
                val error = cointegration.error
                errorQueue.add(error)
                Strategy.Companion.log.debug("Error Queue size: {}", errorQueue.size)
                if (errorQueue.size > errorQueueSize + 1) {
                    errorQueue.poll()
                }
                if (errorQueue.size > errorQueueSize) {
                    Strategy.Companion.log.debug("Kalman filter queue is > $errorQueueSize")
                    val errors: Array<Any> = errorQueue.toTypedArray()
                    val lastValues = DoubleArray(errorQueueSize / 2)
                    var i = errors.size - 1
                    var lastValIndex = 0
                    while (i > errors.size - 1 - errorQueueSize / 2) {
                        lastValues[lastValIndex] = java.lang.Double.valueOf(errors[i].toString())
                        i--
                        lastValIndex++
                    }
                    sd = Math.sqrt(StatUtils.variance(lastValues))
                    val realSd = sdMultiplier * sd
                    Strategy.Companion.log.info("error={}, sd={}", error, realSd)
                    if (Math.abs(error) > realSd) {
                        Strategy.Companion.log.debug("error is bigger than square root of standard deviation")
                        Strategy.Companion.log.debug("Net value {}", tradingContext.netValue)
                        if (secondSymbol.contains("=F")) {
                            //Exchange	Underlying	Product description	Trading Class	Intraday Initial 1	Intraday Maintenance 1	Overnight Initial	Overnight Maintenance	Currency	Has Options
                            //GLOBEX	ES	E-mini S&P 500	                          ES	3665	    2932	7330	5864	USD
                            // 	Yes
                            //ECBOT	YM	Mini Sized Dow Jones Industrial Average $5	YM	3218.125	2574.50	6436.25	5149	USD	Yes
                            baseAmount = 4.0
                            beta = 1.0
                        } else {
                            baseAmount = (tradingContext.netValue * 0.5 * Math.min(4.0, tradingContext.leverage)
                                    / (y + beta * x))
                            Strategy.Companion.log.debug(
                                "baseAmount={},  sd={}, beta={}",
                                baseAmount,
                                cointegration.error,
                                beta
                            )
                        }
                        if (beta > 0 && baseAmount * beta >= 1) {
                            Strategy.Companion.log.info("error={}, sd={}", error, realSd)
                            Strategy.Companion.log.info("{} price {}; {} price {}", firstSymbol, x, secondSymbol, y)
                            return true
                        }
                    }
                }
                return false
            }

    }

    internal inner class KalmanFilterExitCriterion : Criterion {
        private var sdMultiplier = 0.0
        fun setSdMultiplier(sdMultiplier: Double) {
            this.sdMultiplier = sdMultiplier
        }

        @get:Throws(CriterionViolationException::class)
        override val isMet: Boolean
            get() {
                Strategy.Companion.log.debug("Evaluating KalmanFilterExitCriterion criteria")
                try {
                    if (tradingContext.getLastOrderBySymbol(secondSymbol)!!.isLong &&
                        cointegration.error > sdMultiplier * sd ||
                        tradingContext.getLastOrderBySymbol(secondSymbol)!!.isShort &&
                        cointegration.error < -sdMultiplier * sd
                    ) {
                        Strategy.Companion.log.info("error={}, sd={}", cointegration.error, sd)
                        Strategy.Companion.log.info(
                            "{} price {}; {} price {}", firstSymbol, tradingContext.getLastPrice(
                                firstSymbol
                            ),
                            secondSymbol, tradingContext.getLastPrice(secondSymbol)
                        )
                        return true
                    }
                } catch (noOrderAvailable: NoOrderAvailableException) {
                    Strategy.Companion.log.debug("No orders available for $secondSymbol")
                    return false
                } catch (e: PriceNotAvailableException) {
                    Strategy.Companion.log.debug("No price available for some symbol")
                    return false
                }
                return false
            }
    }
}
