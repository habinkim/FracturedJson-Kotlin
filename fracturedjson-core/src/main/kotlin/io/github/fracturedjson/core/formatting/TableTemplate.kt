package io.github.fracturedjson.core.formatting

import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.core.JsonItemType
import io.github.fracturedjson.core.NumberListAlignment
import kotlin.math.max

/**
 * Manages spacing and alignment information for formatting JSON as tables.
 *
 * This class measures JSON items and calculates optimal column widths for
 * table-style formatting. It supports recursive measurement for nested structures.
 */
internal class TableTemplate(
    private val pads: PaddedFormattingTokens,
    private val numberListAlignment: NumberListAlignment
) {
    /** Property name identifier for this template position */
    var locationInParent: String = ""

    /** Column type based on the values seen */
    var type: TableColumnType = TableColumnType.Unknown

    /** Number of rows measured by this template */
    var rowCount: Int = 0

    /** Maximum length of property names */
    var nameLength: Int = 0

    /** Minimum name padding to apply */
    var nameMinimum: Int = 0

    /** Maximum value length seen */
    var maxValueLength: Int = 0

    /** Maximum atomic (non-composite) value length */
    var maxAtomicValueLength: Int = 0

    /** Prefix comment length */
    var prefixCommentLength: Int = 0

    /** Middle comment length */
    var middleCommentLength: Int = 0

    /** Postfix comment length */
    var postfixCommentLength: Int = 0

    /** Combined child template lengths for complex types */
    var compositeValueLength: Int = 0

    /** Complete template width including all components */
    var totalLength: Int = 0

    /** Sub-templates for nested objects/arrays */
    var children: MutableList<TableTemplate> = mutableListOf()

    // Number formatting fields
    private var maxIntegralDigits: Int = 0
    private var maxFractionalDigits: Int = 0
    private var hasExponential: Boolean = false
    private var hasDecimalPoint: Boolean = false

    /**
     * Analyzes objects/arrays for table formatting.
     *
     * @param tableRoot The root item to measure
     * @param recursive Whether to recursively measure nested structures
     */
    fun measureTableRoot(tableRoot: JsonItem, recursive: Boolean) {
        for (child in tableRoot.children) {
            if (child.type == JsonItemType.BlankLine ||
                child.type == JsonItemType.LineComment ||
                child.type == JsonItemType.BlockComment) {
                continue
            }
            measureRowSegment(child, recursive)
        }
        pruneAndRecompute(Int.MAX_VALUE)
    }

    /**
     * Tries to fit the template within the given maximum length.
     * Repeatedly drops inner formatting and recomputes until fit.
     *
     * @param maximumLength The maximum allowed length
     * @return true if the template fits, false otherwise
     */
    fun tryToFit(maximumLength: Int): Boolean {
        if (totalLength <= maximumLength) return true

        var complexity = getTemplateComplexity()
        while (complexity > 0 && totalLength > maximumLength) {
            complexity--
            pruneAndRecompute(complexity)
        }

        return totalLength <= maximumLength
    }

    /**
     * Formats a number with appropriate alignment to the buffer.
     */
    fun formatNumber(
        buffer: FormattingBuffer,
        item: JsonItem,
        commaBeforePadType: String
    ) {
        val value = item.value
        val fieldWidth = getNumberFieldWidth()

        when (numberListAlignment) {
            NumberListAlignment.Left -> {
                buffer.add(value)
                buffer.add(commaBeforePadType)
                buffer.spaces(fieldWidth - value.length)
            }
            NumberListAlignment.Right -> {
                buffer.spaces(fieldWidth - value.length)
                buffer.add(value)
                buffer.add(commaBeforePadType)
            }
            NumberListAlignment.Decimal -> {
                val (leftPad, rightPad) = calculateDecimalPadding(value, fieldWidth)
                buffer.spaces(leftPad)
                buffer.add(value)
                buffer.add(commaBeforePadType)
                buffer.spaces(rightPad)
            }
            NumberListAlignment.Normalize -> {
                // Try to normalize, fall back to left alignment
                val normalized = tryNormalizeNumber(value)
                if (normalized != null) {
                    buffer.spaces(fieldWidth - normalized.length)
                    buffer.add(normalized)
                    buffer.add(commaBeforePadType)
                } else {
                    buffer.add(value)
                    buffer.add(commaBeforePadType)
                    buffer.spaces(fieldWidth - value.length)
                }
            }
        }
    }

    /**
     * Calculates the maximum unsplittable item size.
     */
    fun atomicItemSize(): Int {
        return maxOf(
            maxAtomicValueLength,
            compositeValueLength
        ) + nameLength + prefixCommentLength + middleCommentLength + postfixCommentLength
    }

    /**
     * Updates template measurements for individual row segments.
     */
    private fun measureRowSegment(rowSegment: JsonItem, recursive: Boolean) {
        rowCount++

        // Update lengths
        nameLength = max(nameLength, rowSegment.nameLength)
        prefixCommentLength = max(prefixCommentLength, rowSegment.prefixCommentLength)
        middleCommentLength = max(middleCommentLength, rowSegment.middleCommentLength)
        postfixCommentLength = max(postfixCommentLength, rowSegment.postfixCommentLength)

        // Determine and update column type
        val itemType = classifyType(rowSegment)
        type = mergeTypes(type, itemType)

        when (rowSegment.type) {
            JsonItemType.Number -> {
                maxValueLength = max(maxValueLength, rowSegment.valueLength)
                maxAtomicValueLength = max(maxAtomicValueLength, rowSegment.valueLength)
                analyzeNumber(rowSegment.value)
            }
            JsonItemType.Array, JsonItemType.Object -> {
                if (recursive && rowSegment.children.isNotEmpty()) {
                    measureChildren(rowSegment, recursive)
                } else {
                    maxValueLength = max(maxValueLength, rowSegment.valueLength)
                    maxAtomicValueLength = max(maxAtomicValueLength, rowSegment.valueLength)
                }
            }
            else -> {
                maxValueLength = max(maxValueLength, rowSegment.valueLength)
                maxAtomicValueLength = max(maxAtomicValueLength, rowSegment.valueLength)
            }
        }
    }

    /**
     * Measures children of a composite item and updates child templates.
     */
    private fun measureChildren(parent: JsonItem, recursive: Boolean) {
        for ((index, child) in parent.children.withIndex()) {
            if (child.type == JsonItemType.BlankLine ||
                child.type == JsonItemType.LineComment ||
                child.type == JsonItemType.BlockComment) {
                continue
            }

            val location = if (parent.type == JsonItemType.Object) child.name else index.toString()

            var childTemplate = children.find { it.locationInParent == location }
            if (childTemplate == null) {
                childTemplate = TableTemplate(pads, numberListAlignment)
                childTemplate.locationInParent = location
                children.add(childTemplate)
            }

            childTemplate.measureRowSegment(child, recursive)
        }
    }

    /**
     * Removes incompatible sub-templates and recalculates lengths.
     */
    private fun pruneAndRecompute(maxAllowedComplexity: Int) {
        // Prune children that exceed complexity
        children.removeAll { it.getTemplateComplexity() > maxAllowedComplexity }

        // Recursively prune child templates
        for (child in children) {
            child.pruneAndRecompute(maxAllowedComplexity)
        }

        // Recalculate composite value length
        compositeValueLength = if (children.isEmpty()) {
            0
        } else {
            val childLengths = children.sumOf { it.totalLength }
            val separators = (children.size - 1) * pads.commaLen
            val brackets = when (type) {
                TableColumnType.Array -> pads.arrStartLen() + pads.arrEndLen()
                TableColumnType.Object -> pads.objStartLen() + pads.objEndLen()
                else -> 0
            }
            childLengths + separators + brackets
        }

        // Calculate total length
        totalLength = calculateTotalLength()
    }

    /**
     * Calculates the template complexity (nesting depth).
     */
    private fun getTemplateComplexity(): Int {
        if (children.isEmpty()) return 0
        return 1 + (children.maxOfOrNull { it.getTemplateComplexity() } ?: 0)
    }

    /**
     * Gets the field width for number formatting.
     */
    private fun getNumberFieldWidth(): Int {
        return when (numberListAlignment) {
            NumberListAlignment.Normalize -> {
                if (hasExponential) maxValueLength
                else maxIntegralDigits + (if (hasDecimalPoint) 1 + maxFractionalDigits else 0)
            }
            else -> maxValueLength
        }
    }

    /**
     * Calculates the total length of this template.
     */
    private fun calculateTotalLength(): Int {
        val valueLen = max(maxAtomicValueLength, compositeValueLength)
        return nameLength + prefixCommentLength + middleCommentLength +
               valueLen + postfixCommentLength +
               (if (nameLength > 0) pads.colonLen else 0)
    }

    /**
     * Classifies the type of an item for column typing.
     */
    private fun classifyType(item: JsonItem): TableColumnType {
        return when (item.type) {
            JsonItemType.Null -> TableColumnType.Simple
            JsonItemType.True, JsonItemType.False -> TableColumnType.Simple
            JsonItemType.String -> TableColumnType.Simple
            JsonItemType.Number -> TableColumnType.Number
            JsonItemType.Array -> TableColumnType.Array
            JsonItemType.Object -> TableColumnType.Object
            else -> TableColumnType.Unknown
        }
    }

    /**
     * Merges two column types, returning Mixed if incompatible.
     */
    private fun mergeTypes(existing: TableColumnType, incoming: TableColumnType): TableColumnType {
        return when {
            existing == TableColumnType.Unknown -> incoming
            existing == incoming -> existing
            existing == TableColumnType.Simple && incoming == TableColumnType.Number -> TableColumnType.Mixed
            existing == TableColumnType.Number && incoming == TableColumnType.Simple -> TableColumnType.Mixed
            else -> TableColumnType.Mixed
        }
    }

    /**
     * Analyzes a number string for alignment purposes.
     */
    private fun analyzeNumber(value: String) {
        val lowerValue = value.lowercase()

        if ('e' in lowerValue) {
            hasExponential = true
            return
        }

        val dotIndex = value.indexOf('.')
        if (dotIndex >= 0) {
            hasDecimalPoint = true
            val integral = dotIndex
            val fractional = value.length - dotIndex - 1
            maxIntegralDigits = max(maxIntegralDigits, integral)
            maxFractionalDigits = max(maxFractionalDigits, fractional)
        } else {
            maxIntegralDigits = max(maxIntegralDigits, value.length)
        }
    }

    /**
     * Calculates padding for decimal-aligned numbers.
     */
    private fun calculateDecimalPadding(value: String, fieldWidth: Int): Pair<Int, Int> {
        val dotIndex = value.indexOf('.')
        val eIndex = value.lowercase().indexOf('e')

        val leftPad: Int
        val rightPad: Int

        when {
            eIndex >= 0 -> {
                // Scientific notation - align on 'e' or decimal before it
                val decBeforeE = value.substring(0, eIndex).indexOf('.')
                val alignPoint = if (decBeforeE >= 0) decBeforeE else eIndex
                leftPad = maxIntegralDigits - alignPoint
                rightPad = fieldWidth - value.length - leftPad
            }
            dotIndex >= 0 -> {
                leftPad = maxIntegralDigits - dotIndex
                rightPad = fieldWidth - value.length - leftPad
            }
            else -> {
                leftPad = maxIntegralDigits - value.length
                rightPad = fieldWidth - value.length - leftPad
            }
        }

        return Pair(max(0, leftPad), max(0, rightPad))
    }

    /**
     * Tries to normalize a number to a consistent format.
     */
    private fun tryNormalizeNumber(value: String): String? {
        if (hasExponential) return null

        return try {
            val number = value.toDouble()
            if (number.isNaN() || number.isInfinite()) return null

            val format = if (maxFractionalDigits > 0) {
                "%.${maxFractionalDigits}f"
            } else {
                "%.0f"
            }

            String.format(format, number)
        } catch (e: NumberFormatException) {
            null
        }
    }
}
