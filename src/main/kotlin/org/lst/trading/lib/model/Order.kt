package org.lst.trading.lib.model

import com.ib.client.OrderStatus
import io.codera.quant.config.ContractBuilder.Companion.getFutureMultiplier
import java.time.Instant

interface Order {
    val id: Int
    val amount: Int
    val openPrice: Double
    val openInstant: Instant?
    val instrument: String?
    val orderStatus: OrderStatus?
        get() = OrderStatus.Inactive
    val isLong: Boolean
        get() = amount > 0
    val isShort: Boolean
        get() = !isLong
    val sign: Int
        get() = if (isLong) 1 else -1

    fun calculatePl(currentPrice: Double): Double {
        return if (instrument!!.contains("=F")) {
            amount * (currentPrice - openPrice) *
                    getFutureMultiplier(instrument)!!
        } else amount * (currentPrice - openPrice)
    }
}
