package org.lst.trading.lib.backtest

import org.lst.trading.lib.model.ClosedOrder
import java.time.Instant

open class SimpleClosedOrder(
    var openOrder: SimpleOrder,
    override var closePrice: Double,
    override var closeInstant: Instant
) : ClosedOrder {
    override var pl: Double = calculatePl(closePrice)

    override val id: Int
        get() = openOrder.id
    override val isLong: Boolean
        get() = openOrder.isLong
    override val amount: Int
        get() = openOrder.amount
    override val openPrice: Double
        get() = openOrder.openPrice
    override val openInstant: Instant
        get() = openOrder.openInstant
    override val instrument: String
        get() = openOrder.instrument
}
