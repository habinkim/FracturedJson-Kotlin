package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for complexity and length settings - ported from C# LengthAndComplexityTests.cs
 */
@DisplayName("Length and Complexity")
class LengthAndComplexityTest {

    @Nested
    @DisplayName("Inline Complexity")
    inner class InlineComplexity {

        /**
         * Test a specific piece of input with a variety of MaxInlineComplexity settings, and compare
         * the number of lines in the output to the expected values.
         */
        @ParameterizedTest(name = "maxInlineComplexity={0} -> {1} lines")
        @CsvSource(
            "4, 1",   // All on one line
            "3, 3",   // Outermost brackets on their own lines
            "2, 6",   // Q & R each get their own rows, plus outer [ {...} ]
            "1, 9",   // Q gets broken up. R stays inline.
            "0, 14"   // Maximum expansion, basically
        )
        fun correctLineCountForInlineComplexity(maxInlineComplexity: Int, expectedNumberOfLines: Int) {
            val inputLines = listOf(
                "[",
                "    { \"Q\": [ {\"foo\": \"bar\"}, 678 ], \"R\": [ {}, \"asdf\"] }",
                "]"
            )
            val input = inputLines.joinToString("\n")

            val opts = FracturedJsonOptions(
                maxTotalLineLength = 90,
                jsonEolStyle = EolStyle.Lf,
                maxInlineComplexity = maxInlineComplexity,
                maxCompactArrayComplexity = -1,
                maxTableRowComplexity = -1
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            assertThat(outputLines.size).isEqualTo(expectedNumberOfLines)
        }
    }

    @Nested
    @DisplayName("Compact Array Complexity")
    inner class CompactArrayComplexity {

        /**
         * Tests a known piece of input against multiple values of MaxCompactArrayComplexity.
         */
        @ParameterizedTest(name = "maxCompactArrayComplexity={0} -> {1} lines")
        @CsvSource(
            "2, 5",  // 3 formatted columns across 3 lines plus the outer []
            "1, 9"   // Each subarray gets its own line, plus the outer []
        )
        fun correctLineCountForMultilineCompact(maxCompactArrayComplexity: Int, expectedNumberOfLines: Int) {
            val inputLines = listOf(
                "[",
                "    [1,2,3], [4,5,6], [7,8,9], [null,11,12], [13,14,15], [16,17,18], [19,null,21]",
                "]"
            )
            val input = inputLines.joinToString("\n")

            val opts = FracturedJsonOptions(
                maxTotalLineLength = 60,
                jsonEolStyle = EolStyle.Lf,
                maxInlineComplexity = 2,
                maxCompactArrayComplexity = maxCompactArrayComplexity,
                maxTableRowComplexity = -1
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            assertThat(outputLines.size).isEqualTo(expectedNumberOfLines)
        }
    }

    @Nested
    @DisplayName("Line Length")
    inner class LineLength {

        /**
         * Tests a single piece of sample data with multiple length settings, and compares the number of output
         * lines with the expected output.
         */
        @ParameterizedTest(name = "totalLength={0}, minItems={1} -> {2} lines")
        @CsvSource(
            "100, 3, 1",  // All on one line
            "90, 3, 4",   // Two row compact multiline array, + two for []
            "70, 3, 4",   // Compact multiline array - Kotlin calculation differs from C# template width
            "57, 3, 5",   // Three row compact multiline array, + two for []
            "50, 3, 9",   // Falls back to expanded formatting (1 item per line + 2 for [])
            "50, 2, 6"    // Four row compact multiline array, + two for []
        )
        fun correctLineCountForLineLength(totalLength: Int, minItemsPerRow: Int, expectedNumberOfLines: Int) {
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
                minCompactArrayRowItems = minItemsPerRow
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            assertThat(outputLines.size).isEqualTo(expectedNumberOfLines)
        }
    }
}
