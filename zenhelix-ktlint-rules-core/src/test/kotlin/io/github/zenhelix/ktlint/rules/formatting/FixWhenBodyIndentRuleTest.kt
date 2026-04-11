package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FixWhenBodyIndentRuleTest {

    private val ruleAssertThat = assertThatRule { FixWhenBodyIndentRule() }

    @Nested
    inner class `should fix indent` {

        @Test
        fun `when entries at same level as when keyword`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |is String -> x.length
                |is Int    -> x + 1
                |else      -> 0
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Any) = when (x) {
                    |    is String -> x.length
                    |    is Int    -> x + 1
                    |    else      -> 0
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `nested when with wrong indent - FacturaClientService pattern`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun handle() {
                |    when (error) {
                |        is Business -> {
                |            when (val e = error.error) {
                |            is SoliqError -> throw BadGateway(e.message)
                |            is RawError   -> throw BadGateway("HTTP error")
                |        }
                |        }
                |        else        -> throw BadGateway("Unexpected")
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun handle() {
                    |    when (error) {
                    |        is Business -> {
                    |            when (val e = error.error) {
                    |                is SoliqError -> throw BadGateway(e.message)
                    |                is RawError   -> throw BadGateway("HTTP error")
                    |            }
                    |        }
                    |        else        -> throw BadGateway("Unexpected")
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when entries with multiline body reindented`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) {
                |    when (x) {
                |    is String -> doSomething(
                |        x, y
                |    )
                |    else      -> doOther()
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Any) {
                    |    when (x) {
                    |        is String -> doSomething(
                    |            x, y
                    |        )
                    |        else      -> doOther()
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not fix` {

        @Test
        fun `already correct indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |    is String -> x.length
                |    is Int    -> x + 1
                |    else      -> 0
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `nested when already correct`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun handle() {
                |    when (error) {
                |        is Business -> {
                |            when (val e = error.error) {
                |                is SoliqError -> throw BadGateway(e.message)
                |                is RawError   -> throw BadGateway("HTTP error")
                |            }
                |        }
                |        else        -> throw BadGateway("Unexpected")
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
