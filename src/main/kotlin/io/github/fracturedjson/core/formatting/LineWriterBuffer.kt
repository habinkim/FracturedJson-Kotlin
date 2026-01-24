package io.github.fracturedjson.core.formatting

import java.io.Writer

/**
 * A FormattingBuffer for writing to a Writer (which will often be backed by a file or network stream).
 *
 * Internally it composes each individual line before pushing those into the writer.
 * This allows for trailing whitespace trimming before each line is written.
 *
 * @param writer The Writer to which the formatted content will be written.
 */
class LineWriterBuffer(private val writer: Writer) : FormattingBuffer {
    private val lineBuffer = StringBuilder()

    override fun add(value: String): FormattingBuffer {
        lineBuffer.append(value)
        return this
    }

    override fun add(vararg values: String): FormattingBuffer {
        for (value in values) {
            lineBuffer.append(value)
        }
        return this
    }

    override fun addChar(ch: Char): FormattingBuffer {
        lineBuffer.append(ch)
        return this
    }

    override fun addQuoted(value: String): FormattingBuffer {
        lineBuffer.append('"').append(value).append('"')
        return this
    }

    override fun spaces(count: Int): FormattingBuffer {
        if (count <= 0) return this
        if (count <= SPACE_CACHE.size) {
            lineBuffer.append(SPACE_CACHE, 0, count)
        } else {
            repeat(count) { lineBuffer.append(' ') }
        }
        return this
    }

    companion object {
        private val SPACE_CACHE = CharArray(64) { ' ' }
    }

    override fun endLine(eolString: String): FormattingBuffer {
        addLineToWriter(eolString)
        return this
    }

    override fun flush(): FormattingBuffer {
        addLineToWriter("")
        writer.flush()
        return this
    }

    /**
     * Trims trailing whitespace, appends the EOL string, and writes the line to the writer.
     */
    private fun addLineToWriter(eolString: String) {
        if (lineBuffer.isEmpty() && eolString.isEmpty()) {
            return
        }

        // Figure out where the end of the line's non-whitespace characters is
        var newLength = lineBuffer.length
        while (newLength > 0) {
            val ch = lineBuffer[newLength - 1]
            if (ch != ' ' && ch != '\t') break
            newLength--
        }
        lineBuffer.setLength(newLength)
        lineBuffer.append(eolString)

        // Write the line to the Writer
        writer.write(lineBuffer.toString())
        lineBuffer.clear()
    }
}
