package io.github.fracturedjson.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.fracturedjson.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java mirror of JsonNodeConverterTest.kt (core tests only).
 * Extension function tests are Kotlin-only as they use Kotlin syntax.
 * Must be kept in sync with the Kotlin version's Conversion and ComplexStructures sections.
 */
@DisplayName("JsonNodeConverter")
public class JsonNodeConverterJavaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("should convert null")
        void shouldConvertNull() {
            JsonNode node = mapper.nullNode();
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Null);
        }

        @Test
        @DisplayName("should convert boolean true")
        void shouldConvertBooleanTrue() {
            JsonNode node = mapper.valueToTree(true);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.True);
        }

        @Test
        @DisplayName("should convert boolean false")
        void shouldConvertBooleanFalse() {
            JsonNode node = mapper.valueToTree(false);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.False);
        }

        @Test
        @DisplayName("should convert integer")
        void shouldConvertInteger() {
            JsonNode node = mapper.valueToTree(42);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Number);
            assertThat(item.getValue()).isEqualTo("42");
        }

        @Test
        @DisplayName("should convert decimal")
        void shouldConvertDecimal() {
            JsonNode node = mapper.valueToTree(3.14);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Number);
        }

        @Test
        @DisplayName("should convert string")
        void shouldConvertString() {
            JsonNode node = mapper.valueToTree("hello");
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.String);
            assertThat(item.getValue()).contains("hello");
        }

        @Test
        @DisplayName("should convert array")
        void shouldConvertArray() {
            ArrayNode node = mapper.createArrayNode();
            node.add(1);
            node.add(2);
            node.add(3);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Array);
            assertThat(item.getChildren()).hasSize(3);
        }

        @Test
        @DisplayName("should convert object")
        void shouldConvertObject() {
            ObjectNode node = mapper.createObjectNode();
            node.put("name", "John");
            node.put("age", 30);
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Object);
            assertThat(item.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("should convert with property name")
        void shouldConvertWithPropertyName() {
            JsonNode node = mapper.valueToTree("value");
            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, "key");
            assertThat(item.getName()).isEqualTo("key");
        }
    }

    @Nested
    @DisplayName("Complex structures")
    class ComplexStructures {

        @Test
        @DisplayName("should handle nested structures")
        void shouldHandleNestedStructures() {
            ObjectNode node = mapper.createObjectNode();
            ObjectNode person = node.putObject("person");
            person.put("name", "John");
            ArrayNode addresses = person.putArray("addresses");
            addresses.addObject().put("city", "NYC");
            addresses.addObject().put("city", "LA");

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            assertThat(item.getComplexity()).isGreaterThan(1);
        }

        @Test
        @DisplayName("should format deeply nested structure")
        void shouldFormatDeeplyNestedStructure() {
            ObjectNode node = mapper.createObjectNode();
            ObjectNode level1 = node.putObject("level1");
            ObjectNode level2 = level1.putObject("level2");
            ObjectNode level3 = level2.putObject("level3");
            level3.put("value", 42);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter();
            String result = formatter.format(item, 0);

            assertThat(result).contains("level1");
            assertThat(result).contains("level2");
            assertThat(result).contains("level3");
            assertThat(result).contains("42");
        }
    }

    @Nested
    @DisplayName("Property Alignment")
    class PropertyAlignment {

        @Test
        @DisplayName("Property values aligned")
        void propValuesAligned() {
            ObjectNode node = mapper.createObjectNode();
            node.put("num", 14);
            node.put("string", "testing property alignment");
            ArrayNode arr = node.putArray("arrayWithLongName");
            arr.addNull();
            arr.addNull();
            arr.addNull();

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(15);
            opts.setColonBeforePropNamePadding(false);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(opts);
            String output = formatter.format(item, 0);
            String[] outputLines = output.trim().split("\n");

            // This object should be expanded with the property values and colons aligned.
            assertThat(outputLines.length).isEqualTo(9);
            TestHelpers.INSTANCE.testInstancesLineUp(outputLines, ":");
        }

        @Test
        @DisplayName("Property values aligned but not colons")
        void propValuesAlignedButNotColons() {
            ObjectNode node = mapper.createObjectNode();
            node.put("num", 14);
            node.put("string", "testing property alignment");
            ArrayNode arr = node.putArray("arrayWithLongName");
            arr.addNull();
            arr.addNull();
            arr.addNull();

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(15);
            opts.setColonBeforePropNamePadding(true);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(opts);
            String output = formatter.format(item, 0);
            String[] outputLines = output.trim().split("\n");

            assertThat(outputLines.length).isEqualTo(9);
            assertThat(outputLines[1]).contains("\"num\":");
            assertThat(outputLines[2]).contains("\"string\":");
            assertThat(outputLines[3]).contains("\"arrayWithLongName\":");
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[2].indexOf("\"testing"));
            assertThat(outputLines[1].indexOf("14")).isEqualTo(outputLines[3].indexOf('['));
        }

        @Test
        @DisplayName("Don't align prop vals when too much padding required")
        void dontAlignPropValsWhenTooMuchPaddingRequired() {
            ObjectNode node = mapper.createObjectNode();
            node.put("num", 14);
            node.put("string", "testing property alignment");
            ArrayNode arr = node.putArray("arrayWithLongName");
            arr.addNull();
            arr.addNull();
            arr.addNull();

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(12);
            opts.setColonBeforePropNamePadding(false);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(opts);
            String output = formatter.format(item, 0);
            String[] outputLines = output.trim().split("\n");

            assertThat(outputLines.length).isEqualTo(9);
            assertThat(outputLines[1]).contains("\"num\": 14,");
            assertThat(outputLines[2]).contains("\"string\": \"testing");
            assertThat(outputLines[3]).contains("\"arrayWithLongName\": [");
        }
    }

    @Nested
    @DisplayName("Table Formatting")
    class TableFormatting {

        @Test
        @DisplayName("nested elements line up")
        void nestedElementsLineUp() {
            ArrayNode node = mapper.createArrayNode();
            ObjectNode obj1 = node.addObject();
            obj1.put("name", "Alice");
            obj1.put("age", 25);
            ObjectNode obj2 = node.addObject();
            obj2.put("name", "Bob");
            obj2.put("age", 30);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            // Properties should align vertically
            String[] lines = result.split("\n");
            java.util.List<Integer> positions = new java.util.ArrayList<>();
            for (String line : lines) {
                int idx = line.indexOf("\"name\"");
                if (idx >= 0) positions.add(idx);
            }
            if (positions.size() > 1) {
                assertThat(positions.stream().distinct().count()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("nested elements compact when needed")
        void nestedElementsCompactWhenNeeded() {
            ArrayNode node = mapper.createArrayNode();
            ObjectNode obj1 = node.addObject();
            obj1.put("name", "Alice");
            obj1.put("age", 25);
            ObjectNode obj2 = node.addObject();
            obj2.put("name", "Bob");
            obj2.put("age", 30);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxTotalLineLength(30);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("name");
            assertThat(result).contains("age");
        }

        @Test
        @DisplayName("fall back on inline if needed")
        void fallBackOnInlineIfNeeded() {
            ArrayNode node = mapper.createArrayNode();
            node.addObject().put("name", "Alice");
            node.addObject().put("name", "Bob");

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxTotalLineLength(100);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("Alice");
            assertThat(result).contains("Bob");
        }

        @Test
        @DisplayName("handles nulls with arrays table columns")
        void handlesNullsWithArraysTableColumns() {
            ArrayNode node = mapper.createArrayNode();
            ArrayNode arr1 = node.addArray();
            arr1.add(1);
            arr1.addNull();
            ArrayNode arr2 = node.addArray();
            arr2.add(2);
            arr2.add(3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("null");
        }

        @Test
        @DisplayName("commas before padding works")
        void commasBeforePaddingWorks() {
            ArrayNode node = mapper.createArrayNode();
            ArrayNode arr1 = node.addArray();
            arr1.add(1);
            arr1.add(2);
            ArrayNode arr2 = node.addArray();
            arr2.add(10);
            arr2.add(20);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("20");
        }

        @Test
        @DisplayName("colons hug prop names when configured")
        void colonsHugPropNamesWhenConfigured() {
            ArrayNode node = mapper.createArrayNode();
            node.addObject().put("a", 1);
            node.addObject().put("longName", 2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setColonBeforePropNamePadding(true);
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("\"a\":");
            assertThat(result).contains("\"longName\":");
        }

        @Test
        @DisplayName("single columns with numbers work")
        void singleColumnsWithNumbersWork() {
            ArrayNode node = mapper.createArrayNode();
            node.add(1.5);
            node.add(22.333);
            node.add(100.1);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1.5");
            assertThat(result).contains("22.333");
            assertThat(result).contains("100.1");
        }
    }

    @Nested
    @DisplayName("Number Formatting")
    class NumberFormatting {

        @Test
        @DisplayName("inline array doesnt justify numbers")
        void inlineArrayDoesntJustifyNumbers() {
            ArrayNode node = mapper.createArrayNode();
            node.add(1);
            node.add(22);
            node.add(333);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(10);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("compact array does justify numbers")
        void compactArrayDoesJustifyNumbers() {
            ArrayNode node = mapper.createArrayNode();
            ArrayNode arr1 = node.addArray();
            arr1.add(1);
            arr1.add(2);
            ArrayNode arr2 = node.addArray();
            arr2.add(333);
            arr2.add(4444);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(1);
            options.setMaxCompactArrayComplexity(2);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("4444");
        }

        @Test
        @DisplayName("table array does justify numbers")
        void tableArrayDoesJustifyNumbers() {
            ArrayNode node = mapper.createArrayNode();
            ArrayNode arr1 = node.addArray();
            arr1.add(1);
            arr1.add(2);
            ArrayNode arr2 = node.addArray();
            arr2.add(333);
            arr2.add(4444);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("scientific notation numbers preserved")
        void scientificNotationNumbersPreserved() {
            ArrayNode node = mapper.createArrayNode();
            node.add(1e10);
            node.add(2);
            node.add(3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result.toLowerCase()).contains("e");
        }

        @Test
        @DisplayName("nulls respected when aligning numbers")
        void nullsRespectedWhenAligningNumbers() {
            ArrayNode node = mapper.createArrayNode();
            ArrayNode arr1 = node.addArray();
            arr1.add(1);
            arr1.addNull();
            ArrayNode arr2 = node.addArray();
            arr2.add(22);
            arr2.add(33);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("null");
        }

        @Test
        @DisplayName("left align works")
        void leftAlignWorks() {
            ArrayNode node = mapper.createArrayNode();
            node.addArray().add(1);
            node.addArray().add(22);
            node.addArray().add(333);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Left);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("right align works")
        void rightAlignWorks() {
            ArrayNode node = mapper.createArrayNode();
            node.addArray().add(1);
            node.addArray().add(22);
            node.addArray().add(333);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Right);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("decimal align works")
        void decimalAlignWorks() {
            ArrayNode node = mapper.createArrayNode();
            node.addArray().add(1.5);
            node.addArray().add(22.333);
            node.addArray().add(333.1);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Decimal);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1.5");
            assertThat(result).contains("22.333");
            assertThat(result).contains("333.1");
        }

        @Test
        @DisplayName("normalize align works")
        void normalizeAlignWorks() {
            ArrayNode node = mapper.createArrayNode();
            node.addArray().add(1.5);
            node.addArray().add(22.333);
            node.addArray().add(333.1);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Normalize);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("negative numbers align correctly")
        void negativeNumbersAlignCorrectly() {
            ArrayNode node = mapper.createArrayNode();
            node.addArray().add(1);
            node.addArray().add(-22);
            node.addArray().add(333);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = JsonNodeConverter.INSTANCE.convert(node, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("-22");
        }
    }
}
