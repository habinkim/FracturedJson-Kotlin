package io.github.fracturedjson.gson

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.core.JsonItemType

/**
 * Converts Gson JsonElement to FracturedJson JsonItem.
 *
 * This converter enables formatting of JSON data parsed or created with
 * Gson using FracturedJson's formatting capabilities.
 */
object GsonElementConverter {
    /**
     * Converts a JsonElement to a JsonItem tree.
     *
     * @param element The JsonElement to convert
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the element
     */
    fun convert(element: JsonElement, name: String? = null): JsonItem {
        val item = when {
            element.isJsonNull -> convertNull()
            element.isJsonPrimitive -> convertPrimitive(element.asJsonPrimitive)
            element.isJsonArray -> convertArray(element.asJsonArray)
            element.isJsonObject -> convertObject(element.asJsonObject)
            else -> convertNull() // Fallback for unknown types
        }

        if (name != null) {
            item.name = name
            item.nameLength = name.length + 2
        }

        return item
    }

    private fun convertNull(): JsonItem {
        return JsonItem(JsonItemType.Null).apply {
            value = "null"
            valueLength = 4
            complexity = 0
        }
    }

    private fun convertPrimitive(primitive: JsonPrimitive): JsonItem {
        return when {
            primitive.isBoolean -> {
                val boolVal = primitive.asBoolean
                JsonItem(if (boolVal) JsonItemType.True else JsonItemType.False).apply {
                    value = boolVal.toString()
                    valueLength = if (boolVal) 4 else 5
                    complexity = 0
                }
            }
            primitive.isNumber -> {
                JsonItem(JsonItemType.Number).apply {
                    val text = primitive.asNumber.toString()
                    value = text
                    valueLength = text.length
                    complexity = 0
                }
            }
            primitive.isString -> {
                JsonItem(JsonItemType.String).apply {
                    val escaped = escapeString(primitive.asString)
                    value = "\"$escaped\""
                    valueLength = escaped.length + 2
                    complexity = 0
                }
            }
            else -> convertNull()
        }
    }

    private fun convertArray(array: JsonArray): JsonItem {
        val children = array.map { element ->
            convert(element)
        }

        return JsonItem(JsonItemType.Array).apply {
            this.children = children
            complexity = if (children.isEmpty()) 0 else children.maxOf { it.complexity } + 1
        }
    }

    private fun convertObject(obj: JsonObject): JsonItem {
        val children = obj.entrySet().map { (key, value) ->
            convert(value, key)
        }

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
