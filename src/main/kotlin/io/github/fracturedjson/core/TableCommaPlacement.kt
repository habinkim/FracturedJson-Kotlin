package io.github.fracturedjson.core

/**
 * Specifies where commas should be in table-formatted elements.
 */
enum class TableCommaPlacement {
    /**
     * Commas come right after the element that comes before them.
     */
    BeforePadding,

    /**
     * Commas come after the column padding, all lined with each other.
     */
    AfterPadding,

    /**
     * Commas come right after the element, except in the case of columns of numbers.
     */
    BeforePaddingExceptNumbers
}
