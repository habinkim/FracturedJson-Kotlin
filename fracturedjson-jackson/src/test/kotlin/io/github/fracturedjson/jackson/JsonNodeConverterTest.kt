package io.github.fracturedjson.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JsonNodeConverter")
class JsonNodeConverterTest {

    private val mapper = ObjectMapper()

    @Nested
    @DisplayName("Conversion")
    inner class Conversion {

        @Test
        fun `should convert null`() {
            val node = mapper.nullNode()
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.Null)
        }

        @Test
        fun `should convert boolean true`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(true)
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.True)
        }

        @Test
        fun `should convert boolean false`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(false)
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.False)
        }

        @Test
        fun `should convert integer`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(42)
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("42")
        }

        @Test
        fun `should convert decimal`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(3.14)
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
        }

        @Test
        fun `should convert string`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>("hello")
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.String)
            assertThat(item.value).contains("hello")
        }

        @Test
        fun `should convert array`() {
            val node = mapper.createArrayNode().apply {
                add(1)
                add(2)
                add(3)
            }
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.Array)
            assertThat(item.children).hasSize(3)
        }

        @Test
        fun `should convert object`() {
            val node = mapper.createObjectNode().apply {
                put("name", "John")
                put("age", 30)
            }
            val item = JsonNodeConverter.convert(node)
            assertThat(item.type).isEqualTo(JsonItemType.Object)
            assertThat(item.children).hasSize(2)
        }

        @Test
        fun `should convert with property name`() {
            val node = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>("value")
            val item = JsonNodeConverter.convert(node, "key")
            assertThat(item.name).isEqualTo("key")
        }
    }

    @Nested
    @DisplayName("Extension functions")
    inner class ExtensionFunctions {

        @Test
        fun `should format JsonNode`() {
            val node = mapper.createObjectNode().apply {
                put("key", "value")
            }
            val result = node.toFracturedJson()
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }

        @Test
        fun `should format with custom options`() {
            val node = mapper.createArrayNode().apply {
                add(1)
                add(2)
                add(3)
            }
            val options = FracturedJsonOptions(simpleBracketPadding = false)
            val result = node.toFracturedJson(options)
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should minify JsonNode`() {
            val node = mapper.createObjectNode().apply {
                put("name", "John")
                put("age", 30)
            }
            val result = node.minify()
            assertThat(result).doesNotContain(" ")
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should format JSON via ObjectMapper`() {
            val json = """{"name":"John","age":30}"""
            val result = mapper.formatJson(json)
            assertThat(result).contains("\"name\"")
        }

        @Test
        fun `should serialize and format object`() {
            data class Person(val name: String, val age: Int)
            val person = Person("John", 30)
            val result = mapper.writeValueAsFracturedJson(person)
            assertThat(result).contains("\"name\"")
            assertThat(result).contains("\"John\"")
        }

        @Test
        fun `should use formatWith DSL`() {
            val node = mapper.createArrayNode().apply { add(1); add(2); add(3) }
            val result = node.formatWith {
                simpleBracketPadding = false
            }
            assertThat(result).isEqualTo("[1, 2, 3]")
        }
    }

    @Nested
    @DisplayName("Complex structures")
    inner class ComplexStructures {

        @Test
        fun `should handle nested structures`() {
            val node = mapper.createObjectNode().apply {
                putObject("person").apply {
                    put("name", "John")
                    putArray("addresses").apply {
                        addObject().put("city", "NYC")
                        addObject().put("city", "LA")
                    }
                }
            }
            val item = JsonNodeConverter.convert(node)
            assertThat(item.complexity).isGreaterThan(1)
        }

        @Test
        fun `should format deeply nested structure`() {
            val node = mapper.createObjectNode().apply {
                putObject("level1").apply {
                    putObject("level2").apply {
                        putObject("level3").apply {
                            put("value", 42)
                        }
                    }
                }
            }
            val result = node.toFracturedJson()
            assertThat(result).contains("level1")
            assertThat(result).contains("level2")
            assertThat(result).contains("level3")
            assertThat(result).contains("42")
        }
    }
}
