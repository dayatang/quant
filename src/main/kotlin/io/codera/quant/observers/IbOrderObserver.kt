package io.codera.quant.observers

import com.ib.client.OrderState
import com.ib.client.OrderStatus
import rx.Observable
import rx.subjects.PublishSubject

/**
 *
 */
class IbOrderObserver : OrderObserver {
    private val orderSubject: PublishSubject<OrderState>

    init {
        orderSubject = PublishSubject.create()
    }

    override fun orderState(orderState: OrderState) {
        orderSubject.onNext(orderState)
    }

    override fun orderStatus(
        status: OrderStatus, filled: Double, remaining: Double, avgFillPrice: Double,
        permId: Int, parentId: Int, lastFillPrice: Double, clientId: Int, whyHeld: String, mktCapPrice: Double
    ) {
        super.orderStatus(
            status,
            filled,
            remaining,
            avgFillPrice,
            permId.toLong(),
            parentId,
            lastFillPrice,
            clientId,
            whyHeld
        )
    }

    fun observableOrderState(): Observable<OrderState> {
        return orderSubject.asObservable()
    }
}
