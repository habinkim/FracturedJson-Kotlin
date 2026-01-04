package io.github.fracturedjson.kotlinx

import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType
import io.github.fracturedjson.core.NumberListAlignment
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

        private fun parseJson(json: String): JsonElement = Json.parseToJsonElement(json)

        @Test
        fun `should format weather station data (1BRC style)`() {
            val element = parseJson(weatherDataJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Weather Station Data (1BRC Style) - Default Options [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Seoul")
            assertThat(result).contains("Tokyo")
            assertThat(result).contains("Beijing")
        }

        @Test
        fun `should format weather data with compact options`() {
            val element = parseJson(weatherDataJson)
            val options = FracturedJsonOptions(
                maxTotalLineLength = 100,
                maxInlineComplexity = 1,
                simpleBracketPadding = false
            )
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Weather Station Data - Compact Options (maxLineLength=100, inline=1) [kotlinx]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).isNotEmpty()
        }

        @Test
        fun `should format complex config with default options`() {
            val element = parseJson(complexConfigJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("Complex Configuration - Default Options [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("database")
            assertThat(result).contains("cache")
            assertThat(result).contains("features")
        }

        @Test
        fun `should format config with 2-space indent`() {
            val element = parseJson(complexConfigJson)
            val options = FracturedJsonOptions(indentSpaces = 2)
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Complex Configuration - 2-Space Indent [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("  ") // 2-space indent
        }

        @Test
        fun `should format API response with table alignment`() {
            val element = parseJson(apiResponseJson)
            val result = element.toFracturedJson()

            println("\n${"=".repeat(80)}")
            println("API Response with User Data - Default Options [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Alice")
            assertThat(result).contains("pagination")
        }

        @Test
        fun `should format numeric data with decimal alignment`() {
            val element = parseJson(numericDataJson)
            val options = FracturedJsonOptions(
                numberListAlignment = NumberListAlignment.Decimal
            )
            val result = element.toFracturedJson(options)

            println("\n${"=".repeat(80)}")
            println("Numeric Data - Decimal Alignment [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("measurements")
            assertThat(result).contains("matrix")
        }

        @Test
        fun `should minify complex data`() {
            val element = parseJson(weatherDataJson)
            val result = element.minify()

            println("\n${"=".repeat(80)}")
            println("Weather Data - Minified [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).doesNotContain("\n")
            assertThat(result).doesNotContain("  ")
        }

        @Test
        fun `should compare formatting styles`() {
            val element = parseJson(numericDataJson)

            println("\n${"=".repeat(80)}")
            println("Formatting Style Comparison - Numeric Data [kotlinx.serialization]")
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
            val element = parseJson(apiResponseJson)

            val result = element.formatWith {
                maxTotalLineLength = 100
                indentSpaces = 2
                simpleBracketPadding = false
                nestedBracketPadding = true
            }

            println("\n${"=".repeat(80)}")
            println("API Response - DSL Builder (100 chars, 2-space, no simple padding) [kotlinx]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).isNotEmpty()
        }

        @Test
        fun `should reformat JSON string`() {
            val result = weatherDataJson.reformatJson()

            println("\n${"=".repeat(80)}")
            println("Weather Data - reformatJson() Extension [kotlinx.serialization]")
            println("=".repeat(80))
            println(result)
            println("=".repeat(80))

            assertThat(result).contains("Seoul")
        }
    }
}
