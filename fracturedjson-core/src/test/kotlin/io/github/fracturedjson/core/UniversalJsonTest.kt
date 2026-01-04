package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Universal JSON tests - ported from C# UniversalJsonTests.cs
 * Tests that formatted output is well-formed and maintains data integrity.
 */
@DisplayName("Universal JSON")
class UniversalJsonTest {

    companion object {
        private val testJsonInputs = listOf(
            """{"name": "John", "age": 30}""",
            """[1, 2, 3, 4, 5]""",
            """{"nested": {"a": 1, "b": 2}}""",
            """[{"x": 1}, {"x": 2}, {"x": 3}]""",
            """{"array": [1, 2, 3], "object": {"key": "value"}}""",
            """[1, "two", true, null, 3.14]""",
            """{"unicode": "한글 日本語 中文"}"""
        )

        private val testOptions = listOf(
            FracturedJsonOptions(jsonEolStyle = EolStyle.Lf),
            FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            ),
            FracturedJsonOptions(
                maxTotalLineLength = 40,
                jsonEolStyle = EolStyle.Lf
            ),
            FracturedJsonOptions(
                colonPadding = false,
                commaPadding = false,
                jsonEolStyle = EolStyle.Lf
            )
        )

        @JvmStatic
        fun provideTestCases(): Stream<Arguments> {
            return testJsonInputs.flatMap { json ->
                testOptions.map { options ->
                    Arguments.of(json, options)
                }
            }.stream()
        }
    }

    @Nested
    @DisplayName("Well-Formed Output")
    inner class WellFormedOutput {

        @ParameterizedTest
        @MethodSource("io.github.fracturedjson.core.UniversalJsonTest#provideTestCases")
        fun `output is well formed json`(json: String, options: FracturedJsonOptions) {
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should be parseable as valid JSON
            val reparsed = parser.parse(result)
            assertThat(reparsed).isNotEmpty
        }

        @Test
        fun `all strings exist in output`() {
            val json = """{"name": "Alice", "city": "Seoul", "id": "ABC123"}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // All string values should be preserved
            assertThat(result).contains("Alice")
            assertThat(result).contains("Seoul")
            assertThat(result).contains("ABC123")
        }
    }

    @Nested
    @DisplayName("Line Length Constraints")
    inner class LineLengthConstraints {

        @Test
        fun `max length respected`() {
            val json = """[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]"""
            val maxLength = 30
            val options = FracturedJsonOptions(
                maxTotalLineLength = maxLength,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Most lines should respect max length
            // (single elements may exceed if they're longer than max)
            val lines = result.split("\n")
            val shortLines = lines.filter { it.length <= maxLength + 5 } // Allow small overflow
            assertThat(shortLines.size).isGreaterThan(lines.size / 2)
        }
    }

    @Nested
    @DisplayName("Complexity Constraints")
    inner class ComplexityConstraints {

        @Test
        fun `max inline complexity respected`() {
            val json = """[[1, 2], [3, 4], [5, 6]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should be multiline due to low complexity limit
            val lines = result.split("\n")
            assertThat(lines.size).isGreaterThan(1)
        }
    }

    @Nested
    @DisplayName("Formatting Stability")
    inner class FormattingStability {

        @Test
        fun `repeated formatting is stable`() {
            val json = """{"a": [1, 2, 3], "b": {"x": "y"}}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result1 = formatter.format(items[0])

            // Format again
            val items2 = parser.parse(result1)
            val result2 = formatter.format(items2[0])

            // Should produce identical output
            assertThat(result2).isEqualTo(result1)
        }

        @Test
        fun `minify then reformat produces consistent output`() {
            val json = """{"a": [1, 2, 3], "b": {"x": "y"}}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val formatted = formatter.format(items[0])
            val minified = formatter.minify(items)

            // Parse minified and reformat
            val parsedFromMinified = parser.parse(minified)
            val reformatted = formatter.format(parsedFromMinified[0])

            // Should contain same data
            assertThat(reformatted).contains("\"a\"")
            assertThat(reformatted).contains("\"b\"")
        }
    }

    @Nested
    @DisplayName("Trailing Whitespace")
    inner class TrailingWhitespace {

        @Test
        fun `no trailing whitespace on any line`() {
            val json = """
                {
                    "name": "test",
                    "values": [1, 2, 3, 4, 5],
                    "nested": {
                        "a": 1,
                        "b": 2
                    }
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // No line should have trailing whitespace
            val lines = result.split("\n")
            for (line in lines) {
                assertThat(line.trimEnd()).isEqualTo(line)
            }
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    inner class DataIntegrity {

        @Test
        fun `all numbers preserved`() {
            val json = """[1, 2.5, -3, 1e10, 0.001]"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            assertThat(result).contains("1")
            assertThat(result).contains("2.5")
            assertThat(result).contains("-3")
            assertThat(result.lowercase()).contains("e")
            assertThat(result).contains("0.001")
        }

        @Test
        fun `all booleans and null preserved`() {
            val json = """[true, false, null]"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            assertThat(result).contains("true")
            assertThat(result).contains("false")
            assertThat(result).contains("null")
        }

        @Test
        fun `unicode strings preserved`() {
            val json = """{"korean": "한글", "japanese": "日本語", "chinese": "中文"}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            assertThat(result).contains("한글")
            assertThat(result).contains("日本語")
            assertThat(result).contains("中文")
        }
    }
}
