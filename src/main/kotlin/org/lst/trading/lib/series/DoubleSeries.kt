package org.lst.trading.lib.series

import java.util.function.Function
import java.util.function.ToDoubleFunction

class DoubleSeries : TimeSeries<Double> {
    var name: String

    internal constructor(data: MutableList<Entry<Double>>, name: String) : super(data) {
        this.name = name
    }

    constructor(name: String) : super() {
        this.name = name
    }

    fun merge(other: DoubleSeries, f: (Double, Double) -> Double): DoubleSeries {
        return DoubleSeries(merge(this, other, f).mData, name)
    }

    fun mapToDouble(f: (Double) -> Double): DoubleSeries {
        return DoubleSeries(map(f).mData, name)
    }

    operator fun plus(other: DoubleSeries): DoubleSeries {
        return merge(other, Double::plus)
    }

    operator fun plus(other: Double): DoubleSeries {
        return mapToDouble { it + other }
    }

    fun mul(other: DoubleSeries): DoubleSeries {
        return merge(other, Double::times)
    }

    fun mul(factor: Double): DoubleSeries {
        return mapToDouble { x: Double? -> x!! * factor }
    }

    operator fun div(other: DoubleSeries): DoubleSeries {
        return merge(other, Double::div)
    }

    fun returns(): DoubleSeries {
        return this.div(lag(1)).plus(-1.0)
    }

    val last: Double
        get() = data.last().item

    fun tail(n: Int): DoubleSeries {
        return DoubleSeries(data.subList(size() - n, size()), name)
    }

    fun returns(days: Int): DoubleSeries {
        return this.div(lag(days)).plus(-1.0)
    }

    fun toArray(): DoubleArray {
        return stream().mapToDouble(Entry<Double>::item).toArray()
    }

    override fun toAscending(): DoubleSeries {
        return DoubleSeries(super.toAscending().mData, name)
    }

    override fun toDescending(): DoubleSeries {
        return DoubleSeries(super.toDescending().mData, name)
    }

    override fun lag(k: Int): DoubleSeries = DoubleSeries(super.lag(k).mData, name)

    override fun toString(): String {
        return if (mData.isEmpty()) "DoubleSeries{empty}" else "DoubleSeries{" +
                "mName=" + name +
                ", from=" + mData[0].instant +
                ", to=" + mData[mData.size - 1].instant +
                ", size=" + mData.size +
                '}'
    }
}
