package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChainAfterLambdaIndentRuleTest {

    private val ruleAssertThat = assertThatRule { ChainAfterLambdaIndentRule() }

    private val violationMessage = "Chained call after lambda should be aligned with closing brace"

    @Nested
    inner class `multiline lambda` {

        @Test
        fun `should fix continuation indent after multiline lambda`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    post().uri {
                |        path("/api")
                |    }
                |            .header("X", "Y")
                |            .retrieve()
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(5, 13, violationMessage),
                    LintViolation(6, 13, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    post().uri {
                    |        path("/api")
                    |    }
                    |    .header("X", "Y")
                    |    .retrieve()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should not report when already aligned with closing brace`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    post().uri {
                |        path("/api")
                |    }
                |    .header("X", "Y")
                |    .retrieve()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should fix chain after nested lambda with continuation indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() = client.call {
                |    post().uri("/api") {
                |        it.queryParam("date", date)
                |            .build(lang)
                |    }
                |        .acceptJson()
                |        .contentJson(body)
                |        .exchangeToHttpResult()
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() = client.call {
                    |    post().uri("/api") {
                    |        it.queryParam("date", date)
                    |            .build(lang)
                    |    }
                    |    .acceptJson()
                    |    .contentJson(body)
                    |    .exchangeToHttpResult()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should not touch already correct chain after nested lambda`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() = client.call {
                |    post().uri("/api") {
                |        it.queryParam("date", date)
                |            .build(lang)
                |    }
                |    .acceptJson()
                |    .contentJson(body)
                |    .exchangeToHttpResult()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `inline lambda` {

        @Test
        fun `should fix indent after inline lambda - too few spaces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val result = runCatching { doSomething() }
                |    .getOrThrow()
                |}
                """.trimMargin()
            )
                .hasLintViolation(3, 5, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val result = runCatching { doSomething() }
                    |        .getOrThrow()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should not report when inline lambda chain has correct indent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val result = runCatching { doSomething() }
                |        .getOrThrow()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should fix real world inline lambda chain`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun tryRefresh(token: String): Response = runCatching { client.refresh(token) }
                |.recoverCatching { ex -> client.login() }
                |.getOrThrow()
                """.trimMargin()
            )
                .hasLintViolation(2, 1, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun tryRefresh(token: String): Response = runCatching { client.refresh(token) }
                    |    .recoverCatching { ex -> client.login() }
                    |    .getOrThrow()
                    """.trimMargin()
                )
        }

        @Test
        fun `should fix indented inline lambda chain`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val response = runCatching { client.refresh(token) }
                |    .recoverCatching { ex -> client.login() }
                |    .getOrThrow()
                |}
                """.trimMargin()
            )
                .hasLintViolation(3, 5, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val response = runCatching { client.refresh(token) }
                    |        .recoverCatching { ex -> client.login() }
                    |        .getOrThrow()
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not touch` {

        @Test
        fun `chain without lambda`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val x = listOf(1, 2, 3)
                |    .filter { it > 1 }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `chain on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val x = runCatching { foo() }.getOrThrow()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
