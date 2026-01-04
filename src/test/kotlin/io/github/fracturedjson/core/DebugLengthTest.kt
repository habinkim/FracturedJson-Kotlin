package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.junit.jupiter.api.Test

class DebugLengthTest {
    private fun testLineLength(totalLength: Int, minItems: Int) {
        val inputLines = listOf(
            "[",
            "    [1,2,3], [4,5,6], [7,8,9], [null,11,12], [13,14,15], [16,17,18], [19,null,21]",
            "]"
        )
        val input = inputLines.joinToString("\n")

        val opts = FracturedJsonOptions(
            maxTotalLineLength = totalLength,
            jsonEolStyle = EolStyle.Lf,
            maxInlineComplexity = 2,
            maxCompactArrayComplexity = 2,
            maxTableRowComplexity = 2,
            minCompactArrayRowItems = minItems
        )
        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))
        println("totalLength=$totalLength, minItems=$minItems -> ${output.trimEnd().split('\n').size} lines")
    }

    @Test
    fun debugAllLineLengthCases() {
        testLineLength(100, 3)
        testLineLength(90, 3)
        testLineLength(70, 3)
        testLineLength(57, 3)
        testLineLength(50, 3)
        testLineLength(50, 2)
    }
}
