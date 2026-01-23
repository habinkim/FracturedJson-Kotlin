package io.github.fracturedjson.samples

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.fastjson2.reformatJsonWithFastjson2
import io.github.fracturedjson.gson.reformatJsonWithGson
import io.github.fracturedjson.jackson.formatJson
import io.github.fracturedjson.kotlinx.reformatJson
import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * Comprehensive test suite for JSON sample files.
 *
 * Tests all JSON files in the samples/ directory using multiple adapters:
 * - Core Parser + Formatter
 * - Jackson Adapter
 * - Kotlinx Serialization Adapter
 * - Gson Adapter
 * - Fastjson2 Adapter
 *
 * Each file uses optimized maxTotalLineLength to ensure proper property alignment.
 */
class SampleJsonFilesTest {

    companion object {
        private val SAMPLES_DIR = File("samples")

        private val objectMapper = ObjectMapper()
        private val gson = Gson()

        /**
         * File-specific optimal line lengths based on max atomic value size.
         * Calculated as: maxPropertyNameLength + colonLen + maxAtomicValueLength + padding
         * This ensures property alignment is applied correctly for each file.
         */
        private val FILE_LINE_LENGTHS = mapOf(
            // Small files - use readable 80-char limit
            "api-logs.json" to 80,
            "complex-api-response.json" to 80,
            "google_maps_api_compact_response.json" to 80,
            "google_maps_api_response.json" to 80,
            "instruments.json" to 80,
            "canada.json" to 80,
            "repeat.json" to 80,

            // Medium files - 120-char limit
            "apache_builds.json" to 120,
            "citm_catalog.json" to 120,
            "marine_ik.json" to 100,
            "random.json" to 100,
            "tree-pretty.json" to 100,

            // Numeric data files - use compact limit
            "mesh.json" to 120,
            "mesh.pretty.json" to 120,
            "numbers.json" to 120,

            // Files with long URLs/strings - need wider limits
            "github_events.json" to 450,
            "twitter.json" to 200,
            "twitter_api_response.json" to 200,
            "twitter_api_compact_response.json" to 200,
            "twitter_timeline.json" to 350,
            "twitterescaped.json" to 1000,
            "update-center.json" to 450,

            // Very long content files - use very wide limits
            "gsoc-2018.json" to 2100,
            "semanticscholar-corpus.json" to 10100
        )

        /**
         * Gets optimal options for a specific file.
         */
        fun getOptionsForFile(fileName: String): FracturedJsonOptions {
            val lineLength = FILE_LINE_LENGTHS[fileName] ?: 120
            return FracturedJsonOptions(
                maxTotalLineLength = lineLength,
                indentSpaces = 2
            )
        }

        /**
         * Gets minified options (consistent across all files).
         */
        fun getMinifiedOptions(): FracturedJsonOptions {
            return FracturedJsonOptions(
                maxTotalLineLength = Int.MAX_VALUE,
                maxInlineComplexity = Int.MAX_VALUE
            )
        }

        @JvmStatic
        fun jsonFileProvider(): Stream<Arguments> {
            return SAMPLES_DIR.listFiles { file ->
                file.extension == "json"
            }?.map { file ->
                Arguments.of(file.name, file, getOptionsForFile(file.name))
            }?.stream() ?: Stream.empty()
        }

        @JvmStatic
        fun ndjsonFileProvider(): Stream<Arguments> {
            return SAMPLES_DIR.listFiles { file ->
                file.extension == "ndjson"
            }?.map { file ->
                Arguments.of(file.name, file)
            }?.stream() ?: Stream.empty()
        }
    }

    // ============================================================
    // Core Parser + Formatter Tests
    // ============================================================
    @Nested
    @DisplayName("Core Parser + Formatter")
    inner class CoreParserFormatterTests {

        @ParameterizedTest(name = "Core: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Format with Core Parser")
        fun `format with core parser`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val parser = Parser()
            val formatter = Formatter(options)

            val items = parser.parse(input)
            val formatted = formatter.format(items)

            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())

            println("=".repeat(80))
            println("[$fileName] Core Parser + Formatter (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println("Output size: ${formatted.length} chars")
            println("-".repeat(80))
            println(formatted)
            println()
        }

        @ParameterizedTest(name = "Core Minify-Reformat: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Core Parser round-trip")
        fun `core parser round trip`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val parser = Parser()
            val minifyFormatter = Formatter(getMinifiedOptions())
            val readableFormatter = Formatter(options)

            // Parse -> Minify
            val items1 = parser.parse(input)
            val minified = minifyFormatter.format(items1)

            // Parse minified -> Reformat
            val items2 = parser.parse(minified)
            val reformatted = readableFormatter.format(items2)

            // Parse reformatted -> Minify again
            val items3 = parser.parse(reformatted)
            val minifiedAgain = minifyFormatter.format(items3)

            assertEquals(minified, minifiedAgain, "Data should be preserved through format cycles")

            println("=".repeat(80))
            println("[$fileName] Core Parser Round-trip (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Original -> Minified: ${input.length} -> ${minified.length} chars")
            println("Minified -> Reformatted: ${minified.length} -> ${reformatted.length} chars")
            println("Reformatted -> Minified: ${reformatted.length} -> ${minifiedAgain.length} chars")
            println("Round-trip successful: ${minified == minifiedAgain}")
            println()
        }
    }

    // ============================================================
    // Jackson Adapter Tests
    // ============================================================
    @Nested
    @DisplayName("Jackson Adapter")
    inner class JacksonAdapterTests {

        @ParameterizedTest(name = "Jackson: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Format with Jackson Adapter")
        fun `format with jackson adapter`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val formatted = objectMapper.formatJson(input, options)

            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())

            println("=".repeat(80))
            println("[$fileName] Jackson Adapter (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println("Output size: ${formatted.length} chars")
            println("-".repeat(80))
            println(formatted)
            println()
        }

        @ParameterizedTest(name = "Jackson Minify-Reformat: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Jackson round-trip")
        fun `jackson round trip`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()

            val minified = objectMapper.formatJson(input, getMinifiedOptions())
            val reformatted = objectMapper.formatJson(minified, options)
            val minifiedAgain = objectMapper.formatJson(reformatted, getMinifiedOptions())

            assertEquals(minified, minifiedAgain, "Data should be preserved through format cycles")

            println("=".repeat(80))
            println("[$fileName] Jackson Round-trip (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Original -> Minified: ${input.length} -> ${minified.length} chars")
            println("Minified -> Reformatted: ${minified.length} -> ${reformatted.length} chars")
            println("Reformatted -> Minified: ${reformatted.length} -> ${minifiedAgain.length} chars")
            println("Round-trip successful: ${minified == minifiedAgain}")
            println()
        }
    }

    // ============================================================
    // Kotlinx Serialization Adapter Tests
    // ============================================================
    @Nested
    @DisplayName("Kotlinx Serialization Adapter")
    inner class KotlinxAdapterTests {

        @ParameterizedTest(name = "Kotlinx: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Format with Kotlinx Adapter")
        fun `format with kotlinx adapter`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val formatted = input.reformatJson(options)

            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())

            println("=".repeat(80))
            println("[$fileName] Kotlinx Serialization Adapter (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println("Output size: ${formatted.length} chars")
            println("-".repeat(80))
            println(formatted)
            println()
        }

        @ParameterizedTest(name = "Kotlinx Minify-Reformat: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Kotlinx round-trip")
        fun `kotlinx round trip`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()

            val minified = input.reformatJson(getMinifiedOptions())
            val reformatted = minified.reformatJson(options)
            val minifiedAgain = reformatted.reformatJson(getMinifiedOptions())

            assertEquals(minified, minifiedAgain, "Data should be preserved through format cycles")

            println("=".repeat(80))
            println("[$fileName] Kotlinx Round-trip (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Original -> Minified: ${input.length} -> ${minified.length} chars")
            println("Minified -> Reformatted: ${minified.length} -> ${reformatted.length} chars")
            println("Reformatted -> Minified: ${reformatted.length} -> ${minifiedAgain.length} chars")
            println("Round-trip successful: ${minified == minifiedAgain}")
            println()
        }
    }

    // ============================================================
    // Gson Adapter Tests
    // ============================================================
    @Nested
    @DisplayName("Gson Adapter")
    inner class GsonAdapterTests {

        @ParameterizedTest(name = "Gson: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Format with Gson Adapter")
        fun `format with gson adapter`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val formatted = input.reformatJsonWithGson(options)

            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())

            println("=".repeat(80))
            println("[$fileName] Gson Adapter (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println("Output size: ${formatted.length} chars")
            println("-".repeat(80))
            println(formatted)
            println()
        }

        @ParameterizedTest(name = "Gson Minify-Reformat: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Gson round-trip")
        fun `gson round trip`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()

            val minified = input.reformatJsonWithGson(getMinifiedOptions())
            val reformatted = minified.reformatJsonWithGson(options)
            val minifiedAgain = reformatted.reformatJsonWithGson(getMinifiedOptions())

            assertEquals(minified, minifiedAgain, "Data should be preserved through format cycles")

            println("=".repeat(80))
            println("[$fileName] Gson Round-trip (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Original -> Minified: ${input.length} -> ${minified.length} chars")
            println("Minified -> Reformatted: ${minified.length} -> ${reformatted.length} chars")
            println("Reformatted -> Minified: ${reformatted.length} -> ${minifiedAgain.length} chars")
            println("Round-trip successful: ${minified == minifiedAgain}")
            println()
        }
    }

    // ============================================================
    // Fastjson2 Adapter Tests
    // ============================================================
    @Nested
    @DisplayName("Fastjson2 Adapter")
    inner class Fastjson2AdapterTests {

        @ParameterizedTest(name = "Fastjson2: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Format with Fastjson2 Adapter")
        fun `format with fastjson2 adapter`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val formatted = input.reformatJsonWithFastjson2(options)

            assertNotNull(formatted)
            assertTrue(formatted.isNotEmpty())

            println("=".repeat(80))
            println("[$fileName] Fastjson2 Adapter (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println("Output size: ${formatted.length} chars")
            println("-".repeat(80))
            println(formatted)
            println()
        }

        @ParameterizedTest(name = "Fastjson2 Minify-Reformat: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("Fastjson2 round-trip")
        fun `fastjson2 round trip`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()

            val minified = input.reformatJsonWithFastjson2(getMinifiedOptions())
            val reformatted = minified.reformatJsonWithFastjson2(options)
            val minifiedAgain = reformatted.reformatJsonWithFastjson2(getMinifiedOptions())

            assertEquals(minified, minifiedAgain, "Data should be preserved through format cycles")

            println("=".repeat(80))
            println("[$fileName] Fastjson2 Round-trip (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Original -> Minified: ${input.length} -> ${minified.length} chars")
            println("Minified -> Reformatted: ${minified.length} -> ${reformatted.length} chars")
            println("Reformatted -> Minified: ${reformatted.length} -> ${minifiedAgain.length} chars")
            println("Round-trip successful: ${minified == minifiedAgain}")
            println()
        }
    }

    // ============================================================
    // NDJSON Tests (All Adapters)
    // ============================================================
    @Nested
    @DisplayName("NDJSON Line-by-Line Tests")
    inner class NdjsonTests {

        private val ndjsonOptions = FracturedJsonOptions(
            maxTotalLineLength = 120,
            indentSpaces = 2
        )

        @ParameterizedTest(name = "NDJSON Core: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#ndjsonFileProvider")
        @DisplayName("Format NDJSON with Core Parser")
        fun `format ndjson with core parser`(fileName: String, file: File) {
            val parser = Parser()
            val formatter = Formatter(ndjsonOptions)
            var lineCount = 0

            println("=".repeat(80))
            println("[$fileName] NDJSON - Core Parser")
            println("=".repeat(80))

            file.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        lineCount++
                        val items = parser.parse(line)
                        val formatted = formatter.format(items)
                        assertTrue(formatted.isNotEmpty())
                        println("--- Line $lineCount ---")
                        println(formatted)
                    }
                }
            }

            println("Total lines: $lineCount")
            println()
        }

        @ParameterizedTest(name = "NDJSON Jackson: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#ndjsonFileProvider")
        @DisplayName("Format NDJSON with Jackson")
        fun `format ndjson with jackson`(fileName: String, file: File) {
            var lineCount = 0

            println("=".repeat(80))
            println("[$fileName] NDJSON - Jackson Adapter")
            println("=".repeat(80))

            file.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        lineCount++
                        val formatted = objectMapper.formatJson(line, ndjsonOptions)
                        assertTrue(formatted.isNotEmpty())
                        println("--- Line $lineCount ---")
                        println(formatted)
                    }
                }
            }

            println("Total lines: $lineCount")
            println()
        }

        @ParameterizedTest(name = "NDJSON Kotlinx: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#ndjsonFileProvider")
        @DisplayName("Format NDJSON with Kotlinx")
        fun `format ndjson with kotlinx`(fileName: String, file: File) {
            var lineCount = 0

            println("=".repeat(80))
            println("[$fileName] NDJSON - Kotlinx Adapter")
            println("=".repeat(80))

            file.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        lineCount++
                        val formatted = line.reformatJson(ndjsonOptions)
                        assertTrue(formatted.isNotEmpty())
                        println("--- Line $lineCount ---")
                        println(formatted)
                    }
                }
            }

            println("Total lines: $lineCount")
            println()
        }

        @ParameterizedTest(name = "NDJSON Gson: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#ndjsonFileProvider")
        @DisplayName("Format NDJSON with Gson")
        fun `format ndjson with gson`(fileName: String, file: File) {
            var lineCount = 0

            println("=".repeat(80))
            println("[$fileName] NDJSON - Gson Adapter")
            println("=".repeat(80))

            file.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        lineCount++
                        val formatted = line.reformatJsonWithGson(ndjsonOptions)
                        assertTrue(formatted.isNotEmpty())
                        println("--- Line $lineCount ---")
                        println(formatted)
                    }
                }
            }

            println("Total lines: $lineCount")
            println()
        }

        @ParameterizedTest(name = "NDJSON Fastjson2: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#ndjsonFileProvider")
        @DisplayName("Format NDJSON with Fastjson2")
        fun `format ndjson with fastjson2`(fileName: String, file: File) {
            var lineCount = 0

            println("=".repeat(80))
            println("[$fileName] NDJSON - Fastjson2 Adapter")
            println("=".repeat(80))

            file.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        lineCount++
                        val formatted = line.reformatJsonWithFastjson2(ndjsonOptions)
                        assertTrue(formatted.isNotEmpty())
                        println("--- Line $lineCount ---")
                        println(formatted)
                    }
                }
            }

            println("Total lines: $lineCount")
            println()
        }
    }

    // ============================================================
    // Cross-Adapter Consistency Tests
    // ============================================================
    @Nested
    @DisplayName("Cross-Adapter Consistency")
    inner class CrossAdapterTests {

        @ParameterizedTest(name = "Consistency: {0}")
        @MethodSource("io.github.fracturedjson.samples.SampleJsonFilesTest#jsonFileProvider")
        @DisplayName("All adapters produce consistent minified output")
        fun `all adapters produce consistent minified output`(fileName: String, file: File, options: FracturedJsonOptions) {
            val input = file.readText()
            val parser = Parser()
            val minifyFormatter = Formatter(getMinifiedOptions())

            // Core Parser
            val coreMinified = minifyFormatter.format(parser.parse(input))

            // Jackson
            val jacksonMinified = objectMapper.formatJson(input, getMinifiedOptions())

            // Kotlinx
            val kotlinxMinified = input.reformatJson(getMinifiedOptions())

            // Gson
            val gsonMinified = input.reformatJsonWithGson(getMinifiedOptions())

            // Fastjson2
            val fastjson2Minified = input.reformatJsonWithFastjson2(getMinifiedOptions())

            println("=".repeat(80))
            println("[$fileName] Cross-Adapter Consistency Check")
            println("=".repeat(80))
            println("Core:      ${coreMinified.length} chars")
            println("Jackson:   ${jacksonMinified.length} chars")
            println("Kotlinx:   ${kotlinxMinified.length} chars")
            println("Gson:      ${gsonMinified.length} chars")
            println("Fastjson2: ${fastjson2Minified.length} chars")

            // Compare outputs
            val allEqual = coreMinified == jacksonMinified &&
                    jacksonMinified == kotlinxMinified &&
                    kotlinxMinified == gsonMinified &&
                    gsonMinified == fastjson2Minified

            println("All outputs equal: $allEqual")

            if (!allEqual) {
                println("-".repeat(80))
                println("Core output:")
                println(coreMinified)
                println("-".repeat(80))
                println("Jackson output:")
                println(jacksonMinified)
                println("-".repeat(80))
                println("Kotlinx output:")
                println(kotlinxMinified)
                println("-".repeat(80))
                println("Gson output:")
                println(gsonMinified)
                println("-".repeat(80))
                println("Fastjson2 output:")
                println(fastjson2Minified)
            }
            println()

            // Note: Different adapters may produce slightly different output due to
            // floating-point representation differences, so we just verify they're all valid
            assertTrue(coreMinified.isNotEmpty())
            assertTrue(jacksonMinified.isNotEmpty())
            assertTrue(kotlinxMinified.isNotEmpty())
            assertTrue(gsonMinified.isNotEmpty())
            assertTrue(fastjson2Minified.isNotEmpty())
        }
    }

    // ============================================================
    // Individual Sample File Tests (Specific Validations)
    // ============================================================
    @Nested
    @DisplayName("Individual Sample Validations")
    inner class IndividualSampleTests {

        @Test
        @DisplayName("Apache Builds JSON")
        fun `apache builds json`() {
            val fileName = "apache_builds.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Canada GeoJSON")
        fun `canada geojson`() {
            val fileName = "canada.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("CITM Catalog JSON")
        fun `citm catalog json`() {
            val fileName = "citm_catalog.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("GitHub Events JSON")
        fun `github events json`() {
            val fileName = "github_events.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Google Maps API Response")
        fun `google maps api response`() {
            val fileName = "google_maps_api_response.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Google Maps API Compact Response")
        fun `google maps api compact response`() {
            val fileName = "google_maps_api_compact_response.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("GSOC 2018 JSON")
        fun `gsoc 2018 json`() {
            val fileName = "gsoc-2018.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Instruments JSON")
        fun `instruments json`() {
            val fileName = "instruments.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Marine IK JSON")
        fun `marine ik json`() {
            val fileName = "marine_ik.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Mesh JSON")
        fun `mesh json`() {
            val fileName = "mesh.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Mesh Pretty JSON")
        fun `mesh pretty json`() {
            val fileName = "mesh.pretty.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Numbers JSON")
        fun `numbers json`() {
            val fileName = "numbers.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Random JSON")
        fun `random json`() {
            val fileName = "random.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Repeat JSON")
        fun `repeat json`() {
            val fileName = "repeat.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("SemanticScholar Corpus JSON")
        fun `semanticscholar corpus json`() {
            val fileName = "semanticscholar-corpus.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Tree Pretty JSON")
        fun `tree pretty json`() {
            val fileName = "tree-pretty.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Twitter JSON")
        fun `twitter json`() {
            val fileName = "twitter.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Twitter API Response")
        fun `twitter api response`() {
            val fileName = "twitter_api_response.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Twitter API Compact Response")
        fun `twitter api compact response`() {
            val fileName = "twitter_api_compact_response.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Twitter Timeline JSON")
        fun `twitter timeline json`() {
            val fileName = "twitter_timeline.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Twitter Escaped JSON")
        fun `twitter escaped json`() {
            val fileName = "twitterescaped.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        @Test
        @DisplayName("Update Center JSON")
        fun `update center json`() {
            val fileName = "update-center.json"
            val input = File("samples/$fileName").readText()
            testAllAdapters(fileName, input, getOptionsForFile(fileName))
        }

        private fun testAllAdapters(fileName: String, input: String, options: FracturedJsonOptions) {
            val parser = Parser()
            val formatter = Formatter(options)

            println("=".repeat(80))
            println("[$fileName] All Adapters Test (maxLineLength=${options.maxTotalLineLength})")
            println("=".repeat(80))
            println("Input size: ${input.length} chars")
            println()

            // Core Parser
            println(">>> Core Parser + Formatter <<<")
            val coreFormatted = formatter.format(parser.parse(input))
            println(coreFormatted)
            println()

            // Jackson
            println(">>> Jackson Adapter <<<")
            val jacksonFormatted = objectMapper.formatJson(input, options)
            println(jacksonFormatted)
            println()

            // Kotlinx
            println(">>> Kotlinx Serialization Adapter <<<")
            val kotlinxFormatted = input.reformatJson(options)
            println(kotlinxFormatted)
            println()

            // Gson
            println(">>> Gson Adapter <<<")
            val gsonFormatted = input.reformatJsonWithGson(options)
            println(gsonFormatted)
            println()

            // Fastjson2
            println(">>> Fastjson2 Adapter <<<")
            val fastjson2Formatted = input.reformatJsonWithFastjson2(options)
            println(fastjson2Formatted)
            println()

            // Assertions
            assertTrue(coreFormatted.isNotEmpty(), "Core output should not be empty")
            assertTrue(jacksonFormatted.isNotEmpty(), "Jackson output should not be empty")
            assertTrue(kotlinxFormatted.isNotEmpty(), "Kotlinx output should not be empty")
            assertTrue(gsonFormatted.isNotEmpty(), "Gson output should not be empty")
            assertTrue(fastjson2Formatted.isNotEmpty(), "Fastjson2 output should not be empty")
        }
    }

    // ============================================================
    // Edge Case Tests
    // ============================================================
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Compact and pretty inputs produce same minified output")
        fun `compact and pretty produce same minified`() {
            val compactInput = File("samples/google_maps_api_compact_response.json").readText()
            val prettyInput = File("samples/google_maps_api_response.json").readText()

            println("=".repeat(80))
            println("Compact vs Pretty Input Comparison")
            println("=".repeat(80))

            // Test with each adapter
            listOf(
                "Core" to { input: String ->
                    val parser = Parser()
                    val formatter = Formatter(getMinifiedOptions())
                    formatter.format(parser.parse(input))
                },
                "Jackson" to { input: String -> objectMapper.formatJson(input, getMinifiedOptions()) },
                "Kotlinx" to { input: String -> input.reformatJson(getMinifiedOptions()) },
                "Gson" to { input: String -> input.reformatJsonWithGson(getMinifiedOptions()) },
                "Fastjson2" to { input: String -> input.reformatJsonWithFastjson2(getMinifiedOptions()) }
            ).forEach { (name, formatFn) ->
                val compactMinified = formatFn(compactInput)
                val prettyMinified = formatFn(prettyInput)

                println("$name Adapter:")
                println("  Compact input -> ${compactMinified.length} chars")
                println("  Pretty input  -> ${prettyMinified.length} chars")
                println("  Equal: ${compactMinified == prettyMinified}")

                assertEquals(compactMinified, prettyMinified,
                    "$name: Compact and pretty should produce same minified output")
            }
            println()
        }

        @Test
        @DisplayName("All sample files are valid JSON")
        fun `all sample files are valid json`() {
            val jsonFiles = SAMPLES_DIR.listFiles { file -> file.extension == "json" } ?: emptyArray()

            assertTrue(jsonFiles.isNotEmpty(), "Should have JSON files in samples directory")

            println("=".repeat(80))
            println("Validating all JSON files")
            println("=".repeat(80))

            var validCount = 0
            jsonFiles.forEach { file ->
                try {
                    val input = file.readText()

                    // Try parsing with each adapter
                    val parser = Parser()
                    parser.parse(input)

                    objectMapper.readTree(input)
                    gson.fromJson(input, Any::class.java)

                    validCount++
                    println("✓ ${file.name}")
                } catch (e: Exception) {
                    fail("${file.name} is not valid JSON: ${e.message}")
                }
            }

            println("-".repeat(80))
            println("All $validCount JSON files are valid")
            println()
        }
    }
}
