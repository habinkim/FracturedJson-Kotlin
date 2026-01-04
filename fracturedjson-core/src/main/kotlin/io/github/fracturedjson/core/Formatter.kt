package io.github.fracturedjson.core

import io.github.fracturedjson.core.formatting.*
import java.io.Writer
import kotlin.math.max

/**
 * Formats JSON with intelligent line breaks, alignment, and spacing.
 *
 * The Formatter analyzes JSON structure and applies formatting based on complexity,
 * available space, and configuration options. It supports:
 * - Inline formatting for simple structures
 * - Compact multiline for arrays with many small items
 * - Table formatting for consistent row structures
 * - Expanded formatting for complex nested structures
 *
 * @property options Configuration options for formatting behavior
 * @property stringLengthFunc Function to calculate display length of strings (for Unicode support)
 */
class Formatter(
    var options: FracturedJsonOptions = FracturedJsonOptions(),
    var stringLengthFunc: (String) -> Int = ::stringLengthByCharCount
) {
    private lateinit var pads: PaddedFormattingTokens
    private var currentDepth: Int = 0

    companion object {
        /**
         * Default string length function that counts characters.
         */
        fun stringLengthByCharCount(s: String): Int = s.length
    }

    /**
     * Formats a list of JsonItems and returns the result as a string.
     *
     * @param items The JsonItem tree to format
     * @param startingDepth Initial indentation depth (default 0)
     * @return Formatted JSON string
     */
    fun format(items: List<JsonItem>, startingDepth: Int = 0): String {
        val buffer = StringBuilderBuffer()
        format(items, startingDepth, buffer)
        return buffer.asString()
    }

    /**
     * Formats a list of JsonItems and writes to the provided Writer.
     *
     * @param items The JsonItem tree to format
     * @param startingDepth Initial indentation depth
     * @param writer The Writer to write formatted output to
     */
    fun format(items: List<JsonItem>, startingDepth: Int, writer: Writer) {
        val buffer = LineWriterBuffer(writer)
        format(items, startingDepth, buffer)
    }

    /**
     * Formats a single JsonItem and returns the result as a string.
     *
     * @param item The JsonItem to format
     * @param startingDepth Initial indentation depth (default 0)
     * @return Formatted JSON string
     */
    fun format(item: JsonItem, startingDepth: Int = 0): String {
        return format(listOf(item), startingDepth)
    }

    /**
     * Minifies JSON items, removing unnecessary whitespace while preserving comments if configured.
     *
     * @param items The JsonItem tree to minify
     * @return Minified JSON string
     */
    fun minify(items: List<JsonItem>): String {
        val buffer = StringBuilderBuffer()
        minifyToBuffer(items, buffer)
        return buffer.asString()
    }

    /**
     * Minifies JSON items to a Writer.
     *
     * @param items The JsonItem tree to minify
     * @param writer The Writer to write minified output to
     */
    fun minify(items: List<JsonItem>, writer: Writer) {
        val buffer = LineWriterBuffer(writer)
        minifyToBuffer(items, buffer)
    }

    // ==================== Core Formatting Logic ====================

    private fun format(items: List<JsonItem>, startingDepth: Int, buffer: FormattingBuffer) {
        pads = PaddedFormattingTokens(options, stringLengthFunc)
        currentDepth = startingDepth

        // Compute lengths for all items
        for (item in items) {
            computeItemLengths(item)
        }

        // Format each item
        var needsNewline = false
        for (item in items) {
            if (needsNewline) {
                buffer.endLine(pads.eol)
            }
            formatItem(item, buffer, isRoot = true)
            needsNewline = true
        }

        buffer.flush()
    }

    private fun formatItem(item: JsonItem, buffer: FormattingBuffer, isRoot: Boolean) {
        when (item.type) {
            JsonItemType.Array, JsonItemType.Object -> {
                formatContainer(item, buffer, isRoot)
            }
            JsonItemType.BlankLine -> {
                if (options.preserveBlankLines) {
                    buffer.add(pads.indent(currentDepth))
                }
            }
            JsonItemType.LineComment, JsonItemType.BlockComment -> {
                if (options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(pads.indent(currentDepth))
                    buffer.add(item.value)
                }
            }
            else -> {
                // Primitive values
                buffer.add(pads.indent(currentDepth))
                inlineElement(item, buffer, true)
            }
        }
    }

    private fun formatContainer(item: JsonItem, buffer: FormattingBuffer, isRoot: Boolean) {
        val availableLength = options.maxTotalLineLength - pads.indentLen(currentDepth)

        // Check if we should always expand at this depth
        val forceExpand = options.alwaysExpandDepth >= 0 && currentDepth >= options.alwaysExpandDepth

        // Try inline formatting first
        if (!forceExpand && !item.requiresMultipleLines && item.minimumTotalLength <= availableLength) {
            if (isRoot) buffer.add(pads.indent(currentDepth))
            formatContainerInline(item, buffer)
            return
        }

        // For arrays, try compact multiline
        if (!forceExpand && item.type == JsonItemType.Array &&
            item.complexity <= options.maxCompactArrayComplexity) {
            val compactResult = tryFormatContainerCompactMultiline(item, buffer, isRoot)
            if (compactResult) return
        }

        // Try table formatting for consistent structures
        if (!forceExpand && item.complexity <= options.maxTableRowComplexity) {
            val tableResult = tryFormatContainerTable(item, buffer, isRoot)
            if (tableResult) return
        }

        // Fall back to expanded formatting
        formatContainerExpanded(item, buffer, isRoot)
    }

    private fun formatContainerInline(item: JsonItem, buffer: FormattingBuffer) {
        val paddingType = determinePaddingType(item)

        buffer.add(pads.start(item.type, paddingType))

        val children = getFormattableChildren(item)
        for ((index, child) in children.withIndex()) {
            if (index > 0) {
                buffer.add(pads.comma)
            }
            inlineElement(child, buffer, false)
        }

        buffer.add(pads.end(item.type, paddingType))
    }

    private fun tryFormatContainerCompactMultiline(
        item: JsonItem,
        buffer: FormattingBuffer,
        isRoot: Boolean
    ): Boolean {
        val children = getFormattableChildren(item)
        if (children.size < options.minCompactArrayRowItems) return false

        // Check if all children are simple enough
        if (children.any { it.type == JsonItemType.Array || it.type == JsonItemType.Object }) {
            return false
        }

        // Calculate max item length
        val maxItemLen = children.maxOfOrNull { it.minimumTotalLength } ?: return false
        val availableWidth = options.maxTotalLineLength - pads.indentLen(currentDepth + 1) -
                            pads.arrStartLen() - pads.arrEndLen()

        val itemsPerRow = max(1, availableWidth / (maxItemLen + pads.commaLen))
        if (itemsPerRow < options.minCompactArrayRowItems) return false

        // Format compact multiline
        if (isRoot) buffer.add(pads.indent(currentDepth))
        buffer.add(pads.arrStart())
        buffer.endLine(pads.eol)

        currentDepth++

        var itemsOnRow = 0
        for ((index, child) in children.withIndex()) {
            if (itemsOnRow == 0) {
                buffer.add(pads.indent(currentDepth))
            }

            inlineElement(child, buffer, false)

            if (index < children.size - 1) {
                buffer.add(pads.comma)
            }

            itemsOnRow++
            if (itemsOnRow >= itemsPerRow && index < children.size - 1) {
                buffer.endLine(pads.eol)
                itemsOnRow = 0
            }
        }

        buffer.endLine(pads.eol)
        currentDepth--

        buffer.add(pads.indent(currentDepth))
        buffer.add(pads.arrEnd())

        return true
    }

    private fun tryFormatContainerTable(
        item: JsonItem,
        buffer: FormattingBuffer,
        isRoot: Boolean
    ): Boolean {
        val children = getFormattableChildren(item)
        if (children.isEmpty()) return false

        // Create and measure table template
        val template = TableTemplate(pads, options.numberListAlignment)
        template.measureTableRoot(item, recursive = true)

        // Check if table fits
        val availableWidth = options.maxTotalLineLength - pads.indentLen(currentDepth + 1)
        if (!template.tryToFit(availableWidth)) return false

        // Format as table
        if (isRoot) buffer.add(pads.indent(currentDepth))
        buffer.add(pads.start(item.type))
        buffer.endLine(pads.eol)

        currentDepth++

        for ((index, child) in children.withIndex()) {
            buffer.add(pads.indent(currentDepth))
            formatTableRow(child, buffer, template, index == children.size - 1)
            buffer.endLine(pads.eol)
        }

        currentDepth--

        buffer.add(pads.indent(currentDepth))
        buffer.add(pads.end(item.type))

        return true
    }

    private fun formatTableRow(
        item: JsonItem,
        buffer: FormattingBuffer,
        template: TableTemplate,
        isLast: Boolean
    ) {
        // Format property name if present
        if (item.name.isNotEmpty()) {
            buffer.add("\"${item.name}\"")
            // colonBeforePropNamePadding: if true, colon comes right after name ("name":    value)
            // if false, padding comes before colon ("name   : value")
            if (options.colonBeforePropNamePadding) {
                buffer.add(pads.colon)
                buffer.spaces(template.nameLength - item.nameLength)
            } else {
                buffer.spaces(template.nameLength - item.nameLength)
                buffer.add(pads.colon)
            }
        }

        // Format value based on type
        when (item.type) {
            JsonItemType.Number -> {
                if (template.type == TableColumnType.Number) {
                    val commaStr = if (!isLast) pads.comma else pads.dummyComma
                    template.formatNumber(buffer, item, commaStr)
                } else {
                    buffer.add(item.value)
                    if (!isLast) buffer.add(pads.comma)
                }
            }
            JsonItemType.Array, JsonItemType.Object -> {
                formatContainerInline(item, buffer)
                if (!isLast) buffer.add(pads.comma)
            }
            else -> {
                buffer.add(item.value)
                buffer.spaces(template.maxValueLength - item.valueLength)
                if (!isLast) buffer.add(pads.comma)
            }
        }
    }

    private fun formatContainerExpanded(item: JsonItem, buffer: FormattingBuffer, isRoot: Boolean) {
        val children = getFormattableChildren(item)

        if (isRoot) buffer.add(pads.indent(currentDepth))
        buffer.add(pads.start(item.type))

        if (children.isEmpty()) {
            buffer.add(pads.end(item.type, BracketPaddingType.Empty))
            return
        }

        buffer.endLine(pads.eol)
        currentDepth++

        // Calculate name padding for objects
        val namePadding = if (item.type == JsonItemType.Object) {
            val maxNameLen = children.maxOfOrNull { it.nameLength } ?: 0
            minOf(maxNameLen, options.maxPropNamePadding)
        } else 0

        for ((index, child) in children.withIndex()) {
            formatExpandedChild(child, buffer, namePadding, index == children.size - 1)
        }

        currentDepth--
        buffer.add(pads.indent(currentDepth))
        buffer.add(pads.end(item.type))
    }

    private fun formatExpandedChild(
        child: JsonItem,
        buffer: FormattingBuffer,
        namePadding: Int,
        isLast: Boolean
    ) {
        when (child.type) {
            JsonItemType.BlankLine -> {
                if (options.preserveBlankLines) {
                    buffer.endLine(pads.eol)
                }
            }
            JsonItemType.LineComment, JsonItemType.BlockComment -> {
                if (options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(pads.indent(currentDepth))
                    buffer.add(child.value)
                    buffer.endLine(pads.eol)
                }
            }
            JsonItemType.Array, JsonItemType.Object -> {
                buffer.add(pads.indent(currentDepth))
                formatPropertyName(child, buffer, namePadding)
                formatContainer(child, buffer, isRoot = false)
                if (!isLast) buffer.add(pads.comma)
                buffer.endLine(pads.eol)
            }
            else -> {
                buffer.add(pads.indent(currentDepth))
                formatPropertyName(child, buffer, namePadding)
                buffer.add(child.value)
                if (!isLast) buffer.add(pads.comma)
                buffer.endLine(pads.eol)
            }
        }
    }

    private fun formatPropertyName(item: JsonItem, buffer: FormattingBuffer, namePadding: Int) {
        if (item.name.isEmpty()) return

        buffer.add("\"${item.name}\"")

        // colonBeforePropNamePadding: if true, colon comes right after name ("name":    value)
        // if false, padding comes before colon ("name   : value")
        if (options.colonBeforePropNamePadding) {
            buffer.add(pads.colon)
            buffer.spaces(namePadding - item.nameLength)
        } else {
            buffer.spaces(namePadding - item.nameLength)
            buffer.add(pads.colon)
        }
    }

    // ==================== Inline Element Formatting ====================

    private fun inlineElement(item: JsonItem, buffer: FormattingBuffer, includeComments: Boolean) {
        // Prefix comment
        if (includeComments && item.prefixComment.isNotEmpty() &&
            options.commentPolicy == CommentPolicy.Preserve) {
            buffer.add(item.prefixComment)
            buffer.add(pads.comment)
        }

        // Property name
        if (item.name.isNotEmpty()) {
            buffer.add("\"${item.name}\"")
            buffer.add(pads.colon)
        }

        // Middle comment
        if (includeComments && item.middleComment.isNotEmpty() &&
            options.commentPolicy == CommentPolicy.Preserve) {
            buffer.add(item.middleComment)
            buffer.add(pads.comment)
        }

        // Value
        when (item.type) {
            JsonItemType.Array, JsonItemType.Object -> {
                formatContainerInline(item, buffer)
            }
            else -> {
                buffer.add(item.value)
            }
        }

        // Postfix comment
        if (includeComments && item.postfixComment.isNotEmpty() &&
            options.commentPolicy == CommentPolicy.Preserve) {
            buffer.add(pads.comment)
            buffer.add(item.postfixComment)
        }
    }

    // ==================== Measurement ====================

    private fun computeItemLengths(item: JsonItem) {
        item.nameLength = if (item.name.isNotEmpty()) {
            stringLengthFunc("\"${item.name}\"")
        } else 0

        item.prefixCommentLength = if (item.prefixComment.isNotEmpty()) {
            stringLengthFunc(item.prefixComment) + pads.commentLen
        } else 0

        item.middleCommentLength = if (item.middleComment.isNotEmpty()) {
            stringLengthFunc(item.middleComment) + pads.commentLen
        } else 0

        item.postfixCommentLength = if (item.postfixComment.isNotEmpty()) {
            stringLengthFunc(item.postfixComment) + pads.commentLen
        } else 0

        when (item.type) {
            JsonItemType.Null -> {
                item.valueLength = pads.literalNullLen
                item.complexity = 0
            }
            JsonItemType.True -> {
                item.valueLength = pads.literalTrueLen
                item.complexity = 0
            }
            JsonItemType.False -> {
                item.valueLength = pads.literalFalseLen
                item.complexity = 0
            }
            JsonItemType.String, JsonItemType.Number -> {
                item.valueLength = stringLengthFunc(item.value)
                item.complexity = 0
            }
            JsonItemType.Array, JsonItemType.Object -> {
                computeContainerLengths(item)
            }
            else -> {
                item.valueLength = stringLengthFunc(item.value)
                item.complexity = 0
            }
        }

        item.minimumTotalLength = calculateMinimumLength(item)
    }

    private fun computeContainerLengths(item: JsonItem) {
        val children = getFormattableChildren(item)

        if (children.isEmpty()) {
            item.valueLength = pads.startLen(item.type, BracketPaddingType.Empty) +
                              pads.endLen(item.type, BracketPaddingType.Empty)
            item.complexity = 0
            item.requiresMultipleLines = false
            return
        }

        // Recursively compute lengths for children
        for (child in children) {
            computeItemLengths(child)
        }

        // Calculate complexity
        val maxChildComplexity = children.maxOfOrNull { it.complexity } ?: 0
        item.complexity = maxChildComplexity + 1

        // Calculate inline length
        val paddingType = determinePaddingType(item)
        val bracketsLen = pads.startLen(item.type, paddingType) + pads.endLen(item.type, paddingType)
        val childrenLen = children.sumOf { it.minimumTotalLength }
        val commasLen = if (children.size > 1) (children.size - 1) * pads.commaLen else 0

        item.valueLength = bracketsLen + childrenLen + commasLen

        // Check if multiple lines are required
        item.requiresMultipleLines = children.any { it.requiresMultipleLines } ||
            item.complexity > options.maxInlineComplexity ||
            children.any { it.middleCommentHasNewline } ||
            children.any { it.isPostCommentLineStyle }
    }

    private fun calculateMinimumLength(item: JsonItem): Int {
        var length = item.valueLength

        if (item.name.isNotEmpty()) {
            length += item.nameLength + pads.colonLen
        }

        length += item.prefixCommentLength + item.middleCommentLength + item.postfixCommentLength

        return length
    }

    // ==================== Utility Methods ====================

    private fun determinePaddingType(item: JsonItem): BracketPaddingType {
        val children = getFormattableChildren(item)
        return when {
            children.isEmpty() -> BracketPaddingType.Empty
            children.all { it.complexity == 0 } -> BracketPaddingType.Simple
            else -> BracketPaddingType.Complex
        }
    }

    private fun getFormattableChildren(item: JsonItem): List<JsonItem> {
        return item.children.filter { child ->
            when (child.type) {
                JsonItemType.BlankLine -> options.preserveBlankLines
                JsonItemType.LineComment, JsonItemType.BlockComment ->
                    options.commentPolicy == CommentPolicy.Preserve
                else -> true
            }
        }
    }

    // ==================== Minification ====================

    private fun minifyToBuffer(items: List<JsonItem>, buffer: FormattingBuffer) {
        for (item in items) {
            minifyItem(item, buffer)
        }
        buffer.flush()
    }

    private fun minifyItem(item: JsonItem, buffer: FormattingBuffer) {
        when (item.type) {
            JsonItemType.Array -> {
                buffer.add("[")
                val children = item.children.filter {
                    it.type != JsonItemType.BlankLine &&
                    it.type != JsonItemType.LineComment &&
                    it.type != JsonItemType.BlockComment
                }
                for ((index, child) in children.withIndex()) {
                    if (child.name.isNotEmpty()) {
                        buffer.add("\"${child.name}\":")
                    }
                    minifyItem(child, buffer)
                    if (index < children.size - 1) buffer.add(",")
                }
                buffer.add("]")
            }
            JsonItemType.Object -> {
                buffer.add("{")
                val children = item.children.filter {
                    it.type != JsonItemType.BlankLine &&
                    it.type != JsonItemType.LineComment &&
                    it.type != JsonItemType.BlockComment
                }
                for ((index, child) in children.withIndex()) {
                    if (child.name.isNotEmpty()) {
                        buffer.add("\"${child.name}\":")
                    }
                    minifyItem(child, buffer)
                    if (index < children.size - 1) buffer.add(",")
                }
                buffer.add("}")
            }
            JsonItemType.BlankLine, JsonItemType.LineComment, JsonItemType.BlockComment -> {
                // Skip in minified output
            }
            else -> {
                buffer.add(item.value)
            }
        }
    }
}
