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
class Formatter @JvmOverloads constructor(
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
        val buffer = formatToStringBuilder(items, startingDepth)
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
        val buffer = minifyToStringBuilder(items)
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

    // ==================== Baseline Methods (for benchmark comparison) ====================

    /**
     * Baseline format implementation preserved for benchmark comparison.
     * Uses StringBuilderBuffer with default initial capacity.
     * This is the v0.7.0 implementation before any optimization.
     */
    @ForBenchmark(version = "v0.7.0-baseline", description = "Original format with default StringBuilder capacity")
    fun formatBaseline(items: List<JsonItem>, startingDepth: Int = 0): String {
        val buffer = StringBuilderBuffer()
        format(items, startingDepth, buffer)
        return buffer.asString()
    }

    /**
     * Baseline minify implementation preserved for benchmark comparison.
     * Uses StringBuilderBuffer with default initial capacity.
     * This is the v0.7.0 implementation before any optimization.
     */
    @ForBenchmark(version = "v0.7.0-baseline", description = "Original minify with default StringBuilder capacity")
    fun minifyBaseline(items: List<JsonItem>): String {
        val buffer = StringBuilderBuffer()
        minifyToBuffer(items, buffer)
        return buffer.asString()
    }

    /**
     * v0.7.1 format baseline: StringBuilder initial capacity optimization.
     * Uses minimumTotalLength * 2 as capacity estimate after computeItemLengths().
     */
    @ForBenchmark(version = "v0.7.1-baseline", description = "StringBuilder initial capacity estimation for format")
    fun formatBaselineV071(items: List<JsonItem>, startingDepth: Int = 0): String {
        pads = PaddedFormattingTokens(options, stringLengthFunc)
        currentDepth = startingDepth

        for (item in items) {
            computeItemLengths(item)
        }

        val estimatedSize = (items.sumOf { it.minimumTotalLength } * 2).coerceAtLeast(64)
        val buffer = StringBuilderBuffer(estimatedSize)

        var needsNewline = false
        for (item in items) {
            if (needsNewline) {
                buffer.endLine(pads.eol)
            }
            formatItem(item, buffer, isRoot = true)
            needsNewline = true
        }

        buffer.flush()
        return buffer.asString()
    }

    /**
     * v0.7.1 minify baseline: O(1) heuristic capacity estimation.
     * Uses top-level value lengths / children count as capacity hint.
     */
    @ForBenchmark(version = "v0.7.1-baseline", description = "O(1) heuristic capacity estimation for minify")
    fun minifyBaselineV071(items: List<JsonItem>): String {
        val estimatedSize = items.sumOf { item ->
            if (item.value.isNotEmpty()) item.value.length
            else item.children.size * 32
        }.coerceAtLeast(256)
        val buffer = StringBuilderBuffer(estimatedSize)
        minifyToBuffer(items, buffer)
        return buffer.asString()
    }

    // ==================== Core Formatting Logic ====================

    /**
     * Formats items into a StringBuilderBuffer with pre-estimated capacity.
     * Computes item lengths first, then allocates a buffer based on the estimated output size.
     */
    private fun formatToStringBuilder(items: List<JsonItem>, startingDepth: Int): StringBuilderBuffer {
        pads = PaddedFormattingTokens(options, stringLengthFunc)
        currentDepth = startingDepth

        // Compute lengths first to enable size estimation
        for (item in items) {
            computeItemLengths(item)
        }

        // Estimate output size: pretty-printed JSON is typically 2x the minimum inline length
        val estimatedSize = (items.sumOf { it.minimumTotalLength } * 2).coerceAtLeast(64)
        val buffer = StringBuilderBuffer(estimatedSize)

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
        return buffer
    }

    /**
     * Minifies items into a StringBuilderBuffer with pre-estimated capacity.
     * Uses a cheap O(1) heuristic based on top-level item value lengths
     * rather than a recursive traversal which adds more overhead than it saves.
     */
    private fun minifyToStringBuilder(items: List<JsonItem>): StringBuilderBuffer {
        // Use top-level value lengths as a rough capacity hint.
        // For containers (arrays/objects), value is empty but children hold the data,
        // so we use a simple multiplier on the number of children as fallback.
        val estimatedSize = items.sumOf { item ->
            if (item.value.isNotEmpty()) item.value.length
            else item.children.size * 32 // rough estimate per child
        }.coerceAtLeast(256)
        val buffer = StringBuilderBuffer(estimatedSize)
        minifyToBuffer(items, buffer)
        return buffer
    }

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

    private fun formatContainer(item: JsonItem, buffer: FormattingBuffer, isRoot: Boolean, valueOnNewLine: Boolean = false) {
        // When value is on a new line, use one more indent level for available length calculation
        val effectiveDepth = if (valueOnNewLine) currentDepth + 1 else currentDepth
        val availableLength = options.maxTotalLineLength - pads.indentLen(effectiveDepth)
        // When value is on a new line, only the value needs to fit, not the property name/comments
        val lengthToCheck = if (valueOnNewLine) item.valueLength else item.minimumTotalLength

        // Check if we should always expand at this depth
        // alwaysExpandDepth=0 means expand only depth 0 (root), =1 means expand depths 0 and 1, etc.
        val forceExpand = options.alwaysExpandDepth >= 0 && currentDepth <= options.alwaysExpandDepth

        // Try inline formatting first (maxInlineComplexity < 0 means never inline)
        if (!forceExpand &&
            options.maxInlineComplexity >= 0 &&
            item.complexity <= options.maxInlineComplexity &&
            !item.requiresMultipleLines &&
            lengthToCheck <= availableLength) {
            if (isRoot) buffer.add(pads.indent(currentDepth))
            formatContainerInline(item, buffer)
            return
        }

        // For arrays, try compact multiline (maxCompactArrayComplexity < 0 means never use)
        // Skip compact multiline if requiresMultipleLines is true (e.g., due to line comments)
        if (!forceExpand && item.type == JsonItemType.Array &&
            options.maxCompactArrayComplexity >= 0 &&
            item.complexity <= options.maxCompactArrayComplexity &&
            !item.requiresMultipleLines) {
            val compactResult = tryFormatContainerCompactMultiline(item, buffer, isRoot)
            if (compactResult) return
        }

        // Try table formatting for consistent structures (maxTableRowComplexity < 0 means never use)
        // Note: table formatting is allowed even when forceExpand is true, because it expands content across multiple lines
        if (options.maxTableRowComplexity >= 0 && item.complexity <= options.maxTableRowComplexity) {
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

        // Check if all children are simple enough for compact formatting
        // Standalone comments prevent compact formatting
        if (children.any { it.type == JsonItemType.BlockComment || it.type == JsonItemType.LineComment }) {
            return false
        }
        // Children must be simple enough (complexity check) to fit in compact format
        if (children.any { it.complexity > options.maxCompactArrayComplexity }) {
            return false
        }

        // Calculate available line space at the content depth (matching C# AvailableLineSpace)
        val availableLineSpace = options.maxTotalLineLength - pads.indentLen(currentDepth + 1)

        // Check using average item width (matching C# logic)
        val avgItemWidth = pads.commaLen + children.sumOf { it.minimumTotalLength } / children.size

        if (avgItemWidth * options.minCompactArrayRowItems > availableLineSpace) return false

        // Calculate items per row using max item length for actual formatting
        val maxItemLen = children.maxOfOrNull { it.minimumTotalLength } ?: return false
        val itemsPerRow = max(1, availableLineSpace / (maxItemLen + pads.commaLen))
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

        // Table formatting doesn't work with standalone comments
        if (children.any { it.type == JsonItemType.BlockComment || it.type == JsonItemType.LineComment }) {
            return false
        }

        // Table formatting doesn't work when there's a multiline middle comment
        // (comment between property name and value that requires a line break)
        if (children.any { it.middleCommentHasNewline }) {
            return false
        }

        // Table row elements must be inlineable - check maxInlineComplexity
        // If maxInlineComplexity < 0, no inline formatting is allowed at all
        if (options.maxInlineComplexity < 0) {
            return false
        }
        // Each child must have complexity <= maxInlineComplexity to be inlined in a table row
        if (children.any { it.complexity > options.maxInlineComplexity }) {
            return false
        }

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
                // Format container with aligned elements using child templates
                if (template.children.isNotEmpty()) {
                    formatTableRowComposite(item, buffer, template)
                } else {
                    formatContainerInline(item, buffer)
                }
                if (!isLast) buffer.add(pads.comma)
            }
            else -> {
                buffer.add(item.value)
                buffer.spaces(template.maxValueLength - item.valueLength)
                if (!isLast) buffer.add(pads.comma)
            }
        }
    }

    /**
     * Formats a composite (array/object) table row element with aligned children.
     * Uses child templates to apply padding for proper column alignment.
     */
    private fun formatTableRowComposite(
        item: JsonItem,
        buffer: FormattingBuffer,
        template: TableTemplate
    ) {
        val paddingType = determinePaddingType(item)
        buffer.add(pads.start(item.type, paddingType))

        val children = getFormattableChildren(item)
        for ((index, child) in children.withIndex()) {
            if (index > 0) {
                buffer.add(pads.comma)
            }

            // Output property name if this is an object
            if (child.name.isNotEmpty()) {
                buffer.add("\"${child.name}\"")
                buffer.add(pads.colon)
            }

            // Find the child template for this position
            val childLocation = if (item.type == JsonItemType.Object) child.name else index.toString()
            val childTemplate = template.children.find { it.locationInParent == childLocation }

            if (childTemplate != null) {
                // Format using the child template for alignment
                when (child.type) {
                    JsonItemType.Number -> {
                        if (childTemplate.type == TableColumnType.Number) {
                            // Use number formatting with alignment
                            childTemplate.formatNumber(buffer, child, "")
                        } else {
                            buffer.add(child.value)
                            buffer.spaces(childTemplate.maxValueLength - child.valueLength)
                        }
                    }
                    JsonItemType.Array, JsonItemType.Object -> {
                        // Recursively format nested composites
                        if (childTemplate.children.isNotEmpty()) {
                            formatTableRowComposite(child, buffer, childTemplate)
                        } else {
                            formatContainerInline(child, buffer)
                        }
                    }
                    else -> {
                        buffer.add(child.value)
                        buffer.spaces(childTemplate.maxValueLength - child.valueLength)
                    }
                }
            } else {
                // No template - format inline without padding
                inlineElement(child, buffer, false)
            }
        }

        buffer.add(pads.end(item.type, paddingType))
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
        val namePadding = calculatePropertyNamePadding(children, item.type)

        for ((index, child) in children.withIndex()) {
            // isLastValue: true if remaining children (after this one) are all comments/blank lines
            val isLastValue = isLastValueChild(children, index)
            formatExpandedChild(child, buffer, namePadding, isLastValue)
        }

        currentDepth--
        buffer.add(pads.indent(currentDepth))
        buffer.add(pads.end(item.type))
    }

    /**
     * Calculates the property name padding for object children.
     * Returns 0 (no alignment) if:
     * - Not an object type
     * - The difference between max and min name lengths exceeds maxPropNamePadding
     * - Any child has a multiline middle comment (line break between name and value)
     * - Alignment would cause any line to exceed maxTotalLineLength
     *   (for simple values, the full line is checked; for containers that can wrap, only atomic part)
     */
    private fun calculatePropertyNamePadding(children: List<JsonItem>, itemType: JsonItemType): Int {
        if (itemType != JsonItemType.Object) return 0

        // Only consider children with names (actual value children, not comments/blank lines)
        val valueChildren = children.filter {
            it.name.isNotEmpty() &&
            it.type != JsonItemType.BlankLine &&
            it.type != JsonItemType.LineComment &&
            it.type != JsonItemType.BlockComment
        }
        if (valueChildren.isEmpty()) return 0

        // Check if any child has a multiline comment between name and value
        if (valueChildren.any { it.middleCommentHasNewline }) return 0

        val maxNameLen = valueChildren.maxOfOrNull { it.nameLength } ?: 0
        val minNameLen = valueChildren.minOfOrNull { it.nameLength } ?: 0
        val paddingNeeded = maxNameLen - minNameLen

        // If padding needed exceeds maxPropNamePadding, don't align
        if (paddingNeeded > options.maxPropNamePadding) return 0

        // Calculate "AtomicItemSize" - the MAX space needed for ANY aligned row
        // C# uses: NameLength + ColonLen + MiddleCommentLen + MaxAtomicValueLen + PostfixLen + CommaLen
        // This represents the worst-case line when all properties are aligned.
        val maxMiddleCommentLen = valueChildren.maxOfOrNull { it.middleCommentLength } ?: 0

        // For atomic value length, only consider simple types (arrays/objects can wrap)
        val maxAtomicValueLen = valueChildren
            .filter { it.type != JsonItemType.Array && it.type != JsonItemType.Object }
            .maxOfOrNull { it.valueLength } ?: 0

        val maxPostfixLen = valueChildren.maxOfOrNull { it.postfixCommentLength } ?: 0

        // Calculate AtomicItemSize like C# - the space needed for a fully aligned row
        val atomicItemSize = maxNameLen +
            pads.colonLen +
            maxMiddleCommentLen +
            maxAtomicValueLen +
            maxPostfixLen +
            pads.commaLen

        // Calculate available line space (excluding indent)
        // Note: currentDepth has already been incremented to the children's depth at this point
        val indentLen = pads.indentLen(currentDepth)
        val availableSpace = options.maxTotalLineLength - indentLen

        // If AtomicItemSize exceeds available space, give up on ALL alignment
        if (atomicItemSize > availableSpace) return 0

        return maxNameLen
    }

    /**
     * Checks if the child at the given index is the last "value" child.
     * Returns true if all remaining children (after this index) are non-value types (comments, blank lines).
     */
    private fun isLastValueChild(children: List<JsonItem>, index: Int): Boolean {
        if (index >= children.size - 1) return true
        // Check if all remaining children are non-value types
        return children.drop(index + 1).all { child ->
            child.type == JsonItemType.BlankLine ||
            child.type == JsonItemType.LineComment ||
            child.type == JsonItemType.BlockComment
        }
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
                // Output prefix comment on its own line if present
                if (child.prefixComment.isNotEmpty() && options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(pads.indent(currentDepth))
                    buffer.add(child.prefixComment)
                    buffer.endLine(pads.eol)
                }
                buffer.add(pads.indent(currentDepth))
                formatPropertyName(child, buffer, namePadding)
                // Output middle comment (between property name and value)
                var valueOnNewLine = false
                if (child.middleComment.isNotEmpty() && options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(child.middleComment)
                    // When property alignment is disabled (namePadding=0) due to middleCommentHasNewline,
                    // ALL middle comments should put their values on new lines for visual consistency
                    if (child.middleCommentHasNewline || (namePadding == 0 && child.name.isNotEmpty())) {
                        buffer.endLine(pads.eol)
                        buffer.add(pads.indent(currentDepth + 1))
                        valueOnNewLine = true
                    } else {
                        buffer.add(pads.comment)
                    }
                }
                // Check if the value would exceed line length when on same line as property name
                if (!valueOnNewLine && child.name.isNotEmpty()) {
                    val prefixLen = pads.indentLen(currentDepth) +
                        1 + child.nameLength + 1 +  // "name"
                        (if (namePadding > 0) namePadding - child.nameLength else 0) +
                        pads.colonLen +
                        child.middleCommentLength +
                        (if (child.middleComment.isNotEmpty()) pads.commentLen else 0)
                    val wouldFit = prefixLen + child.minimumTotalLength <= options.maxTotalLineLength
                    if (!wouldFit && !child.requiresMultipleLines) {
                        // Value needs to wrap to new line
                        buffer.endLine(pads.eol)
                        buffer.add(pads.indent(currentDepth + 1))
                        valueOnNewLine = true
                    }
                }
                formatContainer(child, buffer, isRoot = false, valueOnNewLine = valueOnNewLine)
                if (!isLast) buffer.add(pads.comma)
                buffer.endLine(pads.eol)
            }
            else -> {
                // Output prefix comment on its own line if present
                if (child.prefixComment.isNotEmpty() && options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(pads.indent(currentDepth))
                    buffer.add(child.prefixComment)
                    buffer.endLine(pads.eol)
                }
                buffer.add(pads.indent(currentDepth))
                formatPropertyName(child, buffer, namePadding)
                // Output middle comment (between property name and value)
                if (child.middleComment.isNotEmpty() && options.commentPolicy == CommentPolicy.Preserve) {
                    buffer.add(child.middleComment)
                    // When property alignment is disabled (namePadding=0) due to middleCommentHasNewline,
                    // ALL middle comments should put their values on new lines for visual consistency
                    if (child.middleCommentHasNewline || (namePadding == 0 && child.name.isNotEmpty())) {
                        buffer.endLine(pads.eol)
                        buffer.add(pads.indent(currentDepth + 1))
                    } else {
                        buffer.add(pads.comment)
                    }
                }
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
            children.any { it.isPostCommentLineStyle } ||
            // Standalone comments force multiline
            children.any { it.type == JsonItemType.BlockComment || it.type == JsonItemType.LineComment }

        // Line-style middle comments (// comment) force the container to be fully expanded.
        // This is because line comments inherently end with a newline, making inline formatting
        // of the value impossible - each element must be on its own line for proper rendering.
        children.filter { it.isMiddleCommentLineStyle && (it.type == JsonItemType.Array || it.type == JsonItemType.Object) }
            .forEach { it.requiresMultipleLines = true }
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
