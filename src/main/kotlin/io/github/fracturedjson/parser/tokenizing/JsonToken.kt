package io.github.fracturedjson.parser.tokenizing

import io.github.fracturedjson.core.InputPosition

/**
 * A piece of JSON text that makes sense to treat as a whole thing when analyzing a document's structure.
 *
 * For example, a string is a token, regardless of whether it represents a value or an object key.
 *
 * @property type What sort of JSON thing this is.
 * @property text The text that makes up this token from the original input.
 * @property inputPosition Location of the start of this token in the input.
 */
data class JsonToken(
    val type: TokenType,
    val text: String,
    val inputPosition: InputPosition
)
