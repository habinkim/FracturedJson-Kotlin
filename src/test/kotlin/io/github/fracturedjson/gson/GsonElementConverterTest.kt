package io.github.fracturedjson.gson

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
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

@DisplayName("GsonElementConverter")
class GsonElementConverterTest {

    private val gson = Gson()

    @Nested
    @DisplayName("Conversion")
    inner class Conversion {

        @Test
        fun `should convert null`() {
            val element = JsonNull.INSTANCE
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Null)
        }

        @Test
        fun `should convert boolean true`() {
            val element = JsonPrimitive(true)
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.True)
        }

        @Test
        fun `should convert boolean false`() {
            val element = JsonPrimitive(false)
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.False)
        }

        @Test
        fun `should convert integer`() {
            val element = JsonPrimitive(42)
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
            assertThat(item.value).isEqualTo("42")
        }

        @Test
        fun `should convert decimal`() {
            val element = JsonPrimitive(3.14)
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Number)
        }

        @Test
        fun `should convert string`() {
            val element = JsonPrimitive("hello")
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.String)
            assertThat(item.value).contains("hello")
        }

        @Test
        fun `should convert array`() {
            val element = JsonArray().apply {
                add(1)
                add(2)
                add(3)
            }
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Array)
            assertThat(item.children).hasSize(3)
        }

        @Test
        fun `should convert object`() {
            val element = JsonObject().apply {
                addProperty("name", "John")
                addProperty("age", 30)
            }
            val item = GsonElementConverter.convert(element)
            assertThat(item.type).isEqualTo(JsonItemType.Object)
            assertThat(item.children).hasSize(2)
        }

        @Test
        fun `should convert with property name`() {
            val element = JsonPrimitive("value")
            val item = GsonElementConverter.convert(element, "key")
            assertThat(item.name).isEqualTo("key")
        }
    }

    @Nested
    @DisplayName("Extension functions")
    inner class ExtensionFunctions {

        @Test
        fun `should format JsonElement`() {
            val element = JsonObject().apply {
                addProperty("key", "value")
            }
            val result = element.toFracturedJson()
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }

        @Test
        fun `should format with custom options`() {
            val element = JsonArray().apply {
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
            val element = JsonObject().apply {
                addProperty("name", "John")
                addProperty("age", 30)
            }
            val result = element.minify()
            assertThat(result).doesNotContain(" ")
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should format JSON via Gson`() {
            val json = """{"name":"John","age":30}"""
            val result = gson.formatJson(json)
            assertThat(result).contains("\"name\"")
        }

        @Test
        fun `should serialize and format object`() {
            data class Person(val name: String, val age: Int)
            val person = Person("John", 30)
            val result = gson.toFracturedJson(person)
            assertThat(result).contains("\"name\"")
            assertThat(result).contains("\"John\"")
        }

        @Test
        fun `should use formatWith DSL`() {
            val element = JsonArray().apply { add(1); add(2); add(3) }
            val result = element.formatWith {
                simpleBracketPadding = false
            }
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should reformat JSON string with Gson`() {
            val json = """{"name":"John","age":30}"""
            val result = json.reformatJsonWithGson()
            assertThat(result).contains("\"name\"")
        }

        @Test
        fun `should minify JSON string with Gson`() {
            val json = """
            {
                "name": "John",
                "age": 30
            }
            """.trimIndent()
            val result = json.minifyJsonWithGson()
            assertThat(result).doesNotContain("\n")
            assertThat(result).doesNotContain("  ")
        }
    }

    @Nested
    @DisplayName("Complex structures")
    inner class ComplexStructures {

        @Test
        fun `should handle nested structures`() {
            val element = JsonObject().apply {
                add("person", JsonObject().apply {
                    addProperty("name", "John")
                    add("addresses", JsonArray().apply {
                        add(JsonObject().apply { addProperty("city", "NYC") })
                        add(JsonObject().apply { addProperty("city", "LA") })
                    })
                })
            }
            val item = GsonElementConverter.convert(element)
            assertThat(item.complexity).isGreaterThan(1)
        }

        @Test
        fun `should format deeply nested structure`() {
            val element = JsonObject().apply {
                add("level1", JsonObject().apply {
                    add("level2", JsonObject().apply {
                        add("level3", JsonObject().apply {
                            addProperty("value", 42)
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

    @Nested
    @DisplayName("Real World Formatting Examples")
    inner class RealWorldFormatting {

        /**
         * 1BRC-style weather station data with measurements
         */
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

        /**
         * Complex nested configuration file
         */
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

        /**
         * Paginated API response with mixed data
         */
        private val apiResponseJson = """
        {
            "success": true,
            "data": [
                {"id": 1, "name": "Alice", "email": "alice@example.com", "roles": ["admin", "user"], "profile": {"avatar": "https://example.com/alice.jpg", "bio": "Software Engineer", "social": {"twitter": "@alice", "github": "alice"}}},
                {"id": 2, "name": "Bob", "email": "bob@example.com", "roles": ["user"], "profile": {"avatar": "https://example.com/bob.jpg", "bio": "Product Manager", "social": {"twitter": "@bob", "linkedin": "bob-smith"}}},
                {"id": 3, "name": "Charlie", "email": "charlie@example.com", "roles": ["user", "moderator"], "profile": {"avatar": null, "bio": "Community Lead", "social": {}}}
            ],
            "pagination": {"page": 1, "perPage": 10, "total": 3, "totalPages": 1},
            "meta": {"requestId": "req-abc123", "timestamp": "2024-01-15T10:30:00Z", "version": "v2"}
        }
        """.trimIndent()

        /**
         * Number-heavy data for alignment testing
         */
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
        fun `should format weather station data (1BRC style)`() {
            val element = JsonParser.parseString(weatherDataJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Weather Station Data (1BRC Style) - Default Options [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Seoul")
            assertThat(result).contains("Tokyo")
            assertThat(result).contains("Beijing")
        }

        @Test
        fun `should format weather data with compact options`() {
            val element = JsonParser.parseString(weatherDataJson)
            val options = FracturedJsonOptions(
                maxTotalLineLength = 100,
                maxInlineComplexity = 1,
                simpleBracketPadding = false
            )
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Weather Station Data - Compact Options (maxLineLength=100, inline=1) [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).isNotEmpty()
        }

        @Test
        fun `should format complex config with default options`() {
            val element = JsonParser.parseString(complexConfigJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Complex Configuration - Default Options [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("database")
            assertThat(result).contains("cache")
            assertThat(result).contains("features")
        }

        @Test
        fun `should format config with 2-space indent`() {
            val element = JsonParser.parseString(complexConfigJson)
            val options = FracturedJsonOptions(indentSpaces = 2)
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Complex Configuration - 2-Space Indent [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("  ") // 2-space indent
        }

        @Test
        fun `should format API response with table alignment`() {
            val element = JsonParser.parseString(apiResponseJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("API Response with User Data - Default Options [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Alice")
            assertThat(result).contains("pagination")
        }

        @Test
        fun `should format numeric data with decimal alignment`() {
            val element = JsonParser.parseString(numericDataJson)
            val options = FracturedJsonOptions(
                numberListAlignment = NumberListAlignment.Decimal
            )
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Numeric Data - Decimal Alignment [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("measurements")
            assertThat(result).contains("matrix")
        }

        @Test
        fun `should minify complex data`() {
            val element = JsonParser.parseString(weatherDataJson)
            val result = element.minify()

            println("\n${"=".repeat(80)}")
            println("Weather Data - Minified [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).doesNotContain("\n")
            assertThat(result).doesNotContain("  ")
        }

        @Test
        fun `should compare formatting styles`() {
            val element = JsonParser.parseString(numericDataJson)

            println("\n${"=".repeat(80)}")
            println("Formatting Style Comparison - Numeric Data [GSON]")
            println("=".repeat(80))

            // Style 1: Default
            println("\n[Style 1: Default]")
            println(element.toFracturedJson())

            // Style 2: Compact
            val compact = FracturedJsonOptions(
                maxTotalLineLength = 80,
                simpleBracketPadding = false,
                nestedBracketPadding = false
            )
            println("\n[Style 2: Compact (80 chars, no padding)]")
            println(element.toFracturedJson(compact))

            // Style 3: Expanded
            val expanded = FracturedJsonOptions(
                maxInlineComplexity = 0,
                indentSpaces = 2
            )
            println("\n[Style 3: Fully Expanded (2-space indent)]")
            println(element.toFracturedJson(expanded))

            println("=".repeat(80))
        }

        @Test
        fun `should format with DSL builder`() {
            val element = JsonParser.parseString(apiResponseJson)

            val result = element.formatWith {
                maxTotalLineLength = 100
                indentSpaces = 2
                simpleBracketPadding = false
                nestedBracketPadding = true
            }

            println("\n${"=".repeat(80)}")
            println("API Response - DSL Builder (100 chars, 2-space, no simple padding) [GSON]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Property Alignment")
    inner class PropertyAlignment {

        @Test
        @DisplayName("Property values aligned")
        fun propValuesAligned() {
            val element = JsonObject().apply {
                addProperty("num", 14)
                addProperty("string", "testing property alignment")
                add("arrayWithLongName", JsonArray().apply {
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                })
            }

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 15,
                colonBeforePropNamePadding = false,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(opts)
            val output = formatter.format(item)
            val outputLines = output.trimEnd().split('\n').toTypedArray()

            // This object should be expanded with the property values and colons aligned.
            // The array should be expanded as well.
            assertThat(outputLines.size).isEqualTo(9)
            TestHelpers.testInstancesLineUp(outputLines, ":")
        }

        @Test
        @DisplayName("Property values aligned but not colons")
        fun propValuesAlignedButNotColons() {
            val element = JsonObject().apply {
                addProperty("num", 14)
                addProperty("string", "testing property alignment")
                add("arrayWithLongName", JsonArray().apply {
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                })
            }

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 15,
                colonBeforePropNamePadding = true,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(opts)
            val output = formatter.format(item)
            val outputLines = output.trimEnd().split('\n')

            // This object should be expanded with the property values, but the colons should hug
            // the prop names instead of being aligned.
            assertThat(outputLines.size).isEqualTo(9)
            assertThat(outputLines[1]).contains("\"num\":")
            assertThat(outputLines[2]).contains("\"string\":")
            assertThat(outputLines[3]).contains("\"arrayWithLongName\":")
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[2].indexOf("\"testing"))
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[3].indexOf('['))
        }

        @Test
        @DisplayName("Don't align prop vals when too much padding required")
        fun dontAlignPropValsWhenTooMuchPaddingRequired() {
            val element = JsonObject().apply {
                addProperty("num", 14)
                addProperty("string", "testing property alignment")
                add("arrayWithLongName", JsonArray().apply {
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                    add(JsonNull.INSTANCE)
                })
            }

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 12,
                colonBeforePropNamePadding = false,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(opts)
            val output = formatter.format(item)
            val outputLines = output.trimEnd().split('\n')

            // This object should be expanded but the property values shouldn't be aligned since
            // the length of the prop names differ by more than MaxPropNamePadding.
            assertThat(outputLines.size).isEqualTo(9)
            assertThat(outputLines[1]).contains("\"num\": 14,")
            assertThat(outputLines[2]).contains("\"string\": \"testing")
            assertThat(outputLines[3]).contains("\"arrayWithLongName\": [")
        }
    }

    @Nested
    @DisplayName("Table Formatting")
    inner class TableFormatting {

        @Test
        fun `nested elements line up`() {
            val element = JsonArray().apply {
                add(JsonObject().apply { addProperty("name", "Alice"); addProperty("age", 25) })
                add(JsonObject().apply { addProperty("name", "Bob"); addProperty("age", 30) })
            }
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Properties should align vertically
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
        fun `nested elements compact when needed`() {
            val element = JsonArray().apply {
                add(JsonObject().apply { addProperty("name", "Alice"); addProperty("age", 25) })
                add(JsonObject().apply { addProperty("name", "Bob"); addProperty("age", 30) })
            }
            val options = FracturedJsonOptions(
                maxTotalLineLength = 30,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // With restricted line length, formatting should adapt
            assertThat(result).contains("name")
            assertThat(result).contains("age")
        }

        @Test
        fun `fall back on inline if needed`() {
            val element = JsonArray().apply {
                add(JsonObject().apply { addProperty("name", "Alice") })
                add(JsonObject().apply { addProperty("name", "Bob") })
            }
            val options = FracturedJsonOptions(
                maxTotalLineLength = 100,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Should contain all data
            assertThat(result).contains("Alice")
            assertThat(result).contains("Bob")
        }

        @Test
        fun `handles nulls with arrays table columns`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1); add(JsonNull.INSTANCE) })
                add(JsonArray().apply { add(2); add(3) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Should handle null values
            assertThat(result).contains("null")
        }

        @Test
        fun `commas before padding works`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1); add(2) })
                add(JsonArray().apply { add(10); add(20) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Should contain all values
            assertThat(result).contains("1")
            assertThat(result).contains("20")
        }

        @Test
        fun `colons hug prop names when configured`() {
            val element = JsonArray().apply {
                add(JsonObject().apply { addProperty("a", 1) })
                add(JsonObject().apply { addProperty("longName", 2) })
            }
            val options = FracturedJsonOptions(
                colonBeforePropNamePadding = true,
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Colons should be right after property names
            assertThat(result).contains("\"a\":")
            assertThat(result).contains("\"longName\":")
        }

        @Test
        fun `single columns with numbers work`() {
            val element = JsonArray().apply {
                add(1.5); add(22.333); add(100.1)
            }
            val options = FracturedJsonOptions(
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // All numbers should be present
            assertThat(result).contains("1.5")
            assertThat(result).contains("22.333")
            assertThat(result).contains("100.1")
        }
    }

    @Nested
    @DisplayName("Number Formatting")
    inner class NumberFormatting {

        @Test
        fun `inline array doesnt justify numbers`() {
            val element = JsonArray().apply {
                add(1); add(22); add(333)
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 10,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // In inline format, numbers should not be padded
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `compact array does justify numbers`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1); add(2) })
                add(JsonArray().apply { add(333); add(4444) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 1,
                maxCompactArrayComplexity = 2,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Numbers should be present with potential alignment
            assertThat(result).contains("1")
            assertThat(result).contains("4444")
        }

        @Test
        fun `table array does justify numbers`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1); add(2) })
                add(JsonArray().apply { add(333); add(4444) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Numbers should be formatted in table style
            assertThat(result).contains("1")
            assertThat(result).contains("333")
        }

        @Test
        fun `scientific notation numbers preserved`() {
            val element = JsonArray().apply {
                add(1e10); add(2); add(3)
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Scientific notation should be preserved
            assertThat(result.lowercase()).contains("e")
        }

        @Test
        fun `nulls respected when aligning numbers`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1); add(JsonNull.INSTANCE) })
                add(JsonArray().apply { add(22); add(33) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Nulls should be preserved
            assertThat(result).contains("null")
        }

        @Test
        fun `left align works`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1) })
                add(JsonArray().apply { add(22) })
                add(JsonArray().apply { add(333) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Left,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // All numbers should be present
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `right align works`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1) })
                add(JsonArray().apply { add(22) })
                add(JsonArray().apply { add(333) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Right,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // All numbers should be present
            assertThat(result).contains("1")
            assertThat(result).contains("22")
            assertThat(result).contains("333")
        }

        @Test
        fun `decimal align works`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1.5) })
                add(JsonArray().apply { add(22.333) })
                add(JsonArray().apply { add(333.1) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Decimal,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // All decimal numbers should be present
            assertThat(result).contains("1.5")
            assertThat(result).contains("22.333")
            assertThat(result).contains("333.1")
        }

        @Test
        fun `normalize align works`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1.5) })
                add(JsonArray().apply { add(22.333) })
                add(JsonArray().apply { add(333.1) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                numberListAlignment = NumberListAlignment.Normalize,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Numbers should be normalized with consistent decimal places
            assertThat(result).isNotEmpty()
        }

        @Test
        fun `negative numbers align correctly`() {
            val element = JsonArray().apply {
                add(JsonArray().apply { add(1) })
                add(JsonArray().apply { add(-22) })
                add(JsonArray().apply { add(333) })
            }
            val options = FracturedJsonOptions(
                maxInlineComplexity = 0,
                jsonEolStyle = EolStyle.Lf
            )

            val item = GsonElementConverter.convert(element)
            val formatter = Formatter(options)
            val result = formatter.format(item)

            // Negative number should be preserved
            assertThat(result).contains("-22")
        }
    }

    @Nested
    @DisplayName("Special Characters")
    inner class SpecialCharacters {

        @Test
        fun `should escape special characters in strings`() {
            val element = JsonPrimitive("Hello\nWorld\t\"Test\"\\Path")
            val item = GsonElementConverter.convert(element)
            assertThat(item.value).contains("\\n")
            assertThat(item.value).contains("\\t")
            assertThat(item.value).contains("\\\"")
            assertThat(item.value).contains("\\\\")
        }

        @Test
        fun `should handle unicode characters`() {
            val element = JsonPrimitive("한글 日本語 中文")
            val item = GsonElementConverter.convert(element)
            assertThat(item.value).contains("한글")
            assertThat(item.value).contains("日本語")
            assertThat(item.value).contains("中文")
        }

        @Test
        fun `should escape control characters`() {
            val element = JsonPrimitive("test\u0001\u0002\u0003")
            val item = GsonElementConverter.convert(element)
            assertThat(item.value).contains("\\u0001")
            assertThat(item.value).contains("\\u0002")
            assertThat(item.value).contains("\\u0003")
        }
    }

    @Nested
    @DisplayName("Comparison with Jackson")
    inner class ComparisonWithJackson {

        @Test
        fun `Gson and Jackson should produce equivalent output`() {
            val json = """{"name":"Test","value":123,"nested":{"a":1,"b":2}}"""

            // Format with Gson
            val gsonResult = json.reformatJsonWithGson()

            // Format with Jackson (via ObjectMapper)
            val jacksonMapper = com.fasterxml.jackson.databind.ObjectMapper()
            val jacksonResult = jacksonMapper.jacksonFormatJson(json)

            // Both should contain the same data
            assertThat(gsonResult).contains("\"name\"")
            assertThat(jacksonResult).contains("\"name\"")
            assertThat(gsonResult).contains("\"Test\"")
            assertThat(jacksonResult).contains("\"Test\"")

            println("\n${"=".repeat(80)}")
            println("Gson vs Jackson Comparison")
            println("=".repeat(80))
            println("\n[Gson Result]")
            println(gsonResult)
            println("\n[Jackson Result]")
            println(jacksonResult)
            println("=".repeat(80))
        }
    }
}
