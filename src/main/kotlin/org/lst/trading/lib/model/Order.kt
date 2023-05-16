package org.lst.trading.lib.model

import com.ib.client.OrderStatus
import io.codera.quant.config.ContractBuilder.getFutureMultiplier
import java.time.Instant

interface Order {
    val id: Int
    val amount: Double
    val openPrice: Double
    val openInstant: Instant
    val instrument: String
    val orderStatus: OrderStatus
        get() = OrderStatus.Inactive
    val isLong: Boolean
        get() = amount > 0
    val isShort: Boolean
        get() = !isLong
    val sign: Int
        get() = if (isLong) 1 else -1

    fun calculatePl(currentPrice: Double): Double {
        val base = amount * (currentPrice - openPrice)
        return if (instrument.contains("=F")) base * getFutureMultiplier(instrument) else base
    }
}
