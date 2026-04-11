package io.github.zenhelix.ktlint.rules.spring

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SpringEndpointExplicitReturnTypeRuleTest {

    private val ruleAssertThat = assertThatRule { SpringEndpointExplicitReturnTypeRule() }

    private val violationMessage = "Controller endpoint function should have explicit return type (including Unit for void methods)"

    @Nested
    inner class `should not report violation` {

        @Test
        fun `function with explicit return type and expression body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping
                |fun getOrder(id: Long): OrderDto = service.getOrder(id)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function with explicit return type and block body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping("/orders")
                |fun getOrders(): List<OrderDto> {
                |    return service.getOrders()
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function with explicit Unit return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PostMapping("/cancel")
                |fun cancel(id: Long): Unit {
                |    service.cancel(id)
                |}
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function with explicit Unit return type and expression body`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@DeleteMapping("/{id}")
                |fun delete(id: Long): Unit = service.delete(id)
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `private function without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping
                |private fun helperMethod() = service.doSomething()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function without mapping annotation`() {
            // language=kotlin
            ruleAssertThat(
                """
                |fun regularFunction() = service.doSomething()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `function with non-spring annotation`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@Transactional
                |fun process() = service.process()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `custom meta-annotation composing GetMapping is not detected (known limitation)`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@MyGetEndpoint
                |fun getItems() = service.getItems()
                """.trimMargin()
            ).hasNoLintViolations()
        }

        @Test
        fun `nullable return type is explicit`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PostMapping("/{id}/complete")
                |fun complete(id: Long): WizardPostResponse? = wizardService.complete(id)
                """.trimMargin()
            ).hasNoLintViolations()
        }
    }

    @Nested
    inner class `should report violation` {

        @Test
        fun `expression body without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping
                |fun getOrder(id: Long) = service.getOrder(id)
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `block body without return type (implicit Unit)`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PostMapping("/cancel")
                |fun cancel(id: Long) {
                |    service.cancel(id)
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `PostMapping without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PostMapping
                |fun create(dto: CreateRequest) = service.create(dto)
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `PutMapping without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PutMapping("/{id}")
                |fun update(id: Long, dto: UpdateRequest) = service.update(id, dto)
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `DeleteMapping without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@DeleteMapping("/{id}")
                |fun delete(id: Long) {
                |    service.delete(id)
                |}
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `PatchMapping without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@PatchMapping("/{id}")
                |fun patch(id: Long, dto: PatchRequest) = service.patch(id, dto)
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `RequestMapping without return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@RequestMapping("/api")
                |fun handle() = service.handle()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `public function with mapping annotation and no return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping("/items")
                |public fun getItems() = service.getItems()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `internal function with mapping annotation and no return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping("/items")
                |internal fun getItems() = service.getItems()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `protected function with mapping annotation and no return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping("/items")
                |protected fun getItems() = service.getItems()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `override function with mapping annotation and no return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@GetMapping("/items")
                |override fun getItems() = service.getItems()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }

        @Test
        fun `function with multiple annotations including mapping and no return type`() {
            // language=kotlin
            ruleAssertThat(
                """
                |@Validated
                |@GetMapping("/items")
                |fun getItems() = service.getItems()
                """.trimMargin()
            ).hasLintViolationWithoutAutoCorrect(1, 1, violationMessage)
        }
    }
}
