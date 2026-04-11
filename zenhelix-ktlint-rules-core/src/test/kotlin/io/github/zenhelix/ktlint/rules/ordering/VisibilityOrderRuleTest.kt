package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VisibilityOrderRuleTest {

    private val ruleAssertThat = assertThatRule { VisibilityOrderRule() }

    private fun violationMessage(actual: String, context: String): String =
        "'$actual' member should be declared before '$context' members (expected order: public -> internal -> protected -> private)"

    @Nested
    inner class `should not report violation` {

        @Test
        fun `members in correct order - public then internal then protected then private`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun publicFun() {}
                |    internal fun internalFun() {}
                |    protected fun protectedFun() {}
                |    private fun privateFun() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `class with only public members`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    val a: String = ""
                |    fun bar() {}
                |    fun baz() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `empty class`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `internal members before private`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    internal val a: String = ""
                |    internal fun bar() {}
                |    private val b: Int = 0
                |    private fun baz() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `private properties followed by public functions`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Repository(private val dsl: DSLContext) {
                |    private val ft = Tables.FEATURE_TOGGLE
                |    private val ftp = Tables.FEATURE_TOGGLE_PARAMS
                |
                |    override fun findByName(name: String): FeatureToggleDto? = TODO()
                |    override fun findAllWithParams(): List<FeatureToggleDto> = TODO()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `properties ordered by visibility then functions ordered by visibility`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Service {
                |    val publicProp: String = ""
                |    internal val internalProp: Int = 0
                |    private val privateProp: Boolean = false
                |
                |    fun publicFun() {}
                |    internal fun internalFun() {}
                |    private fun privateFun() {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `companion object after private member is allowed`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    fun publicFun() {}
                |    private fun privateFun() {}
                |    companion object {}
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should report violation` {

        @Test
        fun `private function appears before public function`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    private fun privateFun() {}
                |    fun publicFun() {}
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage("public", "private"))
        }

        @Test
        fun `private property appears before public property`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    private val a: Int = 0
                |    val b: String = ""
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage("public", "private"))
        }

        @Test
        fun `override member after private member in same category`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo : Bar() {
                |    fun publicFun() {}
                |    private fun privateFun() {}
                |    override fun toString(): String = "Foo"
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(4, 5, violationMessage("public", "private"))
        }

        @Test
        fun `protected then private then public functions - reports on public`() {
            // language=kotlin
            ruleAssertThat(
                """
                |class Foo {
                |    protected fun protectedFun() {}
                |    private fun privateFun() {}
                |    fun publicFun() {}
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(4, 5, violationMessage("public", "private"))
        }
    }
}
