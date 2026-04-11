package io.github.zenhelix.ktlint.rules

public object LineLengthSettings {

    /** Standard max line length — used by [ExpandLongParameterListRule] to trigger expansion. */
    public const val STANDARD_MAX_LINE_LENGTH: Int = 120

    /** Max line length for collapse rules — used by [CollapseParameterListRule] and [CollapseArgumentListRule]. */
    public const val COLLAPSE_MAX_LINE_LENGTH: Int = 130

    /** Max line length for collapsing parameter lists that contain function-type parameters. */
    public const val COLLAPSE_FUNCTIONAL_MAX_LINE_LENGTH: Int = 120

    /** Hard max line length — used by [CollapseExpressionBodyRule], [CollapseConstructorAnnotationRule], and [ExpandLongParameterListRule]. */
    public const val HARD_MAX_LINE_LENGTH: Int = 160
}
