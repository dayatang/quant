package org.lst.trading.main.strategy

import org.lst.trading.lib.model.Order
import org.lst.trading.lib.model.TradingContext
import org.lst.trading.lib.model.TradingStrategy

class BuyAndHold() : TradingStrategy {
    var mOrders: MutableMap<String, Order> = HashMap()
    lateinit var mContext: TradingContext
    override fun onStart(context: TradingContext) {
        mContext = context
    }

    override fun onTick() {
        if (mOrders.isEmpty()) {
            mContext.instruments.stream()
                .forEach { mOrders[it] = mContext.order(it, true, 1) }
        }
    }
}
