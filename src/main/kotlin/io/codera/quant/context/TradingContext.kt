package io.codera.quant.context

import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.observers.MarketDataObserver
import org.lst.trading.lib.model.ClosedOrder
import org.lst.trading.lib.model.Order
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.TimeSeries
import java.time.Instant
import java.util.stream.Stream

/**
 * Contains all data needed to run strategy:
 * contract prices, balances etc.
 */
interface TradingContext {
    val time: Instant?
        /**
         * Returns the time of current tick
         * @return timestamp
         */
        get() = Instant.now()

    /**
     * Returns last price of the contract
     *
     * @param contract contract name
     * @return  last price
     */
    @Throws(PriceNotAvailableException::class)
    fun getLastPrice(contract: String?): Double

    /**
     * Returns history of prices.
     *
     * @param contract contract name
     * @return historical collection of prices
     */
    fun getHistory(contract: String?): Stream<TimeSeries.Entry<Double?>?>? {
        throw UnsupportedOperationException()
    }

    /**
     * Returns history of prices.
     *
     * @param contract contract name
     * @param numberOfDays seconds of history to return before current time instant
     * @return historical collection of prices
     */
    fun getHistory(contract: String, numberOfDays: Int): DoubleSeries? {
        throw UnsupportedOperationException()
    }

    /**
     * Returns history of prices.
     *
     * @param contract contract name
     * @param numberOfMinutes seconds of history to return before current time instant
     * @return historical collection of prices
     */
    fun getHistoryInMinutes(contract: String, numberOfMinutes: Int): DoubleSeries? {
        throw UnsupportedOperationException()
    }

    /**
     * Adds contract into trading contract.
     *
     * @param contract contract name
     */
    fun addContract(contract: String)

    /**
     * Removes contract from context.
     *
     * @param contract contract name
     */
    fun removeContract(contract: String)

    /**
     * returns a collection of current contracts in context.
     *
     * @return collection of contracts
     */
    val contracts: List<String>

    /**
     * returns funds currently available for trading.
     *
     * @return funds currently available for trading
     */
    val availableFunds: Double

    /**
     * Returns cash balance
     * @return cache balance
     */
    val netValue: Double

    /**
     * Returns leverage
     *
     * @return leverage
     */
    val leverage: Double

    /**
     * Returns contract observer
     *
     * @param contractSymbol
     * @return
     */
    fun getObserver(contractSymbol: String?): MarketDataObserver? {
        throw UnsupportedOperationException()
    }

    /**
     * Place a contract order
     *
     * @param contractSymbol contract symbol
     * @param buy buy or sell
     * @param amount amount
     * @return [Order] object
     */
    @Throws(PriceNotAvailableException::class)
    fun placeOrder(contractSymbol: String?, buy: Boolean, amount: Int): Order

    /**
     * Close existing order
     * @param order order to close
     * @return [ClosedOrder] object
     */
    @Throws(PriceNotAvailableException::class)
    fun closeOrder(order: Order?): ClosedOrder

    /**
     * Returns last order of th symbol
     *
     * @param symbol contract symbol
     * @return [Order] object
     * @throws NoOrderAvailableException if no orders available
     */
    @Throws(NoOrderAvailableException::class)
    fun getLastOrderBySymbol(symbol: String?): Order?

    /**
     * Returns symbol change if available
     * @param symbol symbol
     * @return symbol change since prior day close
     */
    @Throws(PriceNotAvailableException::class)
    fun getChangeBySymbol(symbol: String?): Double {
        throw UnsupportedOperationException()
    }
}
