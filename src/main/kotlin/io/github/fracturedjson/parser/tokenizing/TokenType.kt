package io.github.fracturedjson.parser.tokenizing

/**
 * Types of tokens that can be read from a stream of JSON text.
 *
 * Comments aren't part of the official JSON standard, but we're supporting them anyway.
 * BlankLine isn't typically a token by itself, but we want to try to preserve those.
 */
enum class TokenType {
    /**
     * A character or sequence that doesn't make sense in a JSON document.
     */
    Invalid,

    /**
     * Open square bracket: [
     */
    BeginArray,

    /**
     * Close square bracket: ]
     */
    EndArray,

    /**
     * Open curly bracket: {
     */
    BeginObject,

    /**
     * Close curly bracket: }
     */
    EndObject,

    /**
     * Quotation marks and the characters between them.
     */
    String,

    /**
     * Digits, maybe a sign or a decimal point, occasionally an "e".
     */
    Number,

    /**
     * The keyword null.
     */
    Null,

    /**
     * The keyword true.
     */
    True,

    /**
     * The keyword false.
     */
    False,

    /**
     * A comment beginning with slash-star and ending with star-slash.
     */
    BlockComment,

    /**
     * A comment beginning with two slashes and continuing to the end of the line.
     */
    LineComment,

    /**
     * A line with no characters, or only whitespace.
     */
    BlankLine,

    /**
     * The symbol ","
     */
    Comma,

    /**
     * The symbol ":"
     */
    Colon
}
