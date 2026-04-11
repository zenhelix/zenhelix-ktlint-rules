package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpandLongParameterListRuleTest {

    private val ruleAssertThat = assertThatRule { ExpandLongParameterListRule() }

    private val violationMessage = "Parameter list should be expanded to multiple lines"

    @Nested
    inner class `should expand` {

        @Test
        fun `params on single line inside multiline parens exceeding max length`() {
            val longDefault = "VeryLongDefaultValue".repeat(3)
            // language=kotlin
            ruleAssertThat(
                """
                |public class FooBarService(
                |    private val tokenClient: CbTokenClient, private val clockSkew: Duration = Duration.ofSeconds(30), private val name: String = "$longDefault"
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 27, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |public class FooBarService(
                    |    private val tokenClient: CbTokenClient,
                    |    private val clockSkew: Duration = Duration.ofSeconds(30),
                    |    private val name: String = "$longDefault"
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `class with supertype on single line exceeding max length - params fit on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public class NotificationHttpRestClientImpl(private val restClient: RestClient, private val healthCheckUrl: String) : NotificationHttpRestClient {
                |}
                """.trimMargin()
            )
                .hasLintViolation(1, 44, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |public class NotificationHttpRestClientImpl(
                    |    private val restClient: RestClient, private val healthCheckUrl: String
                    |) : NotificationHttpRestClient {
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `class on single line exceeding max length - params do not fit on one line`() {
            val longDefault = "VeryLongDefaultValue".repeat(3)
            // language=kotlin
            ruleAssertThat(
                """
                |public class FooBarService(private val client: Client, private val clockSkew: Duration = Duration.ofSeconds(30), private val name: String = "$longDefault")
                """.trimMargin()
            )
                .hasLintViolation(1, 27, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |public class FooBarService(
                    |    private val client: Client,
                    |    private val clockSkew: Duration = Duration.ofSeconds(30),
                    |    private val name: String = "$longDefault"
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `expression body on next line - expanding allows collapsing body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public inline fun <reified T : Collection<*>> UriBuilder.queryParamIfPresent(name: String, value: T?): UriBuilder =
                |    apply { value?.also { this.queryParam(name, it) } }
                """.trimMargin()
            )
                // Note: only ExpandLongParameterListRule runs in this test.
                // CollapseExpressionBodyRule would then collapse =\n body in a full format pass.
                // language=kotlin
                .isFormattedAs(
                    """
                    |public inline fun <reified T : Collection<*>> UriBuilder.queryParamIfPresent(
                    |    name: String, value: T?
                    |): UriBuilder =
                    |    apply { value?.also { this.queryParam(name, it) } }
                    """.trimMargin()
                )
        }

        @Test
        fun `single param with expression body on next line - expands to allow collapse`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public fun RestClient.RequestBodySpec.contentFormUrlencoded(builder: FormInserterBuilder.() -> Unit = {}): RestClient.RequestBodySpec =
                |    contentFormUrlencoded().body(FormInserterBuilder().apply(builder).build())
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |public fun RestClient.RequestBodySpec.contentFormUrlencoded(
                    |    builder: FormInserterBuilder.() -> Unit = {}
                    |): RestClient.RequestBodySpec =
                    |    contentFormUrlencoded().body(FormInserterBuilder().apply(builder).build())
                    """.trimMargin()
                )
        }

        @Test
        fun `single param with short expression body on next line - expands to allow collapse`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public fun RestClient.RequestBodySpec.contentFormUrlencoded(formData: MultiValueMap<String, String>): RestClient.RequestBodySpec =
                |    contentFormUrlencoded().body(formData)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |public fun RestClient.RequestBodySpec.contentFormUrlencoded(
                    |    formData: MultiValueMap<String, String>
                    |): RestClient.RequestBodySpec =
                    |    contentFormUrlencoded().body(formData)
                    """.trimMargin()
                )
        }

        @Test
        fun `single param without expression body - does not expand`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun collateralRegistryCircuitBreaker(circuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker {
                |    return circuitBreakerRegistry.create()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single param with expression body already on same line - does not expand`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(bar: String): Int = bar.length
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `expression body with params on one line exceeding hard max`() {
            // Signature fits in 120, but total line > 160 — must expand params
            // language=kotlin
            ruleAssertThat(
                """
                |override fun loadTemplate(templateName: String, templateContent: String): TemplateHandle = FreemarkerTemplateHandle(template(templateName, templateContent), templateConverter)
                """.trimMargin()
            )
                .hasLintViolation(1, 26, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |override fun loadTemplate(
                    |    templateName: String, templateContent: String
                    |): TemplateHandle = FreemarkerTemplateHandle(template(templateName, templateContent), templateConverter)
                    """.trimMargin()
                )
        }

        @Test
        fun `all on one line exceeding max length`() {
            val longParam = "a".repeat(80)
            val longParam2 = "b".repeat(80)
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo($longParam: String, $longParam2: String)
                """.trimMargin()
            )
                .hasLintViolation(1, 8, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun foo(
                    |    $longParam: String,
                    |    $longParam2: String
                    |)
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not expand` {

        @Test
        fun `params already on separate lines`() {
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
        fun `single parameter even on long line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun collateralRegistryCircuitBreaker(circuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker = circuitBreakerRegistry.createCircuitBreaker()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single parameter exceeding hard max is expanded`() {
            val longType = "A".repeat(130)
            // language=kotlin
            ruleAssertThat(
                """
                |fun process(veryLongParam: $longType): Unit = doSomething()
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun process(
                    |    veryLongParam: $longType
                    |): Unit = doSomething()
                    """.trimMargin()
                )
        }

        @Test
        fun `signature fits but expression body makes line long`() {
            // Signature up to '= ' is ~79 chars (fits in 120), expression body makes line >120
            // language=kotlin
            ruleAssertThat(
                """
                |fun getPrevStatuses(status: T, includeCurrent: Boolean = false): Set<T> = transitionsToPrev[status]?.let { if (includeCurrent) it.plus(status) else it }
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `short params on single line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(bar: String, baz: Int)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `short params inside multiline parens`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    val bar: String, val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `interface method under HARD_MAX not expanded`() {
            // 132 chars — exceeds COLLAPSE_MAX (130) but under HARD_MAX (160)
            // Bodyless function → only expand if exceeds HARD_MAX
            // language=kotlin
            ruleAssertThat(
                """
                |interface Repo {
                |    fun findByBankAndCompany(factoringType: FactoringType, bankId: BankId, companyId: CompanyId): CompanyInfoBankRateShortDto?
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `abstract method under HARD_MAX not expanded`() {
            // language=kotlin
            ruleAssertThat(
                """
                |abstract class Repo {
                |    abstract fun findByBankAndCompany(factoringType: FactoringType, bankId: BankId, companyId: CompanyId): CompanyInfoBankRateShortDto?
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should expand interface methods` {

        @Test
        fun `interface method exceeding HARD_MAX is expanded`() {
            val longReturn = "a".repeat(80)
            // language=kotlin
            ruleAssertThat(
                """
                |interface Repo {
                |    fun findByBankAndCompany(factoringType: FactoringType, bankId: BankId, companyId: CompanyId): $longReturn
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |interface Repo {
                    |    fun findByBankAndCompany(
                    |        factoringType: FactoringType, bankId: BankId, companyId: CompanyId
                    |    ): $longReturn
                    |}
                    """.trimMargin()
                )
        }
    }
}
