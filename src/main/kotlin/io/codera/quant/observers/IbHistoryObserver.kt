package io.codera.quant.observers

import com.ib.controller.Bar
import io.codera.quant.config.ContractBuilder
import org.joda.time.*
import org.lst.trading.lib.series.DoubleSeries
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject
import java.time.Instant

/**
 *
 */
class IbHistoryObserver(private val symbol: String) : HistoryObserver {
    private val priceSubject: PublishSubject<DoubleSeries>
    private var doubleSeries: DoubleSeries? = null

    init {
        priceSubject = PublishSubject.create()
    }

    override fun historicalData(bar: Bar) {
        if (doubleSeries == null) {
            doubleSeries = DoubleSeries(symbol)
        }
        val dt = DateTime(bar.time() * 1000)
        if (dt.minuteOfDay().get() >= 390 && dt.minuteOfDay().get() <= 390 + 390) {
            logger.debug("{} {} {}", bar.formattedTime(), symbol, bar.close())
            doubleSeries!!.add(
                ContractBuilder.getSymbolPrice(symbol, bar.close()),
                Instant.ofEpochMilli(
                    LocalDateTime(bar.time() * 1000).toDateTime(DateTimeZone.UTC)
                        .millis
                )
            )
        }
    }

    override fun historicalDataEnd() {
        priceSubject.onNext(doubleSeries)
        logger.debug("End of historic data for $symbol")
    }

    fun observableDoubleSeries(): Observable<DoubleSeries> {
        return priceSubject.asObservable()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IbHistoryObserver::class.java)
    }
}
