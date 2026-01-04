package io.github.fracturedjson.parser

import io.github.fracturedjson.core.CommentPolicy
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Parser")
class ParserTest {

    private val parser = Parser()

    @Nested
    @DisplayName("Primitive values")
    inner class PrimitiveValues {

        @Test
        fun `should parse null`() {
            val result = parser.parse("null")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Null)
            assertThat(result[0].value).isEqualTo("null")
        }

        @Test
        fun `should parse true`() {
            val result = parser.parse("true")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.True)
            assertThat(result[0].value).isEqualTo("true")
        }

        @Test
        fun `should parse false`() {
            val result = parser.parse("false")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.False)
            assertThat(result[0].value).isEqualTo("false")
        }

        @Test
        fun `should parse integer`() {
            val result = parser.parse("42")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].value).isEqualTo("42")
        }

        @Test
        fun `should parse negative number`() {
            val result = parser.parse("-123")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].value).isEqualTo("-123")
        }

        @Test
        fun `should parse decimal number`() {
            val result = parser.parse("3.14159")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].value).isEqualTo("3.14159")
        }

        @Test
        fun `should parse scientific notation`() {
            val result = parser.parse("1.5e10")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].value).isEqualTo("1.5e10")
        }

        @Test
        fun `should parse string`() {
            val result = parser.parse("\"hello world\"")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.String)
            assertThat(result[0].value).isEqualTo("\"hello world\"")
        }

        @Test
        fun `should parse string with escape sequences`() {
            val result = parser.parse("\"line1\\nline2\"")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.String)
        }
    }

    @Nested
    @DisplayName("Arrays")
    inner class Arrays {

        @Test
        fun `should parse empty array`() {
            val result = parser.parse("[]")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Array)
            assertThat(result[0].children).isEmpty()
        }

        @Test
        fun `should parse array with single element`() {
            val result = parser.parse("[1]")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Array)
            assertThat(result[0].children).hasSize(1)
            assertThat(result[0].children[0].type).isEqualTo(JsonItemType.Number)
        }

        @Test
        fun `should parse array with multiple elements`() {
            val result = parser.parse("[1, 2, 3]")
            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(3)
        }

        @Test
        fun `should parse nested arrays`() {
            val result = parser.parse("[[1, 2], [3, 4]]")
            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(2)
            assertThat(result[0].children[0].type).isEqualTo(JsonItemType.Array)
            assertThat(result[0].children[0].children).hasSize(2)
        }

        @Test
        fun `should parse array with trailing comma when allowed`() {
            val options = FracturedJsonOptions(allowTrailingCommas = true)
            val localParser = Parser(options)
            val result = localParser.parse("[1, 2, 3,]")
            assertThat(result[0].children).hasSize(3)
        }

        @Test
        fun `should reject trailing comma by default`() {
            assertThatThrownBy { parser.parse("[1, 2,]") }
                .isInstanceOf(Exception::class.java)
        }
    }

    @Nested
    @DisplayName("Objects")
    inner class Objects {

        @Test
        fun `should parse empty object`() {
            val result = parser.parse("{}")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Object)
            assertThat(result[0].children).isEmpty()
        }

        @Test
        fun `should parse object with single property`() {
            val result = parser.parse("""{"key": "value"}""")
            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(1)
            assertThat(result[0].children[0].name).isEqualTo("key")
        }

        @Test
        fun `should parse object with multiple properties`() {
            val result = parser.parse("""{"name": "John", "age": 30}""")
            assertThat(result).hasSize(1)
            assertThat(result[0].children).hasSize(2)
        }

        @Test
        fun `should parse nested objects`() {
            val result = parser.parse("""{"person": {"name": "John"}}""")
            assertThat(result).hasSize(1)
            assertThat(result[0].children[0].type).isEqualTo(JsonItemType.Object)
            assertThat(result[0].children[0].name).isEqualTo("person")
        }
    }

    @Nested
    @DisplayName("Comments")
    inner class Comments {

        @Test
        fun `should reject comments by default`() {
            assertThatThrownBy { parser.parse("// comment\n{}") }
                .isInstanceOf(Exception::class.java)
        }

        @Test
        fun `should preserve line comments when configured`() {
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Preserve)
            val localParser = Parser(options)
            val result = localParser.parse("// comment\n{}")
            // Comment should be attached or preserved
            assertThat(result).isNotEmpty()
        }

        @Test
        fun `should remove comments when configured`() {
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Remove)
            val localParser = Parser(options)
            val result = localParser.parse("// comment\n{}")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Object)
        }

        @Test
        fun `should handle block comments`() {
            val options = FracturedJsonOptions(commentPolicy = CommentPolicy.Remove)
            val localParser = Parser(options)
            val result = localParser.parse("/* comment */ {}")
            assertThat(result).hasSize(1)
            assertThat(result[0].type).isEqualTo(JsonItemType.Object)
        }
    }

    @Nested
    @DisplayName("Complex structures")
    inner class ComplexStructures {

        @Test
        fun `should parse mixed array`() {
            val result = parser.parse("""[1, "two", true, null, {"key": "value"}]""")
            assertThat(result[0].children).hasSize(5)
            assertThat(result[0].children[0].type).isEqualTo(JsonItemType.Number)
            assertThat(result[0].children[1].type).isEqualTo(JsonItemType.String)
            assertThat(result[0].children[2].type).isEqualTo(JsonItemType.True)
            assertThat(result[0].children[3].type).isEqualTo(JsonItemType.Null)
            assertThat(result[0].children[4].type).isEqualTo(JsonItemType.Object)
        }

        @Test
        fun `should parse deeply nested structure`() {
            val json = """{"a": {"b": {"c": {"d": 1}}}}"""
            val result = parser.parse(json)
            var current = result[0]
            for (depth in 0 until 4) {
                assertThat(current.type).isIn(JsonItemType.Object)
                if (current.children.isNotEmpty()) {
                    current = current.children[0]
                }
            }
        }
    }
}
