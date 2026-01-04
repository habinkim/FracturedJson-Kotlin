package io.github.fracturedjson.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Formatter")
class FormatterTest {

    private val formatter = Formatter()

    @Nested
    @DisplayName("Primitive values")
    inner class PrimitiveValues {

        @Test
        fun `should format null value`() {
            val item = JsonItem.nullItem()
            val result = formatter.format(item)
            assertThat(result).isEqualTo("null")
        }

        @Test
        fun `should format true value`() {
            val item = JsonItem.booleanItem(true)
            val result = formatter.format(item)
            assertThat(result).isEqualTo("true")
        }

        @Test
        fun `should format false value`() {
            val item = JsonItem.booleanItem(false)
            val result = formatter.format(item)
            assertThat(result).isEqualTo("false")
        }

        @Test
        fun `should format string value`() {
            val item = JsonItem.stringItem("hello world")
            val result = formatter.format(item)
            assertThat(result).isEqualTo("\"hello world\"")
        }

        @Test
        fun `should format number value`() {
            val item = JsonItem.numberItem(42)
            val result = formatter.format(item)
            assertThat(result).isEqualTo("42")
        }

        @Test
        fun `should format decimal number`() {
            val item = JsonItem.numberItem(3.14159)
            val result = formatter.format(item)
            assertThat(result).isEqualTo("3.14159")
        }
    }

    @Nested
    @DisplayName("Arrays")
    inner class Arrays {

        @Test
        fun `should format empty array`() {
            val item = JsonItem.arrayItem(emptyList())
            val result = formatter.format(item)
            assertThat(result).isEqualTo("[]")
        }

        @Test
        fun `should format simple array inline`() {
            val item = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(2),
                JsonItem.numberItem(3)
            ))
            val result = formatter.format(item)
            assertThat(result).isEqualTo("[1, 2, 3]")
        }

        @Test
        fun `should format nested array`() {
            val inner = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(2)
            ))
            val item = JsonItem.arrayItem(listOf(inner))
            val result = formatter.format(item)
            assertThat(result).contains("[")
            assertThat(result).contains("]")
        }
    }

    @Nested
    @DisplayName("Objects")
    inner class Objects {

        @Test
        fun `should format empty object`() {
            val item = JsonItem.objectItem(emptyList())
            val result = formatter.format(item)
            assertThat(result).isEqualTo("{}")
        }

        @Test
        fun `should format simple object inline`() {
            val child = JsonItem.stringItem("value")
            child.name = "key"
            val item = JsonItem.objectItem(listOf(child))
            val result = formatter.format(item)
            assertThat(result).contains("\"key\"")
            assertThat(result).contains("\"value\"")
        }

        @Test
        fun `should format object with multiple properties`() {
            val name = JsonItem.stringItem("John")
            name.name = "name"
            val age = JsonItem.numberItem(30)
            age.name = "age"
            val item = JsonItem.objectItem(listOf(name, age))
            val result = formatter.format(item)
            assertThat(result).contains("\"name\"")
            assertThat(result).contains("\"John\"")
            assertThat(result).contains("\"age\"")
            assertThat(result).contains("30")
        }
    }

    @Nested
    @DisplayName("Options")
    inner class FormatterOptions {

        @Test
        fun `should respect indentSpaces option`() {
            val options = FracturedJsonOptions(indentSpaces = 2)
            val localFormatter = Formatter(options)

            val child = JsonItem.stringItem("value")
            child.name = "key"
            val item = JsonItem.objectItem(listOf(child, child.copy()))

            // Force expansion by using lower maxInlineComplexity
            options.maxInlineComplexity = 0
            val result = localFormatter.format(item)

            assertThat(result).contains("  ") // 2 spaces indentation
        }

        @Test
        fun `should respect maxTotalLineLength option`() {
            val options = FracturedJsonOptions(maxTotalLineLength = 40)
            val localFormatter = Formatter(options)

            val children = (1..10).map { JsonItem.numberItem(it) }
            val item = JsonItem.arrayItem(children)

            val result = localFormatter.format(item)
            // Should wrap to multiple lines when line is too long
            val lines = result.split("\n")
            lines.forEach { line ->
                assertThat(line.length).isLessThanOrEqualTo(40)
            }
        }
    }

    @Nested
    @DisplayName("Minification")
    inner class Minification {

        @Test
        fun `should minify simple object`() {
            val name = JsonItem.stringItem("John")
            name.name = "name"
            val age = JsonItem.numberItem(30)
            age.name = "age"
            val item = JsonItem.objectItem(listOf(name, age))

            val result = formatter.minify(listOf(item))
            assertThat(result).doesNotContain(" ")
            assertThat(result).doesNotContain("\n")
        }

        @Test
        fun `should minify nested structure`() {
            val inner = JsonItem.arrayItem(listOf(
                JsonItem.numberItem(1),
                JsonItem.numberItem(2)
            ))
            inner.name = "numbers"
            val item = JsonItem.objectItem(listOf(inner))

            val result = formatter.minify(listOf(item))
            assertThat(result).isEqualTo("{\"numbers\":[1,2]}")
        }
    }

    private fun JsonItem.copy(): JsonItem {
        return JsonItem(this.type).apply {
            name = this@copy.name
            value = this@copy.value
            children = this@copy.children
        }
    }
}
