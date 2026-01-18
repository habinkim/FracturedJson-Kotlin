@file:JvmName("GsonExtensions")
package io.github.fracturedjson.gson

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Writer

/**
 * Extension functions for formatting JSON with Gson types.
 */

// ==================== JsonElement Extensions ====================

/**
 * Formats this JsonElement using FracturedJson formatting.
 *
 * @param options Formatting options (uses defaults if not specified)
 * @return Formatted JSON string
 */
fun JsonElement.toFracturedJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val item = GsonElementConverter.convert(this)
    return Formatter(options).format(item)
}

/**
 * Formats this JsonElement to a Writer.
 *
 * @param writer The Writer to write to
 * @param options Formatting options
 */
fun JsonElement.toFracturedJson(writer: Writer, options: FracturedJsonOptions = FracturedJsonOptions()) {
    val item = GsonElementConverter.convert(this)
    Formatter(options).format(listOf(item), 0, writer)
}

/**
 * Minifies this JsonElement.
 *
 * @return Minified JSON string
 */
fun JsonElement.minify(): String {
    val item = GsonElementConverter.convert(this)
    return Formatter().minify(listOf(item))
}

/**
 * Converts this JsonElement to a JsonItem tree.
 *
 * @return JsonItem representation
 */
fun JsonElement.toJsonItem(): JsonItem {
    return GsonElementConverter.convert(this)
}

// ==================== Gson Extensions ====================

/**
 * Parses JSON and reformats it with FracturedJson.
 *
 * @param json The JSON string to format
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun Gson.formatJson(json: String, options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val element = JsonParser.parseString(json)
    return element.toFracturedJson(options)
}

/**
 * Parses JSON and minifies it.
 *
 * @param json The JSON string to minify
 * @return Minified JSON string
 */
fun Gson.minifyJson(json: String): String {
    val element = JsonParser.parseString(json)
    return element.minify()
}

/**
 * Serializes an object and formats it with FracturedJson.
 *
 * @param value The object to serialize
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun <T> Gson.toFracturedJson(
    value: T,
    options: FracturedJsonOptions = FracturedJsonOptions()
): String {
    val element = this.toJsonTree(value)
    return element.toFracturedJson(options)
}

// ==================== String Extensions ====================

/**
 * Reformats a JSON string using FracturedJson with Gson parser.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun String.reformatJsonWithGson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val element = JsonParser.parseString(this)
    return element.toFracturedJson(options)
}

/**
 * Minifies a JSON string using Gson parser.
 *
 * @return Minified JSON string
 */
fun String.minifyJsonWithGson(): String {
    val element = JsonParser.parseString(this)
    return element.minify()
}

// ==================== Coroutine Extensions ====================

/**
 * Formats this JsonElement asynchronously using FracturedJson.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun JsonElement.toFracturedJsonAsync(
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    toFracturedJson(options)
}

/**
 * Parses and reformats JSON asynchronously.
 *
 * @param json The JSON string to format
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun Gson.formatJsonAsync(
    json: String,
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    formatJson(json, options)
}

/**
 * Minifies this JsonElement asynchronously.
 *
 * @return Minified JSON string
 */
suspend fun JsonElement.minifyAsync(): String = withContext(Dispatchers.Default) {
    minify()
}

// ==================== Builder DSL ====================

/**
 * Formats the given JsonElement with custom options built using DSL.
 *
 * Example:
 * ```kotlin
 * val formatted = jsonElement.formatWith {
 *     maxTotalLineLength = 80
 *     nestedBracketPadding = false
 * }
 * ```
 */
inline fun JsonElement.formatWith(
    block: FracturedJsonOptions.() -> Unit
): String {
    val options = FracturedJsonOptions().apply(block)
    return toFracturedJson(options)
}
