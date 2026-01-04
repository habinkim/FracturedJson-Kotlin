package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Property alignment tests - ported from C# PropertyAlignmentTests.cs
 */
@DisplayName("Property Alignment")
class PropertyAlignmentTest {

    @Nested
    @DisplayName("Basic Alignment")
    inner class BasicAlignment {

        @Test
        @DisplayName("Property values aligned")
        fun propValuesAligned() {
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
            val outputLines = output.trimEnd().split('\n').toTypedArray()

            // This object should be expanded with the property values and colons aligned.
            // The array should be expanded as well.
            assertThat(outputLines.size).isEqualTo(9)
            TestHelpers.testInstancesLineUp(outputLines, ":")
        }

        @Test
        @DisplayName("Property values aligned but not colons")
        fun propValuesAlignedButNotColons() {
            val input = """
                {
                    "num": 14,
                    "string": "testing property alignment",
                    "arrayWithLongName": [null, null, null]
                }
            """.trimIndent()

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 15,
                colonBeforePropNamePadding = true,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            // This object should be expanded with the property values, but the colons should hug
            // the prop names instead of being aligned.
            assertThat(outputLines.size).isEqualTo(9)
            assertThat(outputLines[1]).contains("\"num\":")
            assertThat(outputLines[2]).contains("\"string\":")
            assertThat(outputLines[3]).contains("\"arrayWithLongName\":")
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[2].indexOf("\"testing"))
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[3].indexOf('['))
        }
    }

    @Nested
    @DisplayName("Alignment Limits")
    inner class AlignmentLimits {

        @Test
        @DisplayName("Don't align prop vals when too much padding required")
        fun dontAlignPropValsWhenTooMuchPaddingRequired() {
            val input = """
                {
                    "num": 14,
                    "string": "testing property alignment",
                    "arrayWithLongName": [null, null, null]
                }
            """.trimIndent()

            val opts = FracturedJsonOptions(
                maxPropNamePadding = 12,
                colonBeforePropNamePadding = false,
                maxInlineComplexity = -1,
                maxCompactArrayComplexity = -1,
                jsonEolStyle = EolStyle.Lf
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            // This object should be expanded but the property values shouldn't be aligned since
            // the length of the prop names differ by more than MaxPropNamePadding.
            assertThat(outputLines.size).isEqualTo(9)
            assertThat(outputLines[1]).contains("\"num\": 14,")
            assertThat(outputLines[2]).contains("\"string\": \"testing")
            assertThat(outputLines[3]).contains("\"arrayWithLongName\": [")
        }

        @Test
        @DisplayName("Don't align when simple value too long")
        fun dontAlignWhenSimpleValueTooLong() {
            val input = """
                {
                    "foo": [1, 2, 4],
                    "bar": null,
                    "bazzzz": [0]
                }
            """.trimIndent()

            val opts = FracturedJsonOptions(
                commentPolicy = CommentPolicy.Preserve,
                colonBeforePropNamePadding = false,
                maxTotalLineLength = 36,
                jsonEolStyle = EolStyle.Lf
            )

            val parser = Parser(opts)
            val formatter = Formatter(opts)
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            // If we tried to align the properties here, bar's null would exceed the line length
            // due to the padding. FJ should give up on aligning properties in that case.
            assertThat(output).contains("\"bar\":")
            assertThat(outputLines[1].indexOf(':')).isNotEqualTo(outputLines[outputLines.size - 2].indexOf(':'))
        }
    }

    @Nested
    @DisplayName("Comments and Alignment")
    inner class CommentsAndAlignment {

        @Test
        @DisplayName("Don't align prop vals when multiline comment")
        fun dontAlignPropValsWhenMultilineComment() {
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
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n')

            // Since there's a comment with a line break between a prop label and value,
            // we shouldn't even try to align property values here.
            assertThat(outputLines.size).isEqualTo(11)
            assertThat(outputLines[9].indexOf(':')).isNotEqualTo(outputLines[8].indexOf(':'))
        }

        @Test
        @DisplayName("Align prop vals when simple comment")
        fun alignPropValsWhenSimpleComment() {
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
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n').toTypedArray()

            // Since the comments can all be inlined, this should be table-formatted.
            assertThat(outputLines.size).isEqualTo(5)
            TestHelpers.testInstancesLineUp(outputLines, "[")
        }

        @Test
        @DisplayName("Align prop vals when array wraps")
        fun alignPropValsWhenArrayWraps() {
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
            val output = formatter.format(parser.parse(input))
            val outputLines = output.trimEnd().split('\n').toTypedArray()

            // The lines are too short for foo to be inlined, so it's compact multiline.
            // But there's still enough room for bar if we align the props.
            assertThat(outputLines.size).isEqualTo(7)
            TestHelpers.testInstancesLineUp(outputLines, "[")
            TestHelpers.testInstancesLineUp(outputLines, ":")
        }
    }
}
