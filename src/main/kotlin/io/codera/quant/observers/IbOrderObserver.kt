package io.codera.quant.observers;

import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 *
 */
public class IbOrderObserver implements OrderObserver {

  private final PublishSubject<OrderState> orderSubject;

  public IbOrderObserver() {
    orderSubject = PublishSubject.create();
  }

  @Override
  public void orderState(OrderState orderState) {
    orderSubject.onNext(orderState);
  }

  @Override
  public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice,
                          int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
    OrderObserver.super.orderStatus(status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
  }

  public Observable<OrderState> observableOrderState() {
    return orderSubject.asObservable();
  }


}
