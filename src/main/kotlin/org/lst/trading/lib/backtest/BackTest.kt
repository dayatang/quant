package org.lst.trading.lib.backtest

import io.codera.quant.strategy.Strategy
import org.lst.trading.lib.model.ClosedOrder
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import org.lst.trading.lib.util.Statistics
import org.lst.trading.lib.util.Util
import java.util.*

class BackTest(
    private var deposit: Double,
    private var priceSeries: MultipleDoubleSeries
) {
    class Result(
        var pl: Double,
        var history: DoubleSeries,
        var marginHistory: DoubleSeries,
        var orders: List<ClosedOrder>,
        var initialFund: Double,
        var finalValue: Double,
        var commissions: Double
    ) {

        val accountValueHistory: DoubleSeries
            get() = history.plus(initialFund)
        val returnRate: Double
            get() = finalValue / initialFund - 1
        val annualizedReturn: Double
            get() = returnRate * 250 / daysCount
        val sharpe: Double
            get() = Statistics.sharpe(Statistics.returns(accountValueHistory.toArray()))
        val maxDrawdown: Double
            get() = Statistics.drawdown(accountValueHistory.toArray())[0]
        val maxDrawdownPercent: Double
            get() = Statistics.drawdown(accountValueHistory.toArray())[1]
        val daysCount: Int
            get() = history.size()
    }

    var leverage = 1.0
    var mStrategy: Strategy? = null
    var mContext: BackTestTradingContext? = null
    var mPriceIterator: Iterator<TimeSeries.Entry<MutableList<Double>>>? = null
    var result: Result? = null

    init {
        Util.check(priceSeries.isAscending)
    }

    fun run(strategy: Strategy): Result {
        initialize(strategy)
        while (nextStep());
        return result!!
    }

    private fun initialize(strategy: Strategy) {
        mStrategy = strategy
        mContext = strategy.tradingContext as BackTestTradingContext
        mContext!!.contracts = priceSeries.names
        mContext!!.mHistory = MultipleDoubleSeries(mContext!!.contracts)
        mContext!!.initialFunds = deposit
        mContext!!.leverage = leverage
        mPriceIterator = priceSeries.iterator()
        nextStep()
    }

    fun nextStep(): Boolean {
        if (!mPriceIterator!!.hasNext()) {
            finish()
            return false
        }
        val entry = mPriceIterator!!.next()
        mContext!!.mPrices = entry.item
        mContext!!.time = entry.instant
        mContext!!.mPl.add(mContext!!.pl, entry.instant)
        mContext!!.mFundsHistory.add(mContext!!.availableFunds, entry.instant)
        if (mContext!!.availableFunds < 0) {
            finish()
            return false
        }
        mStrategy!!.onTick()
        mContext!!.mHistory.add(entry)
        return true
    }

    private fun finish() {
        for (order in ArrayList(mContext!!.mOrders)) {
            mContext!!.closeOrder(order)
        }

        // TODO (replace below code with BackTest results implementation
//        mStrategy.onEnd();
        val orders = Collections.unmodifiableList<ClosedOrder>(mContext!!.mClosedOrders)
        result = Result(
            mContext!!.mClosedPl,
            mContext!!.mPl,
            mContext!!.mFundsHistory,
            orders,
            deposit,
            deposit + mContext!!.mClosedPl,
            mContext!!.mCommissions
        )
    }
}
