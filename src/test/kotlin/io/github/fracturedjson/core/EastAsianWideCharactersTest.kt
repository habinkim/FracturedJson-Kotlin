package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for aligning data including double-wide characters.
 * FracturedJson doesn't handle this in the core library, but provides the functionality
 * via the StringLengthFunc property.
 *
 * Ported from C# EastAsianWideCharactersTests.cs
 */
@DisplayName("East Asian Wide Characters")
class EastAsianWideCharactersTest {

    @Test
    @DisplayName("Pads wide chars correctly with default function")
    fun padsWideCharsWithDefaultFunction() {
        val inputLines = listOf(
            "[",
            "    {\"Name\": \"李小龍\", \"Job\": \"Actor\", \"Born\": 1940},",
            "    {\"Name\": \"Mark Twain\", \"Job\": \"Writer\", \"Born\": 1835},",
            "    {\"Name\": \"孫子\", \"Job\": \"General\", \"Born\": -544}",
            "]"
        )
        val input = inputLines.joinToString("\n")

        val opts = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

        // With the default StringLengthFunc, all characters are treated as having the same width as space
        val parser = Parser(opts)
        val formatter = Formatter(opts) // Uses default: stringLengthFunc = { it.length }
        val output = formatter.format(parser.parse(input))
        val outputLines = output.trimEnd().split('\n').toTypedArray()

        // With the default StringLengthFunc, all characters are treated as having the same width,
        // so String.indexOf should give the same number for each row.
        TestHelpers.testInstancesLineUp(outputLines, "Job")
        TestHelpers.testInstancesLineUp(outputLines, "Born")
    }

    @Test
    @DisplayName("Pads wide chars correctly with custom width function")
    fun padsWideCharsWithCustomWidthFunction() {
        val inputLines = listOf(
            "[",
            "    {\"Name\": \"李小龍\", \"Job\": \"Actor\", \"Born\": 1940},",
            "    {\"Name\": \"Mark Twain\", \"Job\": \"Writer\", \"Born\": 1835},",
            "    {\"Name\": \"孫子\", \"Job\": \"General\", \"Born\": -544}",
            "]"
        )
        val input = inputLines.joinToString("\n")

        val opts = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

        val parser = Parser(opts)

        // Using custom wide char string length function
        val formatter = Formatter(opts, stringLengthFunc = ::wideCharStringLength)
        val output = formatter.format(parser.parse(input))
        val outputLines = output.trimEnd().split('\n')

        // In using the WideCharStringLength function, the Asian characters are each treated as 2 spaces wide.
        // Whether these line up visually depends on your font and rendering policies.
        assertThat(outputLines[1].indexOf("Job")).isEqualTo(25)
        assertThat(outputLines[2].indexOf("Job")).isEqualTo(28)
        assertThat(outputLines[3].indexOf("Job")).isEqualTo(26)
    }

    /**
     * Calculate the display width of a string, treating East Asian wide characters as 2 units.
     * This is a simplified implementation that handles common CJK characters.
     */
    private fun wideCharStringLength(str: String): Int {
        return str.codePoints().map { cp ->
            if (isWideCharacter(cp)) 2 else 1
        }.sum()
    }

    /**
     * Determines if a Unicode code point is an East Asian wide character.
     * This is a simplified check covering common CJK ranges.
     */
    private fun isWideCharacter(codePoint: Int): Boolean {
        return when (codePoint) { // CJK Unified Ideographs
            in 0x4E00..0x9FFF -> true
            // CJK Unified Ideographs Extension A
            in 0x3400..0x4DBF -> true
            // CJK Unified Ideographs Extension B
            in 0x20000..0x2A6DF -> true
            // CJK Compatibility Ideographs
            in 0xF900..0xFAFF -> true
            // Hangul Syllables
            in 0xAC00..0xD7AF -> true
            // Hiragana
            in 0x3040..0x309F -> true
            // Katakana
            in 0x30A0..0x30FF -> true
            // Full-width forms
            in 0xFF00..0xFFEF -> true
            else -> false
        }
    }
}
