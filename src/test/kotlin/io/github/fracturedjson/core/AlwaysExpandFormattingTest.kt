package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for AlwaysExpandDepth functionality - ported from C# AlwaysExpandFormattingTests.cs
 */
@DisplayName("Always Expand Formatting")
class AlwaysExpandFormattingTest {

    @Test
    @DisplayName("AlwaysExpandDepth honored")
    fun alwaysExpandDepthHonored() {
        val inputLines = listOf(
            "[",
            "[ {\"x\":1}, false ],",
            "{ \"a\":[2], \"b\":[3] }",
            "]"
        )
        val input = inputLines.joinToString("\n")

        // With high maximum complexity and long line length, it should all be in one line.
        val opts1 = FracturedJsonOptions(
            maxInlineComplexity = 100,
            maxTotalLineLength = Int.MAX_VALUE,
            jsonEolStyle = EolStyle.Lf
        )
        val parser1 = Parser(opts1)
        val formatter1 = Formatter(opts1)
        val output1 = formatter1.format(parser1.parse(input))
        val outputLines1 = output1.trimEnd().split('\n')
        assertThat(outputLines1.size).isEqualTo(1)

        // If we force expanding at depth 0, we should get 4 lines (more or less like the input).
        val opts2 = FracturedJsonOptions(
            maxInlineComplexity = 100,
            maxTotalLineLength = Int.MAX_VALUE,
            alwaysExpandDepth = 0,
            jsonEolStyle = EolStyle.Lf
        )
        val parser2 = Parser(opts2)
        val formatter2 = Formatter(opts2)
        val output2 = formatter2.format(parser2.parse(input))
        val outputLines2 = output2.trimEnd().split('\n')
        assertThat(outputLines2.size).isEqualTo(4)

        // If we force expanding at depth 1, we'll get lots of lines.
        val opts3 = FracturedJsonOptions(
            maxInlineComplexity = 100,
            maxTotalLineLength = Int.MAX_VALUE,
            alwaysExpandDepth = 1,
            jsonEolStyle = EolStyle.Lf
        )
        val parser3 = Parser(opts3)
        val formatter3 = Formatter(opts3)
        val output3 = formatter3.format(parser3.parse(input))
        val outputLines3 = output3.trimEnd().split('\n')
        assertThat(outputLines3.size).isEqualTo(10)
    }

    @Test
    @DisplayName("AlwaysExpandDepth doesn't prevent table formatting")
    fun alwaysExpandDepthDoesntPreventTable() {
        val input = "[ [1, 22, 9 ], [333, 4, 9 ] ]"

        // With AlwaysExpandDepth=0, this whole line isn't allowed to be inlined. But there's no reason
        // why it shouldn't qualify for table formatting. So the 1 should be padded to the size of 333, and
        // 4 should be padded to the size of 22. The commas and 9s should line up.
        val opts = FracturedJsonOptions(
            jsonEolStyle = EolStyle.Lf,
            alwaysExpandDepth = 0
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))
        val outputLines = output.trimEnd().split('\n').toTypedArray()

        assertThat(outputLines.size).isEqualTo(4)
        TestHelpers.testInstancesLineUp(outputLines, ",")
        TestHelpers.testInstancesLineUp(outputLines, "9")
    }
}
