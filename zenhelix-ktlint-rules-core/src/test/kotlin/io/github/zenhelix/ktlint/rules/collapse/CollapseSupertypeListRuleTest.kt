package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseSupertypeListRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseSupertypeListRule() }

    private val violationMessage = "Supertype list fits on the same line as class declaration"

    @Nested
    inner class `should collapse` {

        @Test
        fun `short class with supertype on next line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class ReaderException(val reasons: Set<String>) :
                |    RuntimeException("Errors while read file")
                """.trimMargin()
            )
                .hasLintViolation(1, 50, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class ReaderException(val reasons: Set<String>) : RuntimeException("Errors while read file")
                    """.trimMargin()
                )
        }

        @Test
        fun `class without constructor with supertype on next line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class FooService :
                |    AbstractService()
                """.trimMargin()
            )
                .hasLintViolation(1, 19, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class FooService : AbstractService()
                    """.trimMargin()
                )
        }

        @Test
        fun `class with constructor and supertype call`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class FooException(message: String) :
                |    RuntimeException(message)
                """.trimMargin()
            )
                .hasLintViolation(1, 38, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class FooException(message: String) : RuntimeException(message)
                    """.trimMargin()
                )
        }

        @Test
        fun `class with body and supertype on next line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class FooService(val repo: Repository) :
                |    AbstractService() {
                |    fun doWork() = repo.work()
                |}
                """.trimMargin()
            )
                .hasLintViolation(1, 41, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class FooService(val repo: Repository) : AbstractService() {
                    |    fun doWork() = repo.work()
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should collapse additional cases` {

        @Test
        fun `single generic supertype on new line that fits`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class MyService :
                |    BaseService<String>()
                """.trimMargin()
            )
                .hasLintViolation(1, 18, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class MyService : BaseService<String>()
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple interfaces on new line that fit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class MyAdapter :
                |    Serializable
                """.trimMargin()
            )
                .hasLintViolation(1, 18, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class MyAdapter : Serializable
                    """.trimMargin()
                )
        }

        @Test
        fun `supertype with delegation on new line that fits`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class MyMap(private val delegate: Map<String, Int>) :
                |    Map<String, Int> by delegate
                """.trimMargin()
            )
                .hasLintViolation(1, 54, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class MyMap(private val delegate: Map<String, Int>) : Map<String, Int> by delegate
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
                |class ReaderException(val reasons: Set<String>) : RuntimeException("Errors")
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line would exceed max length`() {
            val longName = "A".repeat(100)
            // language=kotlin
            ruleAssertThat(
                """
                |class $longName(val x: String) :
                |    RuntimeException("error")
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `supertype list with generics exceeding collapse max`() {
            // collapse = ceil(160 * 0.81) = 130; this collapsed line would be >130 chars
            val longGeneric = "VeryLongGenericTypeName" + "A".repeat(40)
            // language=kotlin
            ruleAssertThat(
                """
                |class MyServiceImplementation(val repo: Repository<$longGeneric>) :
                |    AbstractBaseService<$longGeneric>()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `multiline supertype list`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo :
                |    Bar(),
                |    Baz
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
