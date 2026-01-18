package io.github.fracturedjson.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import java.io.Writer

/**
 * Java-friendly utility class for FracturedJson formatting with Jackson.
 *
 * This class provides static methods that can be easily called from Java code.
 *
 * Example usage in Java:
 * ```java
 * ObjectMapper mapper = new ObjectMapper();
 * JsonNode node = mapper.readTree("{\"name\":\"test\"}");
 *
 * // Format with default options
 * String formatted = JacksonSupport.format(node);
 *
 * // Format with custom options
 * FracturedJsonOptions options = new FracturedJsonOptions();
 * options.setMaxTotalLineLength(80);
 * String formatted = JacksonSupport.format(node, options);
 *
 * // Format JSON string directly
 * String formatted = JacksonSupport.formatJson(mapper, jsonString);
 * ```
 */
class JacksonSupport private constructor() {
    companion object {
        // ==================== JsonNode Formatting ====================

        /**
         * Formats a JsonNode using FracturedJson formatting with default options.
         *
         * @param node The JsonNode to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(node: JsonNode): String {
            return node.toFracturedJson()
        }

        /**
         * Formats a JsonNode using FracturedJson formatting.
         *
         * @param node The JsonNode to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(node: JsonNode, options: FracturedJsonOptions): String {
            return node.toFracturedJson(options)
        }

        /**
         * Formats a JsonNode to a Writer with default options.
         *
         * @param node The JsonNode to format
         * @param writer The Writer to write to
         */
        @JvmStatic
        fun format(node: JsonNode, writer: Writer) {
            node.toFracturedJson(writer)
        }

        /**
         * Formats a JsonNode to a Writer.
         *
         * @param node The JsonNode to format
         * @param writer The Writer to write to
         * @param options Formatting options
         */
        @JvmStatic
        fun format(node: JsonNode, writer: Writer, options: FracturedJsonOptions) {
            node.toFracturedJson(writer, options)
        }

        /**
         * Minifies a JsonNode.
         *
         * @param node The JsonNode to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minify(node: JsonNode): String {
            return node.minify()
        }

        /**
         * Converts a JsonNode to a JsonItem tree.
         *
         * @param node The JsonNode to convert
         * @return JsonItem representation
         */
        @JvmStatic
        fun toJsonItem(node: JsonNode): JsonItem {
            return node.toJsonItem()
        }

        // ==================== ObjectMapper Operations ====================

        /**
         * Parses JSON string and reformats it with FracturedJson using default options.
         *
         * @param mapper The ObjectMapper to use for parsing
         * @param json The JSON string to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(mapper: ObjectMapper, json: String): String {
            return mapper.formatJson(json)
        }

        /**
         * Parses JSON string and reformats it with FracturedJson.
         *
         * @param mapper The ObjectMapper to use for parsing
         * @param json The JSON string to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(mapper: ObjectMapper, json: String, options: FracturedJsonOptions): String {
            return mapper.formatJson(json, options)
        }

        /**
         * Parses JSON string and minifies it.
         *
         * @param mapper The ObjectMapper to use for parsing
         * @param json The JSON string to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minifyJson(mapper: ObjectMapper, json: String): String {
            return mapper.minifyJson(json)
        }

        /**
         * Serializes an object and formats it with FracturedJson using default options.
         *
         * @param mapper The ObjectMapper to use for serialization
         * @param value The object to serialize
         * @return Formatted JSON string
         */
        @JvmStatic
        fun <T> writeValueAsFracturedJson(mapper: ObjectMapper, value: T): String {
            return mapper.writeValueAsFracturedJson(value)
        }

        /**
         * Serializes an object and formats it with FracturedJson.
         *
         * @param mapper The ObjectMapper to use for serialization
         * @param value The object to serialize
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun <T> writeValueAsFracturedJson(mapper: ObjectMapper, value: T, options: FracturedJsonOptions): String {
            return mapper.writeValueAsFracturedJson(value, options)
        }

        // ==================== Formatter Factory ====================

        /**
         * Creates a new Formatter with default options.
         *
         * @return A new Formatter instance
         */
        @JvmStatic
        fun createFormatter(): Formatter {
            return Formatter()
        }

        /**
         * Creates a new Formatter with the specified options.
         *
         * @param options Formatting options
         * @return A new Formatter instance
         */
        @JvmStatic
        fun createFormatter(options: FracturedJsonOptions): Formatter {
            return Formatter(options)
        }
    }
}
