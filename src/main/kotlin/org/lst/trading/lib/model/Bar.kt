package org.lst.trading.lib.model

import java.time.Duration
import java.time.Instant

interface Bar : Comparable<Bar> {
    val open: Double
    val high: Double
    val low: Double
    val close: Double
    val volume: Long
    val start: Instant
    val duration: Duration?
    val wAP: Double
    val average: Double
        get() = (high + low) / 2

    override fun compareTo(o: Bar): Int {
        return start.compareTo(o.start)
    }

    val end: Instant?
        get() = start.plus(duration)
}
