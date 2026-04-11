package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BlankLineInsideClassBodyRuleTest {

    private val ruleAssertThat = assertThatRule { BlankLineInsideClassBodyRule() }

    @Nested
    inner class `should add blank lines` {

        @Test
        fun `class with mixed member types`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val x: Int = 1
                |    fun bar(): Int = x
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    val x: Int = 1
                    |    fun bar(): Int = x
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `class with multiline functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar(): Int {
                |        return 42
                |    }
                |    fun baz(): Int {
                |        return 0
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    fun bar(): Int {
                    |        return 42
                    |    }
                    |    fun baz(): Int {
                    |        return 0
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `class with only init block gets blank lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo(val x: Int) {
                |    init {
                |        require(x > 0)
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo(val x: Int) {
                    |
                    |    init {
                    |        require(x > 0)
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `single multiline member gets blank lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar() {
                |        doWork()
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    fun bar() {
                    |        doWork()
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should remove blank lines for compact body` {

        @Test
        fun `companion object with single-line functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |
                |        fun ofTin(tin: String): Tin = Tin(tin)
                |        fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                |
                |    }
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |    companion object {
                    |        fun ofTin(tin: String): Tin = Tin(tin)
                    |        fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `sealed class with single-line data classes`() {
            // language=kotlin
            ruleAssertThat(
                """
                |sealed class TinOrPinfl {
                |
                |    data class Tin(val tin: String) : TinOrPinfl()
                |    data class Pinfl(val pinfl: String) : TinOrPinfl()
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |sealed class TinOrPinfl {
                    |    data class Tin(val tin: String) : TinOrPinfl()
                    |    data class Pinfl(val pinfl: String) : TinOrPinfl()
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not add blank lines` {

        @Test
        fun `class with only properties`() {
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
        fun `companion object with only single-line functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    companion object {
                |        fun ofTin(tin: String): Tin = Tin(tin)
                |        fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `object with only single-line data classes`() {
            // language=kotlin
            ruleAssertThat(
                """
                |sealed class TinOrPinfl {
                |    data class Tin(val tin: String) : TinOrPinfl()
                |    data class Pinfl(val pinfl: String) : TinOrPinfl()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `anonymous object with single member - compact`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun corsConfigurer() = object : WebMvcConfigurer {
                |    override fun addCorsMappings(registry: CorsRegistry) {
                |        registry.addMapping("/**")
                |    }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `anonymous object with blank lines removed`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun corsConfigurer() = object : WebMvcConfigurer {
                |
                |    override fun addCorsMappings(registry: CorsRegistry) {
                |        registry.addMapping("/**")
                |    }
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |fun corsConfigurer() = object : WebMvcConfigurer {
                    |    override fun addCorsMappings(registry: CorsRegistry) {
                    |        registry.addMapping("/**")
                    |    }
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `single member class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun bar(): Int = 42
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already has blank lines`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    val x: Int = 1
                |    fun bar(): Int = x
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `enum class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |enum class Color {
                |    RED,
                |    GREEN,
                |    BLUE
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
