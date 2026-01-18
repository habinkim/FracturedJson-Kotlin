package io.github.fracturedjson.fastjson2

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.core.JsonItemType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Converts Fastjson2 JSONObject/JSONArray to FracturedJson JsonItem.
 *
 * This converter enables formatting of JSON data parsed or created with
 * Fastjson2 using FracturedJson's formatting capabilities.
 */
object FastJson2Converter {
    /**
     * Converts a JSONObject to a JsonItem tree.
     *
     * @param obj The JSONObject to convert
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the object
     */
    fun convert(obj: JSONObject, name: String? = null): JsonItem {
        val item = convertObject(obj)
        if (name != null) {
            item.name = name
        }
        return item
    }

    /**
     * Converts a JSONArray to a JsonItem tree.
     *
     * @param array The JSONArray to convert
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the array
     */
    fun convert(array: JSONArray, name: String? = null): JsonItem {
        val item = convertArray(array)
        if (name != null) {
            item.name = name
        }
        return item
    }

    /**
     * Converts any value to a JsonItem tree.
     * This handles the dynamic typing of Fastjson2 values.
     *
     * @param value The value to convert (can be JSONObject, JSONArray, or primitive)
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the value
     */
    fun convert(value: Any?, name: String? = null): JsonItem {
        val item = convertValue(value)
        if (name != null) {
            item.name = name
        }
        return item
    }

    private fun convertValue(value: Any?): JsonItem {
        return when (value) {
            null -> convertNull()
            is Boolean -> convertBoolean(value)
            is Number -> convertNumber(value)
            is String -> convertString(value)
            is JSONArray -> convertArray(value)
            is JSONObject -> convertObject(value)
            else -> convertNull() // Fallback for unknown types
        }
    }

    private fun convertNull(): JsonItem {
        return JsonItem(JsonItemType.Null).apply {
            this.value = "null"
            complexity = 0
        }
    }

    private fun convertBoolean(value: Boolean): JsonItem {
        return JsonItem(if (value) JsonItemType.True else JsonItemType.False).apply {
            this.value = value.toString()
            complexity = 0
        }
    }

    private fun convertNumber(value: Number): JsonItem {
        return JsonItem(JsonItemType.Number).apply {
            // Preserve original number format as much as possible
            this.value = when (value) {
                is BigDecimal -> value.toPlainString()
                is BigInteger -> value.toString()
                is Double -> {
                    if (value.isInfinite() || value.isNaN()) "null"
                    else formatDouble(value)
                }
                is Float -> {
                    if (value.isInfinite() || value.isNaN()) "null"
                    else formatDouble(value.toDouble())
                }
                else -> value.toString()
            }
            complexity = 0
        }
    }

    private fun formatDouble(value: Double): String {
        // Remove unnecessary trailing zeros and decimal point
        val str = value.toString()
        return if (str.contains('.') && !str.contains('E') && !str.contains('e')) {
            str.trimEnd('0').trimEnd('.')
        } else {
            str
        }
    }

    private fun convertString(value: String): JsonItem {
        return JsonItem(JsonItemType.String).apply {
            // JSON strings need to be quoted
            this.value = "\"${escapeString(value)}\""
            complexity = 0
        }
    }

    private fun convertArray(array: JSONArray): JsonItem {
        val children = array.map { element ->
            convertValue(element)
        }

        return JsonItem(JsonItemType.Array).apply {
            this.children = children
            complexity = if (children.isEmpty()) 0 else children.maxOf { it.complexity } + 1
        }
    }

    private fun convertObject(obj: JSONObject): JsonItem {
        val children = obj.entries.map { (key, value) ->
            convertValue(value).also { it.name = key }
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
