package org.lst.trading.lib.csv

import org.lst.trading.lib.model.Bar
import org.lst.trading.lib.series.DoubleSeries
import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

object CsvReader {
    fun <T : Consumer<Array<String?>?>?> parse(lines: Stream<String>, sep: String, consumer: T) {
        lines.map { l: String ->
            l.split(sep.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        }.forEach(consumer)
    }

    fun <T> parse(lines: Stream<String>, sep: String, hasHeader: Boolean, f: Function<Array<String?>?, T>): List<T> {
        val consumer = ListConsumer(f, hasHeader)
        parse(lines, sep, consumer)
        return consumer.getEntries()
    }

    @SafeVarargs
    fun parse(
        lines: Stream<String>,
        sep: String,
        instantF: ParseFunction<Instant?>,
        vararg columns: ParseFunction<Double?>
    ): MultipleDoubleSeries {
        val columnNames = Stream.of<ParseFunction<Double>>(*columns).map { obj: ParseFunction<Double> -> obj.column }
            .collect(Collectors.toList())
        val series = MultipleDoubleSeries(columnNames)
        val consumer = SeriesConsumer(
            series,
            instantF,
            Function2<Array<String>, List<String>, List<Double>> { parts: Array<String?>, cn: List<String?> ->
                Stream.of<ParseFunction<Double>>(*columns).map { t: ParseFunction<Double> ->
                    t.parse(
                        parts[cn.indexOf(
                            t.column
                        )]
                    )
                }.collect(Collectors.toList())
            })
        parse(lines, sep, consumer)
        return series
    }

    fun parse(
        lines: Stream<String>,
        sep: String,
        instantF: ParseFunction<Instant?>,
        column: ParseFunction<Double?>
    ): DoubleSeries {
        val series = DoubleSeries(column.column)
        val consumer = SeriesConsumer(
            series,
            instantF,
            Function2<Array<String>, List<String>, Double> { parts: Array<String?>, columnNames: List<String?> ->
                column.parse(
                    parts[columnNames.indexOf(
                        column.column
                    )]
                )
            })
        parse(lines, sep, consumer)
        return series
    }

    fun parse(
        lines: Stream<String>,
        open: ParseFunction<Double>,
        high: ParseFunction<Double>,
        low: ParseFunction<Double>,
        close: ParseFunction<Double>,
        volume: ParseFunction<Long>,
        instant: ParseFunction<Instant>
    ): Stream<Bar> {
        return lines
            .map { l: String -> l.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() }
            .flatMap(object : Function<Array<String?>, Stream<out Bar>> {
                var mColumns: List<Any>? = null
                var i = 0
                override fun apply(parts: Array<String?>): Stream<out Bar> {
                    return if (i++ == 0) {
                        mColumns = Stream.of(*parts).map { obj: String -> obj.trim { it <= ' ' } }
                            .collect(Collectors.toList())
                        Stream.empty()
                    } else {
                        Stream.of(
                            object : Bar {
                                override val open = open.parse(parts[mColumns!!.indexOf(open.column)])
                                override val high = high.parse(parts[mColumns!!.indexOf(high.column)])
                                override val low = low.parse(parts[mColumns!!.indexOf(low.column)])
                                override val close = close.parse(parts[mColumns!!.indexOf(close.column)])
                                override val volume = volume.parse(parts[mColumns!!.indexOf(volume.column)])
                                override val start: Instant
                                    get() = instant.parse(parts[mColumns!!.indexOf(instant.column)])
                                override val duration: Duration?
                                    get() = null
                                override val wAP: Double
                                    get() = 0
                            }
                        )
                    }
                }
            })
    }

    interface ParseFunction<T> {
        fun parse(value: String?): T
        val column: String
        fun <F> map(f: Function<T, F>): ParseFunction<F> {
            return object : ParseFunction<F> {
                override fun parse(value: String?): F {
                    return f.apply(this@ParseFunction.parse(value))
                }

                override val column: String
                    get() = this@ParseFunction.column
            }
        }

        companion object {
            fun stripQuotes(): Function<String, String>? {
                return Function { s: String ->
                    var s = s
                    s = s.replaceFirst("^\"".toRegex(), "")
                    s = s.replaceFirst("\"$".toRegex(), "")
                    s
                }
            }

            fun ofColumn(columnName: String): ParseFunction<String> {
                return object : ParseFunction<String?> {
                    override fun parse(value: String?): String? {
                        return value
                    }

                    override val column: String
                        get() = columnName
                }
            }

            fun longColumn(column: String): ParseFunction<Long>? {
                return ofColumn(column).map { s: String -> s.toLong() }
            }

            fun doubleColumn(column: String): ParseFunction<Double?> {
                return ofColumn(column).map { s: String -> s.toDouble() }
            }

            fun localDateTimeColumn(column: String, formatter: DateTimeFormatter): ParseFunction<LocalDateTime>? {
                return ofColumn(column).map { x: String? -> LocalDateTime.from(formatter.parse(x)) }
            }

            fun instantColumn(column: String, formatter: DateTimeFormatter): ParseFunction<Instant>? {
                return ofColumn(column).map { x: String? -> Instant.from(formatter.parse(x)) }
            }

            fun localDateColumn(column: String, formatter: DateTimeFormatter): ParseFunction<LocalDate>? {
                return ofColumn(column).map { x: String? -> LocalDate.from(formatter.parse(x)) }
            }

            fun localTimeColumn(column: String, formatter: DateTimeFormatter): ParseFunction<LocalDate>? {
                return ofColumn(column).map { x: String? -> LocalDate.from(formatter.parse(x)) }
            }
        }
    }

    interface Function2<T1, T2, F> {
        fun apply(t1: T1, t2: T2): F
    }

    private class SeriesConsumer<T>(
        var mSeries: TimeSeries<T>,
        var mInstantParseFunction: ParseFunction<Instant?>,
        var mF: Function2<Array<String>, List<String>?, T>
    ) : Consumer<Array<String>> {
        var i = 0
        var mColumns: List<String>? = null
        override fun accept(parts: Array<String>) {
            if (i++ == 0) {
                mColumns = Stream.of(*parts).map { obj: String -> obj.trim { it <= ' ' } }
                    .collect(Collectors.toList())
            } else {
                val instant = mInstantParseFunction.parse(
                    parts[mColumns!!.indexOf(
                        mInstantParseFunction.column
                    )]
                )
                mSeries.add(mF.apply(parts, mColumns), instant)
            }
        }
    }

    private class ListConsumer<T>(var mF: Function<Array<String?>?, T>, var mHasHeader: Boolean) :
        Consumer<Array<String?>?> {
        var i = 0
        var mEntries: MutableList<T> = ArrayList()
        override fun accept(parts: Array<String?>?) {
            if (i++ > 0 || !mHasHeader) {
                mEntries.add(mF.apply(parts))
            }
        }

        val entries: List<T>
            get() = mEntries
    }
}
