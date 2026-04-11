package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
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

        @Test
        fun `should collapse constructor with multiple annotations`() {
            val code = """
                |@Suppress("unused")
                |class Foo
                |@Inject
                |@Named("bar")
                |constructor(val x: Int)
            """.trimMargin()
            ruleAssertThat(code)
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                .hasLintViolation(2, 10, violationMessage)
                .isFormattedAs(
                    """
                    |@Suppress("unused")
                    |class Foo @Inject @Named("bar")
                    |constructor(val x: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `should collapse constructor with visibility modifier and annotation`() {
            val code = """
                |class Foo
                |@Inject
                |internal constructor(val x: Int)
            """.trimMargin()
            ruleAssertThat(code)
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                .hasLintViolation(1, 10, violationMessage)
                .isFormattedAs(
                    """
                    |class Foo @Inject internal constructor(val x: Int)
                    """.trimMargin()
                )
        }

        @Test
        fun `should collapse constructor annotation on generic class`() {
            val code = """
                |class Container<T : Any>
                |@Inject
                |constructor(val value: T)
            """.trimMargin()
            ruleAssertThat(code)
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                .hasLintViolation(1, 25, violationMessage)
                .isFormattedAs(
                    """
                    |class Container<T : Any> @Inject
                    |constructor(val value: T)
                    """.trimMargin()
                )
        }

        @Test
        fun `should collapse constructor annotation on data class`() {
            val code = """
                |data class Foo
                |@Inject
                |constructor(val x: Int, val y: String)
            """.trimMargin()
            ruleAssertThat(code)
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                .hasLintViolation(1, 15, violationMessage)
                .isFormattedAs(
                    """
                    |data class Foo @Inject
                    |constructor(val x: Int, val y: String)
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

        @Test
        fun `should not collapse when multiple annotations exceed hard max line length`() {
            val code = """
                |@Suppress("unused")
                |class VeryLongClassNameThatTakesUpMostOfTheLineAlreadyAndWillExceedTheLimitWhenAnnotationIsAddedToTheSameLineAsConstructorXXXXXXX
                |@Inject
                |constructor(val x: Int)
            """.trimMargin()
            ruleAssertThat(code)
                .withEditorConfigOverride(MAX_LINE_LENGTH_PROPERTY to 160)
                .hasNoLintViolations()
        }

        @Test
        fun `should not collapse when already on same line`() {
            val code = """
                |class Foo @Inject constructor(val x: Int)
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `should not collapse when class has no constructor annotation`() {
            val code = """
                |class Foo constructor(val x: Int)
            """.trimMargin()
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
