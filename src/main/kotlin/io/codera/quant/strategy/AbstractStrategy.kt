package io.codera.quant.strategy

import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import io.codera.quant.context.TradingContext
import io.codera.quant.exception.CriterionViolationException

/**
 * Abstract strategy class.
 */
abstract class AbstractStrategy(override val tradingContext: TradingContext) : Strategy {
    private var commonCriteria: MutableList<Criterion?> = Lists.newCopyOnWriteArrayList()
    private var entryCriteria: MutableList<Criterion?> = Lists.newCopyOnWriteArrayList()
    private var exitCriteria: MutableList<Criterion?> = Lists.newCopyOnWriteArrayList()
    private var stopLossCriteria: MutableList<Criterion?> = Lists.newCopyOnWriteArrayList()
    private var symbols: MutableList<String> = Lists.newLinkedList()

    override fun addEntryCriterion(criterion: Criterion) {
        criterion.init()
        entryCriteria.add(criterion)
    }

    override fun removeEntryCriterion(criterion: Criterion?) {
        entryCriteria.remove(criterion)
    }

    override fun addCommonCriterion(criterion: Criterion) {
        criterion.init()
        commonCriteria.add(criterion)
    }

    override fun removeCommonCriterion(criterion: Criterion?) {
        commonCriteria.remove(criterion)
    }

    override fun addExitCriterion(criterion: Criterion) {
        criterion.init()
        exitCriteria.add(criterion)
    }

    override fun removeExitCriterion(criterion: Criterion?) {
        exitCriteria.remove(criterion)
    }

    override val isCommonCriteriaMet: Boolean
        get() = testCriteria(commonCriteria)
    override val isEntryCriteriaMet: Boolean
        get() = testCriteria(entryCriteria)
    override val isExitCriteriaMet: Boolean
        get() = !exitCriteria.isEmpty() && testCriteria(exitCriteria)
    override val isStopLossCriteriaMet: Boolean
        get() = !stopLossCriteria.isEmpty() && testCriteria(stopLossCriteria)

    override fun addStopLossCriterion(criterion: Criterion?) {
        Preconditions.checkArgument(criterion != null, "criterion is null")
        stopLossCriteria.add(criterion)
    }

    override val backTestResult: BackTestResult?
        get() {
            throw UnsupportedOperationException()
        }

    override fun addSymbol(symbol: String) {
        symbols.add(symbol)
        tradingContext.addContract(symbol)
    }

    private fun testCriteria(criteria: List<Criterion?>): Boolean {
        if (criteria.size == 0) {
            return true
        }
        for (criterion in criteria) {
            try {
                if (!criterion!!.isMet) {
                    Strategy.log.debug("{} criterion was NOT met", criterion.javaClass.name)
                    return false
                }
                Strategy.log.debug("{} criterion was met", criterion.javaClass.name)
            } catch (e: CriterionViolationException) {
                Strategy.log.debug("{} criterion was NOT met", criterion!!.javaClass.name)
                return false
            }
        }
        return true
    }
}
