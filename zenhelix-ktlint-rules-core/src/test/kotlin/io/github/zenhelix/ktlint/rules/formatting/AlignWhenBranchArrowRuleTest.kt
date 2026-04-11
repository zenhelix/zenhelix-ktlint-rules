package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AlignWhenBranchArrowRuleTest {

    private val ruleAssertThat = assertThatRule { AlignWhenBranchArrowRule() }

    private val violationMessage = "When branch arrows should be aligned"

    @Nested
    inner class `should align` {

        @Test
        fun `unaligned arrows in when expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 -> "a"
                |    longerCondition2 -> "b"
                |    else -> "c"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 15, violationMessage),
                    LintViolation(4, 9, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    condition1       -> "a"
                    |    longerCondition2 -> "b"
                    |    else             -> "c"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when with subject`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (status) {
                |    Status.ACTIVE -> "active"
                |    Status.INACTIVE -> "inactive"
                |    else -> "unknown"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 18, violationMessage),
                    LintViolation(4, 9, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (status) {
                    |    Status.ACTIVE   -> "active"
                    |    Status.INACTIVE -> "inactive"
                    |    else            -> "unknown"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `real world example with method calls`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val response = when {
                |    current != null && !current.refreshExpiresAt.isExpired() -> tryRefreshOrLogin(current.refreshToken)
                |    else -> tokenClient.login()
                |}
                """.trimMargin()
            )
                .hasLintViolation(3, 9, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |val response = when {
                    |    current != null && !current.refreshExpiresAt.isExpired() -> tryRefreshOrLogin(current.refreshToken)
                    |    else                                                     -> tokenClient.login()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when with comments between entries still aligns`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    // first case
                |    condition1 -> "a"
                |    // second case
                |    longerCondition2 -> "b"
                |}
                """.trimMargin()
            )
                .hasLintViolation(3, 15, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    // first case
                    |    condition1       -> "a"
                    |    // second case
                    |    longerCondition2 -> "b"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not align` {

        @Test
        fun `already aligned`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1       -> "a"
                |    longerCondition2 -> "b"
                |    else             -> "c"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single branch`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition -> "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline condition`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 &&
                |        condition2 -> "a"
                |    else -> "b"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single entry when`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition -> "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already aligned arrows with enum values`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (color) {
                |    Color.RED   -> "red"
                |    Color.GREEN -> "green"
                |    Color.BLUE  -> "blue"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `edge cases` {

        @Test
        fun `arrows with is-checks of different length`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (obj) {
                |    is String -> "string"
                |    is Int -> "int"
                |    is LongClassName -> "long"
                |    else -> "other"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 14, violationMessage),
                    LintViolation(3, 11, violationMessage),
                    LintViolation(5, 9, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (obj) {
                    |    is String        -> "string"
                    |    is Int           -> "int"
                    |    is LongClassName -> "long"
                    |    else             -> "other"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `arrows with range checks`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (n) {
                |    in 1..10 -> "small"
                |    in 11..100 -> "medium"
                |    else -> "large"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 13, violationMessage),
                    LintViolation(4, 9, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (n) {
                    |    in 1..10   -> "small"
                    |    in 11..100 -> "medium"
                    |    else       -> "large"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `arrows with enum value checks`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (color) {
                |    Color.RED -> "red"
                |    Color.GREEN -> "green"
                |    Color.BLUE -> "blue"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 14, violationMessage),
                    LintViolation(4, 15, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (color) {
                    |    Color.RED   -> "red"
                    |    Color.GREEN -> "green"
                    |    Color.BLUE  -> "blue"
                    |}
                    """.trimMargin()
                )
        }
    }
}
