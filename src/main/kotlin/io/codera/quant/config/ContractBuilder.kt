package io.codera.quant.config

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import com.ib.client.Contract
import com.ib.client.Types
import org.slf4j.LoggerFactory

/**
 * Builds the contract
 */
object ContractBuilder {
    private val log = LoggerFactory.getLogger(ContractBuilder::class.java)
    private val futuresMap: Map<String, Int> = ImmutableMap.of(
        "ES=F", 50, "YM=F", 5,
        "TF=F", 50
    )

    /**
     * If it's a forex symbol, then for some strategies it is necessary to flip the price to
     * understand the price based on USD (e.g. how many dollars is needed to buy 1 unit of currency
     * in traded pair)
     *
     * @param symbol symbol name
     * @return adjusted price, basically 1/<pair price>
    </pair> */
    fun getSymbolPrice(symbol: String, price: Double): Double {
        if (symbol.contains("/")) {
            val fxSymbols = symbol.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (fxSymbols[0] == "USD") return 1 / price
        }
        return price
    }

    @JvmStatic
    fun getFutureMultiplier(futureSymbol: String): Int {
        // TODO (Dsinyakov) : refactor to throw exception instead of returning null
        return futuresMap[futureSymbol]?: 1
    }

    fun build(symbolName: String): Contract {
        val contract = Contract()
        if (symbolName.contains("/")) {
            log.debug("{} is a Forex symbol", symbolName)
            val fxSymbols = symbolName.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            contract.symbol(fxSymbols[0])
            contract.exchange("IDEALPRO")
            contract.secType(Types.SecType.CASH)
            contract.currency(fxSymbols[1])
            return contract
        } else if (symbolName.contains("=F")) {
            val s = symbolName.replace("=F", "")
            val futuresMap: Map<String, String> = ImmutableMap.of(
                "ES", "GLOBEX",
                "YM", "ECBOT",
                "TF", "NYBOT"
            )
            contract.symbol(s)
            contract.exchange(futuresMap[s])
            //      contract.expiry("201706");
            contract.secType(Types.SecType.FUT)
            contract.currency("USD")
            log.info("Contract $contract")
            return contract
        }
        contract.symbol(symbolName)
        contract.localSymbol(symbolName)
        contract.exchange("SMART")
        contract.primaryExch("ARCA")
        contract.secType(Types.SecType.STK)
        contract.currency("USD")
        return contract
    }
}
