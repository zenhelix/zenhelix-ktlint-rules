package io.github.zenhelix.ktlint.rules

import kotlin.math.ceil

public data class LineLengthSettings(val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH) {
    val hard: Int = maxLineLength
    val collapse: Int = ceil(maxLineLength * COLLAPSE_RATIO).toInt()
    val standard: Int = (maxLineLength * STANDARD_RATIO).toInt()
    val collapseFunctional: Int = (maxLineLength * COLLAPSE_FUNCTIONAL_RATIO).toInt()

    public companion object {
        public const val DEFAULT_MAX_LINE_LENGTH: Int = 160
        private const val COLLAPSE_RATIO: Double = 0.81
        private const val STANDARD_RATIO: Double = 0.75
        private const val COLLAPSE_FUNCTIONAL_RATIO: Double = 0.75
    }
}
