package org.lst.trading.lib.backtest

import org.lst.trading.lib.model.Order
import java.time.Instant

open class SimpleOrder(
    override var id: Int,
    override var instrument: String,
    override var openInstant: Instant,
    override var openPrice: Double,
    override var amount: Double
) : Order
