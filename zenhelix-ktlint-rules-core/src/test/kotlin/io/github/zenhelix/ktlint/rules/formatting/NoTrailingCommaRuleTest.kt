package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoTrailingCommaRuleTest {

    private val ruleAssertThat = assertThatRule { NoTrailingCommaRule() }

    private val violationMessage = "Trailing comma is not allowed"

    @Nested
    inner class `should remove` {

        @Test
        fun `trailing comma in function params`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(a: Int,) {}
                """.trimMargin()
            )
                .hasLintViolation(1, 15, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(a: Int) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma in argument list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(1,)
                """.trimMargin()
            )
                .hasLintViolation(1, 14, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = foo(1)
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma before closing paren on next line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    a: Int,
                |) {}
                """.trimMargin()
            )
                .hasLintViolation(2, 11, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(
                    |    a: Int
                    |) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma in type parameters`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Box<T,>
                """.trimMargin()
            )
                .hasLintViolation(1, 12, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Box<T>
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma in destructuring declaration`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val (a, b,) = pair
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 14, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val (a, b) = pair
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma in multiline argument list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    a,
                |    b,
                |)
                """.trimMargin()
            )
                .hasLintViolation(3, 6, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = foo(
                    |    a,
                    |    b
                    |)
                    """.trimMargin()
                )
        }
        @Test
        fun `trailing comma in collection literal`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = listOf(1, 2, 3,)
                """.trimMargin()
            )
                .hasLintViolation(1, 23, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = listOf(1, 2, 3)
                    """.trimMargin()
                )
        }

        @Test
        fun `trailing comma in annotation parameters`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@Suppress("UNCHECKED_CAST",)
                |fun foo() {}
                """.trimMargin()
            )
                .hasLintViolation(1, 27, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |@Suppress("UNCHECKED_CAST")
                    |fun foo() {}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not remove` {

        @Test
        fun `no trailing comma`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(a: Int) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `comma between params`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(a: Int, b: Int) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
