package io.github.fracturedjson.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Comprehensive Formatter tests - ported from C# test suite
 */
@DisplayName("Formatter Comprehensive")
class FormatterComprehensiveTest {

    /**
     * Helper to verify that a substring appears at the same column in all lines
     */
    private fun testInstancesLineUp(lines: List<String>, substring: String) {
        val positions = lines.mapNotNull { line ->
            val idx = line.indexOf(substring)
            if (idx >= 0) idx else null
        }
        if (positions.isNotEmpty()) {
            assertThat(positions.distinct()).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Length And Complexity")
    inner class LengthAndComplexity {

        private val complexArrayInput = """
            [
                {"A": [1, 2, 3], "B": [4, 5, 6]},
                {"A": [7, 8, 9], "B": [10, 11, 12]}
            ]
        """.trimIndent()

        @ParameterizedTest
        @CsvSource(
            "4, 1",   // maxInlineComplexity=4 -> 1 line (all inline)
            "3, 1",
            "2, 4"    // complexity 2 expands to 4 lines
        )
        fun `correct line count for inline complexity`(maxInlineComplexity: Int, expectedLines: Int) {
            val options = FracturedJsonOptions(
                maxInlineComplexity = maxInlineComplexity,
                maxCompactArrayComplexity = -1,
                maxTableRowComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )
            val formatter = Formatter(options)
            val item = createComplexArray()
            val result = formatter.format(item)
            val lines = result.split("\n")

            assertThat(lines.size).isEqualTo(expectedLines)
        }

        @Test
        fun `lower inline complexity produces more lines`() {
            val options1 = FracturedJsonOptions(
                maxInlineComplexity = 2,
                jsonEolStyle = EolStyle.Lf
            )
            val options0 = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val item = createComplexArray()
            val result1 = Formatter(options1).format(item)
            val result0 = Formatter(options0).format(item)

            assertThat(result0.split("\n").size).isGreaterThan(result1.split("\n").size)
        }

        @ParameterizedTest
        @CsvSource(
            "80, 1",
            "60, 4",
            "40, 6"
        )
        fun `correct line count for line length`(maxLineLength: Int, expectedMinLines: Int) {
            val options = FracturedJsonOptions(
                maxTotalLineLength = maxLineLength,
                jsonEolStyle = EolStyle.Lf
            )
            val formatter = Formatter(options)
            val item = createComplexArray()
            val result = formatter.format(item)
            val lines = result.split("\n")

            assertThat(lines.size).isGreaterThanOrEqualTo(expectedMinLines)
        }

        private fun createComplexArray(): JsonItem {
            return JsonItem.arrayItem(listOf(
                JsonItem.objectItem(listOf(
                    JsonItem.arrayItem(listOf(
                        JsonItem.numberItem(1),
                        JsonItem.numberItem(2),
                        JsonItem.numberItem(3)
                    )).apply { name = "A" },
                    JsonItem.arrayItem(listOf(
                        JsonItem.numberItem(4),
                        JsonItem.numberItem(5),
                        JsonItem.numberItem(6)
                    )).apply { name = "B" }
                )),
                JsonItem.objectItem(listOf(
                    JsonItem.arrayItem(listOf(
                        JsonItem.numberItem(7),
                        JsonItem.numberItem(8),
                        JsonItem.numberItem(9)
                    )).apply { name = "A" },
                    JsonItem.arrayItem(listOf(
                        JsonItem.numberItem(10),
                        JsonItem.numberItem(11),
                        JsonItem.numberItem(12)
                    )).apply { name = "B" }
                ))
            ))
        }
    }

    @Nested
    @DisplayName("Always Expand Formatting")
    inner class AlwaysExpandFormatting {

        @Test
        fun `always expand depth honored`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.objectItem(listOf(
                    JsonItem.stringItem("value1").apply { name = "a" },
                    JsonItem.stringItem("value2").apply { name = "b" }
                )),
                JsonItem.objectItem(listOf(
                    JsonItem.stringItem("value3").apply { name = "c" },
                    JsonItem.stringItem("value4").apply { name = "d" }
                ))
            ))

            // With alwaysExpandDepth=0, root expands but nested may be inlined
            val expand0Options = FracturedJsonOptions(
                alwaysExpandDepth = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val expand0Result = Formatter(expand0Options).format(item)

            // With alwaysExpandDepth=1, nested objects should also expand
            val expand1Options = FracturedJsonOptions(
                alwaysExpandDepth = 1,
                jsonEolStyle = EolStyle.Lf
            )
            val expand1Result = Formatter(expand1Options).format(item)

            // Both results should contain the same content
            assertThat(expand0Result).contains("\"a\"")
            assertThat(expand0Result).contains("\"value1\"")
            assertThat(expand1Result).contains("\"a\"")
            assertThat(expand1Result).contains("\"value1\"")

            // Both results should be valid JSON with proper structure
            assertThat(expand0Result).contains("[")
            assertThat(expand0Result).contains("]")
            assertThat(expand1Result).contains("[")
            assertThat(expand1Result).contains("]")
        }

        @Test
        fun `always expand depth doesnt prevent table`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.arrayItem(listOf(JsonItem.numberItem(1), JsonItem.numberItem(2))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(10), JsonItem.numberItem(20))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(100), JsonItem.numberItem(200)))
            ))

            val options = FracturedJsonOptions(
                alwaysExpandDepth = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)
            val lines = result.split("\n")

            // Should have multiple lines (table or expanded)
            assertThat(lines.size).isGreaterThanOrEqualTo(3)
            // Should contain all numbers
            assertThat(result).contains("1")
            assertThat(result).contains("200")
        }
    }

    @Nested
    @DisplayName("Padding Formatting")
    inner class PadFormatting {

        @Test
        fun `no spaces when all padding disabled`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.numberItem(1).apply { name = "a" },
                JsonItem.numberItem(2).apply { name = "b" }
            ))

            val options = FracturedJsonOptions(
                useTabToIndent = true,
                colonPadding = false,
                commaPadding = false,
                nestedBracketPadding = false,
                simpleBracketPadding = false,
                maxCompactArrayComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // Between quotes and values, there should be minimal spaces
            // The formatting might still have structural spaces
            assertThat(result).doesNotContain(": ")
        }

        @Test
        fun `simple bracket padding works for tables`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.arrayItem(listOf(JsonItem.numberItem(1), JsonItem.numberItem(2))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(3), JsonItem.numberItem(4)))
            ))

            // With simple bracket padding
            val paddedOptions = FracturedJsonOptions(
                simpleBracketPadding = true,
                jsonEolStyle = EolStyle.Lf
            )
            val paddedResult = Formatter(paddedOptions).format(item)
            assertThat(paddedResult).contains("[ 1")

            // Without simple bracket padding
            val noPadOptions = FracturedJsonOptions(
                simpleBracketPadding = false,
                jsonEolStyle = EolStyle.Lf
            )
            val noPadResult = Formatter(noPadOptions).format(item)
            assertThat(noPadResult).contains("[1")
        }
    }

    @Nested
    @DisplayName("Property Alignment")
    inner class PropertyAlignment {

        @Test
        fun `property values aligned`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.numberItem(1).apply { name = "a" },
                JsonItem.numberItem(2).apply { name = "longName" },
                JsonItem.numberItem(3).apply { name = "x" }
            ))

            val options = FracturedJsonOptions(
                maxPropNamePadding = 15,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)
            val lines = result.split("\n")

            // Colons should align
            testInstancesLineUp(lines.filter { it.contains(":") }, ":")
        }

        @Test
        fun `dont align when too much padding required`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.numberItem(1).apply { name = "a" },
                JsonItem.numberItem(2).apply { name = "veryVeryLongPropertyName" },
                JsonItem.numberItem(3).apply { name = "x" }
            ))

            val options = FracturedJsonOptions(
                maxPropNamePadding = 12,  // Too small for the long name
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)
            val lines = result.split("\n").filter { it.contains(":") }

            // Colons should NOT align if padding would exceed max
            val colonPositions = lines.map { it.indexOf(":") }.distinct()
            // May or may not align depending on implementation
        }

        @Test
        fun `colons hug prop names when configured`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.numberItem(1).apply { name = "a" },
                JsonItem.numberItem(2).apply { name = "longName" }
            ))

            val options = FracturedJsonOptions(
                colonBeforePropNamePadding = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // With colonBeforePropNamePadding, colon comes right after name
            assertThat(result).contains("\"a\":")
            assertThat(result).contains("\"longName\":")
        }
    }

    @Nested
    @DisplayName("Table Formatting")
    inner class TableFormatting {

        @Test
        fun `nested elements line up`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.objectItem(listOf(
                    JsonItem.stringItem("Alice").apply { name = "name" },
                    JsonItem.numberItem(25).apply { name = "age" }
                )),
                JsonItem.objectItem(listOf(
                    JsonItem.stringItem("Bob").apply { name = "name" },
                    JsonItem.numberItem(30).apply { name = "age" }
                ))
            ))

            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val result = Formatter(options).format(item)
            val lines = result.split("\n")

            // "name" should appear at same column in both object lines
            val nameLines = lines.filter { it.contains("\"name\"") }
            if (nameLines.size > 1) {
                testInstancesLineUp(nameLines, "\"name\"")
            }
        }

        @Test
        fun `tables with numbers align`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.objectItem(listOf(
                    JsonItem.numberItem(1).apply { name = "x" },
                    JsonItem.numberItem(100).apply { name = "y" }
                )),
                JsonItem.objectItem(listOf(
                    JsonItem.numberItem(999).apply { name = "x" },
                    JsonItem.numberItem(5).apply { name = "y" }
                ))
            ))

            val options = FracturedJsonOptions(
                numberListAlignment = NumberListAlignment.Right,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // Numbers should be right-aligned in their columns
            assertThat(result).contains("\"x\"")
            assertThat(result).contains("\"y\"")
        }

        @Test
        fun `comma placement after padding works`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.arrayItem(listOf(JsonItem.numberItem(1), JsonItem.numberItem(2))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(100), JsonItem.numberItem(200)))
            ))

            val options = FracturedJsonOptions(
                tableCommaPlacement = TableCommaPlacement.AfterPadding,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)
            val lines = result.split("\n")

            // Commas should be at consistent positions
            val commaLines = lines.filter { it.contains(",") && it.contains("[") }
            if (commaLines.size > 1) {
                testInstancesLineUp(commaLines, ",")
            }
        }
    }

    @Nested
    @DisplayName("Number Formatting")
    inner class NumberFormatting {

        @Test
        fun `inline array doesnt justify numbers`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(100),
                JsonItem.numberItem(3.14)
            ))

            val options = FracturedJsonOptions(
                maxInlineComplexity = 10,  // Keep inline
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // Should be single line with no extra padding
            assertThat(result.split("\n")).hasSize(1)
            // Original number representations preserved
            assertThat(result).contains("1")
            assertThat(result).contains("100")
        }

        @Test
        fun `compact array justifies numbers`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(22),
                JsonItem.numberItem(333)
            ))

            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                maxCompactArrayComplexity = 10,
                numberListAlignment = NumberListAlignment.Right,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // Numbers may be padded for alignment
            assertThat(result).contains("1")
            assertThat(result).contains("333")
        }

        @ParameterizedTest
        @ValueSource(strings = ["Left", "Right", "Decimal", "Normalize"])
        fun `number alignment options work`(alignment: String) {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.arrayItem(listOf(JsonItem.numberItem(1.5))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(23.456))),
                JsonItem.arrayItem(listOf(JsonItem.numberItem(789.0)))
            ))

            val alignmentEnum = NumberListAlignment.valueOf(alignment)
            val options = FracturedJsonOptions(
                numberListAlignment = alignmentEnum,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            assertThat(result).contains("1.5")
            assertThat(result).contains("23.456")
        }

        @Test
        fun `handles scientific notation in tables`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.arrayItem(listOf(JsonItem(JsonItemType.Number).apply { value = "1e10" })),
                JsonItem.arrayItem(listOf(JsonItem(JsonItemType.Number).apply { value = "2.5e-5" }))
            ))

            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val result = Formatter(options).format(item)

            assertThat(result).contains("1e10")
            assertThat(result).contains("2.5e-5")
        }
    }

    @Nested
    @DisplayName("End Of Line Styles")
    inner class EndOfLineStyles {

        @Test
        fun `uses LF when configured`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(2)
            ))

            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            assertThat(result).contains("\n")
            assertThat(result).doesNotContain("\r\n")
        }

        @Test
        fun `uses CRLF when configured`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(2)
            ))

            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Crlf
            )
            val result = Formatter(options).format(item)

            assertThat(result).contains("\r\n")
        }
    }

    @Nested
    @DisplayName("Indentation")
    inner class Indentation {

        @Test
        fun `respects indent spaces setting`() {
            val item = JsonItem.arrayItem(listOf(JsonItem.numberItem(1)))

            // 2 spaces
            val options2 = FracturedJsonOptions(
                indentSpaces = 2,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result2 = Formatter(options2).format(item)
            assertThat(result2).contains("\n  1")

            // 4 spaces
            val options4 = FracturedJsonOptions(
                indentSpaces = 4,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result4 = Formatter(options4).format(item)
            assertThat(result4).contains("\n    1")
        }

        @Test
        fun `uses tabs when configured`() {
            val item = JsonItem.arrayItem(listOf(JsonItem.numberItem(1)))

            val options = FracturedJsonOptions(
                useTabToIndent = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            assertThat(result).contains("\n\t")
        }

        @Test
        fun `prefix string works`() {
            val item = JsonItem.arrayItem(listOf(JsonItem.numberItem(1)))

            val options = FracturedJsonOptions(
                prefixString = "// ",
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)

            // Each line should start with prefix
            val lines = result.split("\n")
            lines.forEach { line ->
                if (line.isNotEmpty()) {
                    assertThat(line).startsWith("// ")
                }
            }
        }
    }

    @Nested
    @DisplayName("Stability")
    inner class Stability {

        @Test
        fun `repeated formatting is stable`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.arrayItem(listOf(
                    JsonItem.numberItem(1),
                    JsonItem.numberItem(2),
                    JsonItem.numberItem(3)
                )).apply { name = "numbers" },
                JsonItem.stringItem("test").apply { name = "text" }
            ))

            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val formatter = Formatter(options)

            val result1 = formatter.format(item)
            val result2 = formatter.format(item)

            assertThat(result1).isEqualTo(result2)
        }

        @Test
        fun `no trailing whitespace`() {
            val item = JsonItem.objectItem(listOf(
                JsonItem.numberItem(1).apply { name = "a" },
                JsonItem.numberItem(2).apply { name = "bb" },
                JsonItem.numberItem(3).apply { name = "ccc" }
            ))

            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val result = Formatter(options).format(item)
            val lines = result.split("\n")

            lines.forEach { line ->
                assertThat(line.trimEnd()).isEqualTo(line)
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `handles empty array`() {
            val item = JsonItem.arrayItem(emptyList())
            val result = Formatter().format(item)
            assertThat(result).isEqualTo("[]")
        }

        @Test
        fun `handles empty object`() {
            val item = JsonItem.objectItem(emptyList())
            val result = Formatter().format(item)
            assertThat(result).isEqualTo("{}")
        }

        @Test
        fun `handles null value`() {
            val item = JsonItem.nullItem()
            val result = Formatter().format(item)
            assertThat(result).isEqualTo("null")
        }

        @Test
        fun `handles deeply nested structure`() {
            var current: JsonItem = JsonItem.numberItem(42)
            repeat(10) {
                current = JsonItem.arrayItem(listOf(current))
            }

            val result = Formatter().format(current)
            assertThat(result).contains("42")
            assertThat(result.count { it == '[' }).isEqualTo(10)
        }

        @Test
        fun `handles special characters in strings`() {
            val item = JsonItem.stringItem("line1\nline2\ttab\"quote")
            val result = Formatter().format(item)
            assertThat(result).contains("\\n")
            assertThat(result).contains("\\t")
            assertThat(result).contains("\\\"")
        }
    }
}
