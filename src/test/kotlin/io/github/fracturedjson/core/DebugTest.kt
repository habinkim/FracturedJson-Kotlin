package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Test

class DebugTest {
    @Test
    fun debugEndingComma() {
        val inputLines = listOf(
            "[",
            "/*a*/",
            "1, false",
            "/*b*/",
            "]"
        )
        val input = inputLines.joinToString("\n")

        val opts = FracturedJsonOptions(
            jsonEolStyle = EolStyle.Lf,
            commentPolicy = CommentPolicy.Preserve
        )

        val parser = Parser(opts)
        val items = parser.parse(input)

        println("=== Parsed Items ===")
        items.forEach { item ->
            println("Item type: ${item.type}, value: '${item.value}', requiresMultipleLines: ${item.requiresMultipleLines}")
            item.children.forEach { child ->
                println("  Child type: ${child.type}, value: '${child.value}', prefix: '${child.prefixComment}'")
            }
        }

        val formatter = Formatter(opts)
        val output = formatter.format(items)

        println("\n=== Output ===")
        println(output)
        println()
        println("=== Lines ===")
        val lines = output.trimEnd().split('\n')
        lines.forEachIndexed { i, line ->
            println("Line $i: '$line'")
        }
        println()
        println("Line count: ${lines.size}")
    }
}
