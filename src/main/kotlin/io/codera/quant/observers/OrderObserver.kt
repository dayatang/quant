package io.codera.quant.observers

import com.ib.client.OrderStatus
import com.ib.controller.ApiController.IOrderHandler
import org.slf4j.LoggerFactory

/**
 *
 */
interface OrderObserver : IOrderHandler {
    fun orderStatus(
        status: OrderStatus?, filled: Double, remaining: Double, avgFillPrice: Double,
        permId: Long, parentId: Int, lastFillPrice: Double, clientId: Int, whyHeld: String?
    ) {
        logger.info(
            "Order status update: OrderStatus = {}, filled {}, remaining {}, avgFillPrice =" +
                    " {}, permId = {}, parentId = {}, lastFillPrice = {}, clientId = {}, whyHeld = {}",
            status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId,
            whyHeld
        )
    }

    override fun handle(errorCode: Int, errorMsg: String) {
        logger.error("errorCode = {}, errorMsg = {}", errorCode, errorMsg)
    }

    companion object {
        val logger = LoggerFactory.getLogger(OrderObserver::class.java)
    }
}
