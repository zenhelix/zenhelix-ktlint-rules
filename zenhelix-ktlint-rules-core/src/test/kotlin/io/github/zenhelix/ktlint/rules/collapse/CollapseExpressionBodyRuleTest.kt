package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseExpressionBodyRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseExpressionBodyRule() }

    @Nested
    inner class `should collapse` {

        @Test
        fun `simple single-line expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(): Int =
                |    42
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(): Int = 42
                    """.trimMargin()
                )
        }

        @Test
        fun `expression body with lambda and correct reindent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun exchange(): Result =
                |    doRequest { statusCode, _, meta ->
                |        HttpResult.failure(HttpError("HTTP ${'$'}statusCode"), meta)
                |    }
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun exchange(): Result = doRequest { statusCode, _, meta ->
                    |    HttpResult.failure(HttpError("HTTP ${'$'}statusCode"), meta)
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `indented function with lambda reindent`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun exchange(): Result =
                |        doRequest { statusCode, _, meta ->
                |            HttpResult.failure(HttpError("HTTP ${'$'}statusCode"), meta)
                |        }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun exchange(): Result = doRequest { statusCode, _, meta ->
                    |        HttpResult.failure(HttpError("HTTP ${'$'}statusCode"), meta)
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline expression with parentheses collapses entirely`() {
            // Entire expression fits on one line — collapse fully
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(): Bar =
                |    doSomething(
                |        a, b
                |    )
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(): Bar = doSomething(a, b)
                    """.trimMargin()
                )
        }

        @Test
        fun `property initializer on next line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val companyId =
                |        request.getAttribute(key)
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    val companyId = request.getAttribute(key)
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `property with elvis expression collapses entirely`() {
            // Entire `expr ?: fallback` fits on one line
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val xlWb: Workbook =
                |        WorkbookFactory.create(URL(link).openStream())
                |            ?: throw BankBranchInfoUpdateException("XLS file is NULL")
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    val xlWb: Workbook = WorkbookFactory.create(URL(link).openStream()) ?: throw BankBranchInfoUpdateException("XLS file is NULL")
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `function with short elvis collapses entirely`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun getOrDefault(x: Int?): Int =
                |    x
                |        ?: 0
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun getOrDefault(x: Int?): Int = x ?: 0
                    """.trimMargin()
                )
        }

        @Test
        fun `property initializer with multiline chain collapses entirely`() {
            // Entire `(expr as? Type)?.get(key)` fits on one line — collapse fully
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val companyId =
                |        (request.getAttribute(key) as? Map<*, *>)
                |            ?.get(COMPANY_ID)
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo() {
                    |    val companyId = (request.getAttribute(key) as? Map<*, *>)?.get(COMPANY_ID)
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline if expression at top level`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun check(value: Int): String =
                |    if (value > 0) {
                |        "positive"
                |    } else {
                |        "non-positive"
                |    }
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun check(value: Int): String = if (value > 0) {
                    |    "positive"
                    |} else {
                    |    "non-positive"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline if expression in class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(value: Int): String =
                |        if (value > 0) {
                |            "positive"
                |        } else {
                |            "non-positive"
                |        }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check(value: Int): String = if (value > 0) {
                    |        "positive"
                    |    } else {
                    |        "non-positive"
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline when expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun describe(x: Int): String =
                |    when (x) {
                |        1 -> "one"
                |        2 -> "two"
                |        else -> "other"
                |    }
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun describe(x: Int): String = when (x) {
                    |    1 -> "one"
                    |    2 -> "two"
                    |    else -> "other"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline when expression in class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun describe(x: Int): String =
                |        when (x) {
                |            1 -> "one"
                |            2 -> "two"
                |            else -> "other"
                |        }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun describe(x: Int): String = when (x) {
                    |        1 -> "one"
                    |        2 -> "two"
                    |        else -> "other"
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline try expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun parse(s: String): Int =
                |    try {
                |        s.toInt()
                |    } catch (e: NumberFormatException) {
                |        0
                |    }
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun parse(s: String): Int = try {
                    |    s.toInt()
                    |} catch (e: NumberFormatException) {
                    |    0
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline if-else with nested when`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun resolve(flag: Boolean, x: Int): String =
                |    if (flag) {
                |        when (x) {
                |            1 -> "one"
                |            else -> "other"
                |        }
                |    } else {
                |        "disabled"
                |    }
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun resolve(flag: Boolean, x: Int): String = if (flag) {
                    |    when (x) {
                    |        1 -> "one"
                    |        else -> "other"
                    |    }
                    |} else {
                    |    "disabled"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should collapse multiline expression within collapse max` {

        @Test
        fun `multiline fold call fits within 130`() {
            // Collapsed first line ~97 chars <= COLLAPSE_MAX (130)
            // language=kotlin
            ruleAssertThat(
                """
                |fun toData(result: ParsingResult<List<Rating>>, inn: String): Data? =
                |    result.fold(
                |        onSuccess = { it.firstOrNull() },
                |        onFailure = { null }
                |    )
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun toData(result: ParsingResult<List<Rating>>, inn: String): Data? = result.fold(
                    |    onSuccess = { it.firstOrNull() },
                    |    onFailure = { null }
                    |)
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should fix indent after external collapse` {

        @Test
        fun `multiline apply block with over-indented body`() {
            // Simulates state after standard:function-expression-body converted { return expr } to = expr
            // without reindenting the multiline expression body
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun template(name: String, content: String): Template = config.apply {
                |            clearCache()
                |            loader = StringLoader().apply { putTemplate(name, content) }
                |        }.getTemplate(name)
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun template(name: String, content: String): Template = config.apply {
                    |        clearCache()
                    |        loader = StringLoader().apply { putTemplate(name, content) }
                    |    }.getTemplate(name)
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline if block with over-indented body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(value: Int): String = if (value > 0) {
                |                "positive"
                |            } else {
                |                "non-positive"
                |            }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun check(value: Int): String = if (value > 0) {
                    |        "positive"
                    |    } else {
                    |        "non-positive"
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline when block with over-indented body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun describe(x: Int): String = when (x) {
                |                1 -> "one"
                |                2 -> "two"
                |                else -> "other"
                |            }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun describe(x: Int): String = when (x) {
                    |        1 -> "one"
                    |        2 -> "two"
                    |        else -> "other"
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `multiline try-catch with over-indented body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun parse(s: String): Int = try {
                |                s.toInt()
                |            } catch (e: NumberFormatException) {
                |                0
                |            }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun parse(s: String): Int = try {
                    |        s.toInt()
                    |    } catch (e: NumberFormatException) {
                    |        0
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `if with multiline condition args where body is correctly indented`() {
            // Key edge case: the first whitespace-with-newline in the expression is inside the
            // condition's argument list, but the block body is already correctly indented.
            // The rule must look inside the BLOCK body, not the condition, to detect indent.
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun getAddress(a: String?, b: String?): String? = if (!hasLength(a) && !hasLength(
                |            b
                |        )
                |    ) {
                |        null
                |    } else {
                |        a ?: b
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `if with multiline condition and under-indented body after standard rule`() {
            // Simulates the exact problem: standard:function-expression-body converts
            // { return if(multiline_condition) { ... } } to = if(multiline_condition) { ... }
            // with all whitespace shifted too much, so block body ends up under-indented
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun getAddress(a: String?, b: String?): String? = if (!hasLength(a) && !hasLength(
                |        b
                |    )
                |) {
                |    null
                |} else {
                |    a ?: b
                |}
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    fun getAddress(a: String?, b: String?): String? = if (!hasLength(a) && !hasLength(
                    |            b
                    |        )
                    |    ) {
                    |        null
                    |    } else {
                    |        a ?: b
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
                |fun foo(): Int = 42
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline expression with chain exceeding hard max length`() {
            // Collapsed first line > HARD_MAX_LINE_LENGTH (160), expression is multiline
            // language=kotlin
            ruleAssertThat(
                """
                |    fun getNextStatuses(status: T, includeCurrent: Boolean = false): Set<T> =
                |        transitionsToNext[status]?.let { if (includeCurrent) it.plus(status) else it } ?: throw IllegalArgumentException("not exists")
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed first line would exceed max length`() {
            val longName = "a".repeat(150)
            // language=kotlin
            ruleAssertThat(
                """
                |fun $longName(): Int =
                |    42
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline if when collapsed first line exceeds max length`() {
            // Long function signature + if expression would exceed 160 chars
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun getAddressWithVeryLongNameForTestingPurposes(parsedBankBranch: BankBranchParsingResult, uzBankBranch: BankBranchParsingResult?): Type? =
                |        if (parsedBankBranch.address != null) {
                |            parsedBankBranch.address
                |        } else {
                |            uzBankBranch?.address
                |        }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already correctly indented if expression on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun check(value: Int): String = if (value > 0) {
                |        "positive"
                |    } else {
                |        "non-positive"
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already correctly indented when expression on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun describe(x: Int): String = when (x) {
                |        1 -> "one"
                |        2 -> "two"
                |        else -> "other"
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already correctly indented try expression on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun parse(s: String): Int = try {
                |        s.toInt()
                |    } catch (e: NumberFormatException) {
                |        0
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline when when collapsed first line exceeds max length`() {
            val longParam = "p".repeat(100)
            // language=kotlin
            ruleAssertThat(
                """
                |fun describe($longParam: Int): String =
                |    when ($longParam) {
                |        1 -> "one"
                |        else -> "other"
                |    }
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `property initializer already on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val x = 42
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `property initializer collapsed would exceed max length`() {
            val longName = "a".repeat(150)
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo() {
                |    val $longName =
                |        getValue()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
