package io.github.fracturedjson.core

/**
 * Marks a function or class as an archived baseline implementation preserved for benchmark comparison.
 *
 * Functions annotated with [ForBenchmark] represent the original (pre-optimization) logic
 * that is kept alongside optimized versions. This enables A/B performance comparison
 * between the baseline and improved implementations via JMH benchmarks.
 *
 * These annotated members are NOT part of the public API and should not be used
 * in production code. They exist solely for performance regression testing.
 *
 * @property version The version tag for this baseline snapshot (e.g., "v0.7.0-baseline")
 * @property description Brief description of what this baseline represents
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ForBenchmark(
    val version: String = "v0.7.0-baseline",
    val description: String = ""
)
