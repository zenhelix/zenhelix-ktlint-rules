package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseArgumentListRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseArgumentListRule() }

    private val violationMessage = "Argument list fits on a single line"

    @Nested
    inner class `should collapse` {

        @Test
        fun `two args on separate lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = header(
                |    HttpHeaders.CONTENT_DISPOSITION,
                |    contentDisposition.toString()
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    """.trimMargin()
                )
        }

        @Test
        fun `three args`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    a,
                |    b,
                |    c
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = foo(a, b, c)
                    """.trimMargin()
                )
        }

        @Test
        fun `real world expression body call`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun contentDisposition(cd: ContentDisposition): B = header(
                |    HttpHeaders.CONTENT_DISPOSITION, cd.toString()
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun contentDisposition(cd: ContentDisposition): B = header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    """.trimMargin()
                )
        }

        @Test
        fun `single arg uses hard max threshold`() {
            // Single-arg call at high column — collapsed line ~155 chars, exceeds COLLAPSE_MAX (130)
            // but fits within HARD_MAX (160). Single args should use HARD_MAX.
            val prefix = "a".repeat(120)
            // language=kotlin
            ruleAssertThat(
                """
                |val $prefix = hasLength(
                |    value
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val $prefix = hasLength(value)
                    """.trimMargin()
                )
        }

        @Test
        fun `single arg in if condition after expression body conversion`() {
            // Simulates the case where `{ return if(!hasLength(arg)) }` was converted to `= if(!hasLength(arg))`
            // and the single argument was left wrapped from the original wider context
            val longPrefix = "a".repeat(90)
            // language=kotlin
            ruleAssertThat(
                """
                |fun $longPrefix(): String? = if (!hasLength(
                |    value
                |)) {
                |    null
                |} else {
                |    value
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun $longPrefix(): String? = if (!hasLength(value)) {
                    |    null
                    |} else {
                    |    value
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `named arguments`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    name = "bar",
                |    value = 42
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = foo(name = "bar", value = 42)
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already on single line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(a, b)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `four or more args`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    a,
                |    b,
                |    c,
                |    d
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `lambda argument`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    a,
                |    { it.bar() }
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `grouped args`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    a, b,
                |    c
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `all qualified references - config list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = disable(
                |    SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
                |    SerializationFeature.FAIL_ON_EMPTY_BEANS
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `three qualified references`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = disable(
                |    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                |    DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS,
                |    DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line would exceed max length`() {
            val longArg = "a".repeat(120)
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    "$longArg",
                |    b
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single arg collapsed would exceed hard max`() {
            val longArg = "a".repeat(150)
            // language=kotlin
            ruleAssertThat(
                """
                |val x = foo(
                |    $longArg
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
