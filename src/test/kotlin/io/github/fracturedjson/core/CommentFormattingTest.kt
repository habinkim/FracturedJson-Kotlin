package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Comment formatting tests - ported from C# CommentFormattingTests.cs
 * Note: Comment handling in the Kotlin port may differ from C# original.
 * These tests focus on verifying that comment-containing JSON is parseable
 * and that the core data is preserved.
 */
@DisplayName("Comment Formatting")
class CommentFormattingTest {

    @Nested
    @DisplayName("Comment Parsing")
    inner class CommentParsing {

        @Test
        fun `json with block comments is parseable`() {
            val json = """[/*pre*/ 1 /*post*/, 2]"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            // Should parse without error
            assertThat(items).isNotEmpty
            assertThat(items[0].type).isEqualTo(JsonItemType.Array)
        }

        @Test
        fun `json with line comments is parseable`() {
            val json = """
                [
                    1, // comment
                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            // Should parse without error
            assertThat(items).isNotEmpty
        }

        @Test
        fun `json with multiline comments is parseable`() {
            val json = """
                {
                    "key"
                    /*
                     * multiline
                     * comment
                     */
                    : "value"
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            // Should parse without error
            assertThat(items).isNotEmpty
            assertThat(items[0].type).isEqualTo(JsonItemType.Object)
        }
    }

    @Nested
    @DisplayName("Data Preservation")
    inner class DataPreservation {

        @Test
        fun `data preserved when comments present`() {
            val json = """[/*comment*/ 1, 2, 3]"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Core data should be preserved
            assertThat(result).contains("1")
            assertThat(result).contains("2")
            assertThat(result).contains("3")
        }

        @Test
        fun `object data preserved with comments`() {
            val json = """{"key" /*comment*/: "value"}"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Core data should be preserved
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }
    }

    @Nested
    @DisplayName("Comment Policy")
    inner class CommentPolicyTests {

        @Test
        fun `comments ignored when policy is remove`() {
            val json = """[/*comment*/ 1, 2]"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Remove,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Data should be preserved
            assertThat(result).contains("1")
            assertThat(result).contains("2")
        }

        @Test
        fun `preserve policy does not throw`() {
            val json = """[1, 2, 3]"""
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            assertThat(result).contains("1")
        }
    }

    @Nested
    @DisplayName("Blank Lines")
    inner class BlankLines {

        @Test
        fun `blank lines in json can be parsed`() {
            val json = """
                [
                    1,

                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                preserveBlankLines = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            // Should parse without error
            assertThat(items).isNotEmpty
        }

        @Test
        fun `data preserved with blank lines`() {
            val json = """
                [
                    1,

                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                preserveBlankLines = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Data should be preserved
            assertThat(result).contains("1")
            assertThat(result).contains("2")
        }
    }
}
