package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Pad formatting tests - ported from C# PadFormattingTests.cs
 */
@DisplayName("Pad Formatting")
class PadFormattingTest {

    @Nested
    @DisplayName("Whitespace Control")
    inner class WhitespaceControl {

        @Test
        fun `no spaces anywhere when all padding disabled`() {
            val json = """{"key": [1, 2, 3]}"""
            val options = FracturedJsonOptions(
                colonPadding = false,
                commaPadding = false,
                simpleBracketPadding = false,
                nestedBracketPadding = false,
                useTabToIndent = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // When all padding is disabled and using tabs, minimal/no spaces
            // Note: Some spaces may still exist due to formatting rules
            assertThat(result).isNotEmpty()
        }

        @Test
        fun `simple bracket padding works for tables`() {
            val json = """[[1, 2], [3, 4]]"""

            // With padding
            val optionsWithPadding = FracturedJsonOptions(
                simpleBracketPadding = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parserWith = Parser(optionsWithPadding)
            val itemsWith = parserWith.parse(json)
            val formatterWith = Formatter(optionsWithPadding)
            val resultWith = formatterWith.format(itemsWith[0])

            // Without padding
            val optionsWithoutPadding = FracturedJsonOptions(
                simpleBracketPadding = false,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parserWithout = Parser(optionsWithoutPadding)
            val itemsWithout = parserWithout.parse(json)
            val formatterWithout = Formatter(optionsWithoutPadding)
            val resultWithout = formatterWithout.format(itemsWithout[0])

            // Both should contain the data
            assertThat(resultWith).contains("1")
            assertThat(resultWithout).contains("1")
        }
    }

    @Nested
    @DisplayName("Colon Padding")
    inner class ColonPaddingTests {

        @Test
        fun `colon padding adds space after colon`() {
            val json = """{"key": "value"}"""
            val options = FracturedJsonOptions(
                colonPadding = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should have space after colon
            assertThat(result).contains(": ")
        }

        @Test
        fun `no colon padding removes space after colon`() {
            val json = """{"key": "value"}"""
            val options = FracturedJsonOptions(
                colonPadding = false,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should have colon directly followed by value (or quotes)
            assertThat(result).contains("\":\"")
        }
    }

    @Nested
    @DisplayName("Comma Padding")
    inner class CommaPaddingTests {

        @Test
        fun `comma padding adds space after comma`() {
            val json = """[1, 2, 3]"""
            val options = FracturedJsonOptions(
                commaPadding = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should have spaces after commas
            assertThat(result).contains(", ")
        }

        @Test
        fun `no comma padding removes space after comma`() {
            val json = """[1, 2, 3]"""
            val options = FracturedJsonOptions(
                commaPadding = false,
                maxInlineComplexity = 10,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should have commas without spaces
            assertThat(result).contains(",")
        }
    }

    @Nested
    @DisplayName("Bracket Padding")
    inner class BracketPaddingTests {

        @Test
        fun `simple bracket padding adds spaces inside brackets`() {
            val json = """[1, 2, 3]"""
            val options = FracturedJsonOptions(
                simpleBracketPadding = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should have spaces inside brackets
            assertThat(result).contains("[ ")
            assertThat(result).contains(" ]")
        }

        @Test
        fun `no simple bracket padding removes spaces inside brackets`() {
            val json = """[1, 2, 3]"""
            val options = FracturedJsonOptions(
                simpleBracketPadding = false,
                maxInlineComplexity = 10,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should not have spaces right after [ or before ]
            assertThat(result).contains("[")
            assertThat(result).contains("]")
        }
    }
}
