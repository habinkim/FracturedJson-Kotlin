package io.github.fracturedjson.core

/**
 * Type of a piece of a JSON document including comments and blank lines.
 */
enum class JsonItemType {
    /**
     * The literal value "null"
     */
    Null,

    /**
     * The literal value "false"
     */
    False,

    /**
     * The literal value "true"
     */
    True,

    /**
     * A bunch of characters between quotes.
     */
    String,

    /**
     * A number, possibly in scientific notation.
     */
    Number,

    /**
     * An object - a collection of key/value pairs
     */
    Object,

    /**
     * An array - a list of values
     */
    Array,

    /**
     * A line with nothing but whitespace.
     */
    BlankLine,

    /**
     * A comment beginning with two slashes and continuing to the end of the line.
     */
    LineComment,

    /**
     * A comment starting with slash star and ending with star slash.
     */
    BlockComment
}
