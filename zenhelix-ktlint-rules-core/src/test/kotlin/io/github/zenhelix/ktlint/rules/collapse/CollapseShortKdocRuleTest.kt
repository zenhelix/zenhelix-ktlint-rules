package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseShortKdocRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseShortKdocRule() }

    private val violationMessage = "Short KDoc should be on a single line"

    @Nested
    inner class `should collapse` {

        @Test
        fun `simple single-line KDoc spread across 3 lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * Short description
                | */
                |fun foo() {}
                """.trimMargin()
            )
                .hasLintViolation(1, 1, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |/** Short description */
                    |fun foo() {}
                    """.trimMargin()
                )
        }

        @Test
        fun `KDoc with inline see link is not a tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * Uses email format
                | */
                |fun foo() {}
                """.trimMargin()
            )
                .hasLintViolation(1, 1, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |/** Uses email format */
                    |fun foo() {}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already single line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/** Short description */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with @param tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * Description
                | * @param x the value
                | */
                |fun foo(x: Int) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with multiple content lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * First paragraph
                | * Second paragraph
                | */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with @return tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * Returns something
                | * @return the result
                | */
                |fun foo(): Int = 42
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with @see tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * @see OtherClass
                | */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with @throws tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * @throws IllegalArgumentException if invalid
                | */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with @since tag`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | * Some description
                | * @since 1.0
                | */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `KDoc with empty content lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |/**
                | *
                | */
                |fun foo() {}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
