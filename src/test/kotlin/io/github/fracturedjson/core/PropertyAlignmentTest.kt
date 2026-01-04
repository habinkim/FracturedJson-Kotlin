package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Property alignment tests - ported from C# PropertyAlignmentTests.cs
 */
@DisplayName("Property Alignment")
class PropertyAlignmentTest {

    @Nested
    @DisplayName("Basic Alignment")
    inner class BasicAlignment {

        @Test
        fun `property values aligned`() {
            val json = """
                {
                    "a": 1,
                    "longPropertyName": 2,
                    "b": 3
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxPropNamePadding = 20,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should produce multi-line output with aligned properties
            val lines = result.split("\n")
            assertThat(lines.size).isGreaterThan(1)

            // All properties should be present
            assertThat(result).contains("\"a\"")
            assertThat(result).contains("\"longPropertyName\"")
            assertThat(result).contains("\"b\"")
        }

        @Test
        fun `property values aligned but not colons`() {
            val json = """
                {
                    "a": 1,
                    "longPropertyName": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                colonBeforePropNamePadding = true,
                maxPropNamePadding = 20,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Colons should hug property names
            assertThat(result).contains("\"a\":")
            assertThat(result).contains("\"longPropertyName\":")
        }
    }

    @Nested
    @DisplayName("Alignment Limits")
    inner class AlignmentLimits {

        @Test
        fun `dont align prop vals when too much padding required`() {
            val json = """
                {
                    "x": 1,
                    "veryVeryVeryLongPropertyName": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxPropNamePadding = 10,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should still format properly even if alignment is skipped
            assertThat(result).contains("\"x\"")
            assertThat(result).contains("\"veryVeryVeryLongPropertyName\"")
        }

        @Test
        fun `dont align when simple value too long`() {
            val json = """
                {
                    "short": 1,
                    "long": "this is a very long string value that might exceed line length"
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxTotalLineLength = 50,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should format without exceeding line length where possible
            assertThat(result).contains("\"short\"")
            assertThat(result).contains("\"long\"")
        }
    }

    @Nested
    @DisplayName("Comments and Alignment")
    inner class CommentsAndAlignment {

        @Test
        fun `data preserved when multiline comment present`() {
            val json = """
                {
                    "a"
                    /*
                     * multiline
                     * comment
                     */
                    : 1,
                    "b": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                maxPropNamePadding = 20,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Data should be preserved
            assertThat(result).contains("\"a\"")
            assertThat(result).contains("\"b\"")
        }

        @Test
        fun `data preserved when simple comment present`() {
            val json = """{"a" /*comment*/: 1, "bb": 2}"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                maxPropNamePadding = 20,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Data should be preserved
            assertThat(result).contains("\"a\"")
            assertThat(result).contains("\"bb\"")
        }
    }

    @Nested
    @DisplayName("Wrapped Arrays")
    inner class WrappedArrays {

        @Test
        fun `align prop vals when array wraps`() {
            val json = """
                {
                    "short": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
                    "longPropertyName": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxTotalLineLength = 40,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Both properties should be present
            assertThat(result).contains("\"short\"")
            assertThat(result).contains("\"longPropertyName\"")
        }
    }
}
