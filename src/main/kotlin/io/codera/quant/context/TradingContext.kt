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
    /**
     * Returns the time of current tick
     * @return timestamp
     */
    val time: Instant
        get() = Instant.now()

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
    val leverage: Number

    /**
     * Returns last price of the contract
     *
     * @param symbol contract name
     * @return  last price
     */
    @Throws(PriceNotAvailableException::class)
    fun getLastPrice(symbol: String): Double

    /**
     * Returns history of prices.
     *
     * @param symbol contract name
     * @return historical collection of prices
     */
    fun getHistory(symbol: String): Stream<TimeSeries.Entry<Double>> {
        throw UnsupportedOperationException()
    }

    /**
     * Returns history of prices.
     *
     * @param symbol contract name
     * @param numberOfDays seconds of history to return before current time instant
     * @return historical collection of prices
     */
    fun getHistory(symbol: String, numberOfDays: Int): DoubleSeries {
        throw UnsupportedOperationException()
    }

    /**
     * Returns history of prices.
     *
     * @param symbol contract name
     * @param numberOfMinutes seconds of history to return before current time instant
     * @return historical collection of prices
     */
    fun getHistoryInMinutes(symbol: String, numberOfMinutes: Int): DoubleSeries {
        throw UnsupportedOperationException()
    }

    /**
     * Adds contract into trading contract.
     *
     * @param symbol contract name
     */
    fun addContract(symbol: String)

    /**
     * Removes contract from context.
     *
     * @param symbol contract name
     */
    fun removeContract(symbol: String)

    /**
     * Returns contract observer
     *
     * @param symbol
     * @return
     */
    fun getObserver(symbol: String): MarketDataObserver {
        throw UnsupportedOperationException()
    }

    /**
     * Place a contract order
     *
     * @param symbol contract symbol
     * @param buy buy or sell
     * @param amount amount
     * @return [Order] object
     */
    @Throws(PriceNotAvailableException::class)
    fun placeOrder(symbol: String, buy: Boolean, amount: Double): Order

    /**
     * Close existing order
     * @param order order to close
     * @return [ClosedOrder] object
     */
    @Throws(PriceNotAvailableException::class)
    fun closeOrder(order: Order): ClosedOrder

    /**
     * Returns last order of th symbol
     *
     * @param symbol contract symbol
     * @return [Order] object
     * @throws NoOrderAvailableException if no orders available
     */
    @Throws(NoOrderAvailableException::class)
    fun getLastOrderBySymbol(symbol: String): Order

    /**
     * Returns symbol change if available
     * @param symbol symbol
     * @return symbol change since prior day close
     */
    @Throws(PriceNotAvailableException::class)
    fun getChangeBySymbol(symbol: String): Double {
        throw UnsupportedOperationException()
    }
}
