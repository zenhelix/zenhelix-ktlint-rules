package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoBlankLineBetweenSimilarDeclarationsRuleTest {

    private val ruleAssertThat = assertThatRule { NoBlankLineBetweenSimilarDeclarationsRule() }

    @Nested
    inner class `should remove blank line` {

        @Test
        fun `between consecutive single-line data classes`() {
            // language=kotlin
            ruleAssertThat(
                """
                |sealed class TinOrPinfl {
                |
                |    data class Tin(val tin: String) : TinOrPinfl()
                |
                |    data class Pinfl(val pinfl: String) : TinOrPinfl()
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |sealed class TinOrPinfl {
                    |
                    |    data class Tin(val tin: String) : TinOrPinfl()
                    |    data class Pinfl(val pinfl: String) : TinOrPinfl()
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `between consecutive single-line functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    fun ofTin(tin: String): Tin = Tin(tin)
                |
                |    fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    fun ofTin(tin: String): Tin = Tin(tin)
                    |    fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `between three consecutive single-line functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    fun a(): Int = 1
                |
                |    fun b(): Int = 2
                |
                |    fun c(): Int = 3
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    fun a(): Int = 1
                    |    fun b(): Int = 2
                    |    fun c(): Int = 3
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `between single-line and small multiline class in sealed class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |sealed class Result {
                |
                |    abstract class Success : Result()
                |
                |    abstract class Failure : Result() {
                |        abstract val errors: Set<String>
                |    }
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |sealed class Result {
                    |
                    |    abstract class Success : Result()
                    |    abstract class Failure : Result() {
                    |        abstract val errors: Set<String>
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `between mixed data class and object in sealed class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |sealed class FileUploadResult {
                |
                |    data class Success(val id: UUID) : FileUploadResult()
                |
                |    object AntivirusFailed : FileUploadResult()
                |
                |    data class UploadError(val cause: Exception) : FileUploadResult()
                |
                |    fun getOrThrow(): UUID = when (this) {
                |        is Success -> this.id
                |        is AntivirusFailed -> badRequest()
                |        is UploadError -> internalError()
                |    }
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |sealed class FileUploadResult {
                    |
                    |    data class Success(val id: UUID) : FileUploadResult()
                    |    object AntivirusFailed : FileUploadResult()
                    |    data class UploadError(val cause: Exception) : FileUploadResult()
                    |
                    |    fun getOrThrow(): UUID = when (this) {
                    |        is Success -> this.id
                    |        is AntivirusFailed -> badRequest()
                    |        is UploadError -> internalError()
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }

        @Test
        fun `functions in companion object`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    companion object {
                |
                |        fun ofTin(tin: String): Tin = Tin(tin)
                |
                |        fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                |
                |    }
                |
                |}
                """.trimMargin()
            )
                // language=kotlin
                .isFormattedAs(
                    """
                    |class Foo {
                    |
                    |    companion object {
                    |
                    |        fun ofTin(tin: String): Tin = Tin(tin)
                    |        fun ofPin(pinfl: String): Pinfl = Pinfl(pinfl)
                    |
                    |    }
                    |
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not remove blank line` {

        @Test
        fun `between different declaration types`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    val x: Int = 1
                |
                |    fun bar(): Int = x
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `between multiline function and single-line function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    fun complex(): Int {
                |        return 42
                |    }
                |
                |    fun simple(): Int = 1
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `between small multiline classes when both are multiline`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Outer {
                |
                |    class Inner1 {
                |        val x = 1
                |    }
                |
                |    class Inner2 {
                |        val y = 2
                |    }
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `between multiline data classes with expanded parameters`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Config {
                |
                |    data class ClientProperty(
                |        val address: String? = null,
                |    )
                |
                |    data class ServerProperty(
                |        val enabled: Boolean = true,
                |    )
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `between large multiline classes`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Outer {
                |
                |    class Inner1 {
                |        val x = 1
                |        val y = 2
                |        val z = 3
                |    }
                |
                |    class Inner2 {
                |        val a = 4
                |    }
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `between single-line functions in class with multiline functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class TokenManager {
                |
                |    fun getToken(): TokenInfo = validTokenInfo()
                |
                |    fun refreshOrLogin(): TokenInfo {
                |        val response = client.login()
                |        return response.toTokenInfo()
                |    }
                |
                |    fun validTokenInfo(): TokenInfo? = cached?.toTokenInfo()
                |
                |    fun isExpired(): Boolean = clock.instant().isAfter(expiry)
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `already without blank line`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |
                |    fun a(): Int = 1
                |    fun b(): Int = 2
                |
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
