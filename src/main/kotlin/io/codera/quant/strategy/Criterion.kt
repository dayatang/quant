package io.codera.quant.strategy

import io.codera.quant.exception.CriterionViolationException

/**
 * Criterion interface.
 */
interface Criterion {
    /**
     * To be run if criterion needs tobe initialized .E.g. to get the historical or other data needed
     * for calculation.
     */
    fun init() {}

    @get:Throws(CriterionViolationException::class)
    val isMet: Boolean
}
