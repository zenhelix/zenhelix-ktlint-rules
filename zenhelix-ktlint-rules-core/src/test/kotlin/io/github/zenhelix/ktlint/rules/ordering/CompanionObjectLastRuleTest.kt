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

        @Test
        fun `should not report named companion object at the end`() {
            val code = """
                |class Foo {
                |    fun bar() {}
                |
                |    companion object Factory {
                |        fun create(): Foo = Foo()
                |    }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report companion in interface at the end`() {
            val code = """
                |interface Foo {
                |    fun bar()
                |
                |    companion object {
                |        const val DEFAULT = "default"
                |    }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report companion in enum class at the end`() {
            val code = """
                |enum class Color {
                |    RED, GREEN, BLUE;
                |
                |    companion object {
                |        fun fromString(s: String): Color = valueOf(s)
                |    }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when companion is the only member`() {
            val code = """
                |class Foo {
                |    companion object {
                |        fun create(): Foo = Foo()
                |    }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
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

        @Test
        fun `should report companion object before nested class`() {
            val code = """
                |class Foo {
                |    companion object {
                |        fun create(): Foo = Foo()
                |    }
                |
                |    class Nested
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }

        @Test
        fun `should report companion in interface before function`() {
            val code = """
                |interface Foo {
                |    companion object {
                |        const val DEFAULT = "default"
                |    }
                |
                |    fun bar()
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }

        @Test
        fun `should report companion before property`() {
            val code = """
                |class Foo {
                |    companion object {
                |        const val TAG = "Foo"
                |    }
                |
                |    val name: String = TAG
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
        }
    }
}
