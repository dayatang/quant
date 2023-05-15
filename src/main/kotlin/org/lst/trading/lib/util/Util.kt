package org.lst.trading.lib.util

import org.lst.trading.lib.series.MultipleDoubleSeries
import org.lst.trading.lib.series.TimeSeries
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object Util {
    fun writeCsv(series: MultipleDoubleSeries): Path {
        val data = """
             date,${series.names.stream().collect(Collectors.joining(","))}
             ${
            series.stream().map { e: TimeSeries.Entry<MutableList<Double>> ->
                e.instant.toString() + "," + e.item.stream().map { obj: Double -> obj.toString() }
                    .collect(Collectors.joining(","))
            }.collect(Collectors.joining("\n"))
        }
             """.trimIndent()
        return writeStringToTempFile(data)
    }

    fun writeStringToTempFile(content: String): Path {
        return try {
            writeString(content, Paths.get(File.createTempFile("out-", ".csv").absolutePath))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun writeString(content: String, path: Path): Path {
        return try {
            Files.write(path, content.toByteArray())
            path
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun check(condition: Boolean) {
        if (!condition) {
            throw RuntimeException()
        }
    }

    fun check(condition: Boolean, message: String?) {
        if (!condition) {
            throw RuntimeException(message)
        }
    }
}
