package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollapseEnumEntriesRuleTest {

    private val ruleAssertThat = assertThatRule { CollapseEnumEntriesRule() }

    @Nested
    inner class `should collapse` {

        @Test
        fun `two simple enum entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class SignatureVersion {
                |    DIGEST,
                |    FILE
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |enum class SignatureVersion {
                    |    DIGEST, FILE
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `three simple enum entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Direction {
                |    NORTH,
                |    SOUTH,
                |    WEST
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |enum class Direction {
                    |    NORTH, SOUTH, WEST
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `four simple enum entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Direction {
                |    NORTH,
                |    SOUTH,
                |    EAST,
                |    WEST
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |enum class Direction {
                    |    NORTH, SOUTH, EAST, WEST
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `five simple enum entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Level {
                |    TRACE,
                |    DEBUG,
                |    INFO,
                |    WARN,
                |    ERROR
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |enum class Level {
                    |    TRACE, DEBUG, INFO, WARN, ERROR
                    |}
                    """.trimMargin()
                )
        }

    }

    @Nested
    inner class `should not collapse` {

        @Test
        fun `already on one line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class SignatureVersion {
                |    DIGEST, FILE
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `entry with body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Op {
                |    PLUS {
                |        override fun apply(a: Int, b: Int) = a + b
                |    },
                |    MINUS {
                |        override fun apply(a: Int, b: Int) = a - b
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `entry with annotation`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Status {
                |    @Deprecated
                |    OLD,
                |    NEW
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `entry name too long`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class TaxpayerStatusType {
                |    VAT_PAYER,
                |    VAT_NON_PAYER,
                |    VAT_PAYER_CERTIFICATE_INACTIVE
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `single entry`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Single {
                |    ONLY
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `entries with constructor args`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Color(val hex: String) {
                |    RED("#FF0000"),
                |    GREEN("#00FF00"),
                |    BLUE("#0000FF")
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `too many entries`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Direction {
                |    NORTH,
                |    SOUTH,
                |    EAST,
                |    WEST,
                |    UP,
                |    DOWN
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `entries barely exceeding max entry name length`() {
            // Each entry name exceeds MAX_ENTRY_NAME_LENGTH (15)
            // language=kotlin
            ruleAssertThat(
                """
                |enum class LongNamedEntries {
                |    VERY_LONG_ENTRY_A,
                |    VERY_LONG_ENTRY_B
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `would exceed max line length`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class VeryLongEnumName {
                |    VERY_LONG_ENTRY_NAME_ONE_THAT_TAKES_SPACE,
                |    VERY_LONG_ENTRY_NAME_TWO_THAT_TAKES_SPACE,
                |    VERY_LONG_ENTRY_NAME_THREE_THAT_TAKES_SPACE,
                |    VERY_LONG_ENTRY_NAME_FOUR_THAT_TAKES_MORE_SPACE
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
