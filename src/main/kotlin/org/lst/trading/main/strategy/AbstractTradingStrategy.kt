package org.lst.trading.main.strategy

import org.lst.trading.lib.model.TradingStrategy

abstract class AbstractTradingStrategy : TradingStrategy {
    var weight = 1.0
}
