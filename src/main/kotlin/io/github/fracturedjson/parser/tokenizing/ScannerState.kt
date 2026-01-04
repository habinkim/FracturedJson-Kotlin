package io.github.fracturedjson.parser.tokenizing

import io.github.fracturedjson.core.FracturedJsonException
import io.github.fracturedjson.core.InputPosition

/**
 * Maintains state information during JSON tokenization.
 *
 * This class tracks position data, buffers content, and manages token creation operations
 * as the scanner processes input characters.
 */
class ScannerState {
    /**
     * Buffer for accumulating token characters.
     */
    val buffer: StringBuilder = StringBuilder()

    /**
     * Current position in the input stream.
     */
    var currentPosition: InputPosition = InputPosition.START
        private set

    /**
     * Position where the current token started.
     */
    var tokenPosition: InputPosition = InputPosition.START
        private set

    /**
     * Whether any non-whitespace character has been seen since the last newline.
     * Used to detect blank lines.
     */
    var nonWhitespaceSinceLastNewline: Boolean = false
        private set

    /**
     * Advances the current position by one character.
     *
     * @param isWhitespace Whether the character being advanced over is whitespace
     */
    fun advance(isWhitespace: Boolean) {
        currentPosition = InputPosition(
            index = currentPosition.index + 1,
            row = currentPosition.row,
            column = currentPosition.column + 1
        )
        if (!isWhitespace) {
            nonWhitespaceSinceLastNewline = true
        }
    }

    /**
     * Records a newline, updating position and resetting line-tracking state.
     */
    fun newLine() {
        currentPosition = InputPosition(
            index = currentPosition.index + 1,
            row = currentPosition.row + 1,
            column = 0
        )
        nonWhitespaceSinceLastNewline = false
    }

    /**
     * Marks the current position as the start of a new token.
     */
    fun setTokenStart() {
        tokenPosition = currentPosition
        buffer.clear()
    }

    /**
     * Creates a token from the current buffer contents.
     *
     * @param type The type of token to create
     * @param trimEnd Whether to trim trailing whitespace from the token text
     * @return A new JsonToken with the buffer contents
     */
    fun makeTokenFromBuffer(type: TokenType, trimEnd: Boolean = false): JsonToken {
        val text = if (trimEnd) {
            buffer.toString().trimEnd()
        } else {
            buffer.toString()
        }
        return JsonToken(type, text, tokenPosition)
    }

    /**
     * Creates a token with explicit text.
     *
     * @param type The type of token to create
     * @param text The text content for the token
     * @return A new JsonToken with the specified text
     */
    fun makeToken(type: TokenType, text: String): JsonToken {
        return JsonToken(type, text, tokenPosition)
    }

    /**
     * Throws a FracturedJsonException with position information.
     *
     * @param message The error message
     * @throws FracturedJsonException Always
     */
    fun throwError(message: String): Nothing {
        throw FracturedJsonException.create(message, currentPosition)
    }
}
