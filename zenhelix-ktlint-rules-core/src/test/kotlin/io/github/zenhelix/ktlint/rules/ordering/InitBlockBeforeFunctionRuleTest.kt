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

        @Test
        fun `should not report when multiple init blocks are before functions`() {
            val code = """
                |class Foo {
                |    init { println("first") }
                |    init { println("second") }
                |    fun bar() {}
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when init block is in enum class`() {
            val code = """
                |enum class Color(val rgb: Int) {
                |    RED(0xFF0000),
                |    GREEN(0x00FF00);
                |
                |    init { require(rgb >= 0) }
                |    fun hex(): String = "#${'$'}{rgb.toString(16)}"
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when class has only init block`() {
            val code = """
                |class Foo {
                |    init { println("init") }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when init block is in nested class`() {
            val code = """
                |class Outer {
                |    fun outerFun() {}
                |
                |    class Inner {
                |        init { println("inner init") }
                |        fun innerFun() {}
                |    }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report in object declaration`() {
            val code = """
                |object Singleton {
                |    init { println("init") }
                |    fun doSomething() {}
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
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

        @Test
        fun `should report when init block after function with properties between them`() {
            val code = """
                |class Foo {
                |    val x = 1
                |    fun bar() {}
                |    val y = 2
                |    init { println("init") }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(5, 5, violationMessage)
        }

        @Test
        fun `should report when multiple init blocks with some after functions`() {
            val code = """
                |class Foo {
                |    init { println("first") }
                |    fun bar() {}
                |    init { println("second") }
                |}
            """.trimMargin()
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(4, 5, violationMessage)
        }
    }
}
