package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Unit tests to ensure that everything works regardless of locale.
 * (Really this is just about parsing and formatting numbers consistently.)
 * Ported from C# LocalizationTests.cs
 */
@DisplayName("Localization")
class LocalizationTest {

    @Test
    @DisplayName("Locale doesn't matter for number formatting")
    fun localeDoesntMatter() {
        val inputRows = listOf(
            "[",
            "    { \"a\": 0, \"b\": 7.8 },",
            "    { \"a\": 9988776, \"b\": -0.06 }",
            "]"
        )

        val input = inputRows.joinToString("")
        val opts = FracturedJsonOptions(maxInlineComplexity = 0)

        val originalLocale = Locale.getDefault()
        try {
            // Test with invariant/US locale
            Locale.setDefault(Locale.US)
            val parser1 = Parser(opts)
            val formatter1 = Formatter(opts)
            val outputUs = formatter1.format(parser1.parse(input))

            // Test with Norwegian locale (uses comma as decimal separator)
            Locale.setDefault(Locale("nb", "NO"))
            val parser2 = Parser(opts)
            val formatter2 = Formatter(opts)
            val outputNbNo = formatter2.format(parser2.parse(input))

            // Test with French locale (uses comma as decimal separator)
            Locale.setDefault(Locale.FRANCE)
            val parser3 = Parser(opts)
            val formatter3 = Formatter(opts)
            val outputFrFr = formatter3.format(parser3.parse(input))

            // All outputs should be identical regardless of locale
            assertThat(outputNbNo).isEqualTo(outputUs)
            assertThat(outputFrFr).isEqualTo(outputUs)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
