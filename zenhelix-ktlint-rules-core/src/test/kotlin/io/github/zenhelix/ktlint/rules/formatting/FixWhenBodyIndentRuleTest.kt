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
    inner class `should fix additional cases` {

        @Test
        fun `when used as expression in return statement`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun classify(x: Any): Int {
                |    return when (x) {
                |    is String -> x.length
                |    is Int    -> x + 1
                |    else      -> 0
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun classify(x: Any): Int {
                    |    return when (x) {
                    |        is String -> x.length
                    |        is Int    -> x + 1
                    |        else      -> 0
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when used as property initializer`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val result = when (x) {
                |is String -> "str"
                |is Int    -> "int"
                |else      -> "other"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val result = when (x) {
                    |    is String -> "str"
                    |    is Int    -> "int"
                    |    else      -> "other"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `nested when with wrong indentation on inner when`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun handle(x: Any, y: Any) {
                |    when (x) {
                |        is String -> {
                |            when (y) {
                |            is Int    -> println("int")
                |            is Double -> println("double")
                |            else      -> println("other")
                |            }
                |        }
                |        else -> println("default")
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun handle(x: Any, y: Any) {
                    |    when (x) {
                    |        is String -> {
                    |            when (y) {
                    |                is Int    -> println("int")
                    |                is Double -> println("double")
                    |                else      -> println("other")
                    |            }
                    |        }
                    |        else -> println("default")
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when with single entry wrong indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |is String -> x.length
                |else      -> 0
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Any) = when (x) {
                    |    is String -> x.length
                    |    else      -> 0
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when without subject wrong indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Int) = when {
                |x > 0  -> "positive"
                |x < 0  -> "negative"
                |else   -> "zero"
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Int) = when {
                    |    x > 0  -> "positive"
                    |    x < 0  -> "negative"
                    |    else   -> "zero"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `when with multiline entry body wrong indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) {
                |    when (x) {
                |    is String -> {
                |        println("string")
                |        println("done")
                |    }
                |    else -> println("other")
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Any) {
                    |    when (x) {
                    |        is String -> {
                    |            println("string")
                    |            println("done")
                    |        }
                    |        else -> println("other")
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not fix` {

        @Test
        fun `already correct when in function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) {
                |    when (x) {
                |        is String -> println("str")
                |        is Int    -> println("int")
                |        else      -> println("other")
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `when without subject correct indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Int) = when {
                |    x > 0  -> "positive"
                |    x < 0  -> "negative"
                |    else   -> "zero"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

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
