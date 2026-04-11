package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoBlankLineBetweenConsecutivePropertiesRuleTest {

    private val ruleAssertThat = assertThatRule { NoBlankLineBetweenConsecutivePropertiesRule() }

    private val violationMessage = "Blank line between consecutive properties without block annotations should be removed"

    @Nested
    inner class `regular class` {

        @Test
        fun `should remove blank line between consecutive properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |
                |    val y: String = "a"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 19, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    val x: Int = 1
                    |    val y: String = "a"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should not report when no blank lines between properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |    val y: String = "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove blank line when property has KDoc`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |
                |    /** Doc for y */
                |    val y: String = "a"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 19, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    val x: Int = 1
                    |    /** Doc for y */
                    |    val y: String = "a"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should keep blank line when property has multiline annotation`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |
                |    @Suppress(
                |        "unused",
                |    )
                |    val y: String = "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should not report when inline annotation without blank line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |    @Suppress("unused")
                |    val y: String = "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove blank line before property with inline annotation`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |
                |    @Suppress("unused")
                |    val y: String = "a"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 19, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    val x: Int = 1
                    |    @Suppress("unused")
                    |    val y: String = "a"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `modifier signature` {

        @Test
        fun `should preserve blank line between abstract and non-abstract properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |abstract class Foo {
                |    abstract val x: Int
                |
                |    val y: Boolean get() = true
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove blank line between consecutive abstract properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |abstract class Foo {
                |    abstract val x: Int
                |
                |    abstract val y: String
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 24, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |abstract class Foo {
                    |    abstract val x: Int
                    |    abstract val y: String
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should preserve blank line between public and private properties in regular class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |
                |    private val y: String = "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should preserve blank line between override and regular properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo : Bar() {
                |    override val x: Int = 1
                |
                |    val y: String = "a"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove blank line between properties with same modifiers in regular class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    private val x: Int = 1
                |
                |    private val y: String = "a"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 27, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    private val x: Int = 1
                    |    private val y: String = "a"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `multiline property` {

        @Test
        fun `should preserve blank line after multiline property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val classifier: (Int) -> String = { code ->
                |        when (code) {
                |            1 -> "one"
                |            else -> "other"
                |        }
                |    }
                |
                |    val mapper = defaultMapper()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should preserve blank line before multiline property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val mapper = defaultMapper()
                |
                |    val classifier: (Int) -> String = { code ->
                |        when (code) {
                |            1 -> "one"
                |            else -> "other"
                |        }
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should preserve blank lines in companion object with multiline property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |        const val NAME: String = "foo"
                |
                |        private val classifier: (Int, String?) -> Error? = { statusCode, _ ->
                |            if (statusCode == 423) {
                |                Error.Blocked
                |            } else {
                |                null
                |            }
                |        }
                |
                |        private val mapper = defaultMapper()
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `object declaration` {

        @Test
        fun `should remove uniform blank lines between all properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Constants {
                |    const val A: String = "a"
                |
                |    const val B: String = "b"
                |
                |    const val C: String = "c"
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(2, 30, violationMessage),
                    LintViolation(4, 30, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |object Constants {
                    |    const val A: String = "a"
                    |    const val B: String = "b"
                    |    const val C: String = "c"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should preserve blank lines when mixed pattern indicates intentional grouping`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object CommonPropertyNames {
                |    const val ENABLED_PN: String = "enabled"
                |
                |    const val CREDENTIALS_PN: String = "credentials"
                |
                |    const val HTTP_PN: String = "http"
                |    const val GRPC_PN: String = "grpc"
                |
                |    const val CIRCUIT_BREAKER_PN: String = "circuit-breaker"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should preserve blank lines when two groups separated by blank line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Config {
                |    const val HOST: String = "localhost"
                |    const val PORT: Int = 8080
                |
                |    const val TIMEOUT: Long = 5000L
                |    const val RETRIES: Int = 3
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should not report when no blank lines in object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Small {
                |    const val A: String = "a"
                |    const val B: String = "b"
                |    const val C: String = "c"
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove blank line when property has KDoc in object with uniform pattern`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Documented {
                |    const val A: String = "a"
                |
                |    /** Doc */
                |    const val B: String = "b"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 30, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |object Documented {
                    |    const val A: String = "a"
                    |    /** Doc */
                    |    const val B: String = "b"
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `should remove uniform blank lines in object with only two properties`() {
            // language=kotlin
            ruleAssertThat(
                """
                |object Pair {
                |    const val FIRST: String = "first"
                |
                |    const val SECOND: String = "second"
                |}
                """.trimMargin()
            )
                .hasLintViolation(2, 38, violationMessage)
                // language=kotlin
                .isFormattedAs(
                    """
                    |object Pair {
                    |    const val FIRST: String = "first"
                    |    const val SECOND: String = "second"
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `companion object` {

        @Test
        fun `should preserve mixed blank lines in companion object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |        const val TYPE_A: String = "a"
                |        const val TYPE_B: String = "b"
                |
                |        const val DEFAULT_TIMEOUT: Long = 5000L
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should preserve blank line between public and private properties in companion object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |        const val CIRCUIT_BREAKER_NAME: String = "bank-api-cb"
                |
                |        private val mapper = integrationJsonMapper()
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `should remove uniform blank lines in companion object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |        const val A: String = "a"
                |
                |        const val B: String = "b"
                |
                |        const val C: String = "c"
                |    }
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    LintViolation(3, 34, violationMessage),
                    LintViolation(5, 34, violationMessage),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    companion object {
                    |        const val A: String = "a"
                    |        const val B: String = "b"
                    |        const val C: String = "c"
                    |    }
                    |}
                    """.trimMargin()
                )
        }
    }
}
