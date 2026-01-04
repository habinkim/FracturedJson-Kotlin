package io.github.fracturedjson.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.core.JsonItemType

/**
 * Converts Jackson JsonNode to FracturedJson JsonItem.
 *
 * This converter enables formatting of JSON data parsed or created with
 * Jackson using FracturedJson's formatting capabilities.
 */
object JsonNodeConverter {
    /**
     * Converts a JsonNode to a JsonItem tree.
     *
     * @param node The JsonNode to convert
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the node
     */
    fun convert(node: JsonNode, name: String? = null): JsonItem {
        val item = when {
            node.isNull -> convertNull()
            node.isBoolean -> convertBoolean(node)
            node.isNumber -> convertNumber(node)
            node.isTextual -> convertString(node)
            node.isArray -> convertArray(node as ArrayNode)
            node.isObject -> convertObject(node as ObjectNode)
            else -> convertNull() // Fallback for unknown types
        }

        if (name != null) {
            item.name = name
        }

        return item
    }

    private fun convertNull(): JsonItem {
        return JsonItem(JsonItemType.Null).apply {
            value = "null"
            complexity = 0
        }
    }

    private fun convertBoolean(node: JsonNode): JsonItem {
        val boolVal = node.booleanValue()
        return JsonItem(if (boolVal) JsonItemType.True else JsonItemType.False).apply {
            value = boolVal.toString()
            complexity = 0
        }
    }

    private fun convertNumber(node: JsonNode): JsonItem {
        return JsonItem(JsonItemType.Number).apply {
            // Use the text representation to preserve original format
            value = node.asText()
            complexity = 0
        }
    }

    private fun convertString(node: JsonNode): JsonItem {
        return JsonItem(JsonItemType.String).apply {
            // JSON strings need to be quoted
            value = "\"${escapeString(node.textValue())}\""
            complexity = 0
        }
    }

    private fun convertArray(node: ArrayNode): JsonItem {
        val children = node.map { element ->
            convert(element)
        }

        return JsonItem(JsonItemType.Array).apply {
            this.children = children
            complexity = if (children.isEmpty()) 0 else children.maxOf { it.complexity } + 1
        }
    }

    private fun convertObject(node: ObjectNode): JsonItem {
        val children = node.fields().asSequence().map { (key, value) ->
            convert(value, key)
        }.toList()

        return JsonItem(JsonItemType.Object).apply {
            this.children = children
            complexity = if (children.isEmpty()) 0 else children.maxOf { it.complexity } + 1
        }
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
