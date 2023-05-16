package org.lst.trading.lib.series

import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class MultipleDoubleSeries : TimeSeries<MutableList<Double>> {

    private var mNames: MutableList<String> = ArrayList()

    val names: List<String>
        get() = mNames

    constructor(names: Collection<String>) {
        mNames = ArrayList(names)
    }

    constructor(series: List<DoubleSeries>) {
        for (i in series.indices) {
            if (i == 0) {
                init(series[i])
            } else {
                addSeries(series[i])
            }
        }
    }

    constructor(vararg series: DoubleSeries): this(listOf(*series))

    private fun init(series: DoubleSeries) {
        for (entry in series) {
            val list = LinkedList<Double>()
            list.add(entry.item)
            add(Entry(list, entry.instant))
        }
        mNames.add(series.name)
    }

    private fun addSeries(series: DoubleSeries) {
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

    override fun toString(): String {
        return if (mData.isEmpty()) "MultipleDoubleSeries{empty}" else "MultipleDoubleSeries{" +
                "mNames={" + mNames.stream().collect(Collectors.joining(", ")) +
                ", from=" + mData[0].instant +
                ", to=" + mData[mData.size - 1].instant +
                ", size=" + mData.size +
                '}'
    }
}
