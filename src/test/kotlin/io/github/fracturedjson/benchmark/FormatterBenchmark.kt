package io.github.fracturedjson.benchmark

import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.parser.Parser
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Core formatting pipeline benchmarks.
 *
 * Measures the performance of:
 * 1. Parse-only (JSON string → JsonItem tree)
 * 2. Format-only (JsonItem tree → formatted string)
 * 3. End-to-end (JSON string → formatted string)
 * 4. Minify (JsonItem tree → compact string)
 *
 * Each operation is tested with small (~11KB), medium (~64KB), and large (~2MB) inputs.
 */
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class FormatterBenchmark {

    private lateinit var data: BenchmarkData

    // Pre-parsed JsonItem trees (for format-only benchmarks)
    private lateinit var smallItems: List<JsonItem>
    private lateinit var mediumItems: List<JsonItem>
    private lateinit var largeItems: List<JsonItem>

    private lateinit var parser: Parser
    private lateinit var formatter: Formatter
    private lateinit var compactFormatter: Formatter
    private lateinit var wideFormatter: Formatter

    @Setup(Level.Trial)
    fun setup() {
        data = BenchmarkData()
        data.setup()

        parser = Parser()
        formatter = Formatter(data.defaultOptions)
        compactFormatter = Formatter(data.compactOptions)
        wideFormatter = Formatter(data.wideOptions)

        // Pre-parse for format-only benchmarks
        smallItems = parser.parse(data.smallJson)
        mediumItems = parser.parse(data.mediumJson)
        largeItems = parser.parse(data.largeJson)
    }

    // ─────────────────────────────────────────────
    // Parse-only benchmarks (JSON string → JsonItem)
    // ─────────────────────────────────────────────

    @Benchmark
    fun parseSmall(bh: Blackhole) {
        bh.consume(parser.parse(data.smallJson))
    }

    @Benchmark
    fun parseMedium(bh: Blackhole) {
        bh.consume(parser.parse(data.mediumJson))
    }

    @Benchmark
    fun parseLarge(bh: Blackhole) {
        bh.consume(parser.parse(data.largeJson))
    }

    // ─────────────────────────────────────────────
    // Format-only benchmarks (JsonItem → String)
    // ─────────────────────────────────────────────

    @Benchmark
    fun formatSmall(bh: Blackhole) {
        bh.consume(formatter.format(smallItems))
    }

    @Benchmark
    fun formatMedium(bh: Blackhole) {
        bh.consume(formatter.format(mediumItems))
    }

    @Benchmark
    fun formatLarge(bh: Blackhole) {
        bh.consume(formatter.format(largeItems))
    }

    // ─────────────────────────────────────────────
    // End-to-end benchmarks (Parse + Format)
    // ─────────────────────────────────────────────

    @Benchmark
    fun endToEndSmall(bh: Blackhole) {
        val items = parser.parse(data.smallJson)
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEndMedium(bh: Blackhole) {
        val items = parser.parse(data.mediumJson)
        bh.consume(formatter.format(items))
    }

    @Benchmark
    fun endToEndLarge(bh: Blackhole) {
        val items = parser.parse(data.largeJson)
        bh.consume(formatter.format(items))
    }

    // ─────────────────────────────────────────────
    // Minify benchmarks (JsonItem → compact string)
    // ─────────────────────────────────────────────

    @Benchmark
    fun minifySmall(bh: Blackhole) {
        bh.consume(formatter.minify(smallItems))
    }

    @Benchmark
    fun minifyMedium(bh: Blackhole) {
        bh.consume(formatter.minify(mediumItems))
    }

    @Benchmark
    fun minifyLarge(bh: Blackhole) {
        bh.consume(formatter.minify(largeItems))
    }

    // ─────────────────────────────────────────────
    // Options variation benchmarks
    // ─────────────────────────────────────────────

    @Benchmark
    fun formatMediumCompact(bh: Blackhole) {
        bh.consume(compactFormatter.format(mediumItems))
    }

    @Benchmark
    fun formatMediumWide(bh: Blackhole) {
        bh.consume(wideFormatter.format(mediumItems))
    }
}
