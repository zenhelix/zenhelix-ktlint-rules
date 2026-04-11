package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseIfConditionRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseIfConditionRule() }

    @Nested
    inner class `should collapse condition to one line` {

        @Test
        fun `single-arg function call in condition fits on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(x: String?): String? = if (hasLength(
                |    x
                |)) {
                |    x
                |} else {
                |    null
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(x: String?): String? = if (hasLength(x)) {
                    |    x
                    |} else {
                    |    null
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `binary condition with wrapped arg fits on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(a: String?, b: String?): String? = if (a != null && hasLength(
                |    b
                |)) {
                |    a
                |} else {
                |    null
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(a: String?, b: String?): String? = if (a != null && hasLength(b)) {
                    |    a
                    |} else {
                    |    null
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `condition in class context fits on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(a: String?): Boolean = if (hasLength(
                |        a
                |    )) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check(a: String?): Boolean = if (hasLength(a)) {
                    |        true
                    |    } else {
                    |        false
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should break condition at operators` {

        @Test
        fun `long condition with AND broken at operator`() {
            val longName = "a".repeat(50)
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check($longName: String?, b: String?): Boolean = if (!hasLength($longName) && !hasLength(
                |            b
                |        )
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check($longName: String?, b: String?): Boolean = if (
                    |        !hasLength($longName)
                    |        && !hasLength(b)
                    |    ) {
                    |        true
                    |    } else {
                    |        false
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `condition with OR broken at operator`() {
            val longName = "a".repeat(50)
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check($longName: String?, b: String?): Boolean = if (!hasLength($longName) || !hasLength(
                |            b
                |        )
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check($longName: String?, b: String?): Boolean = if (
                    |        !hasLength($longName)
                    |        || !hasLength(b)
                    |    ) {
                    |        true
                    |    } else {
                    |        false
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple operators broken at each`() {
            val longName = "a".repeat(40)
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check($longName: String?, b: String?, c: String?): Boolean = if (!hasLength($longName) && !hasLength(
                |            b
                |        ) && !hasLength(
                |            c
                |        )
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check($longName: String?, b: String?, c: String?): Boolean = if (
                    |        !hasLength($longName)
                    |        && !hasLength(b)
                    |        && !hasLength(c)
                    |    ) {
                    |        true
                    |    } else {
                    |        false
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not modify` {

        @Test
        fun `single-line condition`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(x: Int): String = if (x > 0) {
                |    "pos"
                |} else {
                |    "neg"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `cleanly formatted at operators but fits on one line - should collapse`() {
            // If the condition fits on one line, collapse it even if already cleanly formatted
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(a: String?, b: String?): Boolean = if (
                |        !hasLength(a)
                |        && !hasLength(b)
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check(a: String?, b: String?): Boolean = if (!hasLength(a) && !hasLength(b)) {
                    |        true
                    |    } else {
                    |        false
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `already cleanly formatted and too long to collapse`() {
            val longName = "a".repeat(50)
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check($longName: String?, b: String?): Boolean = if (
                |        !hasLength($longName)
                |        && !hasLength(b)
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `operand with multiline argument list preserved`() {
            // setOf(...) with many arguments is intentionally expanded — don't reformat
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    if (order != null && setOf(
                |            STATUS_A.code,
                |            STATUS_B.code,
                |            STATUS_C.code,
                |            STATUS_D.code
                |        ).contains(order.status)
                |    ) {
                |        doWork()
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single condition without binary operator`() {
            // Non-binary multiline condition — not handled by this rule
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(a: String?): Boolean = if (
                |        hasLength(a)
                |    ) {
                |        true
                |    } else {
                |        false
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
