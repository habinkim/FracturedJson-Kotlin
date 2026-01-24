package io.github.fracturedjson.core.formatting

/**
 * A FormattingBuffer that accumulates strings in a StringBuilder.
 *
 * This is the most common buffer type, used when you want to get the
 * formatted output as a single string.
 */
class StringBuilderBuffer(initialCapacity: Int = 16) : FormattingBuffer {
    private val buffer = StringBuilder(initialCapacity)

    override fun add(value: String): FormattingBuffer {
        buffer.append(value)
        return this
    }

    override fun add(vararg values: String): FormattingBuffer {
        for (value in values) {
            buffer.append(value)
        }
        return this
    }

    override fun spaces(count: Int): FormattingBuffer {
        repeat(count) {
            buffer.append(' ')
        }
        return this
    }

    override fun endLine(eolString: String): FormattingBuffer {
        trimTrailingWhitespace()
        buffer.append(eolString)
        return this
    }

    override fun flush(): FormattingBuffer {
        trimTrailingWhitespace()
        return this
    }

    /**
     * Returns the accumulated string content.
     */
    fun asString(): String = buffer.toString()

    /**
     * Removes trailing spaces and tabs from the buffer.
     */
    private fun trimTrailingWhitespace() {
        var newLength = buffer.length
        while (newLength > 0) {
            val ch = buffer[newLength - 1]
            if (ch != ' ' && ch != '\t') break
            newLength--
        }
        buffer.setLength(newLength)
    }
}
