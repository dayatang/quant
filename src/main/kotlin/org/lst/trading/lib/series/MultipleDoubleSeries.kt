package org.lst.trading.lib.series

import org.lst.trading.lib.series.TimeSeries.MergeFunction2
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class MultipleDoubleSeries : TimeSeries<MutableList<Double>> {
    var mNames: MutableList<String>

    constructor(names: Collection<String>) {
        mNames = ArrayList(names)
    }

    constructor(series: List<DoubleSeries>) {
        mNames = ArrayList()
        for (i in series.indices) {
            if (i == 0) {
                _init(series[i])
            } else {
                addSeries(series[i])
            }
        }
    }

    constructor(vararg series: DoubleSeries) {
        mNames = ArrayList()
        for (i in series.indices) {
            if (i == 0) {
                _init(series[i])
            } else {
                addSeries(series[i])
            }
        }
    }

    fun _init(series: DoubleSeries) {
        mData = ArrayList<Entry<MutableList<Double>>>()
        for (entry in series) {
            val list = LinkedList<Double>()
            list.add(entry.item)
            add(Entry(list, entry.instant))
        }
        mNames.add(series.name)
    }

    fun addSeries(series: DoubleSeries) {
        mData = merge(this, series) { l: MutableList<Double>, t: Double ->
            l.add(t)
            l
        }.mData
        mNames.add(series.name)
    }

    fun getColumn(name: String): DoubleSeries {
        val index = names.indexOf(name)
        val entries = mData.stream()
            .map { Entry(it.item[index], it.instant) }
            .toList()
        return DoubleSeries(entries, name)
    }

    fun indexOf(name: String): Int {
        return mNames.indexOf(name)
    }

    val names: List<String>
        get() = mNames

    override fun toString(): String {
        return if (mData.isEmpty()) "MultipleDoubleSeries{empty}" else "MultipleDoubleSeries{" +
                "mNames={" + mNames.stream().collect(Collectors.joining(", ")) +
                ", from=" + mData[0].instant +
                ", to=" + mData[mData.size - 1].instant +
                ", size=" + mData.size +
                '}'
    }
}
