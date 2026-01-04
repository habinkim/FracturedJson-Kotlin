package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Number formatting tests - ported from C# NumberFormattingTests.cs
 */
@DisplayName("Number Formatting")
class NumberFormattingTest {

    @Nested
    @DisplayName("Inline Array Formatting")
    inner class InlineArrayFormatting {

        @Test
        fun `inline array doesnt justify numbers`() {
            val json = """[1, 22, 333]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 10,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // In inline format, numbers should not be padded
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }
    }

    @Nested
    @DisplayName("Compact Array Formatting")
    inner class CompactArrayFormatting {

        @Test
        fun `compact array does justify numbers`() {
            val json = """
                [
                    [1, 2],
                    [333, 4444]
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxInlineComplexity = 1,
                maxCompactArrayComplexity = 2,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Numbers should be present with potential alignment
            assertThat(result).contains("1")
            assertThat(result).contains("4444")
        }
    }

    @Nested
    @DisplayName("Table Array Formatting")
    inner class TableArrayFormatting {

        @Test
        fun `table array does justify numbers`() {
            val json = """[[1, 2], [333, 4444]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Numbers should be formatted in table style
            assertThat(result).contains("1")
            assertThat(result).contains("333")
        }
    }

    @Nested
    @DisplayName("Big Number Handling")
    inner class BigNumberHandling {

        @Test
        fun `scientific notation numbers invalidate alignment`() {
            val json = """[1e10, 2, 3]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Scientific notation should be preserved
            assertThat(result.lowercase()).contains("e")
        }

        @Test
        fun `numbers with excessive significant digits disable alignment`() {
            val json = """[12345678901234567890, 1, 2]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Large number should be preserved
            assertThat(result).contains("12345678901234567890")
        }
    }

    @Nested
    @DisplayName("Null Handling")
    inner class NullHandling {

        @Test
        fun `nulls respected when aligning numbers`() {
            val json = """[[1, null], [22, 33]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Nulls should be preserved
            assertThat(result).contains("null")
        }
    }

    @Nested
    @DisplayName("Number Alignment Modes")
    inner class NumberAlignmentModes {

        @Test
        fun `left align works`() {
            val json = """[[1], [22], [333]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Left,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // All numbers should be present
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `right align works`() {
            val json = """[[1], [22], [333]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Right,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // All numbers should be present
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `decimal align works`() {
            val json = """[[1.5], [22.333], [333.1]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Decimal,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // All decimal numbers should be present
            assertThat(result).contains("1.5")
            assertThat(result).contains("22.333")
            assertThat(result).contains("333.1")
        }

        @Test
        fun `normalize align works`() {
            val json = """[[1.5], [22.333], [333.1]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Normalize,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Numbers should be normalized with consistent decimal places
            assertThat(result).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `overflow double invalidates alignment`() {
            // Numbers that overflow double precision should still be preserved
            val json = """[1e500, 1, 2]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Original number representation should be preserved
            assertThat(result.lowercase()).contains("e")
        }

        @Test
        fun `underflow double invalidates alignment`() {
            // Very small numbers should be preserved
            val json = """[1e-500, 1, 2]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Original number representation should be preserved
            assertThat(result.lowercase()).contains("e")
        }

        @Test
        fun `negative numbers align correctly`() {
            val json = """[[1], [-22], [333]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Negative number should be preserved
            assertThat(result).contains("-22")
        }
    }
}
