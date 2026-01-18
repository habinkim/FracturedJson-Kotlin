@file:JvmName("Fastjson2Extensions")
package io.github.fracturedjson.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Writer

/**
 * Extension functions for formatting JSON with Fastjson2 types.
 */

// ==================== JSONObject Extensions ====================

/**
 * Formats this JSONObject using FracturedJson formatting.
 *
 * @param options Formatting options (uses defaults if not specified)
 * @return Formatted JSON string
 */
fun JSONObject.toFracturedJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val item = FastJson2Converter.convert(this)
    return Formatter(options).format(item)
}

/**
 * Formats this JSONObject to a Writer.
 *
 * @param writer The Writer to write to
 * @param options Formatting options
 */
fun JSONObject.toFracturedJson(writer: Writer, options: FracturedJsonOptions = FracturedJsonOptions()) {
    val item = FastJson2Converter.convert(this)
    Formatter(options).format(listOf(item), 0, writer)
}

/**
 * Minifies this JSONObject.
 *
 * @return Minified JSON string
 */
fun JSONObject.minify(): String {
    val item = FastJson2Converter.convert(this)
    return Formatter().minify(listOf(item))
}

/**
 * Converts this JSONObject to a JsonItem tree.
 *
 * @return JsonItem representation
 */
fun JSONObject.toJsonItem(): JsonItem {
    return FastJson2Converter.convert(this)
}

// ==================== JSONArray Extensions ====================

/**
 * Formats this JSONArray using FracturedJson formatting.
 *
 * @param options Formatting options (uses defaults if not specified)
 * @return Formatted JSON string
 */
fun JSONArray.toFracturedJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val item = FastJson2Converter.convert(this)
    return Formatter(options).format(item)
}

/**
 * Formats this JSONArray to a Writer.
 *
 * @param writer The Writer to write to
 * @param options Formatting options
 */
fun JSONArray.toFracturedJson(writer: Writer, options: FracturedJsonOptions = FracturedJsonOptions()) {
    val item = FastJson2Converter.convert(this)
    Formatter(options).format(listOf(item), 0, writer)
}

/**
 * Minifies this JSONArray.
 *
 * @return Minified JSON string
 */
fun JSONArray.minify(): String {
    val item = FastJson2Converter.convert(this)
    return Formatter().minify(listOf(item))
}

/**
 * Converts this JSONArray to a JsonItem tree.
 *
 * @return JsonItem representation
 */
fun JSONArray.toJsonItem(): JsonItem {
    return FastJson2Converter.convert(this)
}

// ==================== String Extensions ====================

/**
 * Reformats a JSON string using FracturedJson with Fastjson2 parser.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun String.reformatJsonWithFastjson2(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val parsed = JSON.parse(this)
    return when (parsed) {
        is JSONObject -> parsed.toFracturedJson(options)
        is JSONArray -> parsed.toFracturedJson(options)
        else -> this
    }
}

/**
 * Minifies a JSON string using Fastjson2 parser.
 *
 * @return Minified JSON string
 */
fun String.minifyJsonWithFastjson2(): String {
    val parsed = JSON.parse(this)
    return when (parsed) {
        is JSONObject -> parsed.minify()
        is JSONArray -> parsed.minify()
        else -> this
    }
}

// ==================== Coroutine Extensions ====================

/**
 * Formats this JSONObject asynchronously using FracturedJson.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun JSONObject.toFracturedJsonAsync(
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    toFracturedJson(options)
}

/**
 * Formats this JSONArray asynchronously using FracturedJson.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun JSONArray.toFracturedJsonAsync(
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    toFracturedJson(options)
}

/**
 * Minifies this JSONObject asynchronously.
 *
 * @return Minified JSON string
 */
suspend fun JSONObject.minifyAsync(): String = withContext(Dispatchers.Default) {
    minify()
}

/**
 * Minifies this JSONArray asynchronously.
 *
 * @return Minified JSON string
 */
suspend fun JSONArray.minifyAsync(): String = withContext(Dispatchers.Default) {
    minify()
}

// ==================== Builder DSL ====================

/**
 * Formats the given JSONObject with custom options built using DSL.
 *
 * Example:
 * ```kotlin
 * val formatted = jsonObject.formatWith {
 *     maxTotalLineLength = 80
 *     nestedBracketPadding = false
 * }
 * ```
 */
inline fun JSONObject.formatWith(
    block: FracturedJsonOptions.() -> Unit
): String {
    val options = FracturedJsonOptions().apply(block)
    return toFracturedJson(options)
}

/**
 * Formats the given JSONArray with custom options built using DSL.
 *
 * Example:
 * ```kotlin
 * val formatted = jsonArray.formatWith {
 *     maxTotalLineLength = 80
 *     nestedBracketPadding = false
 * }
 * ```
 */
inline fun JSONArray.formatWith(
    block: FracturedJsonOptions.() -> Unit
): String {
    val options = FracturedJsonOptions().apply(block)
    return toFracturedJson(options)
}

// ==================== Inline Serialization ====================

/**
 * Serializes an object to formatted JSON using Fastjson2.
 *
 * @param value The object to serialize
 * @param options Formatting options
 * @return Formatted JSON string
 */
inline fun <reified T> toFracturedJson(
    value: T,
    options: FracturedJsonOptions = FracturedJsonOptions()
): String {
    val jsonString = JSON.toJSONString(value)
    return jsonString.reformatJsonWithFastjson2(options)
}
