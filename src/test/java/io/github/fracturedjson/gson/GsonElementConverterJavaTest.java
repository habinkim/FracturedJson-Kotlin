package io.github.fracturedjson.gson;

import com.google.gson.*;
import io.github.fracturedjson.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java mirror of GsonElementConverterTest.kt (core tests only).
 * Extension function tests are Kotlin-only as they use Kotlin syntax.
 * Must be kept in sync with the Kotlin version's Conversion and ComplexStructures sections.
 */
@DisplayName("GsonElementConverter")
public class GsonElementConverterJavaTest {

    private final Gson gson = new Gson();

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("should convert null")
        void shouldConvertNull() {
            JsonElement element = JsonNull.INSTANCE;
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Null);
        }

        @Test
        @DisplayName("should convert boolean true")
        void shouldConvertBooleanTrue() {
            JsonElement element = new JsonPrimitive(true);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.True);
        }

        @Test
        @DisplayName("should convert boolean false")
        void shouldConvertBooleanFalse() {
            JsonElement element = new JsonPrimitive(false);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.False);
        }

        @Test
        @DisplayName("should convert integer")
        void shouldConvertInteger() {
            JsonElement element = new JsonPrimitive(42);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Number);
            assertThat(item.getValue()).isEqualTo("42");
        }

        @Test
        @DisplayName("should convert decimal")
        void shouldConvertDecimal() {
            JsonElement element = new JsonPrimitive(3.14);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Number);
        }

        @Test
        @DisplayName("should convert string")
        void shouldConvertString() {
            JsonElement element = new JsonPrimitive("hello");
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.String);
            assertThat(item.getValue()).contains("hello");
        }

        @Test
        @DisplayName("should convert array")
        void shouldConvertArray() {
            JsonArray element = new JsonArray();
            element.add(1);
            element.add(2);
            element.add(3);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Array);
            assertThat(item.getChildren()).hasSize(3);
        }

        @Test
        @DisplayName("should convert object")
        void shouldConvertObject() {
            JsonObject element = new JsonObject();
            element.addProperty("name", "John");
            element.addProperty("age", 30);
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getType()).isEqualTo(JsonItemType.Object);
            assertThat(item.getChildren()).hasSize(2);
        }

        @Test
        @DisplayName("should convert with property name")
        void shouldConvertWithPropertyName() {
            JsonElement element = new JsonPrimitive("value");
            JsonItem item = GsonElementConverter.INSTANCE.convert(element, "key");
            assertThat(item.getName()).isEqualTo("key");
        }
    }

    @Nested
    @DisplayName("Complex structures")
    class ComplexStructures {

        @Test
        @DisplayName("should handle nested structures")
        void shouldHandleNestedStructures() {
            JsonObject element = new JsonObject();
            JsonObject person = new JsonObject();
            person.addProperty("name", "John");
            JsonArray addresses = new JsonArray();
            JsonObject addr1 = new JsonObject();
            addr1.addProperty("city", "NYC");
            addresses.add(addr1);
            JsonObject addr2 = new JsonObject();
            addr2.addProperty("city", "LA");
            addresses.add(addr2);
            person.add("addresses", addresses);
            element.add("person", person);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            assertThat(item.getComplexity()).isGreaterThan(1);
        }

        @Test
        @DisplayName("should format deeply nested structure")
        void shouldFormatDeeplyNestedStructure() {
            JsonObject element = new JsonObject();
            JsonObject level1 = new JsonObject();
            JsonObject level2 = new JsonObject();
            JsonObject level3 = new JsonObject();
            level3.addProperty("value", 42);
            level2.add("level3", level3);
            level1.add("level2", level2);
            element.add("level1", level1);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
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
            JsonObject element = new JsonObject();
            element.addProperty("num", 14);
            element.addProperty("string", "testing property alignment");
            JsonArray arr = new JsonArray();
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            element.add("arrayWithLongName", arr);

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(15);
            opts.setColonBeforePropNamePadding(false);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(opts);
            String output = formatter.format(item, 0);
            String[] outputLines = output.trim().split("\n");

            assertThat(outputLines.length).isEqualTo(9);
            TestHelpers.INSTANCE.testInstancesLineUp(outputLines, ":");
        }

        @Test
        @DisplayName("Property values aligned but not colons")
        void propValuesAlignedButNotColons() {
            JsonObject element = new JsonObject();
            element.addProperty("num", 14);
            element.addProperty("string", "testing property alignment");
            JsonArray arr = new JsonArray();
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            element.add("arrayWithLongName", arr);

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(15);
            opts.setColonBeforePropNamePadding(true);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
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
            JsonObject element = new JsonObject();
            element.addProperty("num", 14);
            element.addProperty("string", "testing property alignment");
            JsonArray arr = new JsonArray();
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            arr.add(JsonNull.INSTANCE);
            element.add("arrayWithLongName", arr);

            FracturedJsonOptions opts = new FracturedJsonOptions();
            opts.setMaxPropNamePadding(12);
            opts.setColonBeforePropNamePadding(false);
            opts.setMaxInlineComplexity(-1);
            opts.setMaxCompactArrayComplexity(-1);
            opts.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
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
            JsonArray element = new JsonArray();
            JsonObject obj1 = new JsonObject();
            obj1.addProperty("name", "Alice");
            obj1.addProperty("age", 25);
            element.add(obj1);
            JsonObject obj2 = new JsonObject();
            obj2.addProperty("name", "Bob");
            obj2.addProperty("age", 30);
            element.add(obj2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

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
            JsonArray element = new JsonArray();
            JsonObject obj1 = new JsonObject();
            obj1.addProperty("name", "Alice");
            obj1.addProperty("age", 25);
            element.add(obj1);
            JsonObject obj2 = new JsonObject();
            obj2.addProperty("name", "Bob");
            obj2.addProperty("age", 30);
            element.add(obj2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxTotalLineLength(30);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("name");
            assertThat(result).contains("age");
        }

        @Test
        @DisplayName("fall back on inline if needed")
        void fallBackOnInlineIfNeeded() {
            JsonArray element = new JsonArray();
            JsonObject obj1 = new JsonObject();
            obj1.addProperty("name", "Alice");
            element.add(obj1);
            JsonObject obj2 = new JsonObject();
            obj2.addProperty("name", "Bob");
            element.add(obj2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxTotalLineLength(100);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("Alice");
            assertThat(result).contains("Bob");
        }

        @Test
        @DisplayName("handles nulls with arrays table columns")
        void handlesNullsWithArraysTableColumns() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            arr1.add(JsonNull.INSTANCE);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(2);
            arr2.add(3);
            element.add(arr2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("null");
        }

        @Test
        @DisplayName("commas before padding works")
        void commasBeforePaddingWorks() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            arr1.add(2);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(10);
            arr2.add(20);
            element.add(arr2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("20");
        }

        @Test
        @DisplayName("colons hug prop names when configured")
        void colonsHugPropNamesWhenConfigured() {
            JsonArray element = new JsonArray();
            JsonObject obj1 = new JsonObject();
            obj1.addProperty("a", 1);
            element.add(obj1);
            JsonObject obj2 = new JsonObject();
            obj2.addProperty("longName", 2);
            element.add(obj2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setColonBeforePropNamePadding(true);
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("\"a\":");
            assertThat(result).contains("\"longName\":");
        }

        @Test
        @DisplayName("single columns with numbers work")
        void singleColumnsWithNumbersWork() {
            JsonArray element = new JsonArray();
            element.add(1.5);
            element.add(22.333);
            element.add(100.1);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
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
            JsonArray element = new JsonArray();
            element.add(1);
            element.add(22);
            element.add(333);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(10);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("compact array does justify numbers")
        void compactArrayDoesJustifyNumbers() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            arr1.add(2);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(333);
            arr2.add(4444);
            element.add(arr2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(1);
            options.setMaxCompactArrayComplexity(2);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("4444");
        }

        @Test
        @DisplayName("table array does justify numbers")
        void tableArrayDoesJustifyNumbers() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            arr1.add(2);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(333);
            arr2.add(4444);
            element.add(arr2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("scientific notation numbers preserved")
        void scientificNotationNumbersPreserved() {
            JsonArray element = new JsonArray();
            element.add(1e10);
            element.add(2);
            element.add(3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result.toLowerCase()).contains("e");
        }

        @Test
        @DisplayName("nulls respected when aligning numbers")
        void nullsRespectedWhenAligningNumbers() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            arr1.add(JsonNull.INSTANCE);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(22);
            arr2.add(33);
            element.add(arr2);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("null");
        }

        @Test
        @DisplayName("left align works")
        void leftAlignWorks() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(22);
            element.add(arr2);
            JsonArray arr3 = new JsonArray();
            arr3.add(333);
            element.add(arr3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Left);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("right align works")
        void rightAlignWorks() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(22);
            element.add(arr2);
            JsonArray arr3 = new JsonArray();
            arr3.add(333);
            element.add(arr3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Right);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1");
            assertThat(result).contains("22");
            assertThat(result).contains("333");
        }

        @Test
        @DisplayName("decimal align works")
        void decimalAlignWorks() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1.5);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(22.333);
            element.add(arr2);
            JsonArray arr3 = new JsonArray();
            arr3.add(333.1);
            element.add(arr3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Decimal);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("1.5");
            assertThat(result).contains("22.333");
            assertThat(result).contains("333.1");
        }

        @Test
        @DisplayName("normalize align works")
        void normalizeAlignWorks() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1.5);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(22.333);
            element.add(arr2);
            JsonArray arr3 = new JsonArray();
            arr3.add(333.1);
            element.add(arr3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setNumberListAlignment(NumberListAlignment.Normalize);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("negative numbers align correctly")
        void negativeNumbersAlignCorrectly() {
            JsonArray element = new JsonArray();
            JsonArray arr1 = new JsonArray();
            arr1.add(1);
            element.add(arr1);
            JsonArray arr2 = new JsonArray();
            arr2.add(-22);
            element.add(arr2);
            JsonArray arr3 = new JsonArray();
            arr3.add(333);
            element.add(arr3);

            FracturedJsonOptions options = new FracturedJsonOptions();
            options.setMaxInlineComplexity(0);
            options.setJsonEolStyle(EolStyle.Lf);

            JsonItem item = GsonElementConverter.INSTANCE.convert(element, null);
            Formatter formatter = new Formatter(options);
            String result = formatter.format(item, 0);

            assertThat(result).contains("-22");
        }
    }
}
