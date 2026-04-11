package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PropertyBeforeFunctionRuleTest {

    private val ruleAssertThat = assertThatRule { PropertyBeforeFunctionRule() }

    private val violationMessage = "Property should be declared before function declarations"

    @Nested
    inner class `should not report violation` {

        @Test
        fun `properties then functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val a: String = ""
                |    val b: Int = 1
                |    fun bar() {}
                |    fun baz() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `only properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val a: String = ""
                |    val b: Int = 1
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `only functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    fun baz() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `empty class`() {
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
        fun `property after function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    val a: String = ""
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
        }
    }
}
