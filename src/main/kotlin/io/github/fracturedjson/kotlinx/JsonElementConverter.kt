package io.github.fracturedjson.kotlinx

import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.core.JsonItemType
import kotlinx.serialization.json.*

/**
 * Converts kotlinx.serialization JsonElement to FracturedJson JsonItem.
 *
 * This converter enables formatting of JSON data parsed or created with
 * kotlinx.serialization using FracturedJson's formatting capabilities.
 */
object JsonElementConverter {
    /**
     * Converts a JsonElement to a JsonItem tree.
     *
     * @param element The JsonElement to convert
     * @param name Optional property name (for object properties)
     * @return A JsonItem representing the element
     */
    fun convert(element: JsonElement, name: String? = null): JsonItem {
        val item = when (element) {
            is JsonNull -> convertNull()
            is JsonPrimitive -> convertPrimitive(element)
            is JsonArray -> convertArray(element)
            is JsonObject -> convertObject(element)
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
            primitive.isString -> JsonItem(JsonItemType.String).apply {
                // Keep the original JSON string representation (with quotes)
                val text = primitive.toString()
                value = text
                valueLength = text.length
                complexity = 0
            }
            primitive.booleanOrNull != null -> {
                val boolVal = primitive.boolean
                JsonItem(if (boolVal) JsonItemType.True else JsonItemType.False).apply {
                    value = boolVal.toString()
                    valueLength = if (boolVal) 4 else 5
                    complexity = 0
                }
            }
            else -> {
                // Number
                JsonItem(JsonItemType.Number).apply {
                    val text = primitive.content
                    value = text
                    valueLength = text.length
                    complexity = 0
                }
            }
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
        val children = obj.entries.map { (key, value) ->
            convert(value, key)
        }

        return JsonItem(JsonItemType.Object).apply {
            this.children = children
            complexity = if (children.isEmpty()) 0 else children.maxOf { it.complexity } + 1
        }
    }
}
