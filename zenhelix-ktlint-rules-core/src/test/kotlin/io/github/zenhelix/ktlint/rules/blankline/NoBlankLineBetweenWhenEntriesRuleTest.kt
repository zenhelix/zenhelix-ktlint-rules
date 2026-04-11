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
    inner class `should remove blank lines additional cases` {

        @Test
        fun `blank line between single-line entries with else`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (value) {
                |    1 -> "one"
                |
                |    2 -> "two"
                |
                |    else -> "other"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (value) {
                    |    1 -> "one"
                    |    2 -> "two"
                    |    else -> "other"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple blank lines between entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when {
                |    condition1 -> "a"
                |
                |
                |    condition2 -> "b"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when {
                    |    condition1 -> "a"
                    |    condition2 -> "b"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `blank lines in when without subject`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val y = when {
                |    x > 0 -> "positive"
                |
                |    x < 0 -> "negative"
                |
                |    else -> "zero"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val y = when {
                    |    x > 0 -> "positive"
                    |    x < 0 -> "negative"
                    |    else -> "zero"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `is-check entries with blank line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (obj) {
                |    is String -> "string"
                |
                |    is Int -> "int"
                |
                |    is Double -> "double"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = when (obj) {
                    |    is String -> "string"
                    |    is Int -> "int"
                    |    is Double -> "double"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should preserve blank lines` {

        @Test
        fun `no blank line between block-body entries`() {
            // language=kotlin
            ruleAssertThat(
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
            ).hasNoLintViolations()
        }

        @Test
        fun `no blank lines between entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = when (value) {
                |    1 -> "one"
                |    2 -> "two"
                |    3 -> "three"
                |    else -> "other"
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
