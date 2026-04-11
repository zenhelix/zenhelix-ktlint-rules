package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseParameterListRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseParameterListRule() }

    private val violationMessage = "Parameter list fits on a single line"

    @Nested
    inner class `should collapse` {

        @Test
        fun `two params with same modifiers in class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    val bar: String,
                |    val baz: Int
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 10, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(val bar: String, val baz: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `two params without modifiers in function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    bar: String,
                |    baz: Int
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 8, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(bar: String, baz: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `three params without modifiers`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    a: Int,
                |    b: Int,
                |    c: Int
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 8, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(a: Int, b: Int, c: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `all params private val`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    private val a: String,
                |    private val b: Int
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 10, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(private val a: String, private val b: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `should collapse when line with suffix fits within limit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class FooClient(
                |    val restClient: RestClient,
                |    val baseUrl: String
                |) : FooApi
                """.trimMargin()
            )
                .hasLintViolation(1, 16, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class FooClient(val restClient: RestClient, val baseUrl: String) : FooApi
                    """.trimMargin()
                )
        }

        @Test
        fun `short function type parameter still collapses`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun apply(
                |    fn: (Int) -> String
                |) {}
                """.trimMargin()
            )
                .hasLintViolation(1, 10, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun apply(fn: (Int) -> String) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `single param with inline annotation`() {
            // @RequestBody is an inline annotation — safe to collapse
            // language=kotlin
            ruleAssertThat(
                """
                |fun createCallback(
                |    @RequestBody request: CallbackCreateRequest
                |) {}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun createCallback(@RequestBody request: CallbackCreateRequest) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `interface method uses HARD_MAX - collapses when under 160`() {
            // Interface method 132 chars — exceeds COLLAPSE_MAX (130) but fits HARD_MAX (160)
            val longReturn = "a".repeat(20)
            // language=kotlin
            ruleAssertThat(
                """
                |interface Repo {
                |    fun findByBankAndCompany(
                |        factoringType: FactoringType, bankId: BankId, companyId: CompanyId
                |    ): $longReturn?
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |interface Repo {
                    |    fun findByBankAndCompany(factoringType: FactoringType, bankId: BankId, companyId: CompanyId): $longReturn?
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `two params with same inline annotations`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    @Inject val bar: String,
                |    @Inject val baz: Int
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(@Inject val bar: String, @Inject val baz: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `annotated params that fit within COLLAPSE_MAX`() {
            // Collapsed line ~127 chars — fits within COLLAPSE_MAX (130)
            // Previously blocked by hasAnnotatedParameter(), now only hasBlockAnnotatedParameter() blocks
            // language=kotlin
            ruleAssertThat(
                """
                |    fun deleteDoc(
                |        @PathVariable(name = COMPANY_ID) companyId: Long,
                |        @PathVariable(name = FOUNDER_ID) founderId: Long
                |    ) {}
                """.trimMargin()
            )
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                // language=kotlin
                .isFormattedAs(
                    """
                    |    fun deleteDoc(@PathVariable(name = COMPANY_ID) companyId: Long, @PathVariable(name = FOUNDER_ID) founderId: Long) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `annotated params exceeding COLLAPSE_MAX stay expanded`() {
            // Collapsed line ~145 chars — exceeds COLLAPSE_MAX (130), stays expanded
            // language=kotlin
            ruleAssertThat(
                """
                |    public fun cbAuthInterceptor(
                |        @Qualifier("cbTokenManager") tokenManager: CbTokenManager
                |    ): CbAuthInterceptor = CbAuthInterceptor(tokenManager)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already on single line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(bar: String, baz: Int)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `four or more params`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    a: Int,
                |    b: Int,
                |    c: Int,
                |    d: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `mixed modifiers - plain and private val`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class CbTokenClient(
                |    restClientBuilder: RestClient.Builder,
                |    private val credentials: CbCredentials
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `mixed modifiers - val and plain`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    bar: String,
                |    val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `mixed modifiers - val and private val`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Service(
                |    val name: String,
                |    private val repo: Repository
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `data class constructor`() {
            // language=kotlin
            ruleAssertThat(
                """
                |data class User(
                |    val name: String,
                |    val age: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `enum class constructor`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Color(
                |    val hex: String,
                |    val rgb: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `parameter with block annotation on separate line`() {
            // Block annotation (on its own line) — preserve expanded format
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    @Deprecated("use bar2")
                |    val bar: String,
                |    val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `data class with array annotation on separate line`() {
            // Array annotation @field:[...] on its own line — must preserve expanded format
            // Regression: standard:parameter-list-spacing used to collapse these
            // language=kotlin
            ruleAssertThat(
                """
                |data class Foo(
                |    @field:[Min(0) Max(250)]
                |    val name: String,
                |    @field:[Min(0) Max(250)]
                |    val shortName: String?
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `data class with mixed simple and array annotations`() {
            // Mix of @field:Valid (simple) and @field:[...] (array) — all stay on separate lines
            // language=kotlin
            ruleAssertThat(
                """
                |data class Foo(
                |    @field:Valid
                |    val items: List<Item>?,
                |    @field:[NotNull Min(0)]
                |    val count: Int?,
                |    val name: String?
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `grouped parameters - some on same line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    a: Int, b: Int,
                |    c: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line with class prefix exceeds max length`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public class CbTokenManager(
                |    private val tokenClient: CbTokenClient,
                |    private val clockSkew: Duration = Duration.ofSeconds(30),
                |    private val clock: Clock = Clock.systemUTC()
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line with suffix exceeds max length`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public class NotificationHttpRestClientImpl(
                |    private val restClient: RestClient,
                |    private val healthCheckUrl: String
                |) : NotificationHttpRestClient
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function type parameter exceeds functional max length`() {
            // Collapsed line would be ~123 chars, above COLLAPSE_FUNCTIONAL_MAX_LINE_LENGTH (120)
            // language=kotlin
            ruleAssertThat(
                """
                |public suspend inline fun <T, R> Collection<T>.mapTracked(
                |    crossinline transform: suspend (T) -> R
                |): List<R> = map { item -> transform(item) }
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `line would exceed max length`() {
            val longType = "A".repeat(150)
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    bar: $longType,
                |    baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `interface method exceeding HARD_MAX stays expanded`() {
            val longReturn = "a".repeat(120)
            // language=kotlin
            ruleAssertThat(
                """
                |interface Repo {
                |    fun findByBankAndCompany(
                |        factoringType: FactoringType, bankId: BankId, companyId: CompanyId
                |    ): $longReturn
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `regular function with body between COLLAPSE_MAX and HARD_MAX stays expanded`() {
            // ~132 chars — exceeds COLLAPSE_MAX (130) but under HARD_MAX (160)
            // Has body `{}` so NOT bodyless — uses COLLAPSE_MAX threshold
            val longReturn = "a".repeat(40)
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun findByBankAndCompany(
                |        factoringType: FactoringType, bankId: BankId, companyId: CompanyId
                |    ): $longReturn? {
                |        return null
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
