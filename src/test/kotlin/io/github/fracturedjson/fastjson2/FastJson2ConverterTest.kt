package io.github.fracturedjson.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.github.fracturedjson.core.EolStyle
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import io.github.fracturedjson.core.NumberListAlignment
import io.github.fracturedjson.core.TestHelpers
import io.github.fracturedjson.jackson.formatJson as jacksonFormatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FastJson2Converter")
class FastJson2ConverterTest {

    @Nested
    @DisplayName("Conversion")
    inner class Conversion {

        @Test
        fun `should convert null`() {
            val item = FastJson2Converter.convert(null)
            assertThat(item.type).isEqualTo(JsonItemType.Null)
        }

        @Test
        fun `should convert boolean true`() {
            val item = FastJson2Converter.convert(true)
            assertThat(item.type).isEqualTo(JsonItemType.True)
        }

        @Test
        fun `should convert boolean false`() {
            val item = FastJson2Converter.convert(false)
            assertThat(item.type).isEqualTo(JsonItemType.False)
        }

        @Test
        fun `should convert integer`() {
            val item = FastJson2Converter.convert(42)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("42")
        }

        @Test
        fun `should convert long`() {
            val item = FastJson2Converter.convert(9876543210L)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("9876543210")
        }

        @Test
        fun `should convert decimal`() {
            val item = FastJson2Converter.convert(3.14)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
        }

        @Test
        fun `should convert string`() {
            val item = FastJson2Converter.convert("hello")
            assertThat(item.type).isEqualTo(JsonItemType.String)
            assertThat(item.value).contains("hello")
        }

        @Test
        fun `should convert JSONArray`() {
            val array = JSONArray.of(1, 2, 3)
            val item = FastJson2Converter.convert(array)
            assertThat(item.type).isEqualTo(JsonItemType.Array)
            assertThat(item.children).hasSize(3)
        }

        @Test
        fun `should convert JSONObject`() {
            val obj = JSONObject().apply {
                put("name", "John")
                put("age", 30)
            }
            val item = FastJson2Converter.convert(obj)
            assertThat(item.type).isEqualTo(JsonItemType.Object)
            assertThat(item.children).hasSize(2)
        }

        @Test
        fun `should convert with property name`() {
            val item = FastJson2Converter.convert("value", "key")
            assertThat(item.name).isEqualTo("key")
        }

        @Test
        fun `should handle BigDecimal`() {
            val value = java.math.BigDecimal("123456789.123456789")
            val item = FastJson2Converter.convert(value)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("123456789.123456789")
        }

        @Test
        fun `should handle BigInteger`() {
            val value = java.math.BigInteger("12345678901234567890")
            val item = FastJson2Converter.convert(value)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("12345678901234567890")
        }
    }

    @Nested
    @DisplayName("Extension functions")
    inner class ExtensionFunctions {

        @Test
        fun `should format JSONObject`() {
            val obj = JSONObject().apply {
                put("key", "value")
            }
            val result = obj.toFracturedJson()
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }

        @Test
        fun `should format JSONArray`() {
            val array = JSONArray.of(1, 2, 3)
            val options = FracturedJsonOptions(simpleBracketPadding = false)
            val result = array.toFracturedJson(options)
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should minify JSONObject`() {
            val obj = JSONObject().apply {
                put("name", "John")
                put("age", 30)
            }
            val result = obj.minify()
            assertThat(result).doesNotContain(" ")
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should minify JSONArray`() {
            val array = JSONArray.of(1, 2, 3)
            val result = array.minify()
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should use formatWith DSL for JSONObject`() {
            val obj = JSONObject().apply {
                put("a", 1)
                put("b", 2)
            }
            val result = obj.formatWith {
                simpleBracketPadding = false
            }
            assertThat(result).contains("\"a\"")
            assertThat(result).contains("\"b\"")
        }

        @Test
        fun `should use formatWith DSL for JSONArray`() {
            val array = JSONArray.of(1, 2, 3)
            val result = array.formatWith {
                simpleBracketPadding = false
            }
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should reformat JSON string with Fastjson2`() {
            val json = """{"name":"John","age":30}"""
            val result = json.reformatJsonWithFastjson2()
            assertThat(result).contains("\"name\"")
        }

        @Test
        fun `should minify JSON string with Fastjson2`() {
            val json = """
            {
                "name": "John",
                "age": 30
            }
            """.trimIndent()
            val result = json.minifyJsonWithFastjson2()
            assertThat(result).doesNotContain("\n")
            assertThat(result).doesNotContain("  ")
        }
    }

    @Nested
    @DisplayName("Complex structures")
    inner class ComplexStructures {

        @Test
        fun `should handle nested structures`() {
            val obj = JSONObject().apply {
                put("person", JSONObject().apply {
                    put("name", "John")
                    put("addresses", JSONArray().apply {
                        add(JSONObject().apply { put("city", "NYC") })
                        add(JSONObject().apply { put("city", "LA") })
                    })
                })
            }
            val item = FastJson2Converter.convert(obj)
            assertThat(item.complexity).isGreaterThan(1)
        }

        @Test
        fun `should format deeply nested structure`() {
            val obj = JSONObject().apply {
                put("level1", JSONObject().apply {
                    put("level2", JSONObject().apply {
                        put("level3", JSONObject().apply {
                            put("value", 42)
                        })
                    })
                })
            }
            val result = obj.toFracturedJson()
            assertThat(result).contains("level1")
            assertThat(result).contains("level2")
            assertThat(result).contains("level3")
            assertThat(result).contains("42")
        }

        @Test
        fun `should handle mixed types in array`() {
            val array = JSONArray().apply {
                add(1)
                add("two")
                add(true)
                add(null)
                add(JSONObject().apply { put("nested", "object") })
            }
            val item = FastJson2Converter.convert(array)
            assertThat(item.children).hasSize(5)
            assertThat(item.children[0].type).isEqualTo(JsonItemType.Number)
            assertThat(item.children[1].type).isEqualTo(JsonItemType.String)
            assertThat(item.children[2].type).isEqualTo(JsonItemType.True)
            assertThat(item.children[3].type).isEqualTo(JsonItemType.Null)
            assertThat(item.children[4].type).isEqualTo(JsonItemType.Object)
        }
    }

    @Nested
    @DisplayName("Real World Formatting Examples")
    inner class RealWorldFormatting {

        private val weatherDataJson = """
        {
            "stations": [
                {"id": "KR-001", "name": "Seoul", "country": "South Korea", "readings": [
                    {"timestamp": "2024-01-01T00:00:00Z", "temperature": -2.5, "humidity": 45, "pressure": 1013.2},
                    {"timestamp": "2024-01-01T01:00:00Z", "temperature": -3.1, "humidity": 48, "pressure": 1013.5},
                    {"timestamp": "2024-01-01T02:00:00Z", "temperature": -3.8, "humidity": 52, "pressure": 1013.8}
                ], "metadata": {"elevation": 38, "established": 1907, "type": "urban"}},
                {"id": "JP-001", "name": "Tokyo", "country": "Japan", "readings": [
                    {"timestamp": "2024-01-01T00:00:00Z", "temperature": 5.2, "humidity": 60, "pressure": 1015.1},
                    {"timestamp": "2024-01-01T01:00:00Z", "temperature": 4.8, "humidity": 62, "pressure": 1015.3},
                    {"timestamp": "2024-01-01T02:00:00Z", "temperature": 4.5, "humidity": 65, "pressure": 1015.5}
                ], "metadata": {"elevation": 40, "established": 1875, "type": "urban"}},
                {"id": "CN-001", "name": "Beijing", "country": "China", "readings": [
                    {"timestamp": "2024-01-01T00:00:00Z", "temperature": -8.2, "humidity": 30, "pressure": 1020.5},
                    {"timestamp": "2024-01-01T01:00:00Z", "temperature": -9.0, "humidity": 28, "pressure": 1020.8},
                    {"timestamp": "2024-01-01T02:00:00Z", "temperature": -9.5, "humidity": 25, "pressure": 1021.0}
                ], "metadata": {"elevation": 55, "established": 1912, "type": "urban"}}
            ],
            "summary": {
                "totalStations": 3,
                "avgTemperature": -1.3,
                "minTemperature": -9.5,
                "maxTemperature": 5.2,
                "collectionPeriod": {"start": "2024-01-01T00:00:00Z", "end": "2024-01-01T02:00:00Z"}
            }
        }
        """.trimIndent()

        private val complexConfigJson = """
        {
            "application": {
                "name": "FracturedJson Demo",
                "version": "1.0.0",
                "environment": "production"
            },
            "database": {
                "primary": {
                    "host": "db-primary.example.com",
                    "port": 5432,
                    "name": "app_production",
                    "pool": {"min": 5, "max": 20, "idleTimeout": 30000}
                },
                "replicas": [
                    {"host": "db-replica-1.example.com", "port": 5432, "weight": 50},
                    {"host": "db-replica-2.example.com", "port": 5432, "weight": 30},
                    {"host": "db-replica-3.example.com", "port": 5432, "weight": 20}
                ]
            },
            "cache": {
                "redis": {"host": "redis.example.com", "port": 6379, "db": 0},
                "ttl": {"default": 3600, "session": 86400, "static": 604800}
            },
            "features": [
                {"name": "dark_mode", "enabled": true, "rollout": 100},
                {"name": "new_dashboard", "enabled": true, "rollout": 50},
                {"name": "beta_api", "enabled": false, "rollout": 0}
            ],
            "logging": {
                "level": "info",
                "outputs": ["console", "file", "elasticsearch"],
                "format": "json"
            }
        }
        """.trimIndent()

        private val numericDataJson = """
        {
            "measurements": [
                {"sensor": "A1", "values": [1.5, 23.456, 789.0, 0.001, 12345.6789]},
                {"sensor": "B2", "values": [0.1, 2.34, 56.789, 1234.5, 0.0001]},
                {"sensor": "C3", "values": [100, 200, 300, 400, 500]}
            ],
            "matrix": [
                [1, 2, 3, 4, 5],
                [10, 20, 30, 40, 50],
                [100, 200, 300, 400, 500]
            ],
            "coordinates": [
                {"x": 0, "y": 0, "z": 0},
                {"x": 100, "y": 200, "z": 300},
                {"x": -50, "y": 150, "z": -25}
            ]
        }
        """.trimIndent()

        @Test
        fun `should format weather station data`() {
            val obj = JSON.parseObject(weatherDataJson)
            val result = obj.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Weather Station Data - Default Options [FASTJSON2]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Seoul")
            assertThat(result).contains("Tokyo")
            assertThat(result).contains("Beijing")
        }

        @Test
        fun `should format weather data with compact options`() {
            val obj = JSON.parseObject(weatherDataJson)
            val options = FracturedJsonOptions(
                maxTotalLineLength = 100,
                maxInlineComplexity = 1,
                simpleBracketPadding = false
            )
            val result = obj.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Weather Station Data - Compact Options [FASTJSON2]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).isNotEmpty()
        }

        @Test
        fun `should format complex config with default options`() {
            val obj = JSON.parseObject(complexConfigJson)
            val result = obj.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Complex Configuration - Default Options [FASTJSON2]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("database")
            assertThat(result).contains("cache")
            assertThat(result).contains("features")
        }

        @Test
        fun `should format numeric data with decimal alignment`() {
            val obj = JSON.parseObject(numericDataJson)
            val options = FracturedJsonOptions(
                numberListAlignment = NumberListAlignment.Decimal
            )
            val result = obj.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Numeric Data - Decimal Alignment [FASTJSON2]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("measurements")
            assertThat(result).contains("matrix")
        }

        @Test
        fun `should minify complex data`() {
            val obj = JSON.parseObject(weatherDataJson)
            val result = obj.minify()

            println("\n${"=".repeat(80)}")
            println("Weather Data - Minified [FASTJSON2]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).doesNotContain("\n")
            assertThat(result).doesNotContain("  ")
        }
    }

    @Nested
    @DisplayName("Property Alignment")
    inner class PropertyAlignment {

        @Test
        @DisplayName("Property values aligned")
        fun propValuesAligned() {
            val obj = JSONObject().apply {
                put("num", 14)
                put("string", "testing property alignment")
                put("arrayWithLongName", JSONArray.of(null, null, null))
            }

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 15,
                colonBeforePropNamePadding = false,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(obj)
            val formatter = Formatter(opts)
            val output = formatter.format(item)
            val outputLines = output.trimEnd().split('\n').toTypedArray()

            assertThat(outputLines.size).isEqualTo(9)
            TestHelpers.testInstancesLineUp(outputLines, ":")
        }

        @Test
        @DisplayName("Property values aligned but not colons")
        fun propValuesAlignedButNotColons() {
            val obj = JSONObject().apply {
                put("num", 14)
                put("string", "testing property alignment")
                put("arrayWithLongName", JSONArray.of(null, null, null))
            }

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 15,
                colonBeforePropNamePadding = true,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(obj)
            val formatter = Formatter(opts)
            val output = formatter.format(item)
            val outputLines = output.trimEnd().split('\n')

            assertThat(outputLines.size).isEqualTo(9)
            assertThat(outputLines[1]).contains("\"num\":")
            assertThat(outputLines[2]).contains("\"string\":")
            assertThat(outputLines[3]).contains("\"arrayWithLongName\":")
        }
    }

    @Nested
    @DisplayName("Table Formatting")
    inner class TableFormatting {

        @Test
        fun `nested elements line up`() {
            val array = JSONArray().apply {
                add(JSONObject().apply { put("name", "Alice"); put("age", 25) })
                add(JSONObject().apply { put("name", "Bob"); put("age", 30) })
            }
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            val lines = result.split("\n")
            val nameLines = lines.filter { it.contains("\"name\"") }
            if (nameLines.size > 1) {
                val positions = nameLines.mapNotNull { line ->
                    val idx = line.indexOf("\"name\"")
                    if (idx >= 0) idx else null
                }
                if (positions.size > 1) {
                    assertThat(positions.distinct().size).isEqualTo(1)
                }
            }
        }

        @Test
        fun `handles nulls with arrays table columns`() {
            val array = JSONArray().apply {
                add(JSONArray.of(1, null))
                add(JSONArray.of(2, 3))
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            assertThat(result).contains("null")
        }
    }

    @Nested
    @DisplayName("Number Formatting")
    inner class NumberFormatting {

        @Test
        fun `left align works`() {
            val array = JSONArray().apply {
                add(JSONArray.of(1))
                add(JSONArray.of(22))
                add(JSONArray.of(333))
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Left,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `right align works`() {
            val array = JSONArray().apply {
                add(JSONArray.of(1))
                add(JSONArray.of(22))
                add(JSONArray.of(333))
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Right,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `decimal align works`() {
            val array = JSONArray().apply {
                add(JSONArray.of(1.5))
                add(JSONArray.of(22.333))
                add(JSONArray.of(333.1))
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Decimal,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            assertThat(result).contains("1.5")
            assertThat(result).contains("22.333")
            assertThat(result).contains("333.1")
        }

        @Test
        fun `negative numbers align correctly`() {
            val array = JSONArray().apply {
                add(JSONArray.of(1))
                add(JSONArray.of(-22))
                add(JSONArray.of(333))
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = FastJson2Converter.convert(array)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            assertThat(result).contains("-22")
        }
    }

    @Nested
    @DisplayName("Special Characters")
    inner class SpecialCharacters {

        @Test
        fun `should escape special characters in strings`() {
            val item = FastJson2Converter.convert("Hello\nWorld\t\"Test\"\\Path")
            assertThat(item.value).contains("\\n")
            assertThat(item.value).contains("\\t")
            assertThat(item.value).contains("\\\"")
            assertThat(item.value).contains("\\\\")
        }

        @Test
        fun `should handle unicode characters`() {
            val item = FastJson2Converter.convert("한글 日本語 中文")
            assertThat(item.value).contains("한글")
            assertThat(item.value).contains("日本語")
            assertThat(item.value).contains("中文")
        }

        @Test
        fun `should escape control characters`() {
            val item = FastJson2Converter.convert("test\u0001\u0002\u0003")
            assertThat(item.value).contains("\\u0001")
            assertThat(item.value).contains("\\u0002")
            assertThat(item.value).contains("\\u0003")
        }
    }

    @Nested
    @DisplayName("Comparison with Jackson and Gson")
    inner class ComparisonWithOtherLibraries {

        @Test
        fun `Fastjson2 and Jackson should produce equivalent output`() {
            val json = """{"name":"Test","value":123,"nested":{"a":1,"b":2}}"""

            // Format with Fastjson2
            val fastjson2Result = json.reformatJsonWithFastjson2()

            // Format with Jackson
            val jacksonMapper = com.fasterxml.jackson.databind.ObjectMapper()
            val jacksonResult = jacksonMapper.jacksonFormatJson(json)

            // Both should contain the same data
            assertThat(fastjson2Result).contains("\"name\"")
            assertThat(jacksonResult).contains("\"name\"")
            assertThat(fastjson2Result).contains("\"Test\"")
            assertThat(jacksonResult).contains("\"Test\"")

            println("\n${"=".repeat(80)}")
            println("Fastjson2 vs Jackson Comparison")
            println("=".repeat(80))
            println("\n[Fastjson2 Result]")
            println(fastjson2Result)
            println("\n[Jackson Result]")
            println(jacksonResult)
            println("=".repeat(80))
        }

        @Test
        fun `Fastjson2 and Gson should produce equivalent output`() {
            val json = """{"name":"Test","value":123,"nested":{"a":1,"b":2}}"""

            // Format with Fastjson2
            val fastjson2Result = json.reformatJsonWithFastjson2()

            // Format with Gson
            val gsonResult = json.reformatJsonWithGson()

            // Both should contain the same data
            assertThat(fastjson2Result).contains("\"name\"")
            assertThat(gsonResult).contains("\"name\"")

            println("\n${"=".repeat(80)}")
            println("Fastjson2 vs Gson Comparison")
            println("=".repeat(80))
            println("\n[Fastjson2 Result]")
            println(fastjson2Result)
            println("\n[Gson Result]")
            println(gsonResult)
            println("=".repeat(80))
        }
    }

    @Nested
    @DisplayName("Inline Serialization")
    inner class InlineSerialization {

        @Test
        fun `should serialize and format data class`() {
            val person = TestPerson("Alice", 30, true)
            val result = toFracturedJson(person)

            assertThat(result).contains("\"name\"")
            assertThat(result).contains("\"Alice\"")
            assertThat(result).contains("\"age\"")
            assertThat(result).contains("30")
            assertThat(result).contains("\"active\"")
            assertThat(result).contains("true")

            println("\n${"=".repeat(80)}")
            println("Inline Serialization - Data Class")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))
        }

        @Test
        fun `should serialize list of objects`() {
            val people = listOf(
                TestPerson("Alice", 30, true),
                TestPerson("Bob", 25, false)
            )
            val result = toFracturedJson(people)

            assertThat(result).contains("Alice")
            assertThat(result).contains("Bob")

            println("\n${"=".repeat(80)}")
            println("Inline Serialization - List of Objects")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))
        }
    }
}

data class TestPerson(val name: String, val age: Int, val active: Boolean)

private fun String.reformatJsonWithGson(): String {
    val element = com.google.gson.JsonParser.parseString(this)
    return element.toFracturedJson()
}

private fun com.google.gson.JsonElement.toFracturedJson(): String {
    val item = io.github.fracturedjson.gson.GsonElementConverter.convert(this)
    return io.github.fracturedjson.core.Formatter().format(item)
}
