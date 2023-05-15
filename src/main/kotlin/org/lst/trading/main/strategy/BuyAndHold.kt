package org.lst.trading.main.strategy

import org.lst.trading.lib.model.Order
import org.lst.trading.lib.model.TradingContext
import org.lst.trading.lib.model.TradingStrategy

class BuyAndHold : TradingStrategy {
    var mOrders: MutableMap<String?, Order?>? = null
    var mContext: TradingContext? = null
    override fun onStart(context: TradingContext?) {
        mContext = context
    }

    override fun onTick() {
        if (mOrders == null) {
            mOrders = HashMap()
            mContext.getInstruments().stream()
                .forEach { instrument: String? -> mOrders[instrument] = mContext!!.order(instrument, true, 1) }
        }
    }
}
