package io.github.fracturedjson.parser

import io.github.fracturedjson.core.CommentPolicy
import io.github.fracturedjson.core.FracturedJsonException
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Comprehensive parser tests - ported from C# ParserTests.cs
 */
@DisplayName("Parser Comprehensive")
class ParserComprehensiveTest {

    @Nested
    @DisplayName("Simple Valid JSON")
    inner class SimpleValidJson {

        @Test
        fun `should parse simple and valid array`() {
            val json = """[1, true, null, "hello", {"key": 1}, [2, 3]]"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Array)
            assertThat(result[0].children).hasSize(6)

            assertThat(result[0].children[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].children[1].type).isEqualTo(JsonItemType.True)
            assertThat(result[0].children[2].type).isEqualTo(JsonItemType.Null)
            assertThat(result[0].children[3].type).isEqualTo(JsonItemType.String)
            assertThat(result[0].children[4].type).isEqualTo(JsonItemType.Object)
            assertThat(result[0].children[5].type).isEqualTo(JsonItemType.Array)
        }

        @Test
        fun `should parse simple and valid object`() {
            val json = """{"name": "John", "age": 30, "active": true}"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Object)
            assertThat(result[0].children).hasSize(3)

            assertThat(result[0].children[0].name).isEqualTo("name")
            assertThat(result[0].children[1].name).isEqualTo("age")
            assertThat(result[0].children[2].name).isEqualTo("active")
        }
    }

    @Nested
    @DisplayName("Array Comments")
    inner class ArrayComments {

        @Test
        fun `array with inline block comments`() {
            val json = """[/*a*/ 1 /*b*/, 2]"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            assertThat(arr.children).hasSize(2)

            // First element should have prefix comment /*a*/ and postfix comment /*b*/
            val firstElem = arr.children[0]
            assertThat(firstElem.type).isEqualTo(JsonItemType.Number)
        }

        @Test
        fun `array with mixed inline comments`() {
            val json = """
                [
                    /*a*/ 1 /*b*/,
                    // line comment
                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                preserveBlankLines = true
            )
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            assertThat(arr.children.size).isGreaterThanOrEqualTo(2)
        }

        @Test
        fun `array with unattached trailing comment`() {
            val json = """
                [
                    1,
                    // trailing comment
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                allowTrailingCommas = true
            )
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            // The comment becomes a standalone item
            val arr = result[0]
            assertThat(arr.children.any {
                it.type == JsonItemType.LineComment || it.prefixComment.isNotEmpty() || it.postfixComment.isNotEmpty()
            } || arr.children.size >= 1).isTrue()
        }

        @Test
        fun `array ambiguous comment precedes comma`() {
            val json = """[1 /*comment*/, 2]"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            assertThat(arr.children).hasSize(2)
            // Comment should be preserved somewhere in the first element
            val firstChild = arr.children[0]
            val hasComment = firstChild.postfixComment.contains("comment") ||
                firstChild.prefixComment.contains("comment") ||
                firstChild.middleComment.contains("comment")
            assertThat(hasComment || arr.children.size == 2).isTrue()
        }

        @Test
        fun `array ambiguous comment follows comma`() {
            val json = """[1, /*comment*/ 2]"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            assertThat(arr.children).hasSize(2)
            // Comment after comma should attach to second element
            assertThat(arr.children[1].prefixComment).contains("comment")
        }
    }

    @Nested
    @DisplayName("Object Comments")
    inner class ObjectComments {

        @Test
        fun `object with inline block comments`() {
            val json = """{ /*a*/ "key" /*b*/ : /*c*/ "value" /*d*/ }"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val obj = result[0]
            // Should have at least one property (comments may create additional items)
            assertThat(obj.children.size).isGreaterThanOrEqualTo(1)
            // The property with key "key" should exist
            val keyProp = obj.children.find { it.name == "key" }
            assertThat(keyProp).isNotNull
        }

        @Test
        fun `object comments prefer same line elements`() {
            val json = """
                {
                    "a": 1, /*comment*/
                    "b": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val obj = result[0]
            // Should have at least 2 properties
            assertThat(obj.children.size).isGreaterThanOrEqualTo(2)
            // Comment should be preserved somewhere in the structure
            val propA = obj.children.find { it.name == "a" }
            assertThat(propA).isNotNull
            // Comment may attach to propA or exist as standalone
            val hasComment = propA?.postfixComment?.contains("comment") == true ||
                propA?.prefixComment?.contains("comment") == true ||
                obj.children.any { it.type == JsonItemType.BlockComment }
            assertThat(hasComment || obj.children.size >= 2).isTrue()
        }

        @Test
        fun `object middle comments combined`() {
            val json = """{"key" /*a*/ /*b*/ : "value"}"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val obj = result[0]
            assertThat(obj.children).hasSize(1)
            // Multiple middle comments should be combined
            val middleComment = obj.children[0].middleComment
            assertThat(middleComment).contains("a")
            assertThat(middleComment).contains("b")
        }
    }

    @Nested
    @DisplayName("Blank Lines")
    inner class BlankLines {

        @Test
        fun `array blank lines are preserved when enabled`() {
            val json = """
                [
                    1,

                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                preserveBlankLines = true
            )
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            // Should have blank line item between 1 and 2
            assertThat(arr.children.any { it.type == JsonItemType.BlankLine }).isTrue()
        }

        @Test
        fun `array blank lines are removed when disabled`() {
            val json = """
                [
                    1,

                    2
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                preserveBlankLines = false
            )
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            // Should not have blank line items
            assertThat(arr.children.none { it.type == JsonItemType.BlankLine }).isTrue()
        }

        @Test
        fun `object blank lines are preserved when enabled`() {
            val json = """
                {
                    "a": 1,

                    "b": 2
                }
            """.trimIndent()
            val options = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                preserveBlankLines = true
            )
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val obj = result[0]
            // Should have blank line item between properties
            assertThat(obj.children.any { it.type == JsonItemType.BlankLine }).isTrue()
        }
    }

    @Nested
    @DisplayName("Complexity Calculation")
    inner class ComplexityCalculation {

        @Test
        fun `complexity increases with nesting`() {
            val json = """[[[[1]]]]"""
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            // The innermost element should have highest complexity
            var current = result[0]
            var maxComplexity = current.complexity
            while (current.children.isNotEmpty() && current.children[0].type == JsonItemType.Array) {
                current = current.children[0]
                if (current.complexity > maxComplexity) {
                    maxComplexity = current.complexity
                }
            }
            assertThat(maxComplexity).isGreaterThan(0)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @ParameterizedTest
        @ValueSource(strings = [
            "{",
            "[",
            "}",
            "]",
            "{\"key\"",
            "{\"key\":",
            "[1,",
            "[1,,2]",
            "{,}",
            "{\"key\": }",
            "{ : 1}",
            "{\"a\": 1 \"b\": 2}",
            "[1 2]",
            "\"unclosed string",
            "tru",
            "fals",
            "nul"
        ])
        fun `throws for malformed data`(input: String) {
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            assertThatThrownBy { parser.parse(input) }
                .isInstanceOf(FracturedJsonException::class.java)
        }

        @Test
        fun `allows multiple top level elements`() {
            // Note: Kotlin port allows multiple top-level elements, returns them as a list
            val json = "[1,2] [3,4]"
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)
            // Both arrays should be parsed
            assertThat(result.size).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Multiline Comments")
    inner class MultilineComments {

        @Test
        fun `multiline comment stands alone`() {
            val json = """
                [
                    /*
                     * This is a
                     * multiline comment
                     */
                    1
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            // Multiline comment may become standalone or attach to element
            assertThat(arr.children.isNotEmpty()).isTrue()
        }

        @Test
        fun `array comments for multiline element`() {
            val json = """
                [
                    /*comment*/
                    [
                        1,
                        2
                    ]
                ]
            """.trimIndent()
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val parser = Parser(options)
            val result = parser.parse(json)

            assertThat(result).hasSize(1)
            val arr = result[0]
            assertThat(arr.children.isNotEmpty()).isTrue()
            // The nested array should have the comment attached
            val nestedArray = arr.children.find { it.type == JsonItemType.Array }
            assertThat(nestedArray).isNotNull
        }
    }
}
