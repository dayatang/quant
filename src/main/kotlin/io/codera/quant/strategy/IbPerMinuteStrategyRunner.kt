package io.codera.quant.strategy

import org.joda.time.DateTime
import java.util.*

/**
 * Strategy runner that executes every minute
 */
class IbPerMinuteStrategyRunner : StrategyRunner {
    override fun run(strategy: Strategy, symbols: List<String>) {
        for (symbol in symbols) {
            strategy.addSymbol(symbol)
        }
        val timer = Timer(true)
        val dt = DateTime()
        timer.schedule(
            TriggerTick(strategy),
            Date(dt.millis - dt.secondOfMinute * 1000 + 59000),
            60000
        )
    }

    override fun stop(strategy: Strategy?, symbols: List<String?>?) {}
    private inner class TriggerTick internal constructor(private val strategy: Strategy) : TimerTask() {
        override fun run() {
            strategy.onTick()
        }
    }
}
