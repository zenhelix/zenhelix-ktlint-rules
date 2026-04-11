package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseConstructorAnnotationRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseConstructorAnnotationRule() }

    private val violationMessage = "Primary constructor annotation should be on the same line as the class declaration"

    @Nested
    inner class `should collapse` {

        @Test
        fun `annotation constructor on separate line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public class Foo<T>
                |    @PublishedApi
                |    internal constructor()
                """.trimMargin()
            )
                .hasLintViolation(1, 20, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |public class Foo<T> @PublishedApi internal constructor()
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
                |public class Foo<T> @PublishedApi internal constructor()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `no annotation on constructor`() {
            // language=kotlin
            ruleAssertThat(
                """
                |public class Foo<T>
                |    internal constructor()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `collapsed line would exceed 160 chars`() {
            val longName = "A".repeat(140)
            // language=kotlin
            ruleAssertThat(
                """
                |public class $longName<T>
                |    @PublishedApi
                |    internal constructor()
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
