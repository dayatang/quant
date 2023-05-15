package io.codera.quant.strategy.meanrevertion

import com.google.common.util.concurrent.AtomicDouble
import io.codera.quant.util.MathUtil
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression

/**
 *
 */
class ZScore(
    private val firstSymbolHistory: DoubleArray,
    private val secondSymbolHistory: DoubleArray,
    private val lookback: Int
) {
    private var historyArraySize = 0
    private var u: MathUtil = MathUtil()
    private var x: DoubleArray? = null
    private var y: DoubleArray? = null
    private var yPort: DoubleArray? = null
    var mLastCalculatedZScore: AtomicDouble? = null
    private var lastCalculatedHedgeRatio: AtomicDouble? = null
    private var historyIndex = 0

    init {
        require(firstSymbolHistory.size == lookback * 2 - 1) {
            "firstSymbolHistory should be of ${lookback * 2 - 1} size"
        }
        require(secondSymbolHistory.size == lookback * 2 - 1) {
            "secondHistory should be of ${lookback * 2 - 1} size"
        }
    }

    constructor(
        lookback: Int,
    ): this(
        DoubleArray(lookback * 2 - 1),
        DoubleArray(lookback * 2 - 1),
        lookback
    )

    operator fun get(firstSymbolPrice: Double, secondSymbolPrice: Double): Double {
        require(firstSymbolPrice > 0) {"firstSymbolPrice can not be <= 0"}
        require(secondSymbolPrice > 0) {"secondSymbolPrice can not be <= 0"}
        if (firstSymbolHistory[firstSymbolHistory.size - 1] == 0.0 &&
            secondSymbolHistory[secondSymbolHistory.size - 1] == 0.0
        ) {
            firstSymbolHistory[historyIndex] = firstSymbolPrice
            secondSymbolHistory[historyIndex] = secondSymbolPrice
            historyIndex++
            return 0.0
        }
        if (x == null && y == null && yPort == null) {
            x = DoubleArray(lookback)
            y = DoubleArray(lookback)
            yPort = DoubleArray(lookback)
            System.arraycopy(firstSymbolHistory, 0, x, 0, lookback - 1)
            System.arraycopy(secondSymbolHistory, 0, y, 0, lookback - 1)
            for (i in lookback - 1 until lookback * 2 - 1) {
                x!![lookback - 1] = firstSymbolHistory[i]
                y!![lookback - 1] = secondSymbolHistory[i]
                val xMatrix = MatrixUtils.createRealMatrix(lookback, 2)
                xMatrix.setColumn(0, x)
                xMatrix.setColumn(1, u.ones(lookback))
                val ols = OLSMultipleLinearRegression(0.0)
                ols.isNoIntercept = true
                ols.newSampleData(y, xMatrix.data)
                val hedgeRatio = ols.estimateRegressionParameters()[0]
                val yP = -hedgeRatio * x!![lookback - 1] + y!![lookback - 1]
                yPort!![i + 1 - yPort!!.size] = yP
                System.arraycopy(x, 1, x, 0, lookback - 1)
                System.arraycopy(y, 1, y, 0, lookback - 1)
            }
        }
        System.arraycopy(yPort, 1, yPort, 0, lookback - 1)
        x!![lookback - 1] = firstSymbolPrice
        y!![lookback - 1] = secondSymbolPrice
        val xMatrix = MatrixUtils.createRealMatrix(lookback, 2)
        xMatrix.setColumn(0, x)
        xMatrix.setColumn(1, u.ones(lookback))
        val ols = OLSMultipleLinearRegression(0.0)
        ols.isNoIntercept = true
        ols.newSampleData(y, xMatrix.data)
        val hedgeRatio = ols.estimateRegressionParameters()[0]
        if (lastCalculatedHedgeRatio == null) {
            lastCalculatedHedgeRatio = AtomicDouble()
        }
        lastCalculatedHedgeRatio!!.set(hedgeRatio)
        val yP = -hedgeRatio * firstSymbolPrice + secondSymbolPrice
        yPort!![lookback - 1] = yP
        val ds = DescriptiveStatistics(yPort)
        val movingAverage = ds.mean
        val standardDeviation = ds.standardDeviation
        System.arraycopy(x, 1, x, 0, lookback - 1)
        System.arraycopy(y, 1, y, 0, lookback - 1)
        val zScore = (yPort!![lookback - 1] - movingAverage) / standardDeviation
        if (mLastCalculatedZScore == null) {
            mLastCalculatedZScore = AtomicDouble()
        }
        mLastCalculatedZScore!!.set(zScore)
        return mLastCalculatedZScore!!.get()
    }

    val hedgeRatio: Double
        get() = lastCalculatedHedgeRatio!!.get()

    val lastCalculatedZScore: Double
        get() = mLastCalculatedZScore!!.get()
}
