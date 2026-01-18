package io.github.fracturedjson.gson

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import java.io.Writer

/**
 * Java-friendly utility class for FracturedJson formatting with Gson.
 *
 * This class provides static methods that can be easily called from Java code.
 *
 * Example usage in Java:
 * ```java
 * Gson gson = new Gson();
 * JsonElement element = JsonParser.parseString("{\"name\":\"test\"}");
 *
 * // Format with default options
 * String formatted = GsonSupport.format(element);
 *
 * // Format with custom options
 * FracturedJsonOptions options = new FracturedJsonOptions();
 * options.setMaxTotalLineLength(80);
 * String formatted = GsonSupport.format(element, options);
 *
 * // Format JSON string directly
 * String formatted = GsonSupport.formatJson(jsonString);
 * ```
 */
class GsonSupport private constructor() {
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
            return json.reformatJsonWithGson()
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
            return json.reformatJsonWithGson(options)
        }

        /**
         * Parses and minifies a JSON string.
         *
         * @param json The JSON string to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minifyJson(json: String): String {
            return json.minifyJsonWithGson()
        }

        // ==================== Gson Operations ====================

        /**
         * Parses JSON string and reformats it with FracturedJson using default options.
         *
         * @param gson The Gson instance to use
         * @param json The JSON string to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(gson: Gson, json: String): String {
            return gson.formatJson(json)
        }

        /**
         * Parses JSON string and reformats it with FracturedJson.
         *
         * @param gson The Gson instance to use
         * @param json The JSON string to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun formatJson(gson: Gson, json: String, options: FracturedJsonOptions): String {
            return gson.formatJson(json, options)
        }

        /**
         * Parses JSON string and minifies it.
         *
         * @param gson The Gson instance to use
         * @param json The JSON string to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minifyJson(gson: Gson, json: String): String {
            return gson.minifyJson(json)
        }

        /**
         * Serializes an object and formats it with FracturedJson using default options.
         *
         * @param gson The Gson instance to use for serialization
         * @param value The object to serialize
         * @return Formatted JSON string
         */
        @JvmStatic
        fun <T> toFracturedJson(gson: Gson, value: T): String {
            return gson.toFracturedJson(value)
        }

        /**
         * Serializes an object and formats it with FracturedJson.
         *
         * @param gson The Gson instance to use for serialization
         * @param value The object to serialize
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun <T> toFracturedJson(gson: Gson, value: T, options: FracturedJsonOptions): String {
            return gson.toFracturedJson(value, options)
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
