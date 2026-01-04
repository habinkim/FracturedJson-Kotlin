package io.github.fracturedjson.parser

import io.github.fracturedjson.core.FracturedJsonException
import io.github.fracturedjson.parser.tokenizing.TokenScanner
import io.github.fracturedjson.parser.tokenizing.TokenType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Comprehensive tests for TokenScanner - ported from C# TokenizerTests.cs
 */
@DisplayName("TokenScanner")
class TokenScannerTest {

    @Nested
    @DisplayName("Single Token Echo")
    inner class SingleTokenEcho {

        @ParameterizedTest
        @ValueSource(strings = ["{}", "[]", "\"hello\"", "123", "-45.67", "1.5e10", "true", "false", "null"])
        fun `should echo single token`(input: String) {
            val tokens = TokenScanner.scan(input).toList()

            // Filter out structural tokens for simple values
            val contentTokens = tokens.filter {
                it.type != TokenType.BeginObject && it.type != TokenType.EndObject &&
                it.type != TokenType.BeginArray && it.type != TokenType.EndArray
            }

            if (input == "{}" || input == "[]") {
                assertThat(tokens).hasSize(2)
            } else {
                assertThat(contentTokens).hasSize(1)
                assertThat(contentTokens[0].text).isEqualTo(input)
            }
        }

        @Test
        fun `should tokenize empty object`() {
            val tokens = TokenScanner.scan("{}").toList()
            assertThat(tokens).hasSize(2)
            assertThat(tokens[0].type).isEqualTo(TokenType.BeginObject)
            assertThat(tokens[1].type).isEqualTo(TokenType.EndObject)
        }

        @Test
        fun `should tokenize empty array`() {
            val tokens = TokenScanner.scan("[]").toList()
            assertThat(tokens).hasSize(2)
            assertThat(tokens[0].type).isEqualTo(TokenType.BeginArray)
            assertThat(tokens[1].type).isEqualTo(TokenType.EndArray)
        }

        @Test
        fun `should tokenize string with escapes`() {
            val input = "\"hello\\nworld\\t!\""
            val tokens = TokenScanner.scan(input).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].type).isEqualTo(TokenType.String)
            assertThat(tokens[0].text).isEqualTo(input)
        }

        @Test
        fun `should tokenize line comment`() {
            val input = "// this is a comment"
            val tokens = TokenScanner.scan(input).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].type).isEqualTo(TokenType.LineComment)
        }

        @Test
        fun `should tokenize block comment`() {
            val input = "/* block comment */"
            val tokens = TokenScanner.scan(input).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].type).isEqualTo(TokenType.BlockComment)
        }
    }

    @Nested
    @DisplayName("Position Tracking")
    inner class PositionTracking {

        @Test
        fun `should track correct position for second token`() {
            val input = "[   42]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens).hasSize(3)
            assertThat(tokens[0].type).isEqualTo(TokenType.BeginArray)
            assertThat(tokens[0].inputPosition.index).isEqualTo(0)

            assertThat(tokens[1].type).isEqualTo(TokenType.Number)
            assertThat(tokens[1].inputPosition.index).isEqualTo(4)
            assertThat(tokens[1].inputPosition.column).isEqualTo(4)
        }

        @Test
        fun `should track position across newlines`() {
            val input = "[\n  42\n]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens).hasSize(3)
            // Number should be on row 1 (0-indexed), column 2
            assertThat(tokens[1].inputPosition.row).isEqualTo(1)
            assertThat(tokens[1].inputPosition.column).isEqualTo(2)
        }

        @Test
        fun `should track position with CRLF`() {
            val input = "[\r\n  42\r\n]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens).hasSize(3)
            assertThat(tokens[1].inputPosition.row).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @ParameterizedTest
        @ValueSource(strings = [
            "\"unterminated string",
            "\"string with\nnewline\"",
            "1.2.3",
            "1e",
            "1e+",
            "truee",
            "nul"
        ])
        fun `should throw for malformed tokens`(input: String) {
            assertThatThrownBy { TokenScanner.scan(input).toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }

        @Test
        fun `should throw for unterminated block comment`() {
            assertThatThrownBy { TokenScanner.scan("/* unterminated").toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }

        @Test
        fun `should throw for invalid character`() {
            assertThatThrownBy { TokenScanner.scan("[1, @, 2]").toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }

        @Test
        fun `should report correct position for error`() {
            try {
                TokenScanner.scan("[1, @, 2]").toList()
            } catch (e: FracturedJsonException) {
                assertThat(e.inputPosition?.column).isEqualTo(4)
            }
        }
    }

    @Nested
    @DisplayName("Complex Structures")
    inner class ComplexStructures {

        @Test
        fun `should tokenize array with mixed values`() {
            val input = "[1, \"two\", true, null, 3.14]"
            val tokens = TokenScanner.scan(input).toList()

            val types = tokens.map { it.type }
            assertThat(types).containsExactly(
                TokenType.BeginArray,
                TokenType.Number,
                TokenType.Comma,
                TokenType.String,
                TokenType.Comma,
                TokenType.True,
                TokenType.Comma,
                TokenType.Null,
                TokenType.Comma,
                TokenType.Number,
                TokenType.EndArray
            )
        }

        @Test
        fun `should tokenize nested object`() {
            val input = """{"a": {"b": 1}}"""
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.filter { it.type == TokenType.BeginObject }).hasSize(2)
            assertThat(tokens.filter { it.type == TokenType.EndObject }).hasSize(2)
        }

        @Test
        fun `should tokenize with comments`() {
            val input = """
                {
                    // line comment
                    "key": /* block */ "value"
                }
            """.trimIndent()
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.any { it.type == TokenType.LineComment }).isTrue()
            assertThat(tokens.any { it.type == TokenType.BlockComment }).isTrue()
        }

        @Test
        fun `should handle blank lines`() {
            val input = "[\n\n1\n\n]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.filter { it.type == TokenType.BlankLine }).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Number Formats")
    inner class NumberFormats {

        @ParameterizedTest
        @ValueSource(strings = [
            "0", "1", "42", "123456789",
            "-0", "-1", "-42",
            "0.0", "0.123", "123.456",
            "-0.0", "-123.456",
            "1e5", "1E5", "1e+5", "1e-5",
            "1.5e10", "1.5E-10",
            "-1.5e+10"
        ])
        fun `should tokenize valid numbers`(input: String) {
            val tokens = TokenScanner.scan(input).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].type).isEqualTo(TokenType.Number)
            assertThat(tokens[0].text).isEqualTo(input)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "+1",       // Leading plus not allowed
            "01",       // Leading zero not allowed
            "1.",       // Trailing decimal not allowed
            ".5",       // Leading decimal not allowed
            "1e",       // Incomplete exponent
            "1e+"       // Incomplete exponent sign
        ])
        fun `should reject invalid numbers`(input: String) {
            assertThatThrownBy { TokenScanner.scan(input).toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }
    }

    @Nested
    @DisplayName("String Escapes")
    inner class StringEscapes {

        @ParameterizedTest
        @CsvSource(
            "\"\\\"\"  , \\\"",
            "\"\\\\\"  , \\\\",
            "\"\\/\"   , \\/",
            "\"\\b\"   , \\b",
            "\"\\f\"   , \\f",
            "\"\\n\"   , \\n",
            "\"\\r\"   , \\r",
            "\"\\t\"   , \\t"
        )
        fun `should tokenize escape sequences`(input: String, expected: String) {
            val tokens = TokenScanner.scan(input.trim()).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].text).contains(expected.trim())
        }

        @Test
        fun `should tokenize unicode escape`() {
            val input = "\"\\u0041\""
            val tokens = TokenScanner.scan(input).toList()
            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].text).contains("\\u0041")
        }

        @Test
        fun `should reject invalid escape`() {
            assertThatThrownBy { TokenScanner.scan("\"\\x\"").toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }

        @Test
        fun `should reject incomplete unicode escape`() {
            assertThatThrownBy { TokenScanner.scan("\"\\u00\"").toList() }
                .isInstanceOf(FracturedJsonException::class.java)
        }
    }

    @Nested
    @DisplayName("Whitespace Handling")
    inner class WhitespaceHandling {

        @Test
        fun `should skip whitespace between tokens`() {
            val input = "[  1  ,  2  ,  3  ]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.filter { it.type == TokenType.Number }).hasSize(3)
        }

        @Test
        fun `should handle tabs`() {
            val input = "[\t1\t,\t2\t]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.filter { it.type == TokenType.Number }).hasSize(2)
        }

        @Test
        fun `should handle mixed whitespace`() {
            val input = "[ \t\n\r\n 1 \t\n ]"
            val tokens = TokenScanner.scan(input).toList()

            val numbers = tokens.filter { it.type == TokenType.Number }
            assertThat(numbers).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should handle empty input`() {
            val tokens = TokenScanner.scan("").toList()
            assertThat(tokens).isEmpty()
        }

        @Test
        fun `should handle whitespace only`() {
            val tokens = TokenScanner.scan("   \t\n   ").toList()
            // May contain blank lines
            assertThat(tokens.all { it.type == TokenType.BlankLine }).isTrue()
        }

        @Test
        fun `should tokenize deeply nested structure`() {
            val input = "[[[[[[1]]]]]]"
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens.filter { it.type == TokenType.BeginArray }).hasSize(6)
            assertThat(tokens.filter { it.type == TokenType.EndArray }).hasSize(6)
        }

        @Test
        fun `should handle very long string`() {
            val longString = "\"${"a".repeat(10000)}\""
            val tokens = TokenScanner.scan(longString).toList()

            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].type).isEqualTo(TokenType.String)
        }

        @Test
        fun `should handle unicode in strings`() {
            val input = "\"한글 日本語 中文\""
            val tokens = TokenScanner.scan(input).toList()

            assertThat(tokens).hasSize(1)
            assertThat(tokens[0].text).contains("한글")
        }
    }
}
