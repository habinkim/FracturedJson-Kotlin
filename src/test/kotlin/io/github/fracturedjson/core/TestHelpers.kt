package io.github.fracturedjson.core

import org.assertj.core.api.Assertions.assertThat

/**
 * Utility functions to help with unit tests - ported from C# TestHelpers.cs
 */
object TestHelpers {
    /**
     * Tests that all instances of a substring in the given lines appear at the same index position.
     * This is used to verify that elements are properly aligned in the formatted output.
     */
    fun testInstancesLineUp(lines: Array<String>, substring: String) {
        val indices = lines.map { it.indexOf(substring) }
        val indexCount = indices.filter { it >= 0 }.distinct().count()
        assertThat(indexCount).isEqualTo(1)
    }

    /**
     * Tests that all instances of a substring in the given lines appear at the same index position.
     */
    fun testInstancesLineUp(lines: List<String>, substring: String) {
        testInstancesLineUp(lines.toTypedArray(), substring)
    }
}
