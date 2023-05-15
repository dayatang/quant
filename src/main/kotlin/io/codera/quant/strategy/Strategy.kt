package io.codera.quant.strategy

import io.codera.quant.context.TradingContext
import io.codera.quant.exception.PriceNotAvailableException
import org.slf4j.LoggerFactory

/**
 * Strategy interface.
 */
interface Strategy {
    /**
     * Executed every time when new data is received.
     * Drives placing trades based on loaded entry and exit criteria and lot sizes.
     * This method will also update [object if run in backtest mode][BackTestResult].
     */
    fun onTick() {
        if (isCommonCriteriaMet) {
            if (isEntryCriteriaMet) {
                try {
                    openPosition()
                } catch (e: PriceNotAvailableException) {
                    log.error("Price for requested contract is not available")
                }
            } else if (isStopLossCriteriaMet) {
                try {
                    closePosition()
                } catch (e: PriceNotAvailableException) {
                    log.error("Price for requested contract is not available")
                }
            } else if (isExitCriteriaMet) {
                try {
                    closePosition()
                } catch (e: PriceNotAvailableException) {
                    log.error("Price for requested contract is not available")
                }
            }
        }
    }

    /**
     * Calculates the lot size based on configured trade context and strategy logic.
     *
     * @param contract instrument/contract name
     * @param buy      true of buy, false if sell
     * @return size of the lot
     */
    fun getLotSize(contract: String?, buy: Boolean): Int

    /**
     * Checks if common criterion is met for current tick.
     *
     * @return true if met, false otherwise
     */
    val isCommonCriteriaMet: Boolean

    /**
     * Checks if entry criterion is met for current tick.
     *
     * @return true if met, false otherwise
     */
    val isEntryCriteriaMet: Boolean

    /**
     * Checks if exit criterion is met for current tick.
     *
     * @return true if met, false otherwise
     */
    val isExitCriteriaMet: Boolean
    val isStopLossCriteriaMet: Boolean
        /**
         * Checks if stop loss criterion is met for current tick.
         *
         * @return true if met, false otherwise
         */
        get() = false

    /**
     * Adds stop loss criterion.
     *
     * @param criterion common criterion
     */
    fun addStopLossCriterion(criterion: Criterion?) {}

    /**
     * Adds common criterion.
     *
     * @param criterion common criterion
     */
    fun addCommonCriterion(criterion: Criterion)

    /**
     * Adds entry criterion.
     *
     * @param criterion entry criterion
     */
    fun addEntryCriterion(criterion: Criterion)

    /**
     * Removes common criterion.
     *
     * @param criterion common criterion
     */
    fun removeCommonCriterion(criterion: Criterion?)

    /**
     * Removes entry criterion.
     *
     * @param criterion entry criterion
     */
    fun removeEntryCriterion(criterion: Criterion?)

    /**
     * Adds exit criterion.
     *
     * @param criterion exit criterion
     */
    fun addExitCriterion(criterion: Criterion)

    /**
     * Remove exit criterion.
     *
     * @param criterion exit criterion
     */
    fun removeExitCriterion(criterion: Criterion?)

    /**
     * Returns additional data needed for back testing.
     *
     * @return [BackTestResult] object
     */
    val backTestResult: BackTestResult?

    /**
     * Add symbol to run strategy against.
     *
     * @param symbol contract symbol
     */
    fun addSymbol(symbol: String)

    /**
     * Returns strategy [TradingContext]
     *
     * @return
     */
    val tradingContext: TradingContext

    /**
     * Opens position in one or several contracts when entry [Criterion] is met.
     */
    @Throws(PriceNotAvailableException::class)
    fun openPosition()

    /**
     * Closes position for contract when exit [Criterion] is met.
     */
    @Throws(PriceNotAvailableException::class)
    fun closePosition()

    companion object {
        val log = LoggerFactory.getLogger(Strategy::class.java)
    }
}
