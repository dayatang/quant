package org.lst.trading.lib.backtest

import org.lst.trading.lib.model.ClosedOrder
import java.time.Instant

open class SimpleClosedOrder(
    var mOrder: SimpleOrder?,
    override var closePrice: Double,
    override var closeInstant: Instant
) : ClosedOrder {
    override var pl: Double

    init {
        pl = calculatePl(closePrice)
    }

    override val id: Int
        get() = mOrder.getId()
    override val isLong: Boolean
        get() = mOrder!!.isLong
    override val amount: Int
        get() = mOrder.getAmount()
    override val openPrice: Double
        get() = mOrder.getOpenPrice()
    override val openInstant: Instant?
        get() = mOrder.getOpenInstant()
    override val instrument: String?
        get() = mOrder.getInstrument()
}
