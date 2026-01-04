package io.github.fracturedjson.core

/**
 * Exception indicating something went wrong while processing JSON data.
 */
class FracturedJsonException : Exception {
    /**
     * Location in the input at which the error occurred.
     */
    val inputPosition: InputPosition?

    /**
     * Default constructor.
     */
    constructor() : super() {
        inputPosition = null
    }

    /**
     * Constructor that takes a description.
     */
    constructor(message: String) : super(message) {
        inputPosition = null
    }

    /**
     * Constructor that takes a description and a position.
     */
    constructor(message: String, inputPosition: InputPosition) : super(message) {
        this.inputPosition = inputPosition
    }

    /**
     * Constructor that takes a description, exception, and position.
     */
    constructor(
        message: String,
        cause: Throwable,
        inputPosition: InputPosition
    ) : super(message, cause) {
        this.inputPosition = inputPosition
    }

    companion object {
        /**
         * Generates a FracturedJsonException, appending a description of the position to the text.
         */
        fun create(message: String, inputPosition: InputPosition): FracturedJsonException {
            val newMessage = "$message at $inputPosition"
            return FracturedJsonException(newMessage, inputPosition)
        }
    }
}
