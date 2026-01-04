package io.github.fracturedjson.parser.tokenizing

import io.github.fracturedjson.core.FracturedJsonException
import java.io.File
import java.io.Reader

/**
 * Scans JSON text and produces a sequence of tokens.
 *
 * Supports standard JSON syntax plus comments (// and /* */) and blank line preservation.
 */
object TokenScanner {
    /**
     * Scans a file and returns a sequence of tokens.
     */
    fun scan(file: File): Sequence<JsonToken> = sequence {
        file.reader().use { reader ->
            yieldAll(scan(reader.asSequence()))
        }
    }

    /**
     * Scans a string and returns a sequence of tokens.
     */
    fun scan(input: String): Sequence<JsonToken> = scan(input.asSequence())

    /**
     * Scans a reader and returns a sequence of tokens.
     */
    fun scan(reader: Reader): Sequence<JsonToken> = scan(reader.asSequence())

    /**
     * Scans a character sequence and returns a sequence of tokens.
     */
    fun scan(input: Sequence<Char>): Sequence<JsonToken> = sequence {
        val state = ScannerState()
        val iterator = input.iterator()
        var lookahead: Char? = null

        while (lookahead != null || iterator.hasNext()) {
            val ch = lookahead ?: iterator.next()
            lookahead = null

            when {
                ch == '\n' -> {
                    // Check for blank line
                    if (!state.nonWhitespaceSinceLastNewline) {
                        state.setTokenStart()
                        yield(state.makeToken(TokenType.BlankLine, ""))
                    }
                    state.newLine()
                }
                ch == '\r' -> {
                    // Handle \r\n or standalone \r
                    if (iterator.hasNext()) {
                        val next = iterator.next()
                        if (next != '\n') {
                            lookahead = next
                        }
                    }
                    if (!state.nonWhitespaceSinceLastNewline) {
                        state.setTokenStart()
                        yield(state.makeToken(TokenType.BlankLine, ""))
                    }
                    state.newLine()
                }
                ch.isWhitespace() -> {
                    state.advance(isWhitespace = true)
                }
                ch == '{' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.BeginObject))
                }
                ch == '}' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.EndObject))
                }
                ch == '[' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.BeginArray))
                }
                ch == ']' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.EndArray))
                }
                ch == ':' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.Colon))
                }
                ch == ',' -> {
                    state.setTokenStart()
                    yield(processSingleChar(state, ch, TokenType.Comma))
                }
                ch == '"' -> {
                    state.setTokenStart()
                    yield(processString(state, iterator))
                }
                ch == '/' -> {
                    state.setTokenStart()
                    yield(processComment(state, iterator))
                }
                ch == 't' -> {
                    state.setTokenStart()
                    yield(processKeyword(state, iterator, "true", TokenType.True))
                }
                ch == 'f' -> {
                    state.setTokenStart()
                    yield(processKeyword(state, iterator, "false", TokenType.False))
                }
                ch == 'n' -> {
                    state.setTokenStart()
                    yield(processKeyword(state, iterator, "null", TokenType.Null))
                }
                ch == '-' || ch.isDigit() -> {
                    state.setTokenStart()
                    val result = processNumber(state, ch, iterator)
                    yield(result.first)
                    lookahead = result.second
                }
                else -> {
                    state.setTokenStart()
                    state.throwError("Unexpected character: '$ch'")
                }
            }
        }
    }

    private fun processSingleChar(state: ScannerState, ch: Char, type: TokenType): JsonToken {
        state.advance(isWhitespace = false)
        return state.makeToken(type, ch.toString())
    }

    private fun processKeyword(
        state: ScannerState,
        iterator: Iterator<Char>,
        keyword: String,
        type: TokenType
    ): JsonToken {
        state.buffer.append(keyword[0])
        state.advance(isWhitespace = false)

        for (i in 1 until keyword.length) {
            if (!iterator.hasNext()) {
                state.throwError("Unexpected end of input while reading keyword '$keyword'")
            }
            val ch = iterator.next()
            if (ch != keyword[i]) {
                state.throwError("Expected '${keyword[i]}' but found '$ch'")
            }
            state.buffer.append(ch)
            state.advance(isWhitespace = false)
        }

        return state.makeTokenFromBuffer(type)
    }

    private fun processString(state: ScannerState, iterator: Iterator<Char>): JsonToken {
        state.buffer.append('"')
        state.advance(isWhitespace = false)

        var escaped = false

        while (iterator.hasNext()) {
            val ch = iterator.next()

            if (escaped) {
                when (ch) {
                    '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                        state.buffer.append(ch)
                        state.advance(isWhitespace = false)
                    }
                    'u' -> {
                        state.buffer.append(ch)
                        state.advance(isWhitespace = false)
                        // Read 4 hex digits
                        repeat(4) {
                            if (!iterator.hasNext()) {
                                state.throwError("Unexpected end of input in unicode escape")
                            }
                            val hex = iterator.next()
                            if (!hex.isHexDigit()) {
                                state.throwError("Invalid unicode escape character: '$hex'")
                            }
                            state.buffer.append(hex)
                            state.advance(isWhitespace = false)
                        }
                    }
                    else -> {
                        state.throwError("Invalid escape character: '$ch'")
                    }
                }
                escaped = false
            } else {
                when {
                    ch == '\\' -> {
                        state.buffer.append(ch)
                        state.advance(isWhitespace = false)
                        escaped = true
                    }
                    ch == '"' -> {
                        state.buffer.append(ch)
                        state.advance(isWhitespace = false)
                        return state.makeTokenFromBuffer(TokenType.String)
                    }
                    ch < ' ' -> {
                        state.throwError("Invalid control character in string")
                    }
                    else -> {
                        state.buffer.append(ch)
                        state.advance(isWhitespace = false)
                    }
                }
            }
        }

        state.throwError("Unexpected end of input in string")
    }

    private fun processComment(state: ScannerState, iterator: Iterator<Char>): JsonToken {
        state.buffer.append('/')
        state.advance(isWhitespace = false)

        if (!iterator.hasNext()) {
            state.throwError("Unexpected end of input after '/'")
        }

        return when (val next = iterator.next()) {
            '/' -> processLineComment(state, iterator)
            '*' -> processBlockComment(state, iterator)
            else -> state.throwError("Expected '/' or '*' after '/', but found '$next'")
        }
    }

    private fun processLineComment(state: ScannerState, iterator: Iterator<Char>): JsonToken {
        state.buffer.append('/')
        state.advance(isWhitespace = false)

        while (iterator.hasNext()) {
            val ch = iterator.next()
            if (ch == '\n' || ch == '\r') {
                // Don't consume the newline - let main loop handle it
                break
            }
            state.buffer.append(ch)
            state.advance(isWhitespace = false)
        }

        return state.makeTokenFromBuffer(TokenType.LineComment, trimEnd = true)
    }

    private fun processBlockComment(state: ScannerState, iterator: Iterator<Char>): JsonToken {
        state.buffer.append('*')
        state.advance(isWhitespace = false)

        var prevWasStar = false

        while (iterator.hasNext()) {
            val ch = iterator.next()

            when {
                prevWasStar && ch == '/' -> {
                    state.buffer.append(ch)
                    state.advance(isWhitespace = false)
                    return state.makeTokenFromBuffer(TokenType.BlockComment)
                }
                ch == '\n' -> {
                    state.buffer.append(ch)
                    state.newLine()
                    prevWasStar = false
                }
                ch == '\r' -> {
                    // Handle \r\n
                    state.buffer.append(ch)
                    if (iterator.hasNext()) {
                        val next = iterator.next()
                        if (next == '\n') {
                            state.buffer.append(next)
                        } else {
                            state.buffer.append(next)
                        }
                    }
                    state.newLine()
                    prevWasStar = false
                }
                else -> {
                    state.buffer.append(ch)
                    state.advance(isWhitespace = false)
                    prevWasStar = (ch == '*')
                }
            }
        }

        state.throwError("Unexpected end of input in block comment")
    }

    private fun processNumber(
        state: ScannerState,
        firstChar: Char,
        iterator: Iterator<Char>
    ): Pair<JsonToken, Char?> {
        state.buffer.append(firstChar)
        state.advance(isWhitespace = false)

        var phase = if (firstChar == '-') NumberPhase.AfterSign else NumberPhase.WholeDigits

        while (iterator.hasNext()) {
            val ch = iterator.next()

            val (nextPhase, done) = advanceNumberPhase(phase, ch)

            if (done) {
                // This character belongs to the next token
                val token = state.makeTokenFromBuffer(TokenType.Number)
                return Pair(token, ch)
            }

            phase = nextPhase
            state.buffer.append(ch)
            state.advance(isWhitespace = false)
        }

        // Validate final state
        validateNumberFinalState(state, phase)
        return Pair(state.makeTokenFromBuffer(TokenType.Number), null)
    }

    private fun advanceNumberPhase(phase: NumberPhase, ch: Char): Pair<NumberPhase, Boolean> {
        return when (phase) {
            NumberPhase.AfterSign -> {
                when {
                    ch == '0' -> Pair(NumberPhase.AfterLeadingZero, false)
                    ch.isDigit() -> Pair(NumberPhase.WholeDigits, false)
                    else -> throw FracturedJsonException("Expected digit after '-'")
                }
            }
            NumberPhase.AfterLeadingZero -> {
                when {
                    ch == '.' -> Pair(NumberPhase.AfterDecimal, false)
                    ch == 'e' || ch == 'E' -> Pair(NumberPhase.AfterExponent, false)
                    ch.isDigit() -> throw FracturedJsonException("Leading zeros are not allowed")
                    else -> Pair(phase, true) // Done
                }
            }
            NumberPhase.WholeDigits -> {
                when {
                    ch.isDigit() -> Pair(NumberPhase.WholeDigits, false)
                    ch == '.' -> Pair(NumberPhase.AfterDecimal, false)
                    ch == 'e' || ch == 'E' -> Pair(NumberPhase.AfterExponent, false)
                    else -> Pair(phase, true) // Done
                }
            }
            NumberPhase.AfterDecimal -> {
                when {
                    ch.isDigit() -> Pair(NumberPhase.FractionDigits, false)
                    else -> throw FracturedJsonException("Expected digit after decimal point")
                }
            }
            NumberPhase.FractionDigits -> {
                when {
                    ch.isDigit() -> Pair(NumberPhase.FractionDigits, false)
                    ch == 'e' || ch == 'E' -> Pair(NumberPhase.AfterExponent, false)
                    else -> Pair(phase, true) // Done
                }
            }
            NumberPhase.AfterExponent -> {
                when {
                    ch == '+' || ch == '-' -> Pair(NumberPhase.AfterExponentSign, false)
                    ch.isDigit() -> Pair(NumberPhase.ExponentDigits, false)
                    else -> throw FracturedJsonException("Expected digit or sign after exponent")
                }
            }
            NumberPhase.AfterExponentSign -> {
                when {
                    ch.isDigit() -> Pair(NumberPhase.ExponentDigits, false)
                    else -> throw FracturedJsonException("Expected digit after exponent sign")
                }
            }
            NumberPhase.ExponentDigits -> {
                when {
                    ch.isDigit() -> Pair(NumberPhase.ExponentDigits, false)
                    else -> Pair(phase, true) // Done
                }
            }
        }
    }

    private fun validateNumberFinalState(state: ScannerState, phase: NumberPhase) {
        when (phase) {
            NumberPhase.AfterSign -> state.throwError("Unexpected end of number after sign")
            NumberPhase.AfterDecimal -> state.throwError("Unexpected end of number after decimal")
            NumberPhase.AfterExponent -> state.throwError("Unexpected end of number after exponent")
            NumberPhase.AfterExponentSign -> state.throwError("Unexpected end of number after exponent sign")
            else -> { /* Valid final states */ }
        }
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun Reader.asSequence(): Sequence<Char> = sequence {
        var ch: Int
        while (read().also { ch = it } != -1) {
            yield(ch.toChar())
        }
    }

    /**
     * Phases for parsing JSON numbers.
     */
    private enum class NumberPhase {
        AfterSign,
        AfterLeadingZero,
        WholeDigits,
        AfterDecimal,
        FractionDigits,
        AfterExponent,
        AfterExponentSign,
        ExponentDigits
    }
}
