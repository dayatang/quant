package io.codera.quant.observers

import com.ib.controller.ApiController.IAccountHandler
import com.ib.controller.Position
import org.slf4j.LoggerFactory

/**
 *
 */
interface AccountObserver : IAccountHandler {
    override fun accountValue(account: String, key: String, value: String, currency: String) {
        val format = String.format(
            "account: %s, key: %s, value: %s, currency: %s",
            account, key, value, currency
        )
        if (key == "NetLiquidation" && currency == "USD") {
            logger.debug(format)
            setNetValue(java.lang.Double.valueOf(value))
        }
        if (key == "AvailableFunds" && currency == "USD") {
            logger.debug(format)
            setCashBalance(java.lang.Double.valueOf(value))
        }
    }

    override fun accountTime(timeStamp: String) {
        logger.debug(String.format("account time: %s", timeStamp))
    }

    override fun accountDownloadEnd(account: String) {
        logger.debug(String.format("account download end: %s", account))
    }

    override fun updatePortfolio(position: Position) {
        updateSymbolPosition(position.contract().symbol(), position.position())
    }

    fun setCashBalance(balance: Double)
    fun setNetValue(netValue: Double)
    fun updateSymbolPosition(symbol: String?, position: Double)

    companion object {
        val logger = LoggerFactory.getLogger(AccountObserver::class.java)
    }
}
