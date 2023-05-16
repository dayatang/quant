package io.codera.quant.observers

import com.ib.client.Decimal
import rx.Observable
import rx.subjects.PublishSubject

/**
 *
 */
class IbAccountObserver : AccountObserver {
    private val cashBalanceSubject: PublishSubject<Double>
    private val netValueSubject: PublishSubject<Double>

    init {
        cashBalanceSubject = PublishSubject.create()
        netValueSubject = PublishSubject.create()
    }

    override fun setCashBalance(balance: Double) {
        cashBalanceSubject.onNext(balance)
    }

    override fun setNetValue(netValue: Double) {
        AccountObserver.logger.debug("Setting net value")
        netValueSubject.onNext(netValue)
    }

    override fun updateSymbolPosition(symbol: String, position: Decimal) {
        AccountObserver.logger.info("{} position: {}", symbol, position)
    }

    fun observableCashBalance(): Observable<Double> {
        return cashBalanceSubject.asObservable()
    }

    fun observableNetValue(): Observable<Double> {
        return netValueSubject.asObservable()
    }
}
