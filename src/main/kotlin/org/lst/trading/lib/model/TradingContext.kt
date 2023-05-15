package org.lst.trading.lib.model

import org.lst.trading.lib.series.TimeSeries
import java.time.Instant
import java.util.stream.Stream

interface TradingContext {
    val time: Instant?
    fun getLastPrice(instrument: String?): Double
    fun getHistory(instrument: String?): Stream<TimeSeries.Entry<Double?>?>?
    fun order(instrument: String?, buy: Boolean, amount: Int): Order?
    fun close(order: Order?): ClosedOrder?
    val pl: Double
    val instruments: List<String?>
    val availableFunds: Double
    val initialFunds: Double
    val netValue: Double
    val leverage: Double
}
