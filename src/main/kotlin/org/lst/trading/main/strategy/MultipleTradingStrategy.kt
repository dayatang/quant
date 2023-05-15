package org.lst.trading.main.strategy

import org.lst.trading.lib.model.TradingContext
import org.lst.trading.lib.model.TradingStrategy
import java.util.function.Consumer

class MultipleTradingStrategy : TradingStrategy {
    var mStrategies: MutableList<TradingStrategy> = ArrayList()
    fun add(strategy: TradingStrategy): Boolean {
        return mStrategies.add(strategy)
    }

    fun size(): Int {
        return mStrategies.size
    }

    override fun onStart(context: TradingContext) {
        mStrategies.forEach(Consumer { t: TradingStrategy -> t.onStart(context) })
    }

    override fun onTick() {
        mStrategies.forEach(Consumer { obj: TradingStrategy -> obj.onTick() })
    }

    override fun onEnd() {
        mStrategies.forEach(Consumer { obj: TradingStrategy -> obj.onEnd() })
    }

    override fun toString(): String {
        return "MultipleStrategy{" +
                "mStrategies=" + mStrategies +
                '}'
    }

    companion object {
        fun of(vararg strategies: TradingStrategy): MultipleTradingStrategy {
            val strategy = MultipleTradingStrategy()
            for (s in strategies) {
                strategy.add(s)
            }
            return strategy
        }
    }
}
