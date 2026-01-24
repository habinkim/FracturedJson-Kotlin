package io.github.fracturedjson.parser

import io.github.fracturedjson.core.*
import io.github.fracturedjson.parser.tokenizing.JsonToken
import io.github.fracturedjson.parser.tokenizing.TokenScanner
import io.github.fracturedjson.parser.tokenizing.TokenType
import java.io.File
import java.io.Reader
import kotlin.math.max

/**
 * Parses JSON text (with optional comments) into a tree of JsonItem objects.
 *
 * This parser supports:
 * - Standard JSON syntax
 * - Line comments (//)
 * - Block comments (/* */)
 * - Blank line preservation
 * - Trailing commas (when enabled)
 *
 * @property options Configuration options for parsing behavior
 */
class Parser @JvmOverloads constructor(
    var options: FracturedJsonOptions = FracturedJsonOptions()
) {
    /**
     * Parses JSON from a file.
     */
    fun parse(file: File): List<JsonItem> {
        return parseTopLevel(TokenScanner.scan(file))
    }

    /**
     * Parses JSON from a string.
     */
    fun parse(input: String): List<JsonItem> {
        return parseTopLevel(TokenScanner.scan(input))
    }

    /**
     * Parses JSON from a Reader.
     */
    fun parse(reader: Reader): List<JsonItem> {
        return parseTopLevel(TokenScanner.scan(reader))
    }

    /**
     * Parses top-level items from a sequence of tokens.
     */
    fun parseTopLevel(tokens: Sequence<JsonToken>): List<JsonItem> {
        val result = mutableListOf<JsonItem>()
        val iterator = tokens.iterator()
        var unplacedComments = mutableListOf<JsonItem>()

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.type) {
                TokenType.BlankLine -> {
                    if (options.preserveBlankLines) {
                        result.add(createBlankLine(token))
                    }
                }
                TokenType.LineComment, TokenType.BlockComment -> {
                    handleComment(token, result, unplacedComments)
                }
                TokenType.BeginArray -> {
                    val item = parseArray(iterator, token.inputPosition)
                    attachPrefixComments(item, unplacedComments)
                    result.add(item)
                }
                TokenType.BeginObject -> {
                    val item = parseObject(iterator, token.inputPosition)
                    attachPrefixComments(item, unplacedComments)
                    result.add(item)
                }
                else -> {
                    val item = parseSimple(token)
                    attachPrefixComments(item, unplacedComments)
                    result.add(item)
                }
            }
        }

        // Add any remaining unplaced comments
        for (comment in unplacedComments) {
            result.add(comment)
        }

        return result
    }

    private fun parseArray(iterator: Iterator<JsonToken>, startPosition: InputPosition): JsonItem {
        val item = JsonItem(JsonItemType.Array)
        item.inputPosition = startPosition
        val children = mutableListOf<JsonItem>()
        var unplacedComments = mutableListOf<JsonItem>()
        var commaStatus = CommaStatus.EmptyCollection
        var maxComplexity = 0

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.type) {
                TokenType.EndArray -> {
                    if (commaStatus == CommaStatus.CommaSeen && !options.allowTrailingCommas) {
                        throw FracturedJsonException.create("Trailing comma not allowed", token.inputPosition)
                    }
                    // Add remaining comments
                    children.addAll(unplacedComments)
                    item.children = children
                    item.complexity = maxComplexity + 1
                    return item
                }
                TokenType.Comma -> {
                    if (commaStatus != CommaStatus.ElementSeen) {
                        throw FracturedJsonException.create("Unexpected comma", token.inputPosition)
                    }
                    commaStatus = CommaStatus.CommaSeen
                }
                TokenType.BlankLine -> {
                    if (options.preserveBlankLines) {
                        children.add(createBlankLine(token))
                    }
                }
                TokenType.LineComment, TokenType.BlockComment -> {
                    handleCommentInContainer(token, children, unplacedComments)
                }
                else -> {
                    if (commaStatus == CommaStatus.ElementSeen) {
                        throw FracturedJsonException.create("Expected comma or end of array", token.inputPosition)
                    }
                    val child = parseItem(token, iterator)
                    attachPrefixComments(child, unplacedComments)
                    children.add(child)
                    maxComplexity = max(maxComplexity, child.complexity)
                    commaStatus = CommaStatus.ElementSeen
                }
            }
        }

        throw FracturedJsonException.create("Unexpected end of input in array", startPosition)
    }

    private fun parseObject(iterator: Iterator<JsonToken>, startPosition: InputPosition): JsonItem {
        val item = JsonItem(JsonItemType.Object)
        item.inputPosition = startPosition
        val children = mutableListOf<JsonItem>()
        var unplacedComments = mutableListOf<JsonItem>()
        var phase = ObjectPhase.BeforePropName
        var currentPropName = ""
        var currentPropPosition = InputPosition.START
        var middleComments = mutableListOf<JsonItem>()
        var maxComplexity = 0

        while (iterator.hasNext()) {
            val token = iterator.next()

            when (token.type) {
                TokenType.EndObject -> {
                    if (phase == ObjectPhase.AfterComma && !options.allowTrailingCommas) {
                        throw FracturedJsonException.create("Trailing comma not allowed", token.inputPosition)
                    }
                    if (phase == ObjectPhase.AfterPropName || phase == ObjectPhase.AfterColon) {
                        throw FracturedJsonException.create("Unexpected end of object", token.inputPosition)
                    }
                    children.addAll(unplacedComments)
                    item.children = children
                    item.complexity = maxComplexity + 1
                    return item
                }
                TokenType.Comma -> {
                    if (phase != ObjectPhase.AfterPropValue) {
                        throw FracturedJsonException.create("Unexpected comma", token.inputPosition)
                    }
                    phase = ObjectPhase.AfterComma
                }
                TokenType.Colon -> {
                    if (phase != ObjectPhase.AfterPropName) {
                        throw FracturedJsonException.create("Unexpected colon", token.inputPosition)
                    }
                    phase = ObjectPhase.AfterColon
                }
                TokenType.String -> {
                    when (phase) {
                        ObjectPhase.BeforePropName, ObjectPhase.AfterComma -> {
                            currentPropName = extractStringContent(token.text)
                            currentPropPosition = token.inputPosition
                            middleComments.clear()
                            phase = ObjectPhase.AfterPropName
                        }
                        ObjectPhase.AfterColon -> {
                            val child = parseSimple(token)
                            child.name = currentPropName
                            child.nameLength = currentPropName.length + 2
                            child.inputPosition = currentPropPosition
                            attachPrefixComments(child, unplacedComments)
                            attachMiddleComments(child, middleComments, token.inputPosition.row)
                            children.add(child)
                            maxComplexity = max(maxComplexity, child.complexity)
                            phase = ObjectPhase.AfterPropValue
                        }
                        else -> {
                            throw FracturedJsonException.create("Unexpected string", token.inputPosition)
                        }
                    }
                }
                TokenType.BlankLine -> {
                    if (options.preserveBlankLines) {
                        children.add(createBlankLine(token))
                    }
                }
                TokenType.LineComment, TokenType.BlockComment -> {
                    when (phase) {
                        ObjectPhase.AfterPropName, ObjectPhase.AfterColon -> {
                            if (options.commentPolicy == CommentPolicy.Preserve) {
                                middleComments.add(createComment(token))
                            }
                        }
                        else -> {
                            handleCommentInContainer(token, children, unplacedComments)
                        }
                    }
                }
                else -> {
                    if (phase != ObjectPhase.AfterColon) {
                        throw FracturedJsonException.create("Expected property name", token.inputPosition)
                    }
                    val child = parseItem(token, iterator)
                    child.name = currentPropName
                    child.nameLength = currentPropName.length + 2
                    child.inputPosition = currentPropPosition
                    attachPrefixComments(child, unplacedComments)
                    attachMiddleComments(child, middleComments, token.inputPosition.row)
                    children.add(child)
                    maxComplexity = max(maxComplexity, child.complexity)
                    phase = ObjectPhase.AfterPropValue
                }
            }
        }

        throw FracturedJsonException.create("Unexpected end of input in object", startPosition)
    }

    private fun parseItem(token: JsonToken, iterator: Iterator<JsonToken>): JsonItem {
        return when (token.type) {
            TokenType.BeginArray -> parseArray(iterator, token.inputPosition)
            TokenType.BeginObject -> parseObject(iterator, token.inputPosition)
            else -> parseSimple(token)
        }
    }

    private fun parseSimple(token: JsonToken): JsonItem {
        val item = JsonItem()
        item.inputPosition = token.inputPosition

        when (token.type) {
            TokenType.Null -> {
                item.type = JsonItemType.Null
                item.value = "null"
                item.valueLength = 4
            }
            TokenType.True -> {
                item.type = JsonItemType.True
                item.value = "true"
                item.valueLength = 4
            }
            TokenType.False -> {
                item.type = JsonItemType.False
                item.value = "false"
                item.valueLength = 5
            }
            TokenType.Number -> {
                item.type = JsonItemType.Number
                item.value = token.text
                item.valueLength = token.text.length
            }
            TokenType.String -> {
                item.type = JsonItemType.String
                item.value = token.text
                item.valueLength = token.text.length
            }
            else -> {
                throw FracturedJsonException.create("Unexpected token type: ${token.type}", token.inputPosition)
            }
        }

        item.complexity = 0
        return item
    }

    private fun handleComment(
        token: JsonToken,
        items: MutableList<JsonItem>,
        unplacedComments: MutableList<JsonItem>
    ) {
        when (options.commentPolicy) {
            CommentPolicy.TreatAsError -> {
                throw FracturedJsonException.create("Comments are not allowed", token.inputPosition)
            }
            CommentPolicy.Remove -> {
                // Ignore
            }
            CommentPolicy.Preserve -> {
                val comment = createComment(token)
                // Try to attach to previous item if on same line
                if (items.isNotEmpty() && token.type == TokenType.LineComment) {
                    val lastItem = items.last()
                    if (canAttachPostfix(lastItem, token)) {
                        lastItem.postfixComment = token.text
                        lastItem.isPostCommentLineStyle = true
                        return
                    }
                }
                unplacedComments.add(comment)
            }
        }
    }

    private fun handleCommentInContainer(
        token: JsonToken,
        children: MutableList<JsonItem>,
        unplacedComments: MutableList<JsonItem>
    ) {
        when (options.commentPolicy) {
            CommentPolicy.TreatAsError -> {
                throw FracturedJsonException.create("Comments are not allowed", token.inputPosition)
            }
            CommentPolicy.Remove -> {
                // Ignore
            }
            CommentPolicy.Preserve -> {
                val comment = createComment(token)
                // Try to attach to previous item if on same line
                if (children.isNotEmpty() && token.type == TokenType.LineComment) {
                    val lastChild = children.last()
                    if (canAttachPostfix(lastChild, token) &&
                        lastChild.type != JsonItemType.BlankLine &&
                        lastChild.type != JsonItemType.LineComment &&
                        lastChild.type != JsonItemType.BlockComment) {
                        lastChild.postfixComment = token.text
                        lastChild.isPostCommentLineStyle = true
                        return
                    }
                }
                unplacedComments.add(comment)
            }
        }
    }

    private fun createComment(token: JsonToken): JsonItem {
        val item = JsonItem(
            if (token.type == TokenType.LineComment) JsonItemType.LineComment else JsonItemType.BlockComment
        )
        item.inputPosition = token.inputPosition
        item.value = token.text
        return item
    }

    private fun createBlankLine(token: JsonToken): JsonItem {
        val item = JsonItem(JsonItemType.BlankLine)
        item.inputPosition = token.inputPosition
        return item
    }

    private fun attachPrefixComments(item: JsonItem, unplacedComments: MutableList<JsonItem>) {
        if (unplacedComments.isEmpty()) return

        // Combine unplaced comments into prefix
        val prefix = unplacedComments.joinToString(" ") { it.value }
        item.prefixComment = prefix
        unplacedComments.clear()
    }

    private fun attachMiddleComments(item: JsonItem, middleComments: MutableList<JsonItem>, valueRow: Int = -1) {
        if (middleComments.isEmpty()) return

        val middle = middleComments.joinToString(" ") { it.value }
        item.middleComment = middle
        // Track if any middle comment is a line comment (which forces full expansion)
        item.isMiddleCommentLineStyle = middleComments.any { it.type == JsonItemType.LineComment }
        // middleCommentHasNewline is true when:
        // 1. Any middle comment is a line comment (// style - always ends with newline)
        // 2. Any block comment contains newlines within it (/* ... \n ... */)
        // Note: Newlines AFTER block comments in the input are handled by the formatter,
        // not by this flag. This flag controls table formatting eligibility.
        item.middleCommentHasNewline = middleComments.any {
            it.type == JsonItemType.LineComment || it.value.contains('\n')
        }
        middleComments.clear()
    }

    private fun canAttachPostfix(item: JsonItem, token: JsonToken): Boolean {
        // Simple heuristic: can attach if on same line
        return item.inputPosition.row == token.inputPosition.row
    }

    private fun extractStringContent(text: String): String {
        // Remove surrounding quotes
        if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
            return unescapeString(text.substring(1, text.length - 1))
        }
        return text
    }

    private fun unescapeString(s: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val next = s[i + 1]) {
                    '"' -> { result.append('"'); i += 2 }
                    '\\' -> { result.append('\\'); i += 2 }
                    '/' -> { result.append('/'); i += 2 }
                    'b' -> { result.append('\b'); i += 2 }
                    'f' -> { result.append('\u000C'); i += 2 }
                    'n' -> { result.append('\n'); i += 2 }
                    'r' -> { result.append('\r'); i += 2 }
                    't' -> { result.append('\t'); i += 2 }
                    'u' -> {
                        if (i + 5 < s.length) {
                            val hex = s.substring(i + 2, i + 6)
                            try {
                                result.append(hex.toInt(16).toChar())
                                i += 6
                            } catch (e: NumberFormatException) {
                                result.append(c)
                                i++
                            }
                        } else {
                            result.append(c)
                            i++
                        }
                    }
                    else -> { result.append(c); i++ }
                }
            } else {
                result.append(c)
                i++
            }
        }
        return result.toString()
    }

    private enum class CommaStatus {
        EmptyCollection,
        ElementSeen,
        CommaSeen
    }

    private enum class ObjectPhase {
        BeforePropName,
        AfterPropName,
        AfterColon,
        AfterPropValue,
        AfterComma
    }
}
