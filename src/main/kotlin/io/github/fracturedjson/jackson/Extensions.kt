package io.github.fracturedjson.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Writer

/**
 * Extension functions for formatting JSON with Jackson types.
 */

// ==================== JsonNode Extensions ====================

/**
 * Formats this JsonNode using FracturedJson formatting.
 *
 * @param options Formatting options (uses defaults if not specified)
 * @return Formatted JSON string
 */
fun JsonNode.toFracturedJson(options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val item = JsonNodeConverter.convert(this)
    return Formatter(options).format(item)
}

/**
 * Formats this JsonNode to a Writer.
 *
 * @param writer The Writer to write to
 * @param options Formatting options
 */
fun JsonNode.toFracturedJson(writer: Writer, options: FracturedJsonOptions = FracturedJsonOptions()) {
    val item = JsonNodeConverter.convert(this)
    Formatter(options).format(listOf(item), 0, writer)
}

/**
 * Minifies this JsonNode.
 *
 * @return Minified JSON string
 */
fun JsonNode.minify(): String {
    val item = JsonNodeConverter.convert(this)
    return Formatter().minify(listOf(item))
}

/**
 * Converts this JsonNode to a JsonItem tree.
 *
 * @return JsonItem representation
 */
fun JsonNode.toJsonItem(): JsonItem {
    return JsonNodeConverter.convert(this)
}

// ==================== ObjectMapper Extensions ====================

/**
 * Parses JSON and reformats it with FracturedJson.
 *
 * @param json The JSON string to format
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun ObjectMapper.formatJson(json: String, options: FracturedJsonOptions = FracturedJsonOptions()): String {
    val node = this.readTree(json)
    return node.toFracturedJson(options)
}

/**
 * Parses JSON and minifies it.
 *
 * @param json The JSON string to minify
 * @return Minified JSON string
 */
fun ObjectMapper.minifyJson(json: String): String {
    val node = this.readTree(json)
    return node.minify()
}

/**
 * Serializes an object and formats it with FracturedJson.
 *
 * @param value The object to serialize
 * @param options Formatting options
 * @return Formatted JSON string
 */
fun <T> ObjectMapper.writeValueAsFracturedJson(
    value: T,
    options: FracturedJsonOptions = FracturedJsonOptions()
): String {
    val node = this.valueToTree<JsonNode>(value)
    return node.toFracturedJson(options)
}

// ==================== Coroutine Extensions ====================

/**
 * Formats this JsonNode asynchronously using FracturedJson.
 *
 * @param options Formatting options
 * @return Formatted JSON string
 */
suspend fun JsonNode.toFracturedJsonAsync(
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
suspend fun ObjectMapper.formatJsonAsync(
    json: String,
    options: FracturedJsonOptions = FracturedJsonOptions()
): String = withContext(Dispatchers.Default) {
    formatJson(json, options)
}

/**
 * Minifies this JsonNode asynchronously.
 *
 * @return Minified JSON string
 */
suspend fun JsonNode.minifyAsync(): String = withContext(Dispatchers.Default) {
    minify()
}

// ==================== Builder DSL ====================

/**
 * Formats the given JsonNode with custom options built using DSL.
 *
 * Example:
 * ```kotlin
 * val formatted = jsonNode.formatWith {
 *     maxTotalLineLength = 80
 *     nestedBracketPadding = false
 * }
 * ```
 */
inline fun JsonNode.formatWith(
    block: FracturedJsonOptions.() -> Unit
): String {
    val options = FracturedJsonOptions().apply(block)
    return toFracturedJson(options)
}
