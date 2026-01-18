package io.github.fracturedjson.kotlinx

import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.Writer

/**
 * Java-friendly utility class for FracturedJson formatting with kotlinx.serialization.
 *
 * This class provides static methods that can be easily called from Java code.
 *
 * Example usage in Java:
 * ```java
 * Json json = Json.Default;
 * JsonElement element = json.parseToJsonElement("{\"name\":\"test\"}");
 *
 * // Format with default options
 * String formatted = KotlinxSupport.format(element);
 *
 * // Format with custom options
 * FracturedJsonOptions options = new FracturedJsonOptions();
 * options.setMaxTotalLineLength(80);
 * String formatted = KotlinxSupport.format(element, options);
 *
 * // Format JSON string directly
 * String formatted = KotlinxSupport.formatJson(jsonString);
 * ```
 */
class KotlinxSupport private constructor() {
    companion object {
        // ==================== JsonElement Formatting ====================

        /**
         * Formats a JsonElement using FracturedJson formatting with default options.
         *
         * @param element The JsonElement to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(element: JsonElement): String {
            return element.toFracturedJson()
        }

        /**
         * Formats a JsonElement using FracturedJson formatting.
         *
         * @param element The JsonElement to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(element: JsonElement, options: FracturedJsonOptions): String {
            return element.toFracturedJson(options)
        }

        /**
         * Formats a JsonElement to a Writer with default options.
         *
         * @param element The JsonElement to format
         * @param writer The Writer to write to
         */
        @JvmStatic
        fun format(element: JsonElement, writer: Writer) {
            element.toFracturedJson(writer)
        }

        /**
         * Formats a JsonElement to a Writer.
         *
         * @param element The JsonElement to format
         * @param writer The Writer to write to
         * @param options Formatting options
         */
        @JvmStatic
        fun format(element: JsonElement, writer: Writer, options: FracturedJsonOptions) {
            element.toFracturedJson(writer, options)
        }

        /**
         * Minifies a JsonElement.
         *
         * @param element The JsonElement to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minify(element: JsonElement): String {
            return element.minify()
        }

        /**
         * Converts a JsonElement to a JsonItem tree.
         *
         * @param element The JsonElement to convert
         * @return JsonItem representation
         */
        @JvmStatic
        fun toJsonItem(element: JsonElement): JsonItem {
            return element.toJsonItem()
        }

        // ==================== String Formatting ====================

        /**
         * Parses and reformats a JSON string with FracturedJson using default options.
         *
         * @param json The JSON string to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(json: String): String {
            return json.reformatJson()
        }

        /**
         * Parses and reformats a JSON string with FracturedJson.
         *
         * @param json The JSON string to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(json: String, options: FracturedJsonOptions): String {
            return json.reformatJson(options)
        }

        /**
         * Parses and minifies a JSON string.
         *
         * @param json The JSON string to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minifyJson(json: String): String {
            return json.minifyJson()
        }

        // ==================== Parsing Helpers ====================

        /**
         * Parses a JSON string to a JsonElement.
         *
         * @param json The JSON string to parse
         * @return Parsed JsonElement
         */
        @JvmStatic
        fun parse(json: String): JsonElement {
            return Json.parseToJsonElement(json)
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
