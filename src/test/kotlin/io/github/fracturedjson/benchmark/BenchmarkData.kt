package io.github.fracturedjson.benchmark

import io.github.fracturedjson.core.FracturedJsonOptions
import org.openjdk.jmh.annotations.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * Shared benchmark state providing JSON test data of various sizes.
 *
 * Uses sample files from the project's `samples/` directory, categorized by size:
 * - Small (~11KB): google maps API compact response
 * - Medium (~64KB): GitHub events
 * - Large (~2MB): canada GeoJSON coordinates
 */
@State(Scope.Benchmark)
open class BenchmarkData {

    /** Small JSON (~11KB) - simple structure, few nested levels */
    lateinit var smallJson: String

    /** Medium JSON (~64KB) - moderate nesting with arrays of objects */
    lateinit var mediumJson: String

    /** Large JSON (~2MB) - deeply nested numeric arrays (GeoJSON) */
    lateinit var largeJson: String

    /** Default formatting options */
    lateinit var defaultOptions: FracturedJsonOptions

    /** Compact formatting options (shorter line length, less inlining) */
    lateinit var compactOptions: FracturedJsonOptions

    /** Wide formatting options (longer line length, more inlining) */
    lateinit var wideOptions: FracturedJsonOptions

    @Setup(Level.Trial)
    fun setup() {
        val samplesDir = findSamplesDir()

        smallJson = Files.readString(samplesDir.resolve("google_maps_api_compact_response.json"))
        mediumJson = Files.readString(samplesDir.resolve("github_events.json"))
        largeJson = Files.readString(samplesDir.resolve("canada.json"))

        defaultOptions = FracturedJsonOptions()
        compactOptions = FracturedJsonOptions(maxTotalLineLength = 40, maxInlineComplexity = 1)
        wideOptions = FracturedJsonOptions(maxTotalLineLength = 160, maxInlineComplexity = 4)
    }

    private fun findSamplesDir(): Path {
        val candidates = listOf(
            Path.of("samples"),
            Path.of("../samples"),
            Path.of(System.getProperty("user.dir"), "samples")
        )
        return candidates.firstOrNull { Files.isDirectory(it) }
            ?: throw IllegalStateException(
                "Cannot find 'samples/' directory. Run benchmarks from the project root."
            )
    }
}
