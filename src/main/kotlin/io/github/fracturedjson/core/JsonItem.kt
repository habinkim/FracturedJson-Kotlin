package io.github.fracturedjson.core

/**
 * A distinct thing that can be where ever JSON values are expected in a JSON-with-comments doc.
 *
 * This could be an actual data value, such as a string, number, array, etc. (generally referred to
 * here as "elements"), or it could be a blank line or standalone comment. In some cases, comments
 * won't be standalone JsonItems, but will instead be attached to elements to which they seem to belong.
 *
 * Much of this data is produced by the Parser, but some of the properties - like all the length ones -
 * are not set by Parser, but rather, provided for use by Formatter.
 */
class JsonItem(
    /**
     * The type of this JSON item.
     */
    var type: JsonItemType = JsonItemType.Null
) {
    /**
     * Position in the input where this item was found.
     */
    var inputPosition: InputPosition = InputPosition.START

    /**
     * A measure of how deeply nested this item is.
     */
    var complexity: Int = 0

    /**
     * The property name if this item is a value in an object.
     */
    var name: String = ""

    /**
     * The text representation of this item's value (for primitives).
     */
    var value: String = ""

    /**
     * Comment that appears before this item on the same line.
     */
    var prefixComment: String = ""

    /**
     * Comment that appears between the property name and value (for object properties).
     */
    var middleComment: String = ""

    /**
     * Whether the middle comment contains a newline.
     */
    var middleCommentHasNewline: Boolean = false

    /**
     * Whether the middle comment is a line-style comment (// style).
     * Line comments force all child containers to be fully expanded.
     */
    var isMiddleCommentLineStyle: Boolean = false

    /**
     * Comment that appears after this item on the same line.
     */
    var postfixComment: String = ""

    /**
     * Whether the postfix comment is a line-style comment (// style).
     */
    var isPostCommentLineStyle: Boolean = false

    /**
     * Calculated length of the name for formatting purposes.
     * Set to -1 to indicate not yet computed (Formatter will compute).
     * Converters may pre-compute this to avoid redundant stringLengthFunc calls.
     */
    var nameLength: Int = -1

    /**
     * Calculated length of the value for formatting purposes.
     * Set to -1 to indicate not yet computed (Formatter will compute).
     * Converters may pre-compute this to avoid redundant stringLengthFunc calls.
     */
    var valueLength: Int = -1

    /**
     * Calculated length of the prefix comment for formatting purposes.
     */
    var prefixCommentLength: Int = 0

    /**
     * Calculated length of the middle comment for formatting purposes.
     */
    var middleCommentLength: Int = 0

    /**
     * Calculated length of the postfix comment for formatting purposes.
     */
    var postfixCommentLength: Int = 0

    /**
     * Minimum total length this item requires when formatted.
     */
    var minimumTotalLength: Int = 0

    /**
     * Config generation counter from the Formatter that last computed this item's lengths.
     * Used to skip redundant computeItemLengths() calls when the same Formatter
     * is used repeatedly with unchanged options.
     */
    @JvmField
    internal var computedGeneration: Long = -1

    /**
     * Whether this item requires multiple lines when formatted.
     */
    var requiresMultipleLines: Boolean = false

    /**
     * Child items (for arrays and objects).
     */
    var children: List<JsonItem> = emptyList()

    override fun toString(): String {
        val shortName = if (name.length <= 15) name else "${name.substring(0, 12)}..."
        val shortVal = if (value.length <= 15) value else "${value.substring(0, 12)}..."
        return "{ Name = $shortName, Value = $shortVal }"
    }

    companion object {
        /**
         * Creates a JsonItem for a null value.
         */
        fun nullItem(): JsonItem = JsonItem(JsonItemType.Null).apply { value = "null" }

        /**
         * Creates a JsonItem for a boolean value.
         */
        fun booleanItem(value: Boolean): JsonItem = JsonItem(
            if (value) JsonItemType.True else JsonItemType.False
        ).apply { this.value = value.toString().lowercase() }

        /**
         * Creates a JsonItem for a string value.
         */
        fun stringItem(value: String): JsonItem = JsonItem(JsonItemType.String).apply {
            this.value = "\"${escapeString(value)}\""
        }

        /**
         * Creates a JsonItem for a number value.
         */
        fun numberItem(value: Number): JsonItem = JsonItem(JsonItemType.Number).apply {
            this.value = value.toString()
        }

        /**
         * Creates a JsonItem for an array.
         */
        fun arrayItem(children: List<JsonItem>): JsonItem = JsonItem(JsonItemType.Array).apply {
            this.children = children
        }

        /**
         * Creates a JsonItem for an object.
         */
        fun objectItem(children: List<JsonItem>): JsonItem = JsonItem(JsonItemType.Object).apply {
            this.children = children
        }

        private fun escapeString(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (c.code < 0x20) {
                            append("\\u${c.code.toString(16).padStart(4, '0')}")
                        } else {
                            append(c)
                        }
                    }
                }
            }
        }
    }
}
