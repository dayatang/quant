package io.codera.quant.observers

import com.ib.client.Decimal
import com.ib.client.TickType
import com.ib.controller.ApiController.ITopMktDataHandler
import rx.Observable

/**
 *
 */
interface MarketDataObserver : ITopMktDataHandler {
    val symbol: String
    fun priceObservable(): Observable<Price>
    override fun tickSize(tickType: TickType, size: Decimal) {}
    override fun tickString(tickType: TickType, value: String) {}
    override fun tickSnapshotEnd() {}

    //  @Override
    //  default void marketDataType(Types.MktDataType marketDataType) {}
    class Price internal constructor(var tickType: TickType, var price: Double)
}
