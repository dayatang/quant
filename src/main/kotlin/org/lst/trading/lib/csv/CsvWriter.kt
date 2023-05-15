package org.lst.trading.lib.csv

import rx.functions.Func1
import java.io.*
import java.util.stream.Collectors
import java.util.stream.Stream

class CsvWriter<T>(var mColumns: List<Column<T>>) {
    interface Column<T> {
        val name: String?
        val f: Func1<T, Any?>
    }

    var mSeparator = ","
    fun write(values: Stream<T>): Stream<String> {
        return Stream.concat(
            Stream.of(mColumns.stream().map { obj: Column<T> -> obj.name }
                .collect(Collectors.joining(mSeparator))),
            values.map { x: T ->
                mColumns.stream().map { f: Column<T> ->
                    val o = f.f.call(x)
                    o?.toString() ?: ""
                }.collect(Collectors.joining(mSeparator))
            }
        )
    }

    @Throws(IOException::class)
    fun writeToStream(values: Stream<T>, outputStream: OutputStream?) {
        val writer: Writer = BufferedWriter(OutputStreamWriter(outputStream))
        write(values).forEach { x: String? ->
            try {
                writer.write(x)
                writer.write("\n")
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        writer.flush()
    }

    companion object {
        fun <T> create(columns: List<Column<T>>): CsvWriter<T> {
            return CsvWriter(columns)
        }

        fun <T> column(name: String?, f: Func1<T, Any?>): Column<T> {
            return object : Column<T> {
                override val name: String?
                    get() = name
                override val f: Func1<T, Any?>
                    get() = f
            }
        }
    }
}
