package io.github.fracturedjson.kotlinx

import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import kotlinx.serialization.json.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JsonElementConverter")
class JsonElementConverterTest {

    @Nested
    @DisplayName("Conversion")
    inner class Conversion {

        @Test
        fun `should convert null`() {
            val element = JsonNull
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Null)
        }

        @Test
        fun `should convert boolean true`() {
            val element = JsonPrimitive(true)
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.True)
        }

        @Test
        fun `should convert boolean false`() {
            val element = JsonPrimitive(false)
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.False)
        }

        @Test
        fun `should convert number`() {
            val element = JsonPrimitive(42)
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("42")
        }

        @Test
        fun `should convert string`() {
            val element = JsonPrimitive("hello")
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.String)
        }

        @Test
        fun `should convert array`() {
            val element = buildJsonArray {
                add(1)
                add(2)
                add(3)
            }
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Array)
            assertThat(item.children).hasSize(3)
        }

        @Test
        fun `should convert object`() {
            val element = buildJsonObject {
                put("name", "John")
                put("age", 30)
            }
            val item = JsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Object)
            assertThat(item.children).hasSize(2)
            assertThat(item.children[0].name).isEqualTo("name")
        }

        @Test
        fun `should convert with property name`() {
            val element = JsonPrimitive("value")
            val item = JsonElementConverter.convert(element, "key")
            assertThat(item.name).isEqualTo("key")
        }
    }

    @Nested
    @DisplayName("Extension functions")
    inner class ExtensionFunctions {

        @Test
        fun `should format JsonElement`() {
            val element = buildJsonObject {
                put("key", "value")
            }
            val result = element.toFracturedJson()
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }

        @Test
        fun `should format with custom options`() {
            val element = buildJsonArray {
                add(1)
                add(2)
                add(3)
            }
            val options = FracturedJsonOptions(simpleBracketPadding = false)
            val result = element.toFracturedJson(options)
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should minify JsonElement`() {
            val element = buildJsonObject {
                put("name", "John")
                put("age", 30)
            }
            val result = element.minify()
            assertThat(result).doesNotContain(" ")
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should reformat JSON string`() {
            val json = """{"name":"John","age":30}"""
            val result = json.reformatJson()
            assertThat(result).contains("\"name\"")
        }

        @Test
        fun `should use formatWith DSL`() {
            val element = buildJsonArray { add(1); add(2); add(3) }
            val result = element.formatWith {
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
            val element = buildJsonObject {
                put("person", buildJsonObject {
                    put("name", "John")
                    put("addresses", buildJsonArray {
                        add(buildJsonObject { put("city", "NYC") })
                        add(buildJsonObject { put("city", "LA") })
                    })
                })
            }
            val item = JsonElementConverter.convert(element)
            assertThat(item.complexity).isGreaterThan(1)
        }

        @Test
        fun `should format deeply nested structure`() {
            val element = buildJsonObject {
                put("level1", buildJsonObject {
                    put("level2", buildJsonObject {
                        put("level3", buildJsonObject {
                            put("value", 42)
                        })
                    })
                })
            }
            val result = element.toFracturedJson()
            assertThat(result).contains("level1")
            assertThat(result).contains("level2")
            assertThat(result).contains("level3")
            assertThat(result).contains("42")
        }
    }
}
