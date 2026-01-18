package io.github.fracturedjson.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import java.io.Writer

/**
 * Java-friendly utility class for FracturedJson formatting with Fastjson2.
 *
 * This class provides static methods that can be easily called from Java code.
 *
 * Example usage in Java:
 * ```java
 * JSONObject obj = JSON.parseObject("{\"name\":\"test\"}");
 *
 * // Format with default options
 * String formatted = Fastjson2Support.format(obj);
 *
 * // Format with custom options
 * FracturedJsonOptions options = new FracturedJsonOptions();
 * options.setMaxTotalLineLength(80);
 * String formatted = Fastjson2Support.format(obj, options);
 *
 * // Format JSON string directly
 * String formatted = Fastjson2Support.formatJson(jsonString);
 * ```
 */
class Fastjson2Support private constructor() {
    companion object {
        // ==================== JSONObject Formatting ====================

        /**
         * Formats a JSONObject using FracturedJson formatting with default options.
         *
         * @param obj The JSONObject to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(obj: JSONObject): String {
            return obj.toFracturedJson()
        }

        /**
         * Formats a JSONObject using FracturedJson formatting.
         *
         * @param obj The JSONObject to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(obj: JSONObject, options: FracturedJsonOptions): String {
            return obj.toFracturedJson(options)
        }

        /**
         * Formats a JSONObject to a Writer with default options.
         *
         * @param obj The JSONObject to format
         * @param writer The Writer to write to
         */
        @JvmStatic
        fun format(obj: JSONObject, writer: Writer) {
            obj.toFracturedJson(writer)
        }

        /**
         * Formats a JSONObject to a Writer.
         *
         * @param obj The JSONObject to format
         * @param writer The Writer to write to
         * @param options Formatting options
         */
        @JvmStatic
        fun format(obj: JSONObject, writer: Writer, options: FracturedJsonOptions) {
            obj.toFracturedJson(writer, options)
        }

        /**
         * Minifies a JSONObject.
         *
         * @param obj The JSONObject to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minify(obj: JSONObject): String {
            return obj.minify()
        }

        /**
         * Converts a JSONObject to a JsonItem tree.
         *
         * @param obj The JSONObject to convert
         * @return JsonItem representation
         */
        @JvmStatic
        fun toJsonItem(obj: JSONObject): JsonItem {
            return obj.toJsonItem()
        }

        // ==================== JSONArray Formatting ====================

        /**
         * Formats a JSONArray using FracturedJson formatting with default options.
         *
         * @param array The JSONArray to format
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(array: JSONArray): String {
            return array.toFracturedJson()
        }

        /**
         * Formats a JSONArray using FracturedJson formatting.
         *
         * @param array The JSONArray to format
         * @param options Formatting options
         * @return Formatted JSON string
         */
        @JvmStatic
        fun format(array: JSONArray, options: FracturedJsonOptions): String {
            return array.toFracturedJson(options)
        }

        /**
         * Formats a JSONArray to a Writer with default options.
         *
         * @param array The JSONArray to format
         * @param writer The Writer to write to
         */
        @JvmStatic
        fun format(array: JSONArray, writer: Writer) {
            array.toFracturedJson(writer)
        }

        /**
         * Formats a JSONArray to a Writer.
         *
         * @param array The JSONArray to format
         * @param writer The Writer to write to
         * @param options Formatting options
         */
        @JvmStatic
        fun format(array: JSONArray, writer: Writer, options: FracturedJsonOptions) {
            array.toFracturedJson(writer, options)
        }

        /**
         * Minifies a JSONArray.
         *
         * @param array The JSONArray to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minify(array: JSONArray): String {
            return array.minify()
        }

        /**
         * Converts a JSONArray to a JsonItem tree.
         *
         * @param array The JSONArray to convert
         * @return JsonItem representation
         */
        @JvmStatic
        fun toJsonItem(array: JSONArray): JsonItem {
            return array.toJsonItem()
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
            return json.reformatJsonWithFastjson2()
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
            return json.reformatJsonWithFastjson2(options)
        }

        /**
         * Parses and minifies a JSON string.
         *
         * @param json The JSON string to minify
         * @return Minified JSON string
         */
        @JvmStatic
        fun minifyJson(json: String): String {
            return json.minifyJsonWithFastjson2()
        }

        // ==================== Parsing Helpers ====================

        /**
         * Parses a JSON string to a JSONObject.
         *
         * @param json The JSON string to parse
         * @return Parsed JSONObject
         */
        @JvmStatic
        fun parseObject(json: String): JSONObject {
            return JSON.parseObject(json)
        }

        /**
         * Parses a JSON string to a JSONArray.
         *
         * @param json The JSON string to parse
         * @return Parsed JSONArray
         */
        @JvmStatic
        fun parseArray(json: String): JSONArray {
            return JSON.parseArray(json)
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
