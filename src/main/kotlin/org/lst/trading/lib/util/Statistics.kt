package org.lst.trading.lib.util

import org.apache.commons.math3.stat.StatUtils

object Statistics {
    fun drawdown(series: DoubleArray?): DoubleArray {
        var max = Double.MIN_VALUE
        var ddPct = Double.MAX_VALUE
        var dd = Double.MAX_VALUE
        for (x in series!!) {
            dd = Math.min(x - max, dd)
            ddPct = Math.min(x / max - 1, ddPct)
            max = Math.max(max, x)
        }
        return doubleArrayOf(dd, ddPct)
    }

    fun sharpe(dailyReturns: DoubleArray?): Double {
        return StatUtils.mean(dailyReturns) / Math.sqrt(StatUtils.variance(dailyReturns)) * Math.sqrt(250.0)
    }

    fun returns(series: DoubleArray?): DoubleArray {
        if (series!!.size <= 1) {
            return DoubleArray(0)
        }
        val returns = DoubleArray(series.size - 1)
        for (i in 1 until series.size) {
            returns[i - 1] = series[i] / series[i - 1] - 1
        }
        return returns
    }
}
