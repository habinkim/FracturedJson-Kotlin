package io.github.fracturedjson.core

/**
 * Specifies what sort of line endings to use.
 */
enum class EolStyle {
    /**
     * The native environment's line endings will be used.
     */
    Default,

    /**
     * Carriage Return, followed by a line feed. Windows-style.
     */
    Crlf,

    /**
     * Just a line feed. Unix-style (including Mac).
     */
    Lf
}
