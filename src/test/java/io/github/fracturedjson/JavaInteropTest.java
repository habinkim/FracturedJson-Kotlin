package io.github.fracturedjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;

import io.github.fracturedjson.core.FracturedJsonOptions;
import io.github.fracturedjson.jackson.JacksonSupport;
import io.github.fracturedjson.jackson.JacksonExtensions;
import io.github.fracturedjson.gson.GsonSupport;
import io.github.fracturedjson.gson.GsonExtensions;
import io.github.fracturedjson.kotlinx.KotlinxSupport;
import io.github.fracturedjson.kotlinx.KotlinxExtensions;
import io.github.fracturedjson.fastjson2.Fastjson2Support;
import io.github.fracturedjson.fastjson2.Fastjson2Extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Java interoperability with FracturedJson Kotlin library.
 *
 * This test class demonstrates two ways to use the library from Java:
 * 1. Using *Support classes (recommended for Java) - e.g., JacksonSupport.format(node)
 * 2. Using *Extensions classes (via @file:JvmName) - e.g., JacksonExtensions.toFracturedJson(node)
 */
@DisplayName("Java Interoperability Tests")
class JavaInteropTest {

    private static final String SAMPLE_JSON = "{\"name\":\"test\",\"values\":[1,2,3],\"nested\":{\"key\":\"value\"}}";

    // ==================== Jackson Tests ====================

    @Test
    @DisplayName("Jackson: Format JsonNode using JacksonSupport")
    void testJacksonSupportFormat() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(SAMPLE_JSON);

        // Using JacksonSupport (recommended for Java)
        String formatted = JacksonSupport.format(node);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
        assertTrue(formatted.contains("\"test\""));
    }

    @Test
    @DisplayName("Jackson: Format JsonNode with custom options using JacksonSupport")
    void testJacksonSupportFormatWithOptions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(SAMPLE_JSON);

        FracturedJsonOptions options = new FracturedJsonOptions();
        options.setMaxTotalLineLength(40);

        String formatted = JacksonSupport.format(node, options);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\n")); // Should have line breaks with shorter line length
    }

    @Test
    @DisplayName("Jackson: Format using JacksonExtensions (@file:JvmName)")
    void testJacksonExtensions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(SAMPLE_JSON);

        // Using JacksonExtensions (alternative way)
        String formatted = JacksonExtensions.toFracturedJson(node, new FracturedJsonOptions());

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    @Test
    @DisplayName("Jackson: Minify JsonNode")
    void testJacksonMinify() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String prettyJson = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";
        JsonNode node = mapper.readTree(prettyJson);

        String minified = JacksonSupport.minify(node);

        assertNotNull(minified);
        assertFalse(minified.contains("\n"));
        assertTrue(minified.contains("\"name\":\"test\""));
    }

    @Test
    @DisplayName("Jackson: Format JSON string directly")
    void testJacksonFormatJsonString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String formatted = JacksonSupport.formatJson(mapper, SAMPLE_JSON);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    // ==================== Gson Tests ====================

    @Test
    @DisplayName("Gson: Format JsonElement using GsonSupport")
    void testGsonSupportFormat() {
        JsonElement element = JsonParser.parseString(SAMPLE_JSON);

        String formatted = GsonSupport.format(element);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
        assertTrue(formatted.contains("\"test\""));
    }

    @Test
    @DisplayName("Gson: Format with custom options using GsonSupport")
    void testGsonSupportFormatWithOptions() {
        JsonElement element = JsonParser.parseString(SAMPLE_JSON);

        FracturedJsonOptions options = new FracturedJsonOptions();
        options.setMaxTotalLineLength(40);

        String formatted = GsonSupport.format(element, options);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    @DisplayName("Gson: Format using GsonExtensions (@file:JvmName)")
    void testGsonExtensions() {
        JsonElement element = JsonParser.parseString(SAMPLE_JSON);

        String formatted = GsonExtensions.toFracturedJson(element, new FracturedJsonOptions());

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    @Test
    @DisplayName("Gson: Minify JsonElement")
    void testGsonMinify() {
        String prettyJson = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";
        JsonElement element = JsonParser.parseString(prettyJson);

        String minified = GsonSupport.minify(element);

        assertNotNull(minified);
        assertFalse(minified.contains("\n"));
    }

    @Test
    @DisplayName("Gson: Format JSON string directly")
    void testGsonFormatJsonString() {
        String formatted = GsonSupport.formatJson(SAMPLE_JSON);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    // ==================== Kotlinx Serialization Tests ====================

    @Test
    @DisplayName("Kotlinx: Format JSON string using KotlinxSupport")
    void testKotlinxSupportFormat() {
        String formatted = KotlinxSupport.formatJson(SAMPLE_JSON);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
        assertTrue(formatted.contains("\"test\""));
    }

    @Test
    @DisplayName("Kotlinx: Format with custom options using KotlinxSupport")
    void testKotlinxSupportFormatWithOptions() {
        FracturedJsonOptions options = new FracturedJsonOptions();
        options.setMaxTotalLineLength(40);

        String formatted = KotlinxSupport.formatJson(SAMPLE_JSON, options);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    @DisplayName("Kotlinx: Minify JSON string")
    void testKotlinxMinify() {
        String prettyJson = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";

        String minified = KotlinxSupport.minifyJson(prettyJson);

        assertNotNull(minified);
        assertFalse(minified.contains("\n"));
    }

    // ==================== Fastjson2 Tests ====================

    @Test
    @DisplayName("Fastjson2: Format JSONObject using Fastjson2Support")
    void testFastjson2SupportFormatObject() {
        JSONObject obj = JSON.parseObject(SAMPLE_JSON);

        String formatted = Fastjson2Support.format(obj);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
        assertTrue(formatted.contains("\"test\""));
    }

    @Test
    @DisplayName("Fastjson2: Format JSONArray using Fastjson2Support")
    void testFastjson2SupportFormatArray() {
        JSONArray array = JSON.parseArray("[1, 2, 3, {\"key\": \"value\"}]");

        String formatted = Fastjson2Support.format(array);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"key\""));
    }

    @Test
    @DisplayName("Fastjson2: Format with custom options using Fastjson2Support")
    void testFastjson2SupportFormatWithOptions() {
        JSONObject obj = JSON.parseObject(SAMPLE_JSON);

        FracturedJsonOptions options = new FracturedJsonOptions();
        options.setMaxTotalLineLength(40);

        String formatted = Fastjson2Support.format(obj, options);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    @DisplayName("Fastjson2: Format using Fastjson2Extensions (@file:JvmName)")
    void testFastjson2Extensions() {
        JSONObject obj = JSON.parseObject(SAMPLE_JSON);

        String formatted = Fastjson2Extensions.toFracturedJson(obj, new FracturedJsonOptions());

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    @Test
    @DisplayName("Fastjson2: Minify JSONObject")
    void testFastjson2Minify() {
        String prettyJson = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";
        JSONObject obj = JSON.parseObject(prettyJson);

        String minified = Fastjson2Support.minify(obj);

        assertNotNull(minified);
        assertFalse(minified.contains("\n"));
    }

    @Test
    @DisplayName("Fastjson2: Format JSON string directly")
    void testFastjson2FormatJsonString() {
        String formatted = Fastjson2Support.formatJson(SAMPLE_JSON);

        assertNotNull(formatted);
        assertTrue(formatted.contains("\"name\""));
    }

    // ==================== Cross-library Consistency Tests ====================

    @Test
    @DisplayName("All libraries produce consistent output for same input")
    void testCrossLibraryConsistency() throws Exception {
        FracturedJsonOptions options = new FracturedJsonOptions();

        // Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jacksonNode = mapper.readTree(SAMPLE_JSON);
        String jacksonOutput = JacksonSupport.format(jacksonNode, options);

        // Gson
        JsonElement gsonElement = JsonParser.parseString(SAMPLE_JSON);
        String gsonOutput = GsonSupport.format(gsonElement, options);

        // Kotlinx
        String kotlinxOutput = KotlinxSupport.formatJson(SAMPLE_JSON, options);

        // Fastjson2
        JSONObject fastjsonObj = JSON.parseObject(SAMPLE_JSON);
        String fastjsonOutput = Fastjson2Support.format(fastjsonObj, options);

        // All outputs should be identical (same formatting for same content)
        assertEquals(jacksonOutput, gsonOutput, "Jackson and Gson outputs should match");
        assertEquals(gsonOutput, kotlinxOutput, "Gson and Kotlinx outputs should match");
        assertEquals(kotlinxOutput, fastjsonOutput, "Kotlinx and Fastjson2 outputs should match");
    }
}
