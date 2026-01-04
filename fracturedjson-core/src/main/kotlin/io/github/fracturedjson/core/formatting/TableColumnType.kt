package io.github.fracturedjson.core.formatting

/**
 * Represents the type of data found in a table column.
 */
internal enum class TableColumnType {
    /**
     * Initial placeholder before the column type is determined.
     */
    Unknown,

    /**
     * Non-container and non-number values (strings, booleans, null).
     */
    Simple,

    /**
     * Column containing exclusively numeric or null entries.
     */
    Number,

    /**
     * Column containing arrays.
     */
    Array,

    /**
     * Column containing objects.
     */
    Object,

    /**
     * Column containing heterogeneous types.
     */
    Mixed
}
