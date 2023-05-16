package io.codera.quant.strategy

/**
 * Runs strategy in defined trading context.
 */
interface StrategyRunner {
    /**
     * Run specified [Strategy] for symbols collection in given [TradingContext].
     *
     * @param strategy strategy to run
     * @param symbols list
     */
    fun run(strategy: Strategy, symbols: List<String>)

    /**
     * Stop the specified strategy for specified symbols.
     *
     * @param strategy strategy to stop
     * @param symbols symbols list
     */
    fun stop(strategy: Strategy, symbols: List<String>)
}
