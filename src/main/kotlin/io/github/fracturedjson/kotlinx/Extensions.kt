package io.github.fracturedjson.kotlinx

import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.Writer

/**
 * Extension functions for formatting JSON with kotlinx.serialization types.
 */

// ==================== JsonElement Extensions ====================

/**
 * Formats this JsonElement using FracturedJson formatting.
 *
 * @param options Formatting options (uses defaults if not specified)
 * @return Formatted JSON string
 */
fun JsonElement.toFracturedJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val item = JsonElementConverter.convert(this)
    return Formatter(options).format(item)
}

/**
 * Formats this JsonElement to a Writer.
 *
 * @param writer The Writer to write to
 * @param options Formatting options
 */
fun JsonElement.toFracturedJson(writer: Writer, options: FracturedJsonOptions = FracturedJsonOptions()) {
    val item = JsonElementConverter.convert(this)
    Formatter(options).format(listOf(item), 0, writer)
}

/**
 * Minifies this JsonElement.
 *
 * @return Minified JSON string
 */
fun JsonElement.minify(): String {
    val item = JsonElementConverter.convert(this)
    return Formatter().minify(listOf(item))
}

/**
 * Converts this JsonElement to a JsonItem tree.
 *
 * @return JsonItem representation
 */
fun JsonElement.toJsonItem(): JsonItem {
    return JsonElementConverter.convert(this)
}

// ==================== String Extensions ====================

/**
 * Parses this JSON string and reformats it with FracturedJson.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun String.reformatJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val element = Json.parseToJsonElement(this)
    return element.toFracturedJson(options)
}

/**
 * Parses this JSON string and minifies it.
 *
 * @return Minified JSON string
 */
fun String.minifyJson(): String {
    val element = Json.parseToJsonElement(this)
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
 * Parses and reformats this JSON string asynchronously.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun String.reformatJsonAsync(
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    reformatJson(options)
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
 * DSL for building formatted JSON.
 *
 * Example:
 * ```kotlin
 * val json = buildFracturedJson {
 *     maxTotalLineLength = 80
 *     indentSpaces = 2
 * }.format(jsonElement)
 * ```
 */
inline fun buildFracturedJson(
    block: FracturedJsonOptions.() -> Unit
): Formatter {
    val options = FracturedJsonOptions().apply(block)
    return Formatter(options)
}

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
