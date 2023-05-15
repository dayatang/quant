package org.lst.trading.main.strategy.kalman

import org.apache.commons.math3.stat.StatUtils
import org.lst.trading.lib.model.Order
import org.lst.trading.lib.model.TradingContext
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import org.lst.trading.lib.util.Util
import org.lst.trading.main.strategy.AbstractTradingStrategy
import org.lst.trading.main.strategy.kalman.CointegrationTradingStrategy
import org.slf4j.LoggerFactory
import java.util.function.ToDoubleFunction

class CointegrationTradingStrategy(weight: Double, x: String, y: String) : AbstractTradingStrategy() {
    var mReinvest = false
    var mX: String
    var mY: String
    var mContext: TradingContext? = null
    var mCoint: Cointegration? = null
    var alpha: DoubleSeries? = null
    var beta: DoubleSeries? = null
    var xs: DoubleSeries? = null
    var ys: DoubleSeries? = null
    var error: DoubleSeries? = null
    var variance: DoubleSeries? = null
    var model: DoubleSeries? = null
    var mXOrder: Order? = null
    var mYOrder: Order? = null

    constructor(x: String, y: String) : this(1.0, x, y)

    init {
        setWeight(weight)
        mX = x
        mY = y
    }

    override fun onStart(context: TradingContext?) {
        mContext = context
        mCoint = Cointegration(1e-4, 1e-3)
        alpha = DoubleSeries("alpha")
        beta = DoubleSeries("beta")
        xs = DoubleSeries("x")
        ys = DoubleSeries("y")
        error = DoubleSeries("error")
        variance = DoubleSeries("variance")
        model = DoubleSeries("model")
    }

    override fun onTick() {
        val x = mContext!!.getLastPrice(mX)
        val y = mContext!!.getLastPrice(mY)
        val alpha = mCoint.getAlpha()
        val beta = mCoint.getBeta()
        mCoint!!.step(x, y)
        alpha.add(alpha, mContext.getTime())
        beta.add(beta, mContext.getTime())
        xs!!.add(x, mContext.getTime())
        ys!!.add(y, mContext.getTime())
        error!!.add(mCoint.getError(), mContext.getTime())
        variance!!.add(mCoint.getVariance(), mContext.getTime())
        val error = mCoint.getError()
        model!!.add(beta * x + alpha, mContext.getTime())
        if (error.size() > 30) {
            val lastValues: DoubleArray =
                error.reversedStream().mapToDouble(ToDoubleFunction<TimeSeries.Entry<Double?>?> { getItem() }).limit(15)
                    .toArray()
            val sd = Math.sqrt(StatUtils.variance(lastValues))
            if (mYOrder == null && Math.abs(error) > sd) {
                val value = if (mReinvest) mContext.getNetValue() else mContext.getInitialFunds()
                val baseAmount = value * weight * 0.5 * Math.min(4.0, mContext.getLeverage()) / (y + beta * x)
                if (beta > 0 && baseAmount * beta >= 1) {
                    mYOrder = mContext!!.order(mY, error < 0, baseAmount.toInt())
                    mXOrder = mContext!!.order(mX, error > 0, (baseAmount * beta).toInt())
                }
                //log.debug("Order: baseAmount={}, residual={}, sd={}, beta={}", baseAmount, residual, sd, beta);
            } else if (mYOrder != null) {
                if (mYOrder!!.isLong && error > 0 || !mYOrder!!.isLong && error < 0) {
                    mContext!!.close(mYOrder)
                    mContext!!.close(mXOrder)
                    mYOrder = null
                    mXOrder = null
                }
            }
        }
    }

    override fun onEnd() {
        log.debug(
            "Kalman filter statistics: " + Util.writeCsv(
                MultipleDoubleSeries(
                    xs!!, ys!!, alpha!!, beta!!, error!!, variance!!, model!!
                )
            )
        )
    }

    override fun toString(): String {
        return "CointegrationStrategy{" +
                "mY='" + mY + '\'' +
                ", mX='" + mX + '\'' +
                '}'
    }

    companion object {
        private val log = LoggerFactory.getLogger(CointegrationTradingStrategy::class.java)
    }
}
