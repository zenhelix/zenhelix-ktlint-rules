package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoBlankLineInUndocumentedParameterListRuleTest {

    private val ruleAssertThat = assertThatRule { NoBlankLineInUndocumentedParameterListRule() }

    @Nested
    inner class `should remove blank lines` {

        @Test
        fun `blank line after lpar and before rpar`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |
                |    val bar: String,
                |    val baz: Int
                |
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(
                    |    val bar: String,
                    |    val baz: Int
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `blank line only after lpar`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |
                |    val bar: String,
                |    val baz: Int
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(
                    |    val bar: String,
                    |    val baz: Int
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `CompanyRatingDataSource - block annotation but zero ratio`() {
            // @Value on separate line but 0% blank line ratio → remove
            // language=kotlin
            ruleAssertThat(
                """
                |class CompanyRatingDataSource(
                |
                |    private val featureToggleService: FeatureToggleService,
                |    private val parsingClientService: CompanyParsingClientService,
                |    private val companyDataFacade: CompanyDataFacade,
                |    @Value("test")
                |    private val companyRatingRenewPeriod: Long
                |
                |) : Logging
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class CompanyRatingDataSource(
                    |    private val featureToggleService: FeatureToggleService,
                    |    private val parsingClientService: CompanyParsingClientService,
                    |    private val companyDataFacade: CompanyDataFacade,
                    |    @Value("test")
                    |    private val companyRatingRenewPeriod: Long
                    |) : Logging
                    """.trimMargin()
                )
        }

        @Test
        fun `regular class with block annotations and blank lines - IkpuImportBatchService`() {
            // Regular class (not data class) → always remove blank after (
            // language=kotlin
            ruleAssertThat(
                """
                |class IkpuImportBatchService(
                |
                |    @param:Qualifier("goodsBatchJobLauncher")
                |    private val jobLauncher: JobLauncher,
                |    @param:Qualifier("goodsBatchAsyncJobLauncher")
                |    private val asyncJobLauncher: JobLauncher,
                |
                |    @param:Qualifier("explorer")
                |    private val jobExplorer: JobExplorer,
                |    private val ikpuExcelImportJob: Job
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class IkpuImportBatchService(
                    |    @param:Qualifier("goodsBatchJobLauncher")
                    |    private val jobLauncher: JobLauncher,
                    |    @param:Qualifier("goodsBatchAsyncJobLauncher")
                    |    private val asyncJobLauncher: JobLauncher,
                    |
                    |    @param:Qualifier("explorer")
                    |    private val jobExplorer: JobExplorer,
                    |    private val ikpuExcelImportJob: Job
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `block annotation with low ratio - SmallBusinessReference like`() {
            // 1/4 = 25% < 33% → remove blank after (
            // language=kotlin
            ruleAssertThat(
                """
                |data class Foo(
                |
                |    @JsonProperty("code")
                |    val code: String,
                |
                |    @JsonProperty("text_ru")
                |    val textRU: String?,
                |    @JsonProperty("text_uz")
                |    val textUZ: String?,
                |    @JsonProperty("text_oz")
                |    val textOZ: String?,
                |    @JsonProperty("text_en")
                |    val textEN: String?
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |data class Foo(
                    |    @JsonProperty("code")
                    |    val code: String,
                    |
                    |    @JsonProperty("text_ru")
                    |    val textRU: String?,
                    |    @JsonProperty("text_uz")
                    |    val textUZ: String?,
                    |    @JsonProperty("text_oz")
                    |    val textOZ: String?,
                    |    @JsonProperty("text_en")
                    |    val textEN: String?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `inline annotations`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(
                |
                |    @Inject val bar: String,
                |    @Qualifier("db") val baz: Int
                |
                |)
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(
                    |    @Inject val bar: String,
                    |    @Qualifier("db") val baz: Int
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `indented class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Outer {
                |    class Inner(
                |
                |        val foo: String,
                |        val bar: Int
                |
                |    )
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |object Outer {
                    |    class Inner(
                    |        val foo: String,
                    |        val bar: Int
                    |    )
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not remove blank lines` {

        @Test
        fun `no blank lines present`() {
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

        @Test
        fun `parameter list with KDoc documented parameters`() {
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

        @Test
        fun `block annotations with high ratio - CompanyByDirector pattern`() {
            // 1/1 = 100% >= 33% → preserve
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

        @Test
        fun `block annotations with high ratio - FacturaLegal pattern`() {
            // 2/2 = 100% → preserve
            // language=kotlin
            ruleAssertThat(
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
            ).hasNoLintViolations()
        }

        @Test
        fun `function parameter list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun foo(
                |
                |    bar: String,
                |    baz: Int
                |
                |)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
