package io.github.fracturedjson.benchmark

import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonParser
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.fastjson2.FastJson2Converter
import io.github.fracturedjson.gson.GsonElementConverter
import io.github.fracturedjson.jackson.JsonNodeConverter
import io.github.fracturedjson.kotlinx.JsonElementConverter
import io.github.fracturedjson.parser.Parser
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import com.google.gson.JsonElement as GsonJsonElement
import kotlinx.serialization.json.JsonElement as KotlinxJsonElement

/**
 * Adapter comparison benchmarks.
 *
 * Compares the end-to-end performance of each JSON library adapter:
 * 1. Core Parser (no external dependency)
 * 2. Jackson (ObjectMapper + JsonNodeConverter)
 * 3. Gson (JsonParser + GsonElementConverter)
 * 4. kotlinx.serialization (Json + JsonElementConverter)
 * 5. Fastjson2 (JSON + FastJson2Converter)
 *
 * Each benchmark measures the full pipeline: library parse → convert to JsonItem → format.
 */
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class AdapterBenchmark {

    private lateinit var data: BenchmarkData
    private lateinit var formatter: Formatter

    // Library-specific pre-parsed objects (for convert-only benchmarks)
    private lateinit var jacksonNodeMedium: JsonNode
    private lateinit var gsonElementMedium: GsonJsonElement
    private lateinit var kotlinxElementMedium: KotlinxJsonElement

    private lateinit var objectMapper: ObjectMapper
    private lateinit var parser: Parser

    @Setup(Level.Trial)
    fun setup() {
        data = BenchmarkData()
        data.setup()

        formatter = Formatter(data.defaultOptions)
        objectMapper = ObjectMapper()
        parser = Parser()

        // Pre-parse with each library for convert-only benchmarks
        jacksonNodeMedium = objectMapper.readTree(data.mediumJson)
        gsonElementMedium = JsonParser.parseString(data.mediumJson)
        kotlinxElementMedium = Json.parseToJsonElement(data.mediumJson)
    }

    // ─────────────────────────────────────────────
    // End-to-end: parse + convert + format (medium JSON)
    // ─────────────────────────────────────────────

    @Benchmark
    fun endToEnd_CoreParser(bh: Blackhole) {
        val items = parser.parse(data.mediumJson)
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEnd_Jackson(bh: Blackhole) {
        val node = objectMapper.readTree(data.mediumJson)
        val items = listOf(JsonNodeConverter.convert(node))
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEnd_Gson(bh: Blackhole) {
        val element = JsonParser.parseString(data.mediumJson)
        val items = listOf(GsonElementConverter.convert(element))
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEnd_Kotlinx(bh: Blackhole) {
        val element = Json.parseToJsonElement(data.mediumJson)
        val items = listOf(JsonElementConverter.convert(element))
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEnd_Fastjson2(bh: Blackhole) {
        val obj = JSON.parseObject(data.mediumJson)
        val items = listOf(FastJson2Converter.convert(obj))
        bh.consume(formatter.format(items))
    }

    // ─────────────────────────────────────────────
    // Convert-only: library DOM → JsonItem (medium JSON)
    // Isolates the intermediate conversion overhead
    // ─────────────────────────────────────────────

    @Benchmark
    fun convertOnly_Jackson(bh: Blackhole) {
        bh.consume(JsonNodeConverter.convert(jacksonNodeMedium))
    }

    @Benchmark
    fun convertOnly_Gson(bh: Blackhole) {
        bh.consume(GsonElementConverter.convert(gsonElementMedium))
    }

    @Benchmark
    fun convertOnly_Kotlinx(bh: Blackhole) {
        bh.consume(JsonElementConverter.convert(kotlinxElementMedium))
    }

    @Benchmark
    fun convertOnly_Fastjson2(bh: Blackhole) {
        val obj = JSON.parseObject(data.mediumJson)
        bh.consume(FastJson2Converter.convert(obj))
    }

    // ─────────────────────────────────────────────
    // Parse-only: JSON string → library DOM (medium JSON)
    // Baseline for each library's parsing speed
    // ─────────────────────────────────────────────

    @Benchmark
    fun parseOnly_CoreParser(bh: Blackhole) {
        bh.consume(parser.parse(data.mediumJson))
    }

    @Benchmark
    fun parseOnly_Jackson(bh: Blackhole) {
        bh.consume(objectMapper.readTree(data.mediumJson))
    }

    @Benchmark
    fun parseOnly_Gson(bh: Blackhole) {
        bh.consume(JsonParser.parseString(data.mediumJson))
    }

    @Benchmark
    fun parseOnly_Kotlinx(bh: Blackhole) {
        bh.consume(Json.parseToJsonElement(data.mediumJson))
    }

    @Benchmark
    fun parseOnly_Fastjson2(bh: Blackhole) {
        bh.consume(JSON.parseObject(data.mediumJson))
    }
}
