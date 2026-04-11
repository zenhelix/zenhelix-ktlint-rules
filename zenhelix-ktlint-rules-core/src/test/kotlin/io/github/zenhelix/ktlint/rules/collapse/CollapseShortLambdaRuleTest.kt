package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseShortLambdaRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseShortLambdaRule() }

    @Nested
    inner class `should collapse` {

        @Test
        fun `lambda with parameter and short body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val handler = onError { error ->
                |    throw error.toException()
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val handler = onError { error -> throw error.toException() }
                    """.trimMargin()
                )
        }

        @Test
        fun `parameterless lambda with short body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = run {
                |    doSomething()
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = run { doSomething() }
                    """.trimMargin()
                )
        }

        @Test
        fun `lambda with it parameter`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val names = list.map {
                |    it.name
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val names = list.map { it.name }
                    """.trimMargin()
                )
        }

        @Test
        fun `lambda with destructuring parameters`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = map.forEach { (key, value) ->
                |    process(key, value)
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |val x = map.forEach { (key, value) -> process(key, value) }
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = run { doSomething() }
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiple statements`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = run {
                |    log("start")
                |    doSomething()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = run {
                |    doSomething(
                |        a, b, c
                |    )
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `would exceed max line length`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val veryLongVariableName = someObject.veryLongMethodName { parameter ->
                |    parameter.anotherVeryLongMethodName().andEvenMoreChaining().thatMakesItLong()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `body contains nested lambdas`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() {
                |    statuses.forEach { s ->
                |        result.getOrPut(s) { mutableSetOf() }.apply { add(status) }
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `trailing lambda after multiline argument list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun deleteGood(orderId: Long, goodId: UUID) = service.withAction(
                |    orderId, action, "Error"
                |) { _, _, _ ->
                |    doDelete(goodId)
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `lambda with only comment`() {
            // language=kotlin
            ruleAssertThat(
                """
                |val x = run {
                |    // TODO: implement
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `nested lambda with chain calls after brace would be too long`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test() = client.call {
                |    get().uri("/services/np1/bytin/factura") {
                |        it.queryParam("lang", language.value).queryParam("tin", tin).build()
                |    }
                |    .acceptJson()
                |    .exchangeToHttpResult<FacturaLegal>()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
