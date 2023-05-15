package io.codera.quant.observers

import com.ib.client.Decimal
import com.ib.client.TickAttrib
import com.ib.client.TickType
import com.ib.controller.ApiController.ITopMktDataHandler
import io.codera.quant.config.ContractBuilder
import io.codera.quant.observers.MarketDataObserver.Price
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject

/**
 * Wraps IB [ITopMktDataHandler] into observer to simplify
 * access to data (price) feed
 */
class IbMarketDataObserver(override val symbol: String) : MarketDataObserver {

    private val priceSubject: PublishSubject<Price> = PublishSubject.create()

    override fun tickPrice(tickType: TickType, price: Double, attribs: TickAttrib) {
        if (price == -1.0) { // do not update price with bogus value when market is about ot be closed
            return
        }
        val realPrice: Double = ContractBuilder.Companion.getSymbolPrice(symbol, price)
        priceSubject.onNext(Price(tickType, realPrice))
    }

    override fun priceObservable(): Observable<Price> {
        return priceSubject.asObservable()
    }

    override fun tickSize(tickType: TickType, size: Decimal) {
        TODO("Not yet implemented")
    }

    override fun marketDataType(marketDataType: Int) {}
    override fun tickReqParams(tickerId: Int, minTick: Double, bboExchange: String, snapshotPermissions: Int) {}

    companion object {
        private val log = LoggerFactory.getLogger(IbMarketDataObserver::class.java)
    }
}
