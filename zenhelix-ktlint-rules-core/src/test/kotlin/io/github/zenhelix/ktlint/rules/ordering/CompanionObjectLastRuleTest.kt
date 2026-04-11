package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CompanionObjectLastRuleTest {

    private val ruleAssertThat = assertThatRule { CompanionObjectLastRule() }

    private val violationMessage = "Companion object should be declared after all other class members"

    @Nested
    inner class `should not report violation` {

        @Test
        fun `companion object is last member`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    val baz: Int = 1
                |    companion object {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no companion object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    val baz: Int = 1
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `empty class body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should report violation` {

        @Test
        fun `companion before a function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {}
                |    fun bar() {}
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }

        @Test
        fun `companion before a property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {}
                |    val baz: Int = 1
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }
    }
}
