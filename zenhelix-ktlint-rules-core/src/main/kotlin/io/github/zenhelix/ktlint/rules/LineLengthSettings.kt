package io.github.zenhelix.ktlint.rules

import kotlin.math.ceil

public data class LineLengthSettings(public val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH) {
    public val hard: Int = maxLineLength
    public val collapse: Int = ceil(maxLineLength * COLLAPSE_RATIO).toInt()
    public val standard: Int = (maxLineLength * STANDARD_RATIO).toInt()
    public val collapseFunctional: Int = (maxLineLength * COLLAPSE_FUNCTIONAL_RATIO).toInt()

    public companion object {
        public const val DEFAULT_MAX_LINE_LENGTH: Int = 160
        private const val COLLAPSE_RATIO: Double = 0.81
        private const val STANDARD_RATIO: Double = 0.75
        private const val COLLAPSE_FUNCTIONAL_RATIO: Double = 0.75
    }
}
