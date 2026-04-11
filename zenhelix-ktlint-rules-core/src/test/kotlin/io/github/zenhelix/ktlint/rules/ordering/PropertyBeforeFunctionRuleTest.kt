package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
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

        @Test
        fun `should not report delegated property before function`() {
            val code = """
                |class Foo {
                |    val name: String by lazy { "foo" }
                |    fun bar() {}
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report abstract property before abstract function`() {
            val code = """
                |abstract class Foo {
                |    abstract val x: Int
                |    abstract fun bar()
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when class has only properties`() {
            val code = """
                |class Foo {
                |    val x = 1
                |    val y = 2
                |    val z = 3
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
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

        @Test
        fun `should report property after function in object`() {
            val code = """
                |object Foo {
                |    fun bar() {}
                |    val x = 1
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
        }

        @Test
        fun `should report property after function in interface`() {
            val code = """
                |interface Foo {
                |    fun bar()
                |    val x: Int
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
        }

        @Test
        fun `should report multiple properties after function`() {
            val code = """
                |class Foo {
                |    fun bar() {}
                |    val x = 1
                |    val y = 2
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationsWithoutAutoCorrect(
                LintViolation(3, 5, violationMessage),
                LintViolation(4, 5, violationMessage),
            )
        }
    }
}
