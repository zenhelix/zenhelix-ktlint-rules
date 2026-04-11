package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IfBracesRuleTest {

    private val ruleAssertThat = assertThatRule { IfBracesRule() }

    private val violationMessage = "Missing braces around if body"

    @Nested
    inner class `should report` {

        @Test
        fun `single statement without braces and no else`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (x) return
                |}
                """.trimMargin()
            )
                .hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }

        @Test
        fun `single statement on next line without braces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (condition)
                |        doSomething()
                |}
                """.trimMargin()
            )
                .hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }

        @Test
        fun `early return without braces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun validate(x: String?) {
                |    if (x == null) throw IllegalArgumentException("x is null")
                |}
                """.trimMargin()
            )
                .hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }
    }

    @Nested
    inner class `should not report` {

        @Test
        fun `already has braces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (x) { return }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `has else clause`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (x) a else b
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `expression context - property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val x = if (cond) a else b
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `expression context - return`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(): Int {
                |    return if (cond) a else b
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `body is another if`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (x) if (y) { return }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `if without else used in argument position`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    bar(if (x) a else b)
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `expression context - assignment`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    x = if (cond) a else b
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
