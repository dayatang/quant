package org.lst.trading.lib.series

import org.lst.trading.lib.series.TimeSeries.MergeFunction2
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class MultipleDoubleSeries : TimeSeries<List<Double?>?> {
    var mNames: MutableList<String?>

    constructor(names: Collection<String?>?) {
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
        mData = ArrayList<Entry<List<Double>>>()
        for (entry in series) {
            val list = LinkedList<Double?>()
            list.add(entry.mT)
            add(Entry<List<Double>?>(list, entry.mInstant))
        }
        mNames.add(series.mName)
    }

    fun addSeries(series: DoubleSeries) {
        mData = TimeSeries.Companion.merge<List<Double>, Double, List<Double>>(
            this,
            series,
            MergeFunction2<List<Double>, Double, List<Double>> { l: MutableList<Double?>, t: Double? ->
                l.add(t)
                l
            }).mData
        mNames.add(series.mName)
    }

    fun getColumn(name: String): DoubleSeries {
        val index = names.indexOf(name)
        val entries = mData.stream().map<Entry<Double?>>(
            Function<Entry<List<Double?>>, Entry<Double?>> { t: Entry<List<Double?>> ->
                Entry(
                    t.item[index], t.instant
                )
            }).collect(Collectors.toList())
        return DoubleSeries(entries, name)
    }

    fun indexOf(name: String?): Int {
        return mNames.indexOf(name)
    }

    val names: List<String?>
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
