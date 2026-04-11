package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MemberInterleavingRuleTest {

    private val ruleAssertThat = assertThatRule { MemberInterleavingRule() }

    @Nested
    inner class `should not report violation` {

        @Test
        fun `properties then functions - no interleaving`() {
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
        fun `companion object is ignored`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val a: String = ""
                |    companion object {}
                |    fun bar() {}
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
        fun `should not report when members are grouped by category`() {
            val code = """
                |class Foo {
                |    val x = 1
                |    val y = 2
                |
                |    init { println("init") }
                |
                |    constructor(x: Int) : this()
                |
                |    fun bar() {}
                |    fun baz() {}
                |
                |    class Nested
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report in enum class with properties and functions grouped`() {
            val code = """
                |enum class Color(val rgb: Int) {
                |    RED(0xFF0000),
                |    GREEN(0x00FF00);
                |
                |    val hex: String get() = "#${'$'}{rgb.toString(16)}"
                |
                |    fun isRed(): Boolean = this == RED
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report in object declaration`() {
            val code = """
                |object Singleton {
                |    val instance = "singleton"
                |
                |    fun doSomething() {}
                |
                |    class Helper
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not report when only one member category exists`() {
            val code = """
                |class Foo {
                |    fun bar() {}
                |    fun baz() {}
                |    fun qux() {}
                |}
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should report violation` {

        @Test
        fun `property function property - interleaved properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val a: String = ""
                |    fun bar() {}
                |    val b: Int = 1
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(
                4,
                5,
                "Properties should be grouped together, not interleaved with other members"
            )
        }

        @Test
        fun `function property function - interleaved functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {}
                |    val a: String = ""
                |    fun baz() {}
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(
                4,
                5,
                "Functions should be grouped together, not interleaved with other members"
            )
        }
    }
}
