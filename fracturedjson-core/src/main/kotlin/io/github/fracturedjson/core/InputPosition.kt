package io.github.fracturedjson.core

/**
 * Structure representing a location in an input stream.
 *
 * @property index Number of characters from the start of the input.
 * @property row Number of newlines since the start of the input (0-based line number).
 * @property column Number of characters since the latest newline.
 */
data class InputPosition(
    val index: Int,
    val row: Int,
    val column: Int
) {
    companion object {
        /**
         * Default position at the beginning of input.
         */
        val START = InputPosition(0, 0, 0)
    }

    override fun toString(): String = "idx=$index, row=$row, col=$column"
}
