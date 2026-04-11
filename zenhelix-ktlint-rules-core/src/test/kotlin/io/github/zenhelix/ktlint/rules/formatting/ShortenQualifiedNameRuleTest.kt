package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ShortenQualifiedNameRuleTest {

    private val ruleAssertThat = assertThatRule { ShortenQualifiedNameRule() }

    @Nested
    inner class `should shorten` {

        @Test
        fun `qualified name in function parameter`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo(cb: io.github.resilience4j.circuitbreaker.CircuitBreaker) {}
                """.trimMargin()
            )
                .hasLintViolation(5, 13, "Fully qualified name 'io.github.resilience4j.circuitbreaker.CircuitBreaker' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import io.github.resilience4j.circuitbreaker.CircuitBreaker
                    |
                    |fun foo(cb: CircuitBreaker) {}
                    """.trimMargin()
                )
        }

        @Test
        fun `qualified name in return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo(): com.example.model.User = TODO()
                """.trimMargin()
            )
                .hasLintViolation(5, 12, "Fully qualified name 'com.example.model.User' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import com.example.model.User
                    |
                    |fun foo(): User = TODO()
                    """.trimMargin()
                )
        }

        @Test
        fun `qualified name in annotation with use-site target`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |data class Foo(
                |    @field:jakarta.validation.Valid
                |    val bar: Bar?
                |)
                """.trimMargin()
            )
                .hasLintViolation(6, 12, "Fully qualified name 'jakarta.validation.Valid' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import jakarta.validation.Valid
                    |
                    |data class Foo(
                    |    @field:Valid
                    |    val bar: Bar?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `qualified name in annotation with arguments`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |data class Foo(
                |    @field:jakarta.validation.constraints.Digits(integer = 19, fraction = 2)
                |    val bar: Bar?
                |)
                """.trimMargin()
            )
                .hasLintViolation(6, 12, "Fully qualified name 'jakarta.validation.constraints.Digits' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import jakarta.validation.constraints.Digits
                    |
                    |data class Foo(
                    |    @field:Digits(integer = 19, fraction = 2)
                    |    val bar: Bar?
                    |)
                    """.trimMargin()
                )
        }

        @Test
        fun `qualified name in nullable type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo(): java.time.LocalDate? = null
                """.trimMargin()
            )
                .hasLintViolation(5, 12, "Fully qualified name 'java.time.LocalDate' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import java.time.LocalDate
                    |
                    |fun foo(): LocalDate? = null
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple qualified names in same type expression`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo(): List<Triple<Long?, java.time.LocalDate?, java.math.BigDecimal?>> = TODO()
                """.trimMargin()
            )
                .hasLintViolations(
                    com.pinterest.ktlint.test.LintViolation(5, 31, "Fully qualified name 'java.time.LocalDate' should use import"),
                    com.pinterest.ktlint.test.LintViolation(5, 53, "Fully qualified name 'java.math.BigDecimal' should use import"),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import java.math.BigDecimal
                    |import java.time.LocalDate
                    |
                    |fun foo(): List<Triple<Long?, LocalDate?, BigDecimal?>> = TODO()
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple annotations with same FQN adds import once`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |data class Foo(
                |    @field:jakarta.validation.Valid
                |    val bar: Bar?,
                |    @field:jakarta.validation.Valid
                |    val baz: Bar?
                |)
                """.trimMargin()
            )
                .hasLintViolations(
                    com.pinterest.ktlint.test.LintViolation(6, 12, "Fully qualified name 'jakarta.validation.Valid' should use import"),
                    com.pinterest.ktlint.test.LintViolation(8, 12, "Fully qualified name 'jakarta.validation.Valid' should use import"),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import jakarta.validation.Valid
                    |
                    |data class Foo(
                    |    @field:Valid
                    |    val bar: Bar?,
                    |    @field:Valid
                    |    val baz: Bar?
                    |)
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should shorten dot-qualified expressions` {

        @Test
        fun `object reference in with()`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo() = with(com.example.util.SomeMapper) { toModel() }
                """.trimMargin()
            )
                .hasLintViolation(5, 18, "Fully qualified name 'com.example.util.SomeMapper' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import com.example.util.SomeMapper
                    |
                    |fun foo() = with(SomeMapper) { toModel() }
                    """.trimMargin()
                )
        }

        @Test
        fun `constructor call with FQN`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo() = com.example.model.User(name = "test")
                """.trimMargin()
            )
                .hasLintViolation(5, 13, "Fully qualified name 'com.example.model.User' should use import")
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import com.example.model.User
                    |
                    |fun foo() = User(name = "test")
                    """.trimMargin()
                )
        }

        @Test
        fun `multiple dot-qualified FQNs add imports once each`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo() {
                |    with(com.example.util.MapperA) { toModel() }
                |    with(com.example.util.MapperA) { toDto() }
                |}
                """.trimMargin()
            )
                .hasLintViolations(
                    com.pinterest.ktlint.test.LintViolation(6, 10, "Fully qualified name 'com.example.util.MapperA' should use import"),
                    com.pinterest.ktlint.test.LintViolation(7, 10, "Fully qualified name 'com.example.util.MapperA' should use import"),
                )
                // language=kotlin
                .isFormattedAs(
                    """
                    |package com.example
                    |
                    |import com.example.other.Bar
                    |import com.example.util.MapperA
                    |
                    |fun foo() {
                    |    with(MapperA) { toModel() }
                    |    with(MapperA) { toDto() }
                    |}
                    """.trimMargin()
                )
        }
    }

    @Nested
    inner class `should not shorten dot-qualified expressions` {

        @Test
        fun `two-segment property access is not FQN`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |val ft = Tables.FEATURE_TOGGLE
                |
                |fun foo() {
                |    val a = ft.DISPLAY_ORDER
                |    val b = ft.IS_ENABLED
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `backtick-escaped method chain is not FQN`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import org.jooq.impl.DSL
                |
                |fun foo() = DSL.case_(cond).`when`(value).else_(default).`as`(alias)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `jOOQ iif with backtick as is not FQN`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import org.jooq.impl.DSL.iif
                |
                |fun foo() = iif(cond, a, b).`as`(alias)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `method call chain is not FQN`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |fun foo() = list.map { it.toString() }
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `nested class access with FQN is skipped`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |fun foo() = com.example.model.Outer.Inner(name = "test")
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `conflicting simple names from different packages in expressions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |fun foo() {
                |    with(com.pkg1.Mapper) { toA() }
                |    with(com.pkg2.Mapper) { toB() }
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should not shorten` {

        @Test
        fun `already using simple name with import`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import io.github.resilience4j.circuitbreaker.CircuitBreaker
                |
                |fun foo(cb: CircuitBreaker) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `Map Entry style first segment uppercase`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |fun foo(): Map.Entry<String, String>? = null
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `conflicting simple names from different packages`() {
            // language=kotlin
            ruleAssertThat(
                """
                |package com.example
                |
                |import com.example.other.Bar
                |
                |fun foo(a: com.pkg1.Valid, b: com.pkg2.Valid) {}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }
}
