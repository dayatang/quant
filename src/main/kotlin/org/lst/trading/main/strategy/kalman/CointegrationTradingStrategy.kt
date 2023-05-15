package org.lst.trading.main.strategy.kalman

import org.apache.commons.math3.stat.StatUtils
import org.lst.trading.lib.model.Order
import org.lst.trading.lib.model.TradingContext
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import org.lst.trading.lib.util.Util
import org.lst.trading.main.strategy.AbstractTradingStrategy
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class CointegrationTradingStrategy(
    weight: Double,
    val mX: String,
    val mY: String
) : AbstractTradingStrategy(weight) {
    var mReinvest = false
    var mContext: TradingContext? = null
    var mCoint: Cointegration? = null
    var mAlpha: DoubleSeries? = null
    var mBeta: DoubleSeries? = null
    var xs: DoubleSeries? = null
    var ys: DoubleSeries? = null
    var mError: DoubleSeries? = null
    var variance: DoubleSeries? = null
    var model: DoubleSeries? = null
    var mXOrder: Order? = null
    var mYOrder: Order? = null

    constructor(x: String, y: String) : this(1.0, x, y)

    override fun onStart(context: TradingContext) {
        mContext = context
        mCoint = Cointegration(1e-4, 1e-3)
        mAlpha = DoubleSeries("alpha")
        mBeta = DoubleSeries("beta")
        xs = DoubleSeries("x")
        ys = DoubleSeries("y")
        mError = DoubleSeries("error")
        variance = DoubleSeries("variance")
        model = DoubleSeries("model")
    }

    override fun onTick() {
        val x = mContext!!.getLastPrice(mX)
        val y = mContext!!.getLastPrice(mY)
        val alpha = mCoint!!.alpha
        val beta = mCoint!!.beta
        mCoint!!.step(x, y)
        this.mAlpha!!.add(alpha, mContext!!.time)
        this.mBeta!!.add(beta, mContext!!.time)
        xs!!.add(x, mContext!!.time)
        ys!!.add(y, mContext!!.time)
        mError!!.add(mCoint!!.error, mContext!!.time)
        variance!!.add(mCoint!!.variance, mContext!!.time)
        val error = mCoint!!.error
        model!!.add(beta * x + alpha, mContext!!.time)
        if (mError!!.size() > 30) {
            val lastValues: DoubleArray =
                mError!!.reversedStream()
                    .mapToDouble(TimeSeries.Entry<Double>::item).limit(15)
                    .toArray()
            val sd = sqrt(StatUtils.variance(lastValues))
            if (mYOrder == null && abs(error) > sd) {
                val value = if (mReinvest) mContext!!.netValue else mContext!!.initialFunds
                val baseAmount = value * weight * 0.5 * min(4.0, mContext!!.leverage) / (y + beta * x)
                if (beta > 0 && baseAmount * beta >= 1) {
                    mYOrder = mContext!!.order(mY, error < 0, baseAmount.toInt())
                    mXOrder = mContext!!.order(mX, error > 0, (baseAmount * beta).toInt())
                }
                //log.debug("Order: baseAmount={}, residual={}, sd={}, beta={}", baseAmount, residual, sd, beta);
            } else if (mYOrder != null) {
                if (mYOrder!!.isLong && error > 0 || !mYOrder!!.isLong && error < 0) {
                    mContext!!.close(mYOrder!!)
                    mContext!!.close(mXOrder!!)
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
                    xs!!, ys!!, mAlpha!!, mBeta!!, mError!!, variance!!, model!!
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
