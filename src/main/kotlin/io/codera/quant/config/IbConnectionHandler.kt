package io.codera.quant.config

import com.google.common.collect.Lists
import com.ib.controller.ApiController.IConnectionHandler
import org.slf4j.LoggerFactory

/**
 *
 */
class IbConnectionHandler : IConnectionHandler {
    private val accountList = Lists.newArrayList<String>()
    override fun connected() {
        logger.info("Connected")
    }

    override fun disconnected() {
        logger.info("Disconnected")
    }

    override fun accountList(list: List<String>) {
        show("Received account list")
        accountList.clear()
        accountList.addAll(list)
    }

    override fun error(e: Exception) {
        logger.error(e.message)
        e.printStackTrace()
    }

    override fun message(id: Int, errorCode: Int, errorMsg: String) {
        logger.info("Message id: {}, errorCode: {}, errorMsg: {}", id, errorCode, errorMsg)
    }

    override fun show(string: String) {
        logger.info(string)
    }

    fun getAccountList(): List<String> {
        return accountList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IbConnectionHandler::class.java)
    }
}
