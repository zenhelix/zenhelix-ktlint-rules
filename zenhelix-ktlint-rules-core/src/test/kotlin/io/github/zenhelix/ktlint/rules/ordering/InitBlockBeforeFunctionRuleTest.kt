package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InitBlockBeforeFunctionRuleTest {

    private val ruleAssertThat = assertThatRule { InitBlockBeforeFunctionRule() }

    private val violationMessage = "Initializer block should be declared before function declarations"

    @Nested
    inner class `should not report violation` {

        @Test
        fun `init before functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    init {
                |        println("init")
                |    }
                |    fun bar() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no init block`() {
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
        fun `multiple inits before functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    init {
                |        println("first")
                |    }
                |    init {
                |        println("second")
                |    }
                |    fun bar() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should report violation` {

        @Test
        fun `init after function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    init {
                |        println("init")
                |    }
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
        }
    }
}
