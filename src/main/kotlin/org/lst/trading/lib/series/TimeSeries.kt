package org.lst.trading.lib.series

import org.lst.trading.lib.util.Util
import java.time.Instant
import java.util.*
import java.util.function.Function
import java.util.stream.IntStream
import java.util.stream.Stream

open class TimeSeries<T> : Iterable<TimeSeries.Entry<T>?> {
    class Entry<T>(t: T, instant: Instant?) {
        var item: T?
        var instant: Instant?

        init {
            item = t
            this.instant = instant
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val entry = o as Entry<*>
            if (instant != entry.instant) return false
            return if (if (item != null) item != entry.item else entry.item != null) false else true
        }

        override fun hashCode(): Int {
            var result = if (item != null) item.hashCode() else 0
            result = 31 * result + instant.hashCode()
            return result
        }

        override fun toString(): String {
            return "Entry{" +
                    "mInstant=" + instant +
                    ", mT=" + item +
                    '}'
        }
    }

    var mData: MutableList<Entry<T?>>

    constructor() {
        mData = ArrayList()
    }

    protected constructor(data: MutableList<Entry<T?>>) {
        mData = data
    }

    fun size(): Int {
        return mData.size
    }

    val isEmpty: Boolean
        get() = mData.isEmpty()

    fun add(tEntry: Entry<T?>): Boolean {
        return mData.add(tEntry)
    }

    fun add(item: T, instant: Instant?) {
        add(Entry(item, instant))
    }

    fun stream(): Stream<Entry<T?>> {
        return mData.stream()
    }

    fun reversedStream(): Stream<Entry<T?>> {
        Util.check(mData !is LinkedList<*>)
        return IntStream.range(1, mData.size + 1).mapToObj { i: Int -> mData[mData.size - i] }
    }

    override fun iterator(): Iterator<Entry<T>?> {
        return mData.iterator()
    }

    val data: List<Entry<T?>>
        get() = Collections.unmodifiableList(mData)

    operator fun get(index: Int): Entry<T?> {
        return mData[index]
    }

    interface MergeFunction<T, F> {
        fun merge(t1: T, t2: T): F
    }

    interface MergeFunction2<T1, T2, F> {
        fun merge(t1: T1, t2: T2): F
    }

    fun <F> map(f: Function<T?, F>): TimeSeries<F> {
        val newEntries: MutableList<Entry<F>> = ArrayList(size())
        for (entry in mData) {
            newEntries.add(Entry(f.apply(entry.item), entry.instant))
        }
        return TimeSeries(newEntries)
    }

    val isAscending: Boolean
        get() = size() <= 1 || get(0).instant!!.isBefore(get(1).instant)

    open fun toAscending(): TimeSeries<T?> {
        return if (!isAscending) {
            reverse()
        } else this
    }

    open fun toDescending(): TimeSeries<T?> {
        return if (isAscending) {
            reverse()
        } else this
    }

    fun reverse(): TimeSeries<T?> {
        val entries = ArrayList(mData)
        Collections.reverse(entries)
        return TimeSeries(entries)
    }

    open fun lag(k: Int): TimeSeries<T?> {
        return lag(k, false, null)
    }

    fun lag(k: Int, addEmpty: Boolean, emptyVal: T?): TimeSeries<T?> {
        Util.check(k > 0)
        Util.check(mData.size >= k)
        val entries = ArrayList<Entry<T?>>(if (addEmpty) mData.size else mData.size - k)
        if (addEmpty) {
            for (i in 0 until k) {
                entries.add(Entry(emptyVal, mData[i].instant))
            }
        }
        for (i in k until size()) {
            entries.add(Entry(mData[i - k].item, mData[i].instant))
        }
        return TimeSeries(entries)
    }

    override fun toString(): String {
        return if (mData.isEmpty()) "TimeSeries{empty}" else "TimeSeries{" +
                "from=" + mData[0].instant +
                ", to=" + mData[size() - 1].instant +
                ", size=" + mData.size +
                '}'
    }

    companion object {
        fun <T1, T2, F> merge(t1: TimeSeries<T1>, t2: TimeSeries<T2>, f: MergeFunction2<T1, T2, F>): TimeSeries<F> {
            Util.check(t1.isAscending)
            Util.check(t2.isAscending)
            val i1: Iterator<Entry<T1>> = t1.iterator()
            val i2: Iterator<Entry<T2>> = t2.iterator()
            val newEntries: MutableList<Entry<F>> = ArrayList()
            while (i1.hasNext() && i2.hasNext()) {
                var n1 = i1.next()
                var n2 = i2.next()
                while (n2.instant != n1.instant) {
                    if (n1.instant!!.isBefore(n2.instant)) {
                        if (!i1.hasNext()) {
                            break
                        }
                        while (i1.hasNext()) {
                            n1 = i1.next()
                            if (!n1.instant!!.isBefore(n2.instant)) {
                                break
                            }
                        }
                    } else if (n2.instant!!.isBefore(n1.instant)) {
                        while (i2.hasNext()) {
                            n2 = i2.next()
                            if (!n2.instant!!.isBefore(n1.instant)) {
                                break
                            }
                        }
                    }
                }
                if (n2.instant == n1.instant) {
                    newEntries.add(Entry(f.merge(n1.item, n2.item), n1.instant))
                }
            }
            return TimeSeries(newEntries)
        }

        fun <T, F> merge(t1: TimeSeries<T>, t2: TimeSeries<T>, f: MergeFunction<T, F>): TimeSeries<F> {
            return merge<T, T, F>(t1, t2, MergeFunction2<T, T, F> { t1: T1, t2: T2 -> f.merge(t1, t2) })
        }
    }
}
