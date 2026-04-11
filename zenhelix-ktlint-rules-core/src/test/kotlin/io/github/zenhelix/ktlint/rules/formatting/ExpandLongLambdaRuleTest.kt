package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpandLongLambdaRuleTest {

    private val ruleAssertThat = assertThatRule { ExpandLongLambdaRule() }

    @Nested
    inner class `should expand` {

        @Test
        fun `lambda with params exceeding hard max`() {
            // Total line: "    onHttpError = { statusCode, _, meta -> HttpResult.failure(HttpError.UnexpectedError("HTTP $statusCode"), meta) }" = ~120 chars
            // Pad to exceed 160 with a long prefix
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val veryLongVariableNameHere = someObject.configure(onHttpError = { statusCode, _, meta -> HttpResult.failure(HttpError.UnexpectedError("HTTP ${'$'}statusCode"), meta) })
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val veryLongVariableNameHere = someObject.configure(onHttpError = { statusCode, _, meta ->
                    |        HttpResult.failure(HttpError.UnexpectedError("HTTP ${'$'}statusCode"), meta)
                    |    })
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `parameterless lambda exceeding hard max`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val veryLongVariableNameForTestingPurposes = someObject.veryLongMethodNameThatMakesLineExceedMaximumAllowedLength { reallyLongExpressionThatPushesUsOverTheLimit() }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val veryLongVariableNameForTestingPurposes = someObject.veryLongMethodNameThatMakesLineExceedMaximumAllowedLength {
                    |        reallyLongExpressionThatPushesUsOverTheLimit()
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should expand additional cases` {

        @Test
        fun `lambda with arrow and parameter exceeding 160 chars`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val resultOfOperation = someService.processDataWithConfiguration(configurationParameterName = { parameterValue -> transformAndValidateTheParameterValue(parameterValue) })
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val resultOfOperation = someService.processDataWithConfiguration(configurationParameterName = { parameterValue ->
                    |        transformAndValidateTheParameterValue(parameterValue)
                    |    })
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `nested lambda exceeding limit expands outer`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val resultVariable = someObject.outerMethodCallWithLongName(parameterName = { innerObject.innerMethodCallWithLongName { it.transformationCallHere() } })
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val resultVariable = someObject.outerMethodCallWithLongName(parameterName = {
                    |        innerObject.innerMethodCallWithLongName { it.transformationCallHere() }
                    |    })
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `lambda with destructuring exceeding limit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val longVariableName = someService.executeOperationWithCallbackHandler(handlerParam = { (firstComponent, secondComponent) -> processComponents(firstComponent, secondComponent) })
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val longVariableName = someService.executeOperationWithCallbackHandler(handlerParam = { (firstComponent, secondComponent) ->
                    |        processComponents(firstComponent, secondComponent)
                    |    })
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `lambda in chained call exceeding limit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val longVariableNameForTesting = someObject.firstMethodCall().secondMethodCall().thirdMethodCallWithLambda { reallyLongExpressionBodyThatExceedsTheLimit() }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test() {
                    |    val longVariableNameForTesting = someObject.firstMethodCall().secondMethodCall().thirdMethodCallWithLambda {
                    |        reallyLongExpressionBodyThatExceedsTheLimit()
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not expand` {

        @Test
        fun `short lambda within limit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val x = list.map { it.name }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already expanded lambda`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    onError = { error ->
                |        throw error.toException()
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `nested lambda in chained call within limit`() {
            // Reproduces: `.messageConverters { it.addFirst(MappingJackson2HttpMessageConverter(mapper)) }`
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(private val client: Client) {
                |
                |    constructor(restClientBuilder: RestClient.Builder, circuitBreaker: CircuitBreaker? = null) : this(
                |        ResilientRestClient(
                |            restClient = restClientBuilder
                |                .messageConverters { it.addFirst(MappingJackson2HttpMessageConverter(mapper)) }
                |                .build(),
                |            circuitBreaker = circuitBreaker
                |        )
                |    )
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `lambda with multiple statements`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    val x = run { log("start"); doSomething() }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
