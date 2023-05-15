package org.lst.trading.lib.backtest

import com.google.common.collect.Maps
import io.codera.quant.context.TradingContext
import io.codera.quant.exception.NoOrderAvailableException
import io.codera.quant.exception.PriceNotAvailableException
import org.lst.trading.lib.model.ClosedOrder
import org.lst.trading.lib.model.Order
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.math.max

class BackTestTradingContext : TradingContext {
    override var time: Instant = Instant.now()
    var mPrices: List<Double> = emptyList()
    override var contracts: List<String> = ArrayList()
    var mPl = DoubleSeries("pl")
    var mFundsHistory = DoubleSeries("funds")
    lateinit var mHistory: MultipleDoubleSeries
    var initialFunds = 0.0
    var mCommissions = 0.0
    private var orders: MutableMap<String, Order> = Maps.newConcurrentMap()
    private val closePriceMap: MutableMap<String, Double> = Maps.newConcurrentMap()
    var mOrderId = 1
    var mOrders: MutableList<SimpleOrder> = ArrayList()
    var mClosedPl = 0.0
    var mClosedOrders: MutableList<SimpleClosedOrder> = ArrayList()
    override var leverage = 0.0
    override fun getLastPrice(instrument: String): Double {
        logger.info("Time: {}", time.toString())
        val date = Date.from(time)
        val hourMinutes = SimpleDateFormat("HH:mm")
        hourMinutes.timeZone = TimeZone.getTimeZone("UTC")
        val formattedHourMinutes = hourMinutes.format(date)
        val price = mPrices[contracts.indexOf(instrument)]
        if (formattedHourMinutes == "13:00") {
            closePriceMap[instrument] = price
        }
        return price
    }

    override fun getHistory(instrument: String): Stream<TimeSeries.Entry<Double>> {
        val index = contracts.indexOf(instrument)
        return mHistory.reversedStream()
            .map { t -> TimeSeries.Entry(t.item[index], t.instant) }
    }

    override fun addContract(contract: String) {
        throw UnsupportedOperationException()
    }

    override fun removeContract(contract: String) {
        throw UnsupportedOperationException()
    }

    override fun placeOrder(instrument: String, buy: Boolean, amount: Int): Order {
//    check(amount > 0);
        logger.info("OPEN {} in amount {}", instrument, (if (buy) 1 else -1) * amount)
        val price = getLastPrice(instrument)
        val order = SimpleOrder(mOrderId++, instrument, time, price, amount * if (buy) 1 else -1)
        mOrders.add(order)
        orders[instrument] = order
        mCommissions += calculateCommission(order)
        return order
    }

    override fun closeOrder(order: Order): ClosedOrder {
        logger.info("CLOSE {} in amount {}", order.instrument, -order.amount)
        val simpleOrder = order as SimpleOrder
        mOrders.remove(simpleOrder)
        val price = getLastPrice(order.instrument)
        val closedOrder = SimpleClosedOrder(simpleOrder, price, time)
        mClosedOrders.add(closedOrder)
        mClosedPl += closedOrder.pl
        mCommissions += calculateCommission(order)
        if (orders.isNotEmpty()) {
            orders.remove(order.instrument)
        }
        return closedOrder
    }

    @Throws(NoOrderAvailableException::class)
    override fun getLastOrderBySymbol(symbol: String): Order {
        if (!orders.containsKey(symbol)) {
            throw NoOrderAvailableException()
        }
        return orders[symbol]!!
    }

    val pl: Double
        get() = mClosedPl + mOrders.stream()
            .mapToDouble { t: SimpleOrder -> t.calculatePl(getLastPrice(t.instrument)) }.sum() - mCommissions
    override val availableFunds: Double
        get() = netValue - mOrders.stream()
            .mapToDouble { t: SimpleOrder -> Math.abs(t.amount) * t.openPrice / leverage }
            .sum()
    override val netValue: Double
        get() = initialFunds + pl

    fun calculateCommission(order: Order): Double {
        if (order.instrument.contains("/")) {
            return abs(order.amount) * order.openPrice * 0.00002
        } else if (order.instrument.contains("=F")) {
            return abs(order.amount) * 2.04
        }
        val commissions = max(1.0, abs(order.amount) * 0.005)
        logger.debug("Commissions: {}", commissions)
        return commissions
    }

    @Throws(PriceNotAvailableException::class)
    override fun getChangeBySymbol(symbol: String): Double {
        if (!closePriceMap.containsKey(symbol)) {
            throw PriceNotAvailableException()
        }
        val closePrice = closePriceMap[symbol]!!
        val currentPrice = getLastPrice(symbol)
        val diff = BigDecimal.valueOf(currentPrice).add(BigDecimal.valueOf(-closePrice))
        val res = diff.multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(closePrice), RoundingMode.HALF_UP)
        val rounded = res.setScale(2, RoundingMode.HALF_UP)
        return rounded.toDouble()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BackTestTradingContext::class.java)
    }
}
