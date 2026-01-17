package io.github.fracturedjson.samples

import io.github.fracturedjson.kotlinx.reformatJson
import io.github.fracturedjson.core.FracturedJsonOptions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * API log file formatting test.
 *
 * Formats sample log files in the samples/ directory using FracturedJson.
 */
class ApiLogFormattingTest {

    @Test
    fun `format API logs array`() {
        val options = FracturedJsonOptions(
            maxTotalLineLength = 100,
            indentSpaces = 2
        )

        val input = File("samples/api-logs.json").readText()
        val formatted = input.reformatJson(options)

        println("=== API Logs (Table-aligned) ===")
        println(formatted)
        println()
    }

    @Test
    fun `format complex API response`() {
        val options = FracturedJsonOptions(
            maxTotalLineLength = 80,
            indentSpaces = 2
        )

        val input = File("samples/complex-api-response.json").readText()
        val formatted = input.reformatJson(options)

        println("=== Complex API Response ===")
        println(formatted)
        println()
    }

    @Test
    fun `format NDJSON logs line by line`() {
        val options = FracturedJsonOptions(
            maxTotalLineLength = 120,
            indentSpaces = 2
        )

        println("=== NDJSON Logs (Each line formatted) ===")
        File("samples/api-logs.ndjson").useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    val formatted = line.reformatJson(options)
                    println(formatted)
                    println("---")
                }
            }
        }
    }

    @Test
    fun `compare minified vs formatted`() {
        val input = File("samples/api-logs.json").readText()

        // Minified (compact)
        val minified = input.reformatJson(FracturedJsonOptions(
            maxTotalLineLength = Int.MAX_VALUE,
            maxInlineComplexity = Int.MAX_VALUE
        ))

        // Formatted (readable)
        val formatted = input.reformatJson(FracturedJsonOptions(
            maxTotalLineLength = 80,
            indentSpaces = 2
        ))

        println("=== Minified (${minified.length} chars) ===")
        println(minified.take(200) + "...")
        println()
        println("=== Formatted (${formatted.length} chars) ===")
        println(formatted.take(500) + "...")
    }
}
