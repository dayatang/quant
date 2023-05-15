package io.codera.quant.exception

/**
 * Thrown when price is not yet available
 */
class PriceNotAvailableException : Exception {
    constructor() : super()
    constructor(msg: String?) : super(msg)
}
