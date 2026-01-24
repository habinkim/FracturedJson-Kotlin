package io.github.fracturedjson.benchmark

import io.github.fracturedjson.core.Formatter
import io.github.fracturedjson.core.JsonItem
import io.github.fracturedjson.parser.Parser
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Baseline vs Optimized comparison benchmarks.
 *
 * This benchmark class is specifically designed for A/B comparison between
 * the archived baseline implementation (marked with @ForBenchmark) and
 * any new optimized implementations.
 *
 * As optimizations are applied in Phase 12, new benchmark methods will be
 * added here to compare against the baseline.
 *
 * Current comparisons:
 * - formatBaseline vs format (will diverge after optimization)
 * - minifyBaseline vs minify (will diverge after optimization)
 */
@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class BaselineBenchmark {

    private lateinit var data: BenchmarkData
    private lateinit var parser: Parser
    private lateinit var formatter: Formatter

    private lateinit var smallItems: List<JsonItem>
    private lateinit var mediumItems: List<JsonItem>
    private lateinit var largeItems: List<JsonItem>

    @Setup(Level.Trial)
    fun setup() {
        data = BenchmarkData()
        data.setup()

        parser = Parser()
        formatter = Formatter(data.defaultOptions)

        smallItems = parser.parse(data.smallJson)
        mediumItems = parser.parse(data.mediumJson)
        largeItems = parser.parse(data.largeJson)
    }

    // ─────────────────────────────────────────────
    // Baseline format (v0.7.0 - before optimization)
    // ─────────────────────────────────────────────

    @Benchmark
    fun baseline_formatSmall(bh: Blackhole) {
        bh.consume(formatter.formatBaseline(smallItems))
    }

    @Benchmark
    fun baseline_formatMedium(bh: Blackhole) {
        bh.consume(formatter.formatBaseline(mediumItems))
    }

    @Benchmark
    fun baseline_formatLarge(bh: Blackhole) {
        bh.consume(formatter.formatBaseline(largeItems))
    }

    @Benchmark
    fun baseline_minifyMedium(bh: Blackhole) {
        bh.consume(formatter.minifyBaseline(mediumItems))
    }

    // ─────────────────────────────────────────────
    // Current format (will be optimized in Phase 12)
    // Initially identical to baseline; will diverge after optimization.
    // ─────────────────────────────────────────────

    @Benchmark
    fun current_formatSmall(bh: Blackhole) {
        bh.consume(formatter.format(smallItems))
    }

    @Benchmark
    fun current_formatMedium(bh: Blackhole) {
        bh.consume(formatter.format(mediumItems))
    }

    @Benchmark
    fun current_formatLarge(bh: Blackhole) {
        bh.consume(formatter.format(largeItems))
    }

    @Benchmark
    fun current_minifyMedium(bh: Blackhole) {
        bh.consume(formatter.minify(mediumItems))
    }
}
