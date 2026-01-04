package io.github.fracturedjson.core.formatting

/**
 * A place where strings are piled up sequentially to eventually make one big string.
 * Or maybe straight to a stream or whatever.
 *
 * All methods return the buffer instance to enable fluent method chaining.
 */
interface FormattingBuffer {
    /**
     * Add a single string to the buffer.
     */
    fun add(value: String): FormattingBuffer

    /**
     * Add multiple strings to the buffer.
     */
    fun add(vararg values: String): FormattingBuffer

    /**
     * Adds the requested number of spaces to the buffer.
     */
    fun spaces(count: Int): FormattingBuffer

    /**
     * Call this only when sending an end-of-line symbol to the buffer.
     * Doing so helps the buffer with extra post-processing, like trimming trailing whitespace.
     */
    fun endLine(eolString: String): FormattingBuffer

    /**
     * Call this to let the buffer finish up any work in progress.
     */
    fun flush(): FormattingBuffer
}
