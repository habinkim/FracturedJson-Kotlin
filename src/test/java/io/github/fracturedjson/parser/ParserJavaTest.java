package io.github.fracturedjson.parser;

import io.github.fracturedjson.core.CommentPolicy;
import io.github.fracturedjson.core.FracturedJsonOptions;
import io.github.fracturedjson.core.JsonItem;
import io.github.fracturedjson.core.JsonItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Java mirror of ParserTest.kt
 * Must be kept in sync with the Kotlin version.
 */
@DisplayName("Parser")
public class ParserJavaTest {

    private final Parser parser = new Parser();

    @Nested
    @DisplayName("Primitive values")
    class PrimitiveValues {

        @Test
        @DisplayName("should parse null")
        void shouldParseNull() {
            List<JsonItem> result = parser.parse("null");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Null);
            assertThat(result.get(0).getValue()).isEqualTo("null");
        }

        @Test
        @DisplayName("should parse true")
        void shouldParseTrue() {
            List<JsonItem> result = parser.parse("true");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.True);
            assertThat(result.get(0).getValue()).isEqualTo("true");
        }

        @Test
        @DisplayName("should parse false")
        void shouldParseFalse() {
            List<JsonItem> result = parser.parse("false");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.False);
            assertThat(result.get(0).getValue()).isEqualTo("false");
        }

        @Test
        @DisplayName("should parse integer")
        void shouldParseInteger() {
            List<JsonItem> result = parser.parse("42");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Number);
            assertThat(result.get(0).getValue()).isEqualTo("42");
        }

        @Test
        @DisplayName("should parse negative number")
        void shouldParseNegativeNumber() {
            List<JsonItem> result = parser.parse("-123");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Number);
            assertThat(result.get(0).getValue()).isEqualTo("-123");
        }

        @Test
        @DisplayName("should parse decimal number")
        void shouldParseDecimalNumber() {
            List<JsonItem> result = parser.parse("3.14159");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Number);
            assertThat(result.get(0).getValue()).isEqualTo("3.14159");
        }

        @Test
        @DisplayName("should parse scientific notation")
        void shouldParseScientificNotation() {
            List<JsonItem> result = parser.parse("1.5e10");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Number);
            assertThat(result.get(0).getValue()).isEqualTo("1.5e10");
        }

        @Test
        @DisplayName("should parse string")
        void shouldParseString() {
            List<JsonItem> result = parser.parse("\"hello world\"");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.String);
            assertThat(result.get(0).getValue()).isEqualTo("\"hello world\"");
        }

        @Test
        @DisplayName("should parse string with escape sequences")
        void shouldParseStringWithEscapeSequences() {
            List<JsonItem> result = parser.parse("\"line1\\nline2\"");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.String);
        }
    }

    @Nested
    @DisplayName("Arrays")
    class Arrays {

        @Test
        @DisplayName("should parse empty array")
        void shouldParseEmptyArray() {
            List<JsonItem> result = parser.parse("[]");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Array);
            assertThat(result.get(0).getChildren()).isEmpty();
        }

        @Test
        @DisplayName("should parse array with single element")
        void shouldParseArrayWithSingleElement() {
            List<JsonItem> result = parser.parse("[1]");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Array);
            assertThat(result.get(0).getChildren()).hasSize(1);
            assertThat(result.get(0).getChildren().get(0).getType()).isEqualTo(JsonItemType.Number);
        }

        @Test
        @DisplayName("should parse array with multiple elements")
        void shouldParseArrayWithMultipleElements() {
            List<JsonItem> result = parser.parse("[1, 2, 3]");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren()).hasSize(3);
        }

        @Test
        @DisplayName("should parse nested arrays")
        void shouldParseNestedArrays() {
            List<JsonItem> result = parser.parse("[[1, 2], [3, 4]]");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren()).hasSize(2);
            assertThat(result.get(0).getChildren().get(0).getType()).isEqualTo(JsonItemType.Array);
            assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("should parse array with trailing comma when allowed")
        void shouldParseArrayWithTrailingCommaWhenAllowed() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setAllowTrailingCommas(true);
            Parser localParser = new Parser(options);
            List<JsonItem> result = localParser.parse("[1, 2, 3,]");
            assertThat(result.get(0).getChildren()).hasSize(3);
        }

        @Test
        @DisplayName("should reject trailing comma by default")
        void shouldRejectTrailingCommaByDefault() {
            assertThatThrownBy(() -> parser.parse("[1, 2,]"))
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Objects")
    class Objects {

        @Test
        @DisplayName("should parse empty object")
        void shouldParseEmptyObject() {
            List<JsonItem> result = parser.parse("{}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Object);
            assertThat(result.get(0).getChildren()).isEmpty();
        }

        @Test
        @DisplayName("should parse object with single property")
        void shouldParseObjectWithSingleProperty() {
            List<JsonItem> result = parser.parse("{\"key\": \"value\"}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren()).hasSize(1);
            assertThat(result.get(0).getChildren().get(0).getName()).isEqualTo("key");
        }

        @Test
        @DisplayName("should parse object with multiple properties")
        void shouldParseObjectWithMultipleProperties() {
            List<JsonItem> result = parser.parse("{\"name\": \"John\", \"age\": 30}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("should parse nested objects")
        void shouldParseNestedObjects() {
            List<JsonItem> result = parser.parse("{\"person\": {\"name\": \"John\"}}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChildren().get(0).getType()).isEqualTo(JsonItemType.Object);
            assertThat(result.get(0).getChildren().get(0).getName()).isEqualTo("person");
        }
    }

    @Nested
    @DisplayName("Comments")
    class Comments {

        @Test
        @DisplayName("should reject comments by default")
        void shouldRejectCommentsByDefault() {
            assertThatThrownBy(() -> parser.parse("// comment\n{}"))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("should preserve line comments when configured")
        void shouldPreserveLineCommentsWhenConfigured() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setCommentPolicy(CommentPolicy.Preserve);
            Parser localParser = new Parser(options);
            List<JsonItem> result = localParser.parse("// comment\n{}");
            // Comment should be attached or preserved
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("should remove comments when configured")
        void shouldRemoveCommentsWhenConfigured() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setCommentPolicy(CommentPolicy.Remove);
            Parser localParser = new Parser(options);
            List<JsonItem> result = localParser.parse("// comment\n{}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Object);
        }

        @Test
        @DisplayName("should handle block comments")
        void shouldHandleBlockComments() {
            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setCommentPolicy(CommentPolicy.Remove);
            Parser localParser = new Parser(options);
            List<JsonItem> result = localParser.parse("/* comment */ {}");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(JsonItemType.Object);
        }
    }

    @Nested
    @DisplayName("Complex structures")
    class ComplexStructures {

        @Test
        @DisplayName("should parse mixed array")
        void shouldParseMixedArray() {
            List<JsonItem> result = parser.parse("[1, \"two\", true, null, {\"key\": \"value\"}]");
            assertThat(result.get(0).getChildren()).hasSize(5);
            assertThat(result.get(0).getChildren().get(0).getType()).isEqualTo(JsonItemType.Number);
            assertThat(result.get(0).getChildren().get(1).getType()).isEqualTo(JsonItemType.String);
            assertThat(result.get(0).getChildren().get(2).getType()).isEqualTo(JsonItemType.True);
            assertThat(result.get(0).getChildren().get(3).getType()).isEqualTo(JsonItemType.Null);
            assertThat(result.get(0).getChildren().get(4).getType()).isEqualTo(JsonItemType.Object);
        }

        @Test
        @DisplayName("should parse deeply nested structure")
        void shouldParseDeeplyNestedStructure() {
            String json = "{\"a\": {\"b\": {\"c\": {\"d\": 1}}}}";
            List<JsonItem> result = parser.parse(json);
            JsonItem current = result.get(0);
            for (int depth = 0; depth < 4; depth++) {
                assertThat(current.getType()).isIn(JsonItemType.Object, JsonItemType.Number);
                if (!current.getChildren().isEmpty()) {
                    current = current.getChildren().get(0);
                }
            }
        }
    }
}
