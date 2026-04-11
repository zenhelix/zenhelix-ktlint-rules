package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoBlankLineBetweenWhenEntriesRuleTest {

    private val ruleAssertThat = assertThatRule { NoBlankLineBetweenWhenEntriesRule() }

    @Nested
    inner class `should remove blank lines` {

        @Test
        fun `simple when with blank lines between entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 -> "a"
                |
                |    condition2 -> "b"
                |
                |    else -> "c"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    condition1 -> "a"
                    |    condition2 -> "b"
                    |    else -> "c"
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
                |
                |    Status.INACTIVE -> "inactive"
                |
                |    else -> "unknown"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (status) {
                    |    Status.ACTIVE -> "active"
                    |    Status.INACTIVE -> "inactive"
                    |    else -> "unknown"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `nested when expressions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    is Failure -> this
                |
                |    is Success -> when (second) {
                |        is Failure -> second
                |
                |        is Success -> combine(value, second.value)
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    is Failure -> this
                    |    is Success -> when (second) {
                    |        is Failure -> second
                    |        is Success -> combine(value, second.value)
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when with block body entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 -> {
                |        doSomething()
                |        result1
                |    }
                |
                |    condition2 -> {
                |        doOther()
                |        result2
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    condition1 -> {
                    |        doSomething()
                    |        result1
                    |    }
                    |    condition2 -> {
                    |        doOther()
                    |        result2
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should preserve blank lines` {

        @Test
        fun `multiline condition`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 &&
                |        condition2 -> "a"
                |
                |    else -> "b"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already without blank lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 -> "a"
                |    condition2 -> "b"
                |    else -> "c"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
