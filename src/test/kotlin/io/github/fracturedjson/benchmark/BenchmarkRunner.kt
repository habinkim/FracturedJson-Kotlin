package io.github.fracturedjson.benchmark

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.nio.file.Files
import java.nio.file.Path

/**
 * JUnit entry point for running JMH benchmarks.
 *
 * This class acts as a bridge between JUnit test infrastructure and JMH,
 * allowing benchmarks to be executed via `./gradlew benchmark` while keeping
 * all benchmark code in the test source set.
 *
 * Usage:
 *   ./gradlew benchmark                          # Run all benchmarks (full)
 *   ./gradlew benchmark -Pbench.include=Formatter  # Run only Formatter benchmarks
 *   ./gradlew benchmark -Pbench.quick=true        # Quick mode (fewer iterations)
 *   ./gradlew benchmark -Pbench.profiler=gc       # With GC profiler
 */
@Tag("benchmark")
class BenchmarkRunner {

    @Test
    fun runBenchmarks() {
        val resultsDir = Path.of("build/reports/jmh")
        Files.createDirectories(resultsDir)

        val includePattern = System.getProperty("bench.include", ".*Benchmark.*")
        val isQuick = System.getProperty("bench.quick", "false").toBoolean()
        val profiler = System.getProperty("bench.profiler", "").ifBlank { null }

        val opts = OptionsBuilder()
            .include(includePattern)
            .warmupIterations(if (isQuick) 1 else 3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(if (isQuick) 2 else 5)
            .measurementTime(TimeValue.seconds(2))
            .forks(if (isQuick) 0 else 1)
            .resultFormat(ResultFormatType.JSON)
            .result(resultsDir.resolve("results.json").toString())

        if (!isQuick) {
            opts.jvmArgs("-Xms2g", "-Xmx2g", "-XX:+UseG1GC")
        }

        if (profiler != null) {
            opts.addProfiler(profiler)
        }

        val results = Runner(opts.build()).run()

        // Print summary
        println()
        println("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("┃  JMH BENCHMARK RESULTS                                                        ┃")
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        results.forEach { result ->
            val name = result.primaryResult.label.substringAfterLast('.')
            val score = "%.3f".format(result.primaryResult.score)
            val unit = result.primaryResult.scoreUnit
            val error = "%.3f".format(result.primaryResult.scoreError)
            println("┃  ${name.padEnd(35)} $score ± $error $unit".padEnd(78) + "┃")
        }
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        println("┃  Results: build/reports/jmh/results.json".padEnd(78) + "┃")
        println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }
}
