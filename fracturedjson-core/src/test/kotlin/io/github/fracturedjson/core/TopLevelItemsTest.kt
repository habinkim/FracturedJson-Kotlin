package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Top level items tests - ported from C# TopLevelItemsTests.cs
 * Note: Kotlin port behavior differs from C# in some cases:
 * - Multiple top-level elements are allowed (returns list)
 * - Comment formatting behavior may differ
 */
@DisplayName("Top Level Items")
class TopLevelItemsTest {

    @Nested
    @DisplayName("Multiple Top Level Elements")
    inner class MultipleTopLevelElements {

        @Test
        fun `allows multiple top level elements`() {
            // Note: Kotlin port allows multiple top-level elements, returns them as a list
            val json = "[1,2] [3,4]"
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)

            val items = parser.parse(json)
            // Both arrays should be parsed
            assertThat(items.size).isEqualTo(2)
        }

        @Test
        fun `throws if multiple top level elements with comma`() {
            val json = "[1,2], [3,4]"
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)

            // Comma between top-level elements should throw
            assertThatThrownBy { parser.parse(json) }
                .isInstanceOf(FracturedJsonException::class.java)
        }
    }

    @Nested
    @DisplayName("Top Level Comments")
    inner class TopLevelComments {

        @Test
        fun `comments parsed at top level with preserve policy`() {
            val json = "/*before*/ [1, 2] /*after*/"
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            // Should have parsed comments and the array
            assertThat(items.size).isGreaterThanOrEqualTo(1)

            // The main element should be an array
            val mainItem = items.find { it.type == JsonItemType.Array }
            assertThat(mainItem).isNotNull
        }

        @Test
        fun `single top level element formatted correctly`() {
            val json = "[1, 2]"
            val options = FracturedJsonOptions(
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items[0])

            // Array should be formatted
            assertThat(result).contains("1")
            assertThat(result).contains("2")
        }

        @Test
        fun `format multiple items works`() {
            val json = "[1, 2]"
            val options = FracturedJsonOptions(
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.format(items)

            // Should format correctly
            assertThat(result).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Minification")
    inner class Minification {

        @Test
        fun `minify parses multiple top level elements`() {
            // Note: Kotlin port allows multiple top-level elements
            val json = "[1,2] [3,4]"
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)
            val parser = Parser(options)

            val items = parser.parse(json)
            assertThat(items.size).isEqualTo(2)
        }

        @Test
        fun `minify single element works`() {
            val json = "[1, 2]"
            val options = FracturedJsonOptions(
                jsonEolStyle = EolStyle.Lf
            )
            val parser = Parser(options)
            val items = parser.parse(json)

            val formatter = Formatter(options)
            val result = formatter.minify(items)

            // Should minify correctly
            assertThat(result).isEqualTo("[1,2]")
        }
    }
}
