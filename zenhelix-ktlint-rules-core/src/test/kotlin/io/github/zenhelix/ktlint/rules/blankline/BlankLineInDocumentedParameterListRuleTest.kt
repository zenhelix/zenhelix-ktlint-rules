package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BlankLineInDocumentedParameterListRuleTest {

    private val ruleAssertThat = assertThatRule { BlankLineInDocumentedParameterListRule() }

    @Nested
    inner class `should add blank lines for KDoc` {

        @Test
        fun `constructor with KDoc params missing blank lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    /** The bar value */
                |    val bar: String,
                |    /** The baz value */
                |    val baz: Int
                |)
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(1, 11, "Expected blank line after '(' in documented parameter list"),
                    LintViolation(5, 17, "Expected blank line before ')' in documented parameter list"),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(
                    |
                    |    /** The bar value */
                    |    val bar: String,
                    |    /** The baz value */
                    |    val baz: Int
                    |
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `already has blank lines with KDoc`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |
                |    /** The bar value */
                |    val bar: String,
                |    /** The baz value */
                |    val baz: Int
                |
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should add blank line for block annotations` {

        @Test
        fun `all params separated - 100 percent ratio`() {
            // FacturaLegal pattern: all params annotated, all separated
            // language=kotlin
            ruleAssertThat(
                """
                |data class FacturaLegal(
                |    @JsonProperty("ns10Code")
                |    val ns10Code: Int?,
                |
                |    @JsonProperty("ns11Code")
                |    val ns11Code: Int?,
                |
                |    @JsonProperty("name")
                |    val name: String?
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 25, "Expected blank line after '(' in documented parameter list")
                // language=kotlin
                .isFormattedAs(
                    """
                    |data class FacturaLegal(
                    |
                    |    @JsonProperty("ns10Code")
                    |    val ns10Code: Int?,
                    |
                    |    @JsonProperty("ns11Code")
                    |    val ns11Code: Int?,
                    |
                    |    @JsonProperty("name")
                    |    val name: String?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `2 params both separated - CompanyByDirector pattern`() {
            // language=kotlin
            ruleAssertThat(
                """
                |data class CompanyByDirector(
                |    @JsonProperty("tin")
                |    val tin: String?,
                |
                |    @JsonProperty("name")
                |    val name: String?
                |)
                """.trimMargin()
            )
                .hasLintViolation(1, 30, "Expected blank line after '(' in documented parameter list")
                // language=kotlin
                .isFormattedAs(
                    """
                    |data class CompanyByDirector(
                    |
                    |    @JsonProperty("tin")
                    |    val tin: String?,
                    |
                    |    @JsonProperty("name")
                    |    val name: String?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `mixed annotated and plain params with grouping`() {
            // MySoliqCompanyDataJsonEntity pattern: ~45% ratio
            // language=kotlin
            ruleAssertThat(
                """
                |data class MySoliqCompanyData(
                |    @JsonProperty("inn")
                |    val inn: String?,
                |
                |    @JsonProperty("accountNumber")
                |    val accountNumber: String?,
                |    @JsonProperty("mfo")
                |    val mfo: String?,
                |
                |    @JsonProperty("directorFullName")
                |    val directorFullName: String?,
                |    @JsonProperty("accountantFullName")
                |    val accountantFullName: String?
                |)
                """.trimMargin()
            )
                // 2 blank lines / 4 transitions = 50% >= 33%
                .hasLintViolation(1, 31, "Expected blank line after '(' in documented parameter list")
                // language=kotlin
                .isFormattedAs(
                    """
                    |data class MySoliqCompanyData(
                    |
                    |    @JsonProperty("inn")
                    |    val inn: String?,
                    |
                    |    @JsonProperty("accountNumber")
                    |    val accountNumber: String?,
                    |    @JsonProperty("mfo")
                    |    val mfo: String?,
                    |
                    |    @JsonProperty("directorFullName")
                    |    val directorFullName: String?,
                    |    @JsonProperty("accountantFullName")
                    |    val accountantFullName: String?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `with supertype adds blank line before rpar too`() {
            // Contract pattern with supertype
            // language=kotlin
            ruleAssertThat(
                """
                |data class Response(
                |    @JsonProperty("status")
                |    override val status: Int,
                |
                |    @JsonProperty("siteId")
                |    val siteId: String?
                |) : RawResponse
                """.trimMargin()
            )
                .isFormattedAs(
                    """
                    |data class Response(
                    |
                    |    @JsonProperty("status")
                    |    override val status: Int,
                    |
                    |    @JsonProperty("siteId")
                    |    val siteId: String?
                    |
                    |) : RawResponse
                    """.trimMargin()
                )
        }

        @Test
        fun `already formatted correctly`() {
            // language=kotlin
            ruleAssertThat(
                """
                |data class CompanyByDirector(
                |
                |    @JsonProperty("tin")
                |    val tin: String?,
                |
                |    @JsonProperty("name")
                |    val name: String?
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should not add blank lines` {

        @Test
        fun `regular class with block annotations - not data class`() {
            // Only data class constructors trigger blank line for block annotations
            // language=kotlin
            ruleAssertThat(
                """
                |class IkpuImportBatchService(
                |    @param:Qualifier("goodsBatchJobLauncher")
                |    private val jobLauncher: JobLauncher,
                |
                |    @param:Qualifier("explorer")
                |    private val jobExplorer: JobExplorer
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `low ratio - SmallBusinessReference pattern`() {
            // 1 blank line / 4 transitions = 25% < 33%
            // language=kotlin
            ruleAssertThat(
                """
                |data class SmallBusinessReference(
                |    @JsonProperty("code")
                |    override val code: String,
                |
                |    @JsonProperty("text_ru")
                |    override val textRU: String?,
                |    @JsonProperty("text_uz")
                |    override val textUZ: String?,
                |    @JsonProperty("text_oz")
                |    override val textOZ: String?,
                |    @JsonProperty("text_en")
                |    override val textEN: String?
                |) : DictionaryItem
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `dense list with block annotations - BankApiTokenGridRowResponse`() {
            // 0 blank lines = 0% ratio
            // language=kotlin
            ruleAssertThat(
                """
                |data class BankApiTokenGridRowResponse(
                |    @JsonProperty("bankId")
                |    val bankId: Long,
                |    @JsonProperty("tokenId")
                |    val tokenId: Long,
                |    @JsonProperty("issuedAt")
                |    val issuedAt: OffsetDateTime,
                |    @JsonProperty("expiration")
                |    val expiration: OffsetDateTime
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no block annotations - CompanyRatingDataSource pattern`() {
            // @Value is on separate line but ratio = 0%
            // language=kotlin
            ruleAssertThat(
                """
                |class CompanyRatingDataSource(
                |    private val featureToggleService: FeatureToggleService,
                |    private val parsingClientService: CompanyParsingClientService,
                |    private val companyDataFacade: CompanyDataFacade,
                |    @Value("test")
                |    private val companyRatingRenewPeriod: Long
                |) : Logging
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single param`() {
            // language=kotlin
            ruleAssertThat(
                """
                |data class ProviderBindingResponseDto(
                |    @JsonProperty("providers")
                |    val providers: List<String>?
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no annotations plain params - EdoPendingOperationService pattern`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class EdoPendingOperationService(
                |    private val operationDataFacade: OperationDataFacade,
                |    private val orderDomainService: OrderDomainService,
                |    private val documentDomainService: DocumentDomainService,
                |    private val featureToggleService: FeatureToggleService
                |) : Logging
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `inline annotations do not trigger`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    @Inject val bar: String,
                |    @Qualifier("db") val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `data class constructor with KDoc is skipped`() {
            // language=kotlin
            ruleAssertThat(
                """
                |data class Foo(
                |    /** The bar value */
                |    val bar: String,
                |    /** The baz value */
                |    val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function parameter list is not constructor`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |    @Inject bar: String,
                |    @Inject baz: Int
                |) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no annotations or KDoc on parameters`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |    val bar: String,
                |    val baz: Int
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
