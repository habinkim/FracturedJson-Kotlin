package io.github.fracturedjson.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java mirror of FormatterTest.kt
 * Must be kept in sync with the Kotlin version.
 */
@DisplayName("Formatter")
public class FormatterJavaTest {

    private final Formatter formatter = new Formatter();

    @Nested
    @DisplayName("Primitive values")
    class PrimitiveValues {

        @Test
        @DisplayName("should format null value")
        void shouldFormatNullValue() {
            JsonItem item = JsonItem.Companion.nullItem();
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("should format true value")
        void shouldFormatTrueValue() {
            JsonItem item = JsonItem.Companion.booleanItem(true);
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("true");
        }

        @Test
        @DisplayName("should format false value")
        void shouldFormatFalseValue() {
            JsonItem item = JsonItem.Companion.booleanItem(false);
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("false");
        }

        @Test
        @DisplayName("should format string value")
        void shouldFormatStringValue() {
            JsonItem item = JsonItem.Companion.stringItem("hello world");
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("\"hello world\"");
        }

        @Test
        @DisplayName("should format number value")
        void shouldFormatNumberValue() {
            JsonItem item = JsonItem.Companion.numberItem(42);
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("42");
        }

        @Test
        @DisplayName("should format decimal number")
        void shouldFormatDecimalNumber() {
            JsonItem item = JsonItem.Companion.numberItem(3.14159);
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("3.14159");
        }
    }

    @Nested
    @DisplayName("Arrays")
    class Arrays {

        @Test
        @DisplayName("should format empty array")
        void shouldFormatEmptyArray() {
            JsonItem item = JsonItem.Companion.arrayItem(Collections.emptyList());
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should format simple array inline")
        void shouldFormatSimpleArrayInline() {
            JsonItem item = JsonItem.Companion.arrayItem(List.of(
                JsonItem.Companion.numberItem(1),
                JsonItem.Companion.numberItem(2),
                JsonItem.Companion.numberItem(3)
            ));
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("[1, 2, 3]");
        }

        @Test
        @DisplayName("should format nested array")
        void shouldFormatNestedArray() {
            JsonItem inner = JsonItem.Companion.arrayItem(List.of(
                JsonItem.Companion.numberItem(1),
                JsonItem.Companion.numberItem(2)
            ));
            JsonItem item = JsonItem.Companion.arrayItem(List.of(inner));
            String result = formatter.format(item, 0);
            assertThat(result).contains("[");
            assertThat(result).contains("]");
        }
    }

    @Nested
    @DisplayName("Objects")
    class Objects {

        @Test
        @DisplayName("should format empty object")
        void shouldFormatEmptyObject() {
            JsonItem item = JsonItem.Companion.objectItem(Collections.emptyList());
            String result = formatter.format(item, 0);
            assertThat(result).isEqualTo("{}");
        }

        @Test
        @DisplayName("should format simple object inline")
        void shouldFormatSimpleObjectInline() {
            JsonItem child = JsonItem.Companion.stringItem("value");
            child.setName("key");
            JsonItem item = JsonItem.Companion.objectItem(List.of(child));
            String result = formatter.format(item, 0);
            assertThat(result).contains("\"key\"");
            assertThat(result).contains("\"value\"");
        }

        @Test
        @DisplayName("should format object with multiple properties")
        void shouldFormatObjectWithMultipleProperties() {
            JsonItem name = JsonItem.Companion.stringItem("John");
            name.setName("name");
            JsonItem age = JsonItem.Companion.numberItem(30);
            age.setName("age");
            JsonItem item = JsonItem.Companion.objectItem(List.of(name, age));
            String result = formatter.format(item, 0);
            assertThat(result).contains("\"name\"");
            assertThat(result).contains("\"John\"");
            assertThat(result).contains("\"age\"");
            assertThat(result).contains("30");
        }
    }

    @Nested
    @DisplayName("Options")
    class FormatterOptions {

        @Test
        @DisplayName("should respect indentSpaces option")
        void shouldRespectIndentSpacesOption() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setIndentSpaces(2);
            options.setMaxInlineComplexity(0);
            Formatter localFormatter = new Formatter(options);

            JsonItem child1 = JsonItem.Companion.stringItem("value");
            child1.setName("key");
            JsonItem child2 = JsonItem.Companion.stringItem("value2");
            child2.setName("key2");
            JsonItem item = JsonItem.Companion.objectItem(List.of(child1, child2));

            String result = localFormatter.format(item, 0);
            assertThat(result).contains("  "); // 2 spaces indentation
        }

        @Test
        @DisplayName("should respect maxTotalLineLength option")
        void shouldRespectMaxTotalLineLengthOption() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxTotalLineLength(40);
            Formatter localFormatter = new Formatter(options);

            List<JsonItem> children = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(JsonItem.Companion::numberItem)
                .toList();
            JsonItem item = JsonItem.Companion.arrayItem(children);

            String result = localFormatter.format(item, 0);
            // Should wrap to multiple lines when line is too long
            String[] lines = result.split("\n");
            for (String line : lines) {
                assertThat(line.length()).isLessThanOrEqualTo(40);
            }
        }
    }

    @Nested
    @DisplayName("Minification")
    class Minification {

        @Test
        @DisplayName("should minify simple object")
        void shouldMinifySimpleObject() {
            JsonItem name = JsonItem.Companion.stringItem("John");
            name.setName("name");
            JsonItem age = JsonItem.Companion.numberItem(30);
            age.setName("age");
            JsonItem item = JsonItem.Companion.objectItem(List.of(name, age));

            String result = formatter.minify(List.of(item));
            assertThat(result).doesNotContain(" ");
            assertThat(result).doesNotContain("\n");
        }

        @Test
        @DisplayName("should minify nested structure")
        void shouldMinifyNestedStructure() {
            JsonItem inner = JsonItem.Companion.arrayItem(List.of(
                JsonItem.Companion.numberItem(1),
                JsonItem.Companion.numberItem(2)
            ));
            inner.setName("numbers");
            JsonItem item = JsonItem.Companion.objectItem(List.of(inner));

            String result = formatter.minify(List.of(item));
            assertThat(result).isEqualTo("{\"numbers\":[1,2]}");
        }
    }
}
