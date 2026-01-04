package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests to make sure commas are only where they're supposed to be - ported from C# EndingCommaFormattingTests.cs
 */
@DisplayName("Ending Comma Formatting")
class EndingCommaFormattingTest {

    /**
     * Tests that comments at the end of an expanded object/array don't cause commas before them.
     */
    @Test
    @DisplayName("No commas for comments in expanded format")
    fun noCommasForCommentsExpanded() {
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
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))
        val outputLines = output.trimEnd().split('\n')

        // Both comments here are standalone, so we're not allowed to format this as inline or compact-array.
        // The row types are dissimilar, so they won't be table-formatted either.
        assertThat(outputLines.size).isEqualTo(6)

        // There should only be one comma - between the 1 and false.
        val commaCount = output.count { it == ',' }
        assertThat(commaCount).isEqualTo(1)
    }

    /**
     * Tests that comments at the end of a table-formatted object/array don't cause commas before them.
     */
    @Test
    @DisplayName("No commas for comments in table format")
    fun noCommasForCommentsTable() {
        val inputLines = listOf(
            "[",
            "/*a*/",
            "[1], [false]",
            "/*b*/",
            "]"
        )
        val input = inputLines.joinToString("\n")

        val opts = FracturedJsonOptions(
            jsonEolStyle = EolStyle.Lf,
            commentPolicy = CommentPolicy.Preserve
        )

        val parser = Parser(opts)
        val formatter = Formatter(opts)
        val output = formatter.format(parser.parse(input))
        val outputLines = output.trimEnd().split('\n')

        // Both comments here are standalone, so we're not allowed to format this as inline or compact-array.
        // With standalone comments, table formatting is skipped in favor of expanded formatting.
        assertThat(outputLines.size).isEqualTo(6)

        // There should only be one comma - between [1] and [false].
        val commaCount = output.count { it == ',' }
        assertThat(commaCount).isEqualTo(1)
    }
}
