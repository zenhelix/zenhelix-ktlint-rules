package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseMethodChainRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseMethodChainRule() }

    @Nested
    inner class `should collapse` {

        @Test
        fun `simple receiver dot method call`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    service
                |        .doWork()
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    service.doWork()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `receiver dot method with multiline args and reindent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    orderSecurityService
                |        .validateBankOrderAccess(
                |            orderId,
                |            bankId,
                |            accessorType,
                |            actionType,
                |            type,
                |        )
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    orderSecurityService.validateBankOrderAccess(
                    |        orderId,
                    |        bankId,
                    |        accessorType,
                    |        actionType,
                    |        type,
                    |    )
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `safe access operator`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(x: Foo?) {
                |    x
                |        ?.bar()
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(x: Foo?) {
                    |    x?.bar()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `nested in lambda with reindent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    withBank { details ->
                |        orderSecurityService
                |            .validateAccess(
                |                orderId,
                |                details.bankId,
                |            )
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    withBank { details ->
                    |        orderSecurityService.validateAccess(
                    |            orderId,
                    |            details.bankId,
                    |        )
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    service.doWork()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `lambda chain - receiver ends with closing brace`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    post().uri {
                |        path
                |    }
                |    .header(AUTHORIZATION, auth)
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line would exceed max length`() {
            val longMethod = "a".repeat(155)
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    service
                |        .$longMethod()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multi-step chain - receiver is also a dot qualified expression`() {
            // restClientBuilder.messageConverters{}.build() — receiver is a DQE, don't collapse
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    restClientBuilder
                |        .messageConverters { it.add(converter) }
                |        .build()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `chain continuation after lambda in builder pattern`() {
            // post().uri{}.header().exchange() — semantic chain, don't collapse
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    post().uri {
                |        path
                |    }
                |    .header(AUTHORIZATION, auth)
                |    .exchangeToHttpResult()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `expression body of function - builder pattern preserved`() {
            // `= dsl\n.fetchExists(...)` — expression body with builder entry point
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun exists(companyId: Long, inn: String): Boolean = dsl
                |        .fetchExists(
                |            TABLE,
                |            TABLE.COMPANY_ID.eq(companyId)
                |                .and(TABLE.INN.eq(inn))
                |        )
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `line comment before dot - would absorb code into comment`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() = restClient
                |    .get()
                |    .uri(url) // TODO fix this
                |    .exchangeToHttpResult()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `safe access with lambda - functional chain`() {
            // errorClassifier()?.let { ... } ?: ... — functional chain, preserve vertical layout
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    errorClassifier(statusCode, responseBody)
                |        ?.let { HttpResult.failure(HttpError.Business(it), meta) }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `method call with lambda body`() {
            // .apply { ... } — lambda chain, preserve layout
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    config
                |        .apply { clearCache() }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `part of longer chain - node is left side of parent DQE`() {
            // a\n.b()\n.c() — collapsing a.b() would break the chain layout
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    service
                |        .prepare()
                |        .execute()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
