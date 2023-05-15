package org.lst.trading.main.strategy.kalman

import org.la4j.Matrix

class Cointegration(var mDelta: Double, var mR: Double) {
    var mFilter: KalmanFilter
    var mNobs = 2

    init {
        val vw = Matrix.identity(mNobs).multiply(mDelta / (1 - mDelta))
        val a = Matrix.identity(mNobs)
        val x = Matrix.zero(mNobs, 1)
        mFilter = KalmanFilter(mNobs, 1)
        mFilter.updateMatrix = a
        mFilter.state = x
        mFilter.stateCovariance = Matrix.zero(mNobs, mNobs)
        mFilter.updateCovariance = vw
        mFilter.measurementCovariance = Matrix.constant(1, 1, mR)
    }

    fun step(x: Double, y: Double) {
        mFilter.extractionMatrix = Matrix.from1DArray(1, 2, doubleArrayOf(1.0, x))
        mFilter.step(Matrix.constant(1, 1, y))
    }

    val alpha: Double
        get() = mFilter.state!!.getRow(0)[0]
    val beta: Double
        get() = mFilter.state!!.getRow(1)[0]
    val variance: Double
        get() = mFilter.innovationCovariance!!.get(0, 0)
    val error: Double
        get() = mFilter.innovation!!.get(0, 0)
}
