package io.github.fracturedjson.core

import io.github.fracturedjson.parser.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.StringWriter

/**
 * Tests that both output methods produce the same output - ported from C# BufferEquivalenceTests.cs
 */
@DisplayName("Buffer Equivalence")
class BufferEquivalenceTest {

    companion object {
        private val standardJsonSamples = listOf(
            """{"name": "John", "age": 30}""",
            """[1, 2, 3, 4, 5]""",
            """{"nested": {"a": 1, "b": 2}, "array": [1, 2, 3]}""",
            """[{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]""",
            """{"complex": [{"x": 1}, {"x": 2}], "simple": true}"""
        )

        private val jsonWithComments = listOf(
            """{ /* comment */ "a": 1 }""",
            """[ // line comment
                1, 2, 3
            ]""",
            """{"key": /* inline */ "value"}"""
        )

        private val optionsSets = listOf(
            FracturedJsonOptions(),
            FracturedJsonOptions(jsonEolStyle = EolStyle.Lf),
            FracturedJsonOptions(
                nestedBracketPadding = false,
                simpleBracketPadding = true,
                colonPadding = false,
                commaPadding = false,
                indentSpaces = 3,
                prefixString = "\t\t",
                jsonEolStyle = EolStyle.Crlf
            )
        )

        @JvmStatic
        fun generateUniversalParams(): List<Array<Any>> {
            val params = mutableListOf<Array<Any>>()

            for (json in standardJsonSamples) {
                for (options in optionsSets) {
                    params.add(arrayOf(json, options))
                }
            }

            for (json in jsonWithComments) {
                for (options in optionsSets) {
                    val moddedOpts = options.copy(
                        commentPolicy = CommentPolicy.Preserve,
                        preserveBlankLines = true
                    )
                    params.add(arrayOf(json, moddedOpts))
                }
            }

            return params
        }
    }

    @Nested
    @DisplayName("Reformat Equivalence")
    inner class ReformatEquivalence {

        /**
         * Tests that format to String and format to Writer produce the same output.
         */
        @ParameterizedTest(name = "Output equivalence for: {0}")
        @MethodSource("io.github.fracturedjson.core.BufferEquivalenceTest#generateUniversalParams")
        fun reformatSameForBothOverrides(inputText: String, options: FracturedJsonOptions) {
            val parser = Parser(options)
            val items = parser.parse(inputText)

            // Format directly as a string
            val formatter1 = Formatter(options)
            val formattedAsString = formatter1.format(items, 0)

            // Format to a Writer
            val stringWriter = StringWriter()
            val formatter2 = Formatter(options)
            formatter2.format(items, 0, stringWriter)
            val formattedViaWriter = stringWriter.toString()

            assertThat(formattedViaWriter).isEqualTo(formattedAsString)
        }
    }

    @Nested
    @DisplayName("Basic Consistency")
    inner class BasicConsistency {

        @Test
        @DisplayName("Multiple formats of same input produce same output")
        fun multipleFormatsConsistent() {
            val input = """{"a": 1, "b": [2, 3, 4], "c": {"nested": true}}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

            val parser = Parser(options)
            val items = parser.parse(input)
            val formatter = Formatter(options)

            val output1 = formatter.format(items)
            val output2 = formatter.format(items)
            val output3 = formatter.format(items)

            assertThat(output2).isEqualTo(output1)
            assertThat(output3).isEqualTo(output1)
        }

        @Test
        @DisplayName("String and Writer outputs match for single item")
        fun stringAndWriterMatchForSingleItem() {
            val input = """{"key": "value", "number": 42}"""
            val options = FracturedJsonOptions(jsonEolStyle = EolStyle.Lf)

            val parser = Parser(options)
            val items = parser.parse(input)
            val formatter = Formatter(options)

            // Format as string
            val asString = formatter.format(items[0])

            // Format to writer
            val writer = StringWriter()
            formatter.format(listOf(items[0]), 0, writer)
            val viaWriter = writer.toString()

            assertThat(viaWriter).isEqualTo(asString)
        }
    }
}
