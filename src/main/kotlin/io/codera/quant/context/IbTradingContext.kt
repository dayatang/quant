package io.codera.quant.context

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.ib.client.*
import com.ib.controller.ApiController
import io.codera.quant.config.ContractBuilder
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import io.codera.quant.observers.*
import io.codera.quant.observers.MarketDataObserver.Price
import org.lst.trading.lib.backtest.SimpleClosedOrder
import org.lst.trading.lib.backtest.SimpleOrder
import org.lst.trading.lib.model.ClosedOrder
import org.lst.trading.lib.model.Order
import org.lst.trading.lib.series.DoubleSeries
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Subscriber
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Interactive Brokers trading context.
 */
class IbTradingContext(
    private val controller: ApiController,
    private var orderType: OrderType,
    override val leverage: Number,
) : TradingContext {
    override var contracts: MutableList<String> = ArrayList()
    private var ibContracts: MutableMap<String, Contract> = HashMap()
    private var ibOrders: MutableMap<String, Order> = HashMap()
    private var contractPrices: MutableMap<String, MutableMap<TickType, Double>> = HashMap()
    private var observers: MutableMap<String, MarketDataObserver> = HashMap()
    private val orderId = AtomicInteger(0)
    override var availableFunds = 0.0
        private set
    override var netValue = 0.0
        private set
    private var dbConnection: Connection? = null

    init {
        contracts = Lists.newArrayList()
        contractPrices = Maps.newConcurrentMap()
        observers = Maps.newConcurrentMap()
        ibContracts = Maps.newConcurrentMap()
        ibOrders = Maps.newConcurrentMap()
        val accountObserver = IbAccountObserver()
        accountObserver
            .observableCashBalance().subscribe { aDouble: Double -> availableFunds = aDouble }
        accountObserver
            .observableNetValue().subscribe { aDouble: Double -> netValue = aDouble }
        controller.reqAccountUpdates(true, "", accountObserver)
    }

    constructor(
        controller: ApiController,
        orderType: OrderType,
        connection: Connection?,
        leverage: Number
    ) : this(controller, orderType, leverage) {
        this.dbConnection = connection
    }

    @Throws(PriceNotAvailableException::class)
    override fun getLastPrice(symbol: String): Double {
        val price = contractPrices[symbol]?.get(TickType.ASK)?: throw PriceNotAvailableException()
        if (dbConnection != null) {
            try {
                val sql = "INSERT INTO quotes (symbol, price) VALUES (?, ?)"
                val stmt = dbConnection!!.prepareStatement(sql)
                stmt.setString(1, symbol)
                stmt.setDouble(2, price)
                stmt.execute()
            } catch (e: SQLException) {
                log.error("Could not insert record into database: $symbol - $price", e)
            }
        }
        return price
    }

    @Throws(PriceNotAvailableException::class)
    fun getLastPrice(contract: String, tickType: TickType): Double =
        contractPrices[contract]?.get(tickType)?: throw PriceNotAvailableException()

    override fun addContract(symbol: String) {
        contracts.add(symbol)
        val contract = ContractBuilder.build(symbol)
        ibContracts[symbol] = contract
        val marketDataObserver = IbMarketDataObserver(symbol)
        observers[symbol] = marketDataObserver
        controller.reqTopMktData(contract, "",
            false, false, marketDataObserver)
        marketDataObserver.priceObservable().subscribe(object : Subscriber<Price>() {
            override fun onCompleted() {}
            override fun onError(throwable: Throwable) {}
            override fun onNext(price: Price) {
                if (contractPrices.containsKey(symbol)) {
                    contractPrices[symbol]!![price.tickType] = price.price
                } else {
                    val map: MutableMap<TickType, Double> = Maps.newConcurrentMap()
                    map[price.tickType] = price.price
                    contractPrices[symbol] = map
                }
            }
        })
    }

    override fun removeContract(symbol: String) {
        contracts.remove(symbol)
        contractPrices.remove(symbol)
        ibContracts.remove(symbol)
        controller.cancelTopMktData(observers[symbol])
    }

    override fun getObserver(symbol: String): MarketDataObserver {
        return observers[symbol]!!
    }

    @Throws(PriceNotAvailableException::class)
    override fun placeOrder(symbol: String, buy: Boolean, amount: Double): Order {
        val order = IbOrder(
            orderId.getAndIncrement(),
            symbol,
            Instant.now(),
            getLastPrice(symbol),
            if (buy) amount else -amount,
            submitIbOrder(symbol, buy, amount, getLastPrice(symbol))
        )
        ibOrders[symbol] = order
        return order
    }

    @Throws(PriceNotAvailableException::class)
    override fun closeOrder(order: Order): ClosedOrder {
        log.debug(
            "Amount taken from {} order that isLong {} : {}", order!!.instrument,
            order.isLong,
            order.amount
        )
        val closedOrder = IbClosedOrder(
            order as SimpleOrder,
            Instant.now(),
            getLastPrice(order.instrument),
            submitIbOrder(
                order.instrument,
                order.isShort,
                order.amount,
                getLastPrice(order.instrument)
            )
        )
        ibOrders.remove(order.instrument)
        ibOrders[order.instrument] = closedOrder
        return closedOrder
    }

    fun setOrderType(orderType: OrderType) {
        this.orderType = orderType
    }

    @Throws(NoOrderAvailableException::class)
    override fun getLastOrderBySymbol(symbol: String): Order {
        if (!ibOrders.containsKey(symbol)) {
            throw NoOrderAvailableException()
        }
        return ibOrders[symbol]!!
    }

    override fun getHistory(symbol: String, daysOfHistory: Int): DoubleSeries {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        val date = LocalDateTime.now().format(formatter)
        val contract = ContractBuilder.build(symbol)
        val historyObserver = IbHistoryObserver(symbol)
        controller.reqHistoricalData(
            contract, date, daysOfHistory, Types.DurationUnit.DAY,
            Types.BarSize._1_min, Types.WhatToShow.TRADES, false, false, historyObserver
        )
        return historyObserver.observableDoubleSeries()
            .toBlocking()
            .first()
    }

    override fun getHistoryInMinutes(symbol: String, numberOfMinutes: Int): DoubleSeries {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        val date = LocalDateTime.now().format(formatter)
        val contract = ContractBuilder.build(symbol)
        val historyObserver: HistoryObserver = IbHistoryObserver(symbol)
        controller.reqHistoricalData(
            contract, date, numberOfMinutes * 60, Types.DurationUnit.SECOND,
            Types.BarSize._1_min, Types.WhatToShow.TRADES, false, false, historyObserver
        )
        var history = (historyObserver as IbHistoryObserver).observableDoubleSeries()
            .toBlocking()
            .first()
        // We might need to pull history for last day if time of request is after market is closed
        if (history!!.size() == 0 || history.size() < numberOfMinutes) {
            controller.reqHistoricalData(
                contract, date, 1, Types.DurationUnit.DAY,
                Types.BarSize._1_min, Types.WhatToShow.TRADES, false, false, historyObserver
            )
            history = historyObserver.observableDoubleSeries()
                .toBlocking()
                .first()
            return history!!.tail(numberOfMinutes)
        }
        return history
    }

    private fun submitIbOrder(
        contractSymbol: String,
        buy: Boolean,
        amount: Double,
        price: Double
    ): Observable<OrderState> {
        var amount = amount
        val ibOrder = Order()
        if (buy) {
            ibOrder.action(Types.Action.BUY)
        } else {
            ibOrder.action(Types.Action.SELL)
            amount = -amount
        }
        ibOrder.orderType(orderType)
        if (orderType == OrderType.LMT) {
            ibOrder.lmtPrice(price)
        }
        ibOrder.totalQuantity(Decimal.get(abs(amount)))
        val orderObserver = IbOrderObserver()
        log.debug("Sending order for {} in amount of {}", contractSymbol, amount)
        controller.placeOrModifyOrder(ibContracts[contractSymbol], ibOrder, orderObserver)
        return orderObserver.observableOrderState()
    }

    inner class IbOrder(
        id: Int,
        contractSymbol: String,
        openInstant: Instant,
        openPrice: Double,
        amount: Double,
        observableOrderState: Observable<OrderState>
    ) : SimpleOrder(id, contractSymbol, openInstant, openPrice, amount) {

        override var orderStatus: OrderStatus = OrderStatus.Inactive

        init {
            observableOrderState.subscribe { newOrderState: OrderState -> orderStatus = newOrderState.status() }
            log.info("{} OPEN order in amount of {} at price {}", contractSymbol, amount, openPrice)
        }
    }

    inner class IbClosedOrder(
        openOrder: SimpleOrder,
        closeInstant: Instant,
        closePrice: Double,
        observableOrderState: Observable<OrderState>
    ) : SimpleClosedOrder(openOrder, closePrice, closeInstant) {

        override var orderStatus: OrderStatus = OrderStatus.Inactive

        init {
            observableOrderState.subscribe { newOrderState: OrderState ->
                orderStatus = newOrderState.status()
                if (newOrderState.status() == OrderStatus.Filled) {
                    ibOrders.remove(openOrder.instrument)
                }
            }
            log.info(
                "{} CLOSE order in amount of {} at price {}",
                openOrder.instrument, -openOrder.amount, closePrice
            )
        }
    }

    @Throws(PriceNotAvailableException::class)
    override fun getChangeBySymbol(symbol: String): Double {
        val closePrice = getLastPrice(symbol, TickType.CLOSE)
        val currentPrice = getLastPrice(symbol)
        val diff = BigDecimal.valueOf(currentPrice).add(BigDecimal.valueOf(-closePrice))
        val res = diff.multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(closePrice), RoundingMode.HALF_UP)
        val rounded = res.setScale(2, RoundingMode.HALF_UP)
        return rounded.toDouble()
    }

    @Throws(PriceNotAvailableException::class)
    fun getChangeBySymbol(symbol: String, price: Double): Double {
        val closePrice = getLastPrice(symbol, TickType.CLOSE)
        val diff = BigDecimal.valueOf(price).add(BigDecimal.valueOf(-closePrice))
        val res = diff.multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(closePrice), RoundingMode.HALF_UP)
        return res.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    companion object {
        private val log = LoggerFactory.getLogger(IbTradingContext::class.java)
    }
}
