package io.codera.quant.observers

import com.ib.client.Decimal
import com.ib.client.OrderStatus
import com.ib.controller.ApiController.IOrderHandler
import org.slf4j.LoggerFactory

/**
 *
 */
interface OrderObserver : IOrderHandler {

    override fun orderStatus(
        status: OrderStatus, filled: Decimal, remaining: Decimal, avgFillPrice: Double, permId: Int,
        parentId: Int, lastFillPrice: Double, clientId: Int, whyHeld: String?, mktCapPrice: Double
    ) {
        logger.info(
            "Order status update: OrderStatus = {}, filled {}, remaining {}, avgFillPrice = {}, permId = {}, " +
                    " parentId = {}, lastFillPrice = {}, clientId = {}, whyHeld = {}, mktCapPrice = {}",
            status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId,
            whyHeld, mktCapPrice
        )
    }

    override fun handle(errorCode: Int, errorMsg: String) {
        logger.error("errorCode = {}, errorMsg = {}", errorCode, errorMsg)
    }

    companion object {
        val logger = LoggerFactory.getLogger(OrderObserver::class.java)
    }
}
