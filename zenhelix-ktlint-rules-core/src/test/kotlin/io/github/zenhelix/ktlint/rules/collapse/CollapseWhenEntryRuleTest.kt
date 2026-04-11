package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseWhenEntryRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseWhenEntryRule() }

    @Nested
    inner class `should collapse` {

        @Test
        fun `single throw expression in braces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |    is String -> {
                |        x.length
                |    }
                |    is Int    -> {
                |        x + 1
                |    }
                |    else      -> {
                |        throw IllegalStateException()
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Any) = when (x) {
                    |    is String -> x.length
                    |    is Int    -> x + 1
                    |    else      -> throw IllegalStateException()
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `qualified reference preserved after collapse - imports stay valid`() {
            // Regression: replaceBlockWithExpression created a single IDENTIFIER leaf
            // for "CompanyType.CLIENT", breaking PSI references and causing no-unused-imports
            // to remove the import. Using replaceBlockWithStatementNode preserves AST structure.
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Long?) = when (x) {
                |    1L   -> {
                |        CompanyType.CLIENT
                |    }
                |    2L   -> {
                |        CompanyType.CUSTOMER
                |    }
                |    else -> {
                |        null
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(x: Long?) = when (x) {
                    |    1L   -> CompanyType.CLIENT
                    |    2L   -> CompanyType.CUSTOMER
                    |    else -> null
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `single function call in braces`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun handle(result: Result) = when (result) {
                |    is Result.Success -> {
                |        process(result.data)
                |    }
                |    is Result.Error   -> {
                |        handleError(result.cause)
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun handle(result: Result) = when (result) {
                    |    is Result.Success -> process(result.data)
                    |    is Result.Error   -> handleError(result.cause)
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should collapse multiline when` {

        @Test
        fun `inner when with single branch`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(error: Error) = when (error) {
                |    is HttpError.Business -> {
                |        when (error.error) {
                |            is ParsingError.BlockedByOneId -> result
                |        }
                |    }
                |    else                  -> null
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(error: Error) = when (error) {
                    |    is HttpError.Business -> when (error.error) {
                    |        is ParsingError.BlockedByOneId -> result
                    |    }
                    |    else                  -> null
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `deeply nested inner when - handleOperationResult pattern`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun handleResult() {
                |    operation().foldWithMeta(
                |        onFailure = { error, meta ->
                |            when (error) {
                |                is HttpError.Business -> {
                |                    when (val soliqError = error.error) {
                |                        is SoliqError -> throw BadGatewayException(soliqError.message)
                |                        is RawError   -> throw BadGatewayException("HTTP error")
                |                    }
                |                }
                |                else                  -> {
                |                    throw BadGatewayException("Unexpected")
                |                }
                |            }
                |        }
                |    )
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun handleResult() {
                    |    operation().foldWithMeta(
                    |        onFailure = { error, meta ->
                    |            when (error) {
                    |                is HttpError.Business -> when (val soliqError = error.error) {
                    |                    is SoliqError -> throw BadGatewayException(soliqError.message)
                    |                    is RawError   -> throw BadGatewayException("HTTP error")
                    |                }
                    |                else                  -> throw BadGatewayException("Unexpected")
                    |            }
                    |        }
                    |    )
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `inner when with multiple branches`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(error: Error) = when (error) {
                |    is HttpError.Business -> {
                |        when (error.error) {
                |            is ParsingError.BlockedByOneId -> result
                |            is ParsingError.NotFound       -> null
                |        }
                |    }
                |    else                  -> null
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun test(error: Error) = when (error) {
                    |    is HttpError.Business -> when (error.error) {
                    |        is ParsingError.BlockedByOneId -> result
                    |        is ParsingError.NotFound       -> null
                    |    }
                    |    else                  -> null
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already without braces`() {
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
        fun `multiple statements in block`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |    is String -> {
                |        log(x)
                |        x.length
                |    }
                |    else      -> {
                |        log("default")
                |        0
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `inner when with multiline entries - handleOperationResult pattern`() {
            // Inner when entries contain multiline expressions → don't collapse
            // language=kotlin
            ruleAssertThat(
                """
                |fun handleResult() {
                |    when (error) {
                |        is HttpError.Business -> {
                |            when (val soliqError = error.error) {
                |                is SoliqError -> throw BadGatewayException(
                |                    soliqError.message ?: "default"
                |                )
                |                is RawError   -> throw BadGatewayException("HTTP error")
                |            }
                |        }
                |        else                  -> throw BadGatewayException("Unexpected")
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line would exceed standard max length`() {
            // Each collapsed line would be > 120 chars
            // language=kotlin
            ruleAssertThat(
                """
                |val goodsResult = when (request) {
                |    is InvoiceCreateRequest -> {
                |        enrichedGoods.map { it.toGoodJsonEntity() } to enrichedGoods.map { it.toCreateModel(orderId) }.map { service.createGood(it) }
                |    }
                |    else                    -> {
                |        emptyList<EdoOrderDataJsonEntity.GoodsInfoJsonEntity.GoodJsonEntity>() to emptyList<EdoGoodDto>()
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `empty block with block comment`() {
            // Block containing only a comment is not a real statement — preserve braces
            // Regression: collapsing turned `else -> { /* comment */ }` into `else -> // comment`
            // language=kotlin
            ruleAssertThat(
                """
                |fun reuse(taskId: String) {
                |    when (val task = getTask(taskId)) {
                |        is Completed -> persistResult(task.result)
                |        is Failed    -> persistResult(task.result)
                |        else         -> {
                |            /* task still running — will be handled later */
                |        }
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `empty block with eol comment`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun reuse(taskId: String) {
                |    when (val task = getTask(taskId)) {
                |        is Completed -> persistResult(task.result)
                |        is Failed    -> persistResult(task.result)
                |        else         -> {
                |            // task still running
                |        }
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `block with only line comment as body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |    is String -> x.length
                |    else      -> {
                |        // not implemented
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline expression in block`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun test(x: Any) = when (x) {
                |    is String -> {
                |        doSomething(
                |            x, y, z
                |        )
                |    }
                |    else      -> {
                |        doOther(
                |            a, b
                |        )
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
