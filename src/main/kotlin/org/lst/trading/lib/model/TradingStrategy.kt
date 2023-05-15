package org.lst.trading.lib.model

interface TradingStrategy {
    fun onStart(context: TradingContext?) {}
    fun onTick() {}
    fun onEnd() {}
}
