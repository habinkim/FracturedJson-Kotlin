package io.github.fracturedjson.core.formatting

import io.github.fracturedjson.core.EolStyle
import io.github.fracturedjson.core.FracturedJsonOptions
import io.github.fracturedjson.core.JsonItemType

/**
 * Manages formatting tokens with appropriate padding based on options.
 *
 * This class pre-computes commonly used formatting strings (brackets, commas, colons, etc.)
 * with appropriate padding and caches them for efficient access during formatting.
 *
 * @param options The formatting options to use
 * @param strLenFunc Function to calculate display length of strings (for Unicode support)
 */
internal class PaddedFormattingTokens(
    options: FracturedJsonOptions,
    private val strLenFunc: (String) -> Int
) {
    // Token strings
    val comma: String
    val colon: String
    val comment: String
    val eol: String
    val dummyComma: String

    // Token lengths
    val commaLen: Int
    val colonLen: Int
    val commentLen: Int
    val literalNullLen: Int
    val literalTrueLen: Int
    val literalFalseLen: Int
    val prefixStringLen: Int

    // Bracket arrays indexed by BracketPaddingType ordinal
    private val arrStarts: Array<String>
    private val arrEnds: Array<String>
    private val objStarts: Array<String>
    private val objEnds: Array<String>

    private val arrStartLens: IntArray
    private val arrEndLens: IntArray
    private val objStartLens: IntArray
    private val objEndLens: IntArray

    // Cached indent strings
    private val indentCache = mutableListOf<String>()
    private val baseIndent: String
    private val prefixString: String

    init {
        // Set up EOL string based on options
        eol = when (options.jsonEolStyle) {
            EolStyle.Crlf -> "\r\n"
            EolStyle.Lf -> "\n"
            EolStyle.Default -> System.lineSeparator()
        }

        // Set up indent string
        baseIndent = if (options.useTabToIndent) {
            "\t"
        } else {
            " ".repeat(options.indentSpaces)
        }

        prefixString = options.prefixString
        prefixStringLen = strLenFunc(prefixString)

        // Set up tokens with padding
        comma = if (options.commaPadding) ", " else ","
        colon = if (options.colonPadding) ": " else ":"
        comment = if (options.commentPadding) " " else ""
        dummyComma = " ".repeat(comma.length)

        commaLen = strLenFunc(comma)
        colonLen = strLenFunc(colon)
        commentLen = strLenFunc(comment)

        // Calculate literal lengths
        literalNullLen = strLenFunc("null")
        literalTrueLen = strLenFunc("true")
        literalFalseLen = strLenFunc("false")

        // Set up bracket padding variations
        val simplePad = if (options.simpleBracketPadding) " " else ""
        val nestedPad = if (options.nestedBracketPadding) " " else ""

        // Array brackets: [empty], [simple ], [ nested ]
        arrStarts = arrayOf(
            "[",                    // Empty
            "[$simplePad",          // Simple
            "[$nestedPad"           // Complex/Nested
        )
        arrEnds = arrayOf(
            "]",                    // Empty
            "$simplePad]",          // Simple
            "$nestedPad]"           // Complex/Nested
        )

        // Object brackets: {empty}, {simple }, { nested }
        objStarts = arrayOf(
            "{",                    // Empty
            "{$simplePad",          // Simple
            "{$nestedPad"           // Complex/Nested
        )
        objEnds = arrayOf(
            "}",                    // Empty
            "$simplePad}",          // Simple
            "$nestedPad}"           // Complex/Nested
        )

        // Calculate bracket lengths
        arrStartLens = arrStarts.map { strLenFunc(it) }.toIntArray()
        arrEndLens = arrEnds.map { strLenFunc(it) }.toIntArray()
        objStartLens = objStarts.map { strLenFunc(it) }.toIntArray()
        objEndLens = objEnds.map { strLenFunc(it) }.toIntArray()

        // Initialize indent cache with base level
        indentCache.add(prefixString)
    }

    // Array bracket accessors
    fun arrStart(paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        arrStarts[paddingType.ordinal]

    fun arrEnd(paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        arrEnds[paddingType.ordinal]

    fun arrStartLen(paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        arrStartLens[paddingType.ordinal]

    fun arrEndLen(paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        arrEndLens[paddingType.ordinal]

    // Object bracket accessors
    fun objStart(paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        objStarts[paddingType.ordinal]

    fun objEnd(paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        objEnds[paddingType.ordinal]

    fun objStartLen(paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        objStartLens[paddingType.ordinal]

    fun objEndLen(paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        objEndLens[paddingType.ordinal]

    // Generic bracket accessors based on type
    fun start(type: JsonItemType, paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        when (type) {
            JsonItemType.Array -> arrStart(paddingType)
            JsonItemType.Object -> objStart(paddingType)
            else -> throw IllegalArgumentException("Type must be Array or Object")
        }

    fun end(type: JsonItemType, paddingType: BracketPaddingType = BracketPaddingType.Complex): String =
        when (type) {
            JsonItemType.Array -> arrEnd(paddingType)
            JsonItemType.Object -> objEnd(paddingType)
            else -> throw IllegalArgumentException("Type must be Array or Object")
        }

    fun startLen(type: JsonItemType, paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        when (type) {
            JsonItemType.Array -> arrStartLen(paddingType)
            JsonItemType.Object -> objStartLen(paddingType)
            else -> throw IllegalArgumentException("Type must be Array or Object")
        }

    fun endLen(type: JsonItemType, paddingType: BracketPaddingType = BracketPaddingType.Complex): Int =
        when (type) {
            JsonItemType.Array -> arrEndLen(paddingType)
            JsonItemType.Object -> objEndLen(paddingType)
            else -> throw IllegalArgumentException("Type must be Array or Object")
        }

    /**
     * Returns an indent string for the given nesting level.
     * Caches indent strings for efficient repeated access.
     */
    fun indent(level: Int): String {
        require(level >= 0) { "Indent level must be non-negative" }

        // Extend cache if needed
        while (indentCache.size <= level) {
            val prevIndent = indentCache.last()
            indentCache.add(prevIndent + baseIndent)
        }

        return indentCache[level]
    }

    /**
     * Returns the display length of an indent at the given level.
     */
    fun indentLen(level: Int): Int = strLenFunc(indent(level))
}
