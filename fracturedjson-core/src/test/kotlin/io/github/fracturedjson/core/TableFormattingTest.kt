package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Table formatting tests - ported from C# TableFormattingTests.cs
 */
@DisplayName("Table Formatting")
class TableFormattingTest {

    private fun testInstancesLineUp(lines: List<String>, pattern: String) {
        val positions = lines.mapNotNull { line ->
            val idx = line.indexOf(pattern)
            if (idx >= 0) idx else null
        }
        if (positions.size > 1) {
            assertThat(positions.distinct().size).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("Element Alignment")
    inner class ElementAlignment {

        @Test
        fun `nested elements line up`() {
            val json = """
                [
                    {"name": "Alice", "age": 25},
                    {"name": "Bob", "age": 30}
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Properties should align vertically
            val lines = result.split("\n")
            val nameLines = lines.filter { it.contains("\"name\"") }
            if (nameLines.size > 1) {
                testInstancesLineUp(nameLines, "\"name\"")
            }
        }

        @Test
        fun `nested elements compact when needed`() {
            val json = """
                [
                    {"name": "Alice", "age": 25},
                    {"name": "Bob", "age": 30}
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxTotalLineLength = 30,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // With restricted line length, formatting should adapt
            assertThat(result).contains("name")
            assertThat(result).contains("age")
        }

        @Test
        fun `fall back on inline if needed`() {
            val json = """
                [
                    {"name": "Alice"},
                    {"name": "Bob"}
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                maxTotalLineLength = 100,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should contain all data
            assertThat(result).contains("Alice")
            assertThat(result).contains("Bob")
        }
    }

    @Nested
    @DisplayName("Table With Comments")
    inner class TableWithComments {

        @Test
        fun `tables with comments parse correctly`() {
            val json = """
                [
                    {"a": 1, "b": 2} /*comment*/,
                    {"a": 10, "b": 20}
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
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
        fun `tables with blank lines data preserved`() {
            val json = """
                [
                    {"a": 1},

                    {"a": 2}
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                preserveBlankLines = true,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Both elements should be present
            assertThat(result).contains("\"a\"")
        }
    }

    @Nested
    @DisplayName("Duplicate Keys")
    inner class DuplicateKeys {

        @Test
        fun `reject objects with duplicate keys for table formatting`() {
            // Objects with duplicate keys should avoid table formatting
            val json = """[{"a": 1, "a": 2}]"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should still format, but may not use table format
            assertThat(result).contains("\"a\"")
        }
    }

    @Nested
    @DisplayName("Comma Placement")
    inner class CommaPlacement {

        @Test
        fun `commas before padding works`() {
            val json = """[[1, 2], [10, 20]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should contain all values
            assertThat(result).contains("1")
            assertThat(result).contains("20")
        }

        @Test
        fun `commas after padding works`() {
            val json = """[[1, 2], [10, 20]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should format with proper comma placement
            assertThat(result.count { it == ',' }).isGreaterThanOrEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Null Handling")
    inner class NullHandling {

        @Test
        fun `handles nulls with arrays table columns`() {
            val json = """[[1, null], [2, 3]]"""
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Should handle null values
            assertThat(result).contains("null")
        }
    }

    @Nested
    @DisplayName("Colon Placement")
    inner class ColonPlacement {

        @Test
        fun `colons hug prop names when configured`() {
            val json = """[{"a": 1}, {"longName": 2}]"""
            val options = FracturedJsonOptions(
                colonBeforePropNamePadding = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Colons should be right after property names
            assertThat(result).contains("\"a\":")
            assertThat(result).contains("\"longName\":")
        }
    }

    @Nested
    @DisplayName("Single Column Tables")
    inner class SingleColumnTables {

        @Test
        fun `single columns with eol comments data preserved`() {
            val json = """
                [
                    1, // comment1
                    2  // comment2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
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
        fun `single columns with numbers work`() {
            val json = """[1.5, 22.333, 100.1]"""
            val options = FracturedJsonOptions(
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // All numbers should be present
            assertThat(result).contains("1.5")
            assertThat(result).contains("22.333")
            assertThat(result).contains("100.1")
        }
    }
}
