package org.lst.trading.main.strategy

import org.lst.trading.lib.model.TradingStrategy

abstract class AbstractTradingStrategy(var weight: Double = 1.0) : TradingStrategy {
}
