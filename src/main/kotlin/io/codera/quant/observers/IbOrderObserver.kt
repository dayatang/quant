package io.codera.quant.observers

import com.ib.client.Decimal
import com.ib.client.OrderState
import com.ib.client.OrderStatus
import rx.Observable
import rx.subjects.PublishSubject

/**
 *
 */
class IbOrderObserver : OrderObserver {

    private val orderSubject: PublishSubject<OrderState> = PublishSubject.create()

    override fun orderState(orderState: OrderState) {
        orderSubject.onNext(orderState)
    }

    fun observableOrderState(): Observable<OrderState> {
        return orderSubject.asObservable()
    }
}
