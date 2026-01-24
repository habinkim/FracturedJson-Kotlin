package io.github.fracturedjson.core.formatting

/**
 * A do-nothing FormattingBuffer implementation.
 *
 * This follows the Null Object pattern to avoid null reference checks.
 * Useful for scenarios where a buffer is required but no actual output is needed.
 */
object NullBuffer : FormattingBuffer {
    override fun add(value: String): FormattingBuffer = this

    override fun add(vararg values: String): FormattingBuffer = this

    override fun addChar(ch: Char): FormattingBuffer = this

    override fun addQuoted(value: String): FormattingBuffer = this

    override fun spaces(count: Int): FormattingBuffer = this

    override fun endLine(eolString: String): FormattingBuffer = this

    override fun flush(): FormattingBuffer = this
}
