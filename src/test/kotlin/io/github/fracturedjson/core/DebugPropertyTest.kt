package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Test

class DebugPropertyTest {
    @Test
    fun debugDontAlignWhenSimpleValueTooLong() {
        // Uses block comment (/* this is foo */) like the C# original
        val input = """
            {
                "foo": /* this is foo */
                    [1, 2, 4],
                "bar": null,
                "bazzzz": /* this is baz */ [0]
            }
        """.trimIndent()

        // Note: This test uses maxTotalLineLength = 36
        // The point is that with property name alignment, bar's line would exceed 36 chars
        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            colonBeforePropNamePadding = false,
            maxTotalLineLength = 36,
            jsonEolStyle = EolStyle.Lf
        )

        println("=== Options: maxTotalLineLength=${opts.maxTotalLineLength}")

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        val item = items.first()

        println("=== Item Structure ===")
        for (child in item.children) {
            println("Child: name=${child.name}, type=${child.type}, middleComment='${child.middleComment}', middleCommentHasNewline=${child.middleCommentHasNewline}, isMiddleCommentLineStyle=${child.isMiddleCommentLineStyle}")
        }
        println("======================")

        val output = formatter.format(item)
        val outputLines = output.trimEnd().split('\n')

        println("=== Don't Align When Simple Value Too Long ===")
        outputLines.forEachIndexed { i, line ->
            println("$i: |$line| (len=${line.length})")
        }
        println("==================")
        println("Contains 'bar':? ${output.contains("\"bar\":")}")
        println("Line 1 colon pos: ${outputLines.getOrNull(1)?.indexOf(':')}")
        println("Last value line colon pos: ${outputLines.getOrNull(outputLines.size - 2)?.indexOf(':')}")
    }

    @Test
    fun debugDontAlignPropValsWhenMultilineComment() {
        val input = """
            {
                "foo": // this is foo
                    [1, 2, 4],
                "bar": null,
                "bazzzz": /* this is baz */ [0]
            }
        """.trimIndent()

        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            colonBeforePropNamePadding = false,
            jsonEolStyle = EolStyle.Lf
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        val item = items.first()

        println("=== Item Structure ===")
        for (child in item.children) {
            println("Child: name=${child.name}, type=${child.type}, middleComment='${child.middleComment}', middleCommentHasNewline=${child.middleCommentHasNewline}, isMiddleCommentLineStyle=${child.isMiddleCommentLineStyle}")
        }
        println("======================")

        val output = formatter.format(item)
        val outputLines = output.trimEnd().split('\n')

        println("=== Don't Align Prop Vals When Multiline Comment ===")
        outputLines.forEachIndexed { i, line ->
            println("$i: |$line|")
        }
        println("==================")
        println("Expected lines: 11")
        println("Actual lines: ${outputLines.size}")
    }

    @Test
    fun debugAlignPropValsWhenSimpleComment() {
        val input = """
            {
                "foo": /* this is foo */
                    [1, 2, 4],
                "bar": null,
                "bazzzz": /* this is baz */ [0]
            }
        """.trimIndent()

        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            colonBeforePropNamePadding = false,
            maxTotalLineLength = 80,
            jsonEolStyle = EolStyle.Lf
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        val item = items.first()

        println("=== Item Structure ===")
        for (child in item.children) {
            println("Child: name=${child.name}, type=${child.type}, middleComment=${child.middleComment}, middleCommentHasNewline=${child.middleCommentHasNewline}, isMiddleCommentLineStyle=${child.isMiddleCommentLineStyle}")
        }
        println("======================")

        val output = formatter.format(item)
        val outputLines = output.trimEnd().split('\n')

        println("=== Align Prop Vals When Simple Comment ===")
        outputLines.forEachIndexed { i, line ->
            println("$i: |$line| (len=${line.length})")
        }
        println("==================")
        println("Expected lines: 5 (table-formatted)")
        println("Actual lines: ${outputLines.size}")
    }

    @Test
    fun debugAlignPropValsWhenArrayWraps() {
        val input = """
            {
                "foo": /* this is foo */
                    [1, 2, 4],
                "bar": null,
                "bazzzz": /* this is baz */ [0]
            }
        """.trimIndent()

        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            colonBeforePropNamePadding = false,
            maxTotalLineLength = 38,
            jsonEolStyle = EolStyle.Lf
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        val item = items.first()

        println("=== Item Structure ===")
        for (child in item.children) {
            println("Child: name=${child.name}, nameLength=${child.nameLength}, type=${child.type}, middleComment=${child.middleComment}, middleCommentLength=${child.middleCommentLength}, middleCommentHasNewline=${child.middleCommentHasNewline}")
        }
        println("======================")

        val output = formatter.format(item)
        val outputLines = output.trimEnd().split('\n')

        println("=== Align Prop Vals When Array Wraps ===")
        outputLines.forEachIndexed { i, line ->
            println("$i: |$line| (len=${line.length})")
            if (line.contains(":")) {
                println("   Colon position: ${line.indexOf(':')}")
            }
        }
        println("==================")
        println("Expected lines: 7")
        println("Actual lines: ${outputLines.size}")
        println("Expected: colons and '[' should line up")
    }


    @Test
    fun debugCommasBeforePaddingWithComments() {
        val input = """
            [
                [ 1 /* q */, "a" ], /* w */
                [ 22, "bbb" ], // x
                [ 3.33 /* sss */, "cc" ] /* y */
            ]
        """.trimIndent()

        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            maxTotalLineLength = 40,
            jsonEolStyle = EolStyle.Lf,
            numberListAlignment = NumberListAlignment.Decimal,
            tableCommaPlacement = TableCommaPlacement.BeforePadding
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val items = parser.parse(input)
        
        println("=== Parsed Structure ===")
        val root = items.first()
        println("Root type: ${root.type}, children: ${root.children.size}")
        for ((i, child) in root.children.withIndex()) {
            println("Child $i: type=${child.type}, children=${child.children.size}")
            for ((j, grandchild) in child.children.withIndex()) {
                println("  Grandchild $j: type=${grandchild.type}, value='${grandchild.value}', middleComment='${grandchild.middleComment}'")
            }
            println("  postfixComment='${child.postfixComment}'")
        }
        println("========================")
        
        val output = formatter.format(root)
        println("=== Formatted Output ===")
        output.trimEnd().split('\n').forEachIndexed { i, line ->
            println("$i: |$line|")
        }
        println("========================")
    }
}
