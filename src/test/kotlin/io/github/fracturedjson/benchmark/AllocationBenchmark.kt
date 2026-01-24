package io.github.fracturedjson.benchmark

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.jackson.JsonNodeConverter
import io.github.fracturedjson.parser.Parser
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.StringWriter
import java.util.concurrent.TimeUnit

/**
 * Memory allocation benchmarks.
 *
 * Designed to be run with the GC profiler to measure:
 * - Allocation rate (bytes/op)
 * - GC pressure (gc.alloc.rate, gc.count, gc.time)
 *
 * These benchmarks highlight the intermediate object creation overhead
 * that Phase 12 optimizations aim to reduce.
 *
 * Usage:
 *   ./gradlew benchmark -Dbench.include=AllocationBenchmark -Dbench.profiler=gc
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgs = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"])
open class AllocationBenchmark {

    private lateinit var data: BenchmarkData
    private lateinit var parser: Parser
    private lateinit var formatter: Formatter
    private lateinit var objectMapper: ObjectMapper

    // Pre-parsed data
    private lateinit var mediumItems: List<JsonItem>
    private lateinit var largeItems: List<JsonItem>
    private lateinit var jacksonNodeLarge: JsonNode

    @Setup(Level.Trial)
    fun setup() {
        data = BenchmarkData()
        data.setup()

        parser = Parser()
        formatter = Formatter(data.defaultOptions)
        objectMapper = ObjectMapper()

        mediumItems = parser.parse(data.mediumJson)
        largeItems = parser.parse(data.largeJson)
        jacksonNodeLarge = objectMapper.readTree(data.largeJson)
    }

    // ─────────────────────────────────────────────
    // StringBuilder vs Writer output comparison
    // ─────────────────────────────────────────────

    @Benchmark
    fun formatToString_medium(bh: Blackhole) {
        bh.consume(formatter.format(mediumItems))
    }

    @Benchmark
    fun formatToWriter_medium(bh: Blackhole) {
        val writer = StringWriter(data.mediumJson.length * 2)
        formatter.format(mediumItems, 0, writer)
        bh.consume(writer.toString())
    }

    @Benchmark
    fun formatToString_large(bh: Blackhole) {
        bh.consume(formatter.format(largeItems))
    }

    @Benchmark
    fun formatToWriter_large(bh: Blackhole) {
        val writer = StringWriter(data.largeJson.length * 2)
        formatter.format(largeItems, 0, writer)
        bh.consume(writer.toString())
    }

    // ─────────────────────────────────────────────
    // Full pipeline allocation: parse → convert → format
    // Baseline for streaming optimization comparison
    // ─────────────────────────────────────────────

    @Benchmark
    fun fullPipeline_CoreParser_large(bh: Blackhole) {
        val items = parser.parse(data.largeJson)
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun fullPipeline_Jackson_large(bh: Blackhole) {
        val node = objectMapper.readTree(data.largeJson)
        val items = listOf(JsonNodeConverter.convert(node))
        bh.consume(formatter.format(items))
    }

    // ─────────────────────────────────────────────
    // Repeated formatting (measures Formatter reuse overhead)
    // ─────────────────────────────────────────────

    @Benchmark
    @OperationsPerInvocation(10)
    fun repeatedFormat_reuse(bh: Blackhole) {
        repeat(10) {
            bh.consume(formatter.format(mediumItems))
        }
    }

    @Benchmark
    @OperationsPerInvocation(10)
    fun repeatedFormat_newInstance(bh: Blackhole) {
        repeat(10) {
            val f = Formatter(data.defaultOptions)
            bh.consume(f.format(mediumItems))
        }
    }

    // ─────────────────────────────────────────────
    // Conversion-only allocation (DOM → JsonItem)
    // ─────────────────────────────────────────────

    @Benchmark
    fun conversionAlloc_Jackson_large(bh: Blackhole) {
        bh.consume(JsonNodeConverter.convert(jacksonNodeLarge))
    }
}
