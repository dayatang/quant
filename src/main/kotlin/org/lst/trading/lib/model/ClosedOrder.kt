package org.lst.trading.lib.model

import java.time.Instant

interface ClosedOrder : Order {
    val closePrice: Double
    val closeInstant: Instant
    val pl: Double
        get() = calculatePl(closePrice)
}
