package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Test

class DebugTableTest {
    @Test
    fun debugTableFormatting() {
        val input = "[ [1, 22, 9 ], [333, 4, 9 ] ]"
        val opts = FracturedJsonOptions(
            jsonEolStyle = EolStyle.Lf,
            alwaysExpandDepth = 0
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)

        // Parse and check the item structure
        val items = parser.parse(input)
        val item = items.first()
        println("=== Item Structure ===")
        println("Root type: ${item.type}, complexity: ${item.complexity}")
        println("Root children: ${item.children.size}")
        for (i in item.children.indices) {
            val child = item.children[i]
            println("  Child $i: type=${child.type}, complexity=${child.complexity}, children=${child.children.size}")
            for (j in child.children.indices) {
                val grandchild = child.children[j]
                println("    Grandchild $j: type=${grandchild.type}, value=${grandchild.value}, valueLength=${grandchild.valueLength}")
            }
        }
        println("======================")

        val output = formatter.format(item)
        println("=== Table Formatting Output ===")
        output.trimEnd().split('\n').forEachIndexed { i, line ->
            println("$i: |$line|")
        }
        println("===============================")
    }

    @Test
    fun debugNestedObjectTable() {
        val input = """{"nested": {"a": 1, "b": 2}}"""
        val opts = FracturedJsonOptions(
            jsonEolStyle = EolStyle.Lf,
            maxInlineComplexity = 0,  // Force expansion
            maxCompactArrayComplexity = 2,
            maxTableRowComplexity = 2
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        val output = formatter.format(items.first())

        println("=== Nested Object Table Output ===")
        output.trimEnd().split('\n').forEachIndexed { i, line ->
            println("$i: |$line|")
        }
        println("==================================")
    }

    @Test
    fun debugPropertyAlignment() {
        val input = """
            {
                "num": 14,
                "string": "testing property alignment",
                "arrayWithLongName": [null, null, null]
            }
        """.trimIndent()

        val opts = FracturedJsonOptions(
            maxPropNamePadding = 15,
            colonBeforePropNamePadding = false,
            maxInlineComplexity = -1,
            maxCompactArrayComplexity = -1,
            jsonEolStyle = EolStyle.Lf
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))

        println("=== Property Alignment Output ===")
        output.trimEnd().split('\n').forEachIndexed { i, line ->
            println("$i: |$line|")
        }
        println("================================")
    }
}
