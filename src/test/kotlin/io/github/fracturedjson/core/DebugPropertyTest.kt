package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Test

class DebugPropertyTest {
    @Test
    fun debugDontAlignWhenSimpleValueTooLong() {
        val input = """
            {
                "foo": [1, 2, 4],
                "bar": null,
                "bazzzz": [0]
            }
        """.trimIndent()

        // Note: This test uses maxTotalLineLength = 36
        // The point is that with property name alignment, bar's line would exceed 36 chars
        val opts = FracturedJsonOptions(
            commentPolicy = CommentPolicy.Preserve,
            colonBeforePropNamePadding = false,
            maxTotalLineLength = 36,
            maxPropNamePadding = 16,
            jsonEolStyle = EolStyle.Lf
        )

        println("=== Options: maxTotalLineLength=${opts.maxTotalLineLength}, maxPropNamePadding=${opts.maxPropNamePadding}")

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))
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
            println("Child: name=${child.name}, type=${child.type}, middleComment='${child.middleComment}', middleCommentHasNewline=${child.middleCommentHasNewline}")
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
            println("Child: name=${child.name}, type=${child.type}, middleComment=${child.middleComment}, middleCommentHasNewline=${child.middleCommentHasNewline}")
        }
        println("======================")

        val output = formatter.format(item)
        val outputLines = output.trimEnd().split('\n')

        println("=== Align Prop Vals When Array Wraps ===")
        outputLines.forEachIndexed { i, line ->
            println("$i: |$line| (len=${line.length})")
        }
        println("==================")
        println("Expected lines: 7")
        println("Actual lines: ${outputLines.size}")
    }
}
