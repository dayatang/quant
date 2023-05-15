package org.lst.trading.lib.util

import org.lst.trading.lib.series.DoubleSeries
import rx.Observable

interface HistoricalPriceService {
    fun getHistoricalAdjustedPrices(symbol: String): Observable<DoubleSeries?>
}
