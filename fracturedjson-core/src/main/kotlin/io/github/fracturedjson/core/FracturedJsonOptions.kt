package io.github.fracturedjson.core

/**
 * Configuration options for JSON formatting.
 *
 * This data class contains all settings that control how JSON output is formatted,
 * including line lengths, indentation, spacing, comment handling, and more.
 */
data class FracturedJsonOptions(
    /**
     * Controls what sort of line endings to use.
     */
    var jsonEolStyle: EolStyle = EolStyle.Default,

    /**
     * Maximum length of a text row before the content is considered complex enough to expand.
     * Default is 120.
     */
    var maxTotalLineLength: Int = 120,

    /**
     * Maximum nesting depth for elements to be inlined on a single line.
     * Default is 2.
     */
    var maxInlineComplexity: Int = 2,

    /**
     * Maximum complexity for compact multi-line array formatting.
     * Default is 2.
     */
    var maxCompactArrayComplexity: Int = 2,

    /**
     * Maximum complexity for table-formatted rows.
     * Default is 2.
     */
    var maxTableRowComplexity: Int = 2,

    /**
     * Maximum padding to add after property names for alignment.
     * Default is 16.
     */
    var maxPropNamePadding: Int = 16,

    /**
     * If true, the colon comes before the property name padding.
     * Default is false.
     */
    var colonBeforePropNamePadding: Boolean = false,

    /**
     * Controls where commas are placed in table-formatted elements.
     */
    var tableCommaPlacement: TableCommaPlacement = TableCommaPlacement.BeforePaddingExceptNumbers,

    /**
     * Minimum number of items per row in compact array formatting.
     * Default is 3.
     */
    var minCompactArrayRowItems: Int = 3,

    /**
     * Depth at which to always expand elements regardless of complexity.
     * Use -1 to disable. Default is -1.
     */
    var alwaysExpandDepth: Int = -1,

    /**
     * Whether to add spaces inside brackets of nested structures.
     * Default is true.
     */
    var nestedBracketPadding: Boolean = true,

    /**
     * Whether to add spaces inside brackets of simple structures.
     * Default is false.
     */
    var simpleBracketPadding: Boolean = false,

    /**
     * Whether to add space after colons.
     * Default is true.
     */
    var colonPadding: Boolean = true,

    /**
     * Whether to add space after commas.
     * Default is true.
     */
    var commaPadding: Boolean = true,

    /**
     * Whether to add space around comments.
     * Default is true.
     */
    var commentPadding: Boolean = true,

    /**
     * How lists or columns of numbers should be aligned.
     */
    var numberListAlignment: NumberListAlignment = NumberListAlignment.Decimal,

    /**
     * Number of spaces to use for each indentation level.
     * Default is 4.
     */
    var indentSpaces: Int = 4,

    /**
     * Whether to use tabs instead of spaces for indentation.
     * Default is false.
     */
    var useTabToIndent: Boolean = false,

    /**
     * String to prefix each line with (e.g., for embedding in code).
     * Default is empty.
     */
    var prefixString: String = "",

    /**
     * Policy for handling comments in the input.
     */
    var commentPolicy: CommentPolicy = CommentPolicy.TreatAsError,

    /**
     * Whether to preserve blank lines from the input.
     * Default is false.
     */
    var preserveBlankLines: Boolean = false,

    /**
     * Whether to allow trailing commas in arrays and objects.
     * Default is false.
     */
    var allowTrailingCommas: Boolean = false
) {
    companion object {
        /**
         * Returns a new instance with recommended default settings.
         */
        fun recommended(): FracturedJsonOptions = FracturedJsonOptions()
    }
}
