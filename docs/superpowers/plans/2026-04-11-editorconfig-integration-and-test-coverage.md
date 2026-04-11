# EditorConfig Integration, Self-Sufficiency & Test Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make zenhelix-ktlint-rules self-sufficient by reading `max_line_length` from `.editorconfig`, adding `RunAfterRule` to prevent conflicts with standard rules, and strengthening thin test coverage.

**Architecture:** `LineLengthSettings` becomes a data class computed from `max_line_length`. `ZenhelixRule` reads the EditorConfig property in `beforeFirstNode()` and exposes computed settings to subclasses. All rules that conflict with standard rules declare `RunAfterRule` dependencies.

**Tech Stack:** Kotlin 2.1.20, KtLint 1.8.0 (`ktlint-rule-engine-core`), JUnit 5, AssertJ, KtLint test framework (`ktlint-test`)

---

## Phase 1: Core Infrastructure (EditorConfig Integration)

### Task 1: Convert LineLengthSettings to data class

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/LineLengthSettings.kt`
- Create: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/LineLengthSettingsTest.kt`

- [ ] **Step 1: Write tests for LineLengthSettings**

```kotlin
package io.github.zenhelix.ktlint.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class LineLengthSettingsTest {

    @Nested
    inner class `default values` {

        @Test
        fun `should use 160 as default max line length`() {
            val settings = LineLengthSettings()
            assertThat(settings.hard).isEqualTo(160)
            assertThat(settings.collapse).isEqualTo(130)
            assertThat(settings.standard).isEqualTo(120)
            assertThat(settings.collapseFunctional).isEqualTo(120)
        }
    }

    @Nested
    inner class `proportional scaling` {

        @Test
        fun `should scale proportionally for max line length 120`() {
            val settings = LineLengthSettings(120)
            assertThat(settings.hard).isEqualTo(120)
            assertThat(settings.collapse).isEqualTo(98) // ceil(120 * 0.81)
            assertThat(settings.standard).isEqualTo(90) // floor(120 * 0.75)
            assertThat(settings.collapseFunctional).isEqualTo(90)
        }

        @Test
        fun `should scale proportionally for max line length 140`() {
            val settings = LineLengthSettings(140)
            assertThat(settings.hard).isEqualTo(140)
            assertThat(settings.collapse).isEqualTo(114) // ceil(140 * 0.81)
            assertThat(settings.standard).isEqualTo(105) // floor(140 * 0.75)
            assertThat(settings.collapseFunctional).isEqualTo(105)
        }

        @Test
        fun `should scale proportionally for max line length 200`() {
            val settings = LineLengthSettings(200)
            assertThat(settings.hard).isEqualTo(200)
            assertThat(settings.collapse).isEqualTo(162) // ceil(200 * 0.81)
            assertThat(settings.standard).isEqualTo(150) // floor(200 * 0.75)
            assertThat(settings.collapseFunctional).isEqualTo(150)
        }
    }

    @Nested
    inner class `edge cases` {

        @Test
        fun `should handle max value (off) by using default`() {
            val settings = LineLengthSettings(Int.MAX_VALUE)
            assertThat(settings.hard).isEqualTo(Int.MAX_VALUE)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.LineLengthSettingsTest" --no-build-cache`
Expected: Compilation error — `LineLengthSettings` is still an object, not a data class with constructor

- [ ] **Step 3: Implement LineLengthSettings as data class**

Replace the entire content of `LineLengthSettings.kt` with:

```kotlin
package io.github.zenhelix.ktlint.rules

import kotlin.math.ceil

public data class LineLengthSettings(val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH) {

    /** Hard max line length — absolute maximum, used by collapse-expression-body, collapse-constructor-annotation, expand rules. */
    val hard: Int = maxLineLength

    /** Max line length for collapse rules — used by collapse-parameter-list, collapse-argument-list, collapse-enum-entries, collapse-supertype-list. */
    val collapse: Int = ceil(maxLineLength * COLLAPSE_RATIO).toInt()

    /** Standard max line length — used by expand-long-parameter-list, collapse-short-lambda, collapse-when-entry. */
    val standard: Int = (maxLineLength * STANDARD_RATIO).toInt()

    /** Max line length for collapsing parameter lists that contain function-type parameters. */
    val collapseFunctional: Int = (maxLineLength * COLLAPSE_FUNCTIONAL_RATIO).toInt()

    public companion object {
        public const val DEFAULT_MAX_LINE_LENGTH: Int = 160

        private const val COLLAPSE_RATIO: Double = 0.81
        private const val STANDARD_RATIO: Double = 0.75
        private const val COLLAPSE_FUNCTIONAL_RATIO: Double = 0.75
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.LineLengthSettingsTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/LineLengthSettings.kt \
       zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/LineLengthSettingsTest.kt
git commit -m "refactor: convert LineLengthSettings from object to data class with proportional scaling"
```

---

### Task 2: Add EditorConfig support to ZenhelixRule base class

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/ZenhelixRule.kt`

- [ ] **Step 1: Modify ZenhelixRule to read max_line_length from EditorConfig**

Replace the entire content of `ZenhelixRule.kt` with:

```kotlin
package io.github.zenhelix.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY

public abstract class ZenhelixRule(
    ruleId: RuleId,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
    usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : Rule(
    ruleId = ruleId,
    about = ZENHELIX_ABOUT,
    visitorModifiers = visitorModifiers,
    usesEditorConfigProperties = usesEditorConfigProperties + MAX_LINE_LENGTH_PROPERTY,
),
    RuleAutocorrectApproveHandler {

    protected var lineLengthSettings: LineLengthSettings = LineLengthSettings()
        private set

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        val maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        lineLengthSettings = if (maxLineLength == Int.MAX_VALUE) {
            LineLengthSettings()
        } else {
            LineLengthSettings(maxLineLength)
        }
    }

    protected inline fun emitAndCorrect(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        offset: Int,
        errorMessage: String,
        correct: () -> Unit,
    ) {
        val decision = emit(offset, errorMessage, true)
        if (decision == AutocorrectDecision.ALLOW_AUTOCORRECT) {
            correct()
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :zenhelix-ktlint-rules-core:compileKotlin --no-build-cache`
Expected: Compilation errors in rules that reference `LineLengthSettings.CONSTANT_NAME` — this is expected and will be fixed in Task 3

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/ZenhelixRule.kt
git commit -m "feat: add EditorConfig max_line_length support to ZenhelixRule base class"
```

---

### Task 3: Migrate all rules from static constants to dynamic LineLengthSettings

**Files:**
- Modify: 12 rule files that reference `LineLengthSettings.CONSTANT_NAME`

Each rule needs two changes:
1. Remove `import io.github.zenhelix.ktlint.rules.LineLengthSettings`
2. Replace `LineLengthSettings.CONSTANT_NAME` with `lineLengthSettings.propertyName`

**Mapping of old constants to new properties:**
- `LineLengthSettings.HARD_MAX_LINE_LENGTH` → `lineLengthSettings.hard`
- `LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH` → `lineLengthSettings.collapse`
- `LineLengthSettings.STANDARD_MAX_LINE_LENGTH` → `lineLengthSettings.standard`
- `LineLengthSettings.COLLAPSE_FUNCTIONAL_MAX_LINE_LENGTH` → `lineLengthSettings.collapseFunctional`

- [ ] **Step 1: Update CollapseParameterListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseParameterListRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace the `when` block (~lines 61-65):
```kotlin
// OLD:
val maxLength = when {
    hasFunctionalParameter(params) -> LineLengthSettings.COLLAPSE_FUNCTIONAL_MAX_LINE_LENGTH
    node.isBodylessFunction() -> LineLengthSettings.HARD_MAX_LINE_LENGTH
    else -> LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH
}

// NEW:
val maxLength = when {
    hasFunctionalParameter(params) -> lineLengthSettings.collapseFunctional
    node.isBodylessFunction() -> lineLengthSettings.hard
    else -> lineLengthSettings.collapse
}
```

- [ ] **Step 2: Update CollapseArgumentListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseArgumentListRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace the `if` block (~lines 54-58):
```kotlin
// OLD:
val maxLength = if (args.size == 1) {
    LineLengthSettings.HARD_MAX_LINE_LENGTH
} else {
    LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH
}

// NEW:
val maxLength = if (args.size == 1) {
    lineLengthSettings.hard
} else {
    lineLengthSettings.collapse
}
```

- [ ] **Step 3: Update CollapseExpressionBodyRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseExpressionBodyRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace all references:
```kotlin
// Line ~79: val maxLength = LineLengthSettings.HARD_MAX_LINE_LENGTH
val maxLength = lineLengthSettings.hard

// Line ~115: if (fullLength > LineLengthSettings.HARD_MAX_LINE_LENGTH) return false
if (fullLength > lineLengthSettings.hard) return false
```

- [ ] **Step 4: Update CollapseConstructorAnnotationRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseConstructorAnnotationRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 64):
```kotlin
// OLD:
if (lineBeforeWs.length + 1 + constructorSingleLine.length > LineLengthSettings.HARD_MAX_LINE_LENGTH) return

// NEW:
if (lineBeforeWs.length + 1 + constructorSingleLine.length > lineLengthSettings.hard) return
```

- [ ] **Step 5: Update CollapseEnumEntriesRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseEnumEntriesRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 56):
```kotlin
// OLD:
if (column + collapsedEntries.length > LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH) return

// NEW:
if (column + collapsedEntries.length > lineLengthSettings.collapse) return
```

- [ ] **Step 6: Update CollapseIfConditionRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseIfConditionRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 81):
```kotlin
// OLD:
if (collapsedLine.length + suffixLength <= LineLengthSettings.HARD_MAX_LINE_LENGTH) {

// NEW:
if (collapsedLine.length + suffixLength <= lineLengthSettings.hard) {
```

- [ ] **Step 7: Update CollapseMethodChainRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseMethodChainRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 99):
```kotlin
// OLD:
if (collapsedLength > LineLengthSettings.HARD_MAX_LINE_LENGTH) return

// NEW:
if (collapsedLength > lineLengthSettings.hard) return
```

- [ ] **Step 8: Update CollapseShortLambdaRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseShortLambdaRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 69):
```kotlin
// OLD:
if (column + collapsedText.length + suffixLength > LineLengthSettings.STANDARD_MAX_LINE_LENGTH) return

// NEW:
if (column + collapsedText.length + suffixLength > lineLengthSettings.standard) return
```

- [ ] **Step 9: Update CollapseSupertypeListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseSupertypeListRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 58):
```kotlin
// OLD:
if (collapsedLength > LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH) return

// NEW:
if (collapsedLength > lineLengthSettings.collapse) return
```

- [ ] **Step 10: Update CollapseWhenEntryRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseWhenEntryRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace both references:
```kotlin
// Line ~80:
// OLD: if (collapsedLength > LineLengthSettings.STANDARD_MAX_LINE_LENGTH) return
if (collapsedLength > lineLengthSettings.standard) return

// Line ~98:
// OLD: if (collapsedFirstLineLength > LineLengthSettings.STANDARD_MAX_LINE_LENGTH) return
if (collapsedFirstLineLength > lineLengthSettings.standard) return
```

- [ ] **Step 11: Update ExpandLongParameterListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongParameterListRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace all 7 references:
```kotlin
// Lines ~58, ~63, ~68 (3 occurrences): LineLengthSettings.HARD_MAX_LINE_LENGTH → lineLengthSettings.hard
// Line ~114: LineLengthSettings.STANDARD_MAX_LINE_LENGTH → lineLengthSettings.standard
// Line ~147: LineLengthSettings.STANDARD_MAX_LINE_LENGTH → lineLengthSettings.standard
// Line ~151: LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH → lineLengthSettings.collapse
// Line ~180: LineLengthSettings.STANDARD_MAX_LINE_LENGTH → lineLengthSettings.standard
```

- [ ] **Step 12: Update ExpandLongLambdaRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongLambdaRule.kt`

Remove import: `import io.github.zenhelix.ktlint.rules.LineLengthSettings`

Replace (~line 54):
```kotlin
// OLD:
if (lineLength <= LineLengthSettings.HARD_MAX_LINE_LENGTH) return

// NEW:
if (lineLength <= lineLengthSettings.hard) return
```

- [ ] **Step 13: Verify full compilation**

Run: `./gradlew :zenhelix-ktlint-rules-core:compileKotlin :zenhelix-ktlint-rules-spring:compileKotlin --no-build-cache`
Expected: SUCCESS — all rules compile with dynamic values

- [ ] **Step 14: Run all existing tests**

Run: `./gradlew test --no-build-cache`
Expected: All existing tests PASS (default `LineLengthSettings()` produces same values as old constants)

- [ ] **Step 15: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/ \
       zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/
git commit -m "refactor: migrate all rules from static LineLengthSettings constants to dynamic values"
```

---

## Phase 2: RunAfterRule Additions

### Task 4: Add RunAfterRule to all conflicting rules

**Files:**
- Modify: 16 rule files listed below

Each rule needs:
1. Add import: `import com.pinterest.ktlint.rule.engine.core.api.RuleId` (if not already present)
2. Add import: `import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier` (if not already present)
3. Add a companion object constant for the standard rule ID
4. Add `visitorModifiers = setOf(...)` to the constructor

The pattern for each rule (example for NoTrailingCommaRule):
```kotlin
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED

public class NoTrailingCommaRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-trailing-comma"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_TRAILING_COMMA_DECLARATION_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_TRAILING_COMMA_CALL_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {
    // ... existing code ...

    private companion object {
        val STANDARD_TRAILING_COMMA_DECLARATION_RULE_ID = RuleId("standard:trailing-comma-on-declaration-site")
        val STANDARD_TRAILING_COMMA_CALL_RULE_ID = RuleId("standard:trailing-comma-on-call-site")
    }
}
```

- [ ] **Step 1: Update NoTrailingCommaRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/NoTrailingCommaRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:trailing-comma-on-declaration-site`
- `standard:trailing-comma-on-call-site`

- [ ] **Step 2: Update CollapseArgumentListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseArgumentListRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:argument-list-wrapping`

- [ ] **Step 3: Update CollapseExpressionBodyRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseExpressionBodyRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:function-expression-body`

- [ ] **Step 4: Update CollapseMethodChainRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseMethodChainRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:chain-method-continuation`

- [ ] **Step 5: Update CollapseIfConditionRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseIfConditionRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:multiline-expression-wrapping`

- [ ] **Step 6: Update CollapseShortLambdaRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseShortLambdaRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:function-literal`

- [ ] **Step 7: Update CollapseWhenEntryRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseWhenEntryRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:when-entry-bracing`

- [ ] **Step 8: Update CollapseSupertypeListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseSupertypeListRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:wrapping`

- [ ] **Step 9: Update ExpandLongParameterListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongParameterListRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:parameter-list-wrapping`

- [ ] **Step 10: Update ExpandLongLambdaRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongLambdaRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:function-literal`

- [ ] **Step 11: Update BlankLineInsideClassBodyRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/blankline/BlankLineInsideClassBodyRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:no-empty-first-line-in-class-body`

- [ ] **Step 12: Update BlankLineInDocumentedParameterListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/blankline/BlankLineInDocumentedParameterListRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:no-blank-line-in-list`

- [ ] **Step 13: Update NoBlankLineInUndocumentedParameterListRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/blankline/NoBlankLineInUndocumentedParameterListRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:no-blank-line-in-list`

- [ ] **Step 14: Update NoBlankLineBetweenWhenEntriesRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/blankline/NoBlankLineBetweenWhenEntriesRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:blank-line-between-when-conditions`

- [ ] **Step 15: Update ChainAfterLambdaIndentRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/ChainAfterLambdaIndentRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:indent`

- [ ] **Step 16: Update FixWhenBodyIndentRule**

File: `zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/formatting/FixWhenBodyIndentRule.kt`

Add `visitorModifiers` with RunAfterRule for:
- `standard:indent`

- [ ] **Step 17: Verify full build**

Run: `./gradlew build --no-build-cache`
Expected: All compilation and existing tests PASS

- [ ] **Step 18: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/main/kotlin/io/github/zenhelix/ktlint/rules/
git commit -m "feat: add RunAfterRule to 16 rules for conflict prevention with standard ktlint rules"
```

---

## Phase 3: EditorConfig Template & Documentation

### Task 5: Create .editorconfig template

**Files:**
- Create: `docs/editorconfig-template.txt`

- [ ] **Step 1: Create the template file**

```ini
# Recommended .editorconfig settings for zenhelix-ktlint-rules
# Copy this section into your project's .editorconfig file.
#
# These settings disable standard KtLint rules that conflict with
# zenhelix custom rules. Without these settings, you may get
# conflicting lint violations or unexpected formatting.

[*.{kt,kts}]
max_line_length = 160

# --- Disable conflicting standard rules ---

# Trailing comma (conflicts with zenhelix:no-trailing-comma)
ktlint_standard_trailing-comma-on-declaration-site = disabled
ktlint_standard_trailing-comma-on-call-site = disabled

# Wrapping (conflicts with zenhelix collapse/expand rules)
ktlint_standard_argument-list-wrapping = disabled
ktlint_standard_parameter-list-wrapping = disabled
ktlint_standard_wrapping = disabled
ktlint_standard_multiline-expression-wrapping = disabled

# Blank lines (conflicts with zenhelix blankline rules)
ktlint_standard_no-blank-line-in-list = disabled
ktlint_standard_no-blank-line-before-rbrace = disabled
ktlint_standard_blank-line-before-declaration = disabled
ktlint_standard_blank-line-between-when-conditions = disabled
ktlint_standard_no-empty-first-line-in-class-body = disabled

# Spacing (conflicts with zenhelix blankline/ordering rules)
ktlint_standard_spacing-between-declarations-with-annotations = disabled
ktlint_standard_spacing-between-declarations-with-comments = disabled
ktlint_standard_modifier-list-spacing = disabled

# Annotation (conflicts with zenhelix:collapse-constructor-annotation)
ktlint_standard_annotation = disabled
ktlint_standard_annotation-spacing = disabled

# Indent (conflicts with zenhelix formatting rules)
ktlint_standard_indent = disabled
ktlint_standard_string-template-indent = disabled

# Signature (conflicts with zenhelix collapse rules)
ktlint_standard_function-signature = disabled
ktlint_standard_class-signature = disabled
ktlint_standard_chain-method-continuation = disabled

# Other (conflicts with zenhelix collapse rules)
ktlint_standard_when-entry-bracing = disabled
ktlint_standard_function-literal = disabled
```

- [ ] **Step 2: Commit**

```bash
git add docs/editorconfig-template.txt
git commit -m "docs: add recommended .editorconfig template for disabling conflicting standard rules"
```

---

## Phase 4: Strengthen Thin Tests

### Task 6: Add tests for InitBlockBeforeFunctionRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/InitBlockBeforeFunctionRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

Add the following tests to the existing test class (inside appropriate `@Nested` inner classes):

```kotlin
@Test
fun `should not report when multiple init blocks are before functions`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    init { println("first") }
        |    init { println("second") }
        |    fun bar() {}
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report when init block is in enum class`() {
    // language=kotlin
    val code = """
        |enum class Color(val rgb: Int) {
        |    RED(0xFF0000),
        |    GREEN(0x00FF00);
        |
        |    init { require(rgb >= 0) }
        |    fun hex(): String = "#${'\$'}{rgb.toString(16)}"
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report when class has only init block`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    init { println("init") }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report when init block is in nested class`() {
    // language=kotlin
    val code = """
        |class Outer {
        |    fun outerFun() {}
        |
        |    class Inner {
        |        init { println("inner init") }
        |        fun innerFun() {}
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report when init block after function with properties between them`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val x = 1
        |    fun bar() {}
        |    val y = 2
        |    init { println("init") }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(5, 5, violationMessage)
}

@Test
fun `should report when multiple init blocks with some after functions`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    init { println("first") }
        |    fun bar() {}
        |    init { println("second") }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(4, 5, violationMessage)
}

@Test
fun `should not report when init block is before companion functions`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    init { println("init") }
        |    fun bar() {}
        |
        |    companion object {
        |        fun create(): Foo = Foo()
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report in object declaration`() {
    // language=kotlin
    val code = """
        |object Singleton {
        |    init { println("init") }
        |    fun doSomething() {}
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.ordering.InitBlockBeforeFunctionRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/InitBlockBeforeFunctionRuleTest.kt
git commit -m "test: add edge case tests for InitBlockBeforeFunctionRule"
```

---

### Task 7: Add tests for CollapseConstructorAnnotationRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseConstructorAnnotationRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

Add these tests:

```kotlin
@Test
fun `should collapse constructor with multiple annotations`() {
    // language=kotlin
    val code = """
        |@Suppress("unused")
        |class Foo
        |@Inject
        |@Named("bar")
        |constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |@Suppress("unused")
            |class Foo @Inject @Named("bar") constructor(val x: Int)
            """.trimMargin()
        )
}

@Test
fun `should not collapse when multiple annotations exceed hard max line length`() {
    // language=kotlin
    val code = """
        |@Suppress("unused")
        |class VeryLongClassNameThatTakesUpMostOfTheLineAlreadyAndWillExceedTheLimitWhenAnnotationIsAddedToTheSameLineAsConstructor
        |@Inject
        |constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should collapse constructor with annotation with parameters`() {
    // language=kotlin
    val code = """
        |class Foo
        |@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        |constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(val x: Int)
            """.trimMargin()
        )
}

@Test
fun `should not collapse when already on same line`() {
    // language=kotlin
    val code = """
        |class Foo @Inject constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should collapse constructor with visibility modifier and annotation`() {
    // language=kotlin
    val code = """
        |class Foo
        |@Inject
        |internal constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo @Inject internal constructor(val x: Int)
            """.trimMargin()
        )
}

@Test
fun `should collapse constructor annotation on generic class`() {
    // language=kotlin
    val code = """
        |class Container<T : Any>
        |@Inject
        |constructor(val value: T)
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Container<T : Any> @Inject constructor(val value: T)
            """.trimMargin()
        )
}

@Test
fun `should not collapse when class has no constructor annotation`() {
    // language=kotlin
    val code = """
        |class Foo constructor(val x: Int)
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should collapse constructor annotation on data class`() {
    // language=kotlin
    val code = """
        |data class Foo
        |@Inject
        |constructor(val x: Int, val y: String)
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |data class Foo @Inject constructor(val x: Int, val y: String)
            """.trimMargin()
        )
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.collapse.CollapseConstructorAnnotationRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseConstructorAnnotationRuleTest.kt
git commit -m "test: add edge case tests for CollapseConstructorAnnotationRule"
```

---

### Task 8: Add tests for CompanionObjectLastRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/CompanionObjectLastRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should not report named companion object at the end`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    fun bar() {}
        |
        |    companion object Factory {
        |        fun create(): Foo = Foo()
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report companion object before nested class`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    companion object {
        |        fun create(): Foo = Foo()
        |    }
        |
        |    class Nested
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
}

@Test
fun `should not report companion in interface`() {
    // language=kotlin
    val code = """
        |interface Foo {
        |    fun bar()
        |
        |    companion object {
        |        const val DEFAULT = "default"
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report companion in interface before function`() {
    // language=kotlin
    val code = """
        |interface Foo {
        |    companion object {
        |        const val DEFAULT = "default"
        |    }
        |
        |    fun bar()
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
}

@Test
fun `should not report companion in enum class at the end`() {
    // language=kotlin
    val code = """
        |enum class Color {
        |    RED, GREEN, BLUE;
        |
        |    companion object {
        |        fun fromString(s: String): Color = valueOf(s)
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report when companion is the only member`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    companion object {
        |        fun create(): Foo = Foo()
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report companion before property`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    companion object {
        |        const val TAG = "Foo"
        |    }
        |
        |    val name: String = TAG
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(2, 5, violationMessage)
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.ordering.CompanionObjectLastRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/CompanionObjectLastRuleTest.kt
git commit -m "test: add edge case tests for CompanionObjectLastRule"
```

---

### Task 9: Add tests for PropertyBeforeFunctionRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/PropertyBeforeFunctionRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should not report delegated property before function`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val name: String by lazy { "foo" }
        |    fun bar() {}
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report property after function in object`() {
    // language=kotlin
    val code = """
        |object Foo {
        |    fun bar() {}
        |    val x = 1
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
}

@Test
fun `should not report abstract property before abstract function`() {
    // language=kotlin
    val code = """
        |abstract class Foo {
        |    abstract val x: Int
        |    abstract fun bar()
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report property after function in interface`() {
    // language=kotlin
    val code = """
        |interface Foo {
        |    fun bar()
        |    val x: Int
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
}

@Test
fun `should not report properties in companion object`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val x = 1
        |    fun bar() {}
        |
        |    companion object {
        |        val TAG = "Foo"
        |        fun create(): Foo = Foo()
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report when class has only properties`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val x = 1
        |    val y = 2
        |    val z = 3
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report multiple properties after function`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    fun bar() {}
        |    val x = 1
        |    val y = 2
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(3, 5, violationMessage)
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.ordering.PropertyBeforeFunctionRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/PropertyBeforeFunctionRuleTest.kt
git commit -m "test: add edge case tests for PropertyBeforeFunctionRule"
```

---

### Task 10: Add tests for MemberInterleavingRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/MemberInterleavingRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should not report when members are grouped by category`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val x = 1
        |    val y = 2
        |
        |    init { println("init") }
        |
        |    constructor(x: Int) : this()
        |
        |    fun bar() {}
        |    fun baz() {}
        |
        |    class Nested
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report in enum class`() {
    // language=kotlin
    val code = """
        |enum class Color(val rgb: Int) {
        |    RED(0xFF0000),
        |    GREEN(0x00FF00);
        |
        |    val hex: String get() = "#${'\$'}{rgb.toString(16)}"
        |
        |    fun isRed(): Boolean = this == RED
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not report in object declaration`() {
    // language=kotlin
    val code = """
        |object Singleton {
        |    val instance = "singleton"
        |
        |    fun doSomething() {}
        |
        |    class Helper
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report functions split by nested class`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    fun bar() {}
        |    class Nested
        |    fun baz() {}
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(4, 5, "Function 'baz' is interleaved with other member categories")
}

@Test
fun `should report properties split by function`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    val x = 1
        |    fun bar() {}
        |    val y = 2
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(4, 5, "Property 'y' is interleaved with other member categories")
}

@Test
fun `should not report when only one member category exists`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    fun bar() {}
        |    fun baz() {}
        |    fun qux() {}
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should report init blocks split by functions`() {
    // language=kotlin
    val code = """
        |class Foo {
        |    init { println("first") }
        |    fun bar() {}
        |    init { println("second") }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(4, 5, "Init block is interleaved with other member categories")
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.ordering.MemberInterleavingRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/ordering/MemberInterleavingRuleTest.kt
git commit -m "test: add edge case tests for MemberInterleavingRule"
```

---

### Task 11: Add tests for FixWhenBodyIndentRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/FixWhenBodyIndentRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should fix when used as expression in return statement`() {
    // language=kotlin
    val code = """
        |fun foo(x: Int): String {
        |    return when (x) {
        |            1 -> "one"
        |            else -> "other"
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |fun foo(x: Int): String {
            |    return when (x) {
            |        1 -> "one"
            |        else -> "other"
            |    }
            |}
            """.trimMargin()
        )
}

@Test
fun `should fix when used as property initializer`() {
    // language=kotlin
    val code = """
        |val result = when (x) {
        |        1 -> "one"
        |        else -> "other"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val result = when (x) {
            |    1 -> "one"
            |    else -> "other"
            |}
            """.trimMargin()
        )
}

@Test
fun `should not fix already correct when in function`() {
    // language=kotlin
    val code = """
        |fun foo(x: Int): String {
        |    return when (x) {
        |        1 -> "one"
        |        2 -> "two"
        |        else -> "other"
        |    }
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should fix nested when with wrong indentation`() {
    // language=kotlin
    val code = """
        |fun foo(x: Int, y: Int): String = when (x) {
        |    1 -> when (y) {
        |            1 -> "one-one"
        |            else -> "one-other"
        |    }
        |    else -> "other"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |fun foo(x: Int, y: Int): String = when (x) {
            |    1 -> when (y) {
            |        1 -> "one-one"
            |        else -> "one-other"
            |    }
            |    else -> "other"
            |}
            """.trimMargin()
        )
}

@Test
fun `should fix when with single entry`() {
    // language=kotlin
    val code = """
        |when (x) {
        |        else -> println("only")
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (x) {
            |    else -> println("only")
            |}
            """.trimMargin()
        )
}

@Test
fun `should not fix when without subject`() {
    // language=kotlin
    val code = """
        |when {
        |    x > 0 -> println("positive")
        |    else -> println("non-positive")
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should fix when with multiline entry body`() {
    // language=kotlin
    val code = """
        |when (x) {
        |        1 -> {
        |            println("one")
        |            println("uno")
        |        }
        |        else -> println("other")
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (x) {
            |    1 -> {
            |        println("one")
            |        println("uno")
            |    }
            |    else -> println("other")
            |}
            """.trimMargin()
        )
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.formatting.FixWhenBodyIndentRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/FixWhenBodyIndentRuleTest.kt
git commit -m "test: add edge case tests for FixWhenBodyIndentRule"
```

---

### Task 12: Add tests for ExpandLongLambdaRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongLambdaRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should not expand lambda under hard max line length`() {
    // language=kotlin
    val code = """
        |val x = listOf(1, 2, 3).map { it.toString() }
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not expand lambda already on multiple lines`() {
    // language=kotlin
    val code = """
        |val x = listOf(1, 2, 3).map {
        |    it.toString()
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should expand lambda with arrow and parameter`() {
    // language=kotlin
    val code = """
        |val x = listOf(1, 2, 3).associateWith { value -> "This is a very long string that will definitely make this line exceed the hard maximum line length of one hundred and sixty characters total" }
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(1, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = listOf(1, 2, 3).associateWith { value ->
            |    "This is a very long string that will definitely make this line exceed the hard maximum line length of one hundred and sixty characters total"
            |}
            """.trimMargin()
        )
}

@Test
fun `should expand nested lambda exceeding limit`() {
    // language=kotlin
    val code = """
        |val x = listOf(1, 2, 3).map { listOf(it).filter { inner -> inner.toString().length > 0 && inner.toString().contains("very long condition to exceed max") } }
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(1, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = listOf(1, 2, 3).map {
            |    listOf(it).filter { inner -> inner.toString().length > 0 && inner.toString().contains("very long condition to exceed max") }
            |}
            """.trimMargin()
        )
}

@Test
fun `should expand lambda with destructuring declaration`() {
    // language=kotlin
    val code = """
        |val x = mapOf("a" to 1, "b" to 2).entries.forEach { (key, value) -> println("Key: ${'\$'}key, Value: ${'\$'}value - this is a very long line that will definitely exceed the hard maximum line length limit") }
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(1, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = mapOf("a" to 1, "b" to 2).entries.forEach { (key, value) ->
            |    println("Key: ${'\$'}key, Value: ${'\$'}value - this is a very long line that will definitely exceed the hard maximum line length limit")
            |}
            """.trimMargin()
        )
}

@Test
fun `should expand lambda in chained call exceeding limit`() {
    // language=kotlin
    val code = """
        |val x = listOf(1, 2, 3).filter { it > 0 }.map { it.toString() }.joinToString { "item: ${'\$'}it - some additional text to make this line very long and exceed the hard maximum line length" }
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(1, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = listOf(1, 2, 3).filter { it > 0 }.map { it.toString() }.joinToString {
            |    "item: ${'\$'}it - some additional text to make this line very long and exceed the hard maximum line length"
            |}
            """.trimMargin()
        )
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.formatting.ExpandLongLambdaRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/ExpandLongLambdaRuleTest.kt
git commit -m "test: add edge case tests for ExpandLongLambdaRule"
```

---

### Task 13: Add tests for NoBlankLineBetweenWhenEntriesRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/blankline/NoBlankLineBetweenWhenEntriesRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should remove blank line between single-line entries with else`() {
    // language=kotlin
    val code = """
        |val x = when (y) {
        |    1 -> "one"
        |
        |    else -> "other"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = when (y) {
            |    1 -> "one"
            |    else -> "other"
            |}
            """.trimMargin()
        )
}

@Test
fun `should remove multiple blank lines between entries`() {
    // language=kotlin
    val code = """
        |val x = when (y) {
        |    1 -> "one"
        |
        |
        |    2 -> "two"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val x = when (y) {
            |    1 -> "one"
            |    2 -> "two"
            |}
            """.trimMargin()
        )
}

@Test
fun `should not remove blank line between entries with block body`() {
    // language=kotlin
    val code = """
        |when (y) {
        |    1 -> {
        |        println("one")
        |        println("uno")
        |    }
        |
        |    2 -> println("two")
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should remove blank lines in when without subject`() {
    // language=kotlin
    val code = """
        |when {
        |    x > 0 -> println("positive")
        |
        |    x < 0 -> println("negative")
        |
        |    else -> println("zero")
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when {
            |    x > 0 -> println("positive")
            |    x < 0 -> println("negative")
            |    else -> println("zero")
            |}
            """.trimMargin()
        )
}

@Test
fun `should not report when no blank lines exist`() {
    // language=kotlin
    val code = """
        |when (x) {
        |    1 -> "one"
        |    2 -> "two"
        |    3 -> "three"
        |    else -> "other"
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should handle when with is-check entries`() {
    // language=kotlin
    val code = """
        |when (obj) {
        |    is String -> println("string")
        |
        |    is Int -> println("int")
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (obj) {
            |    is String -> println("string")
            |    is Int -> println("int")
            |}
            """.trimMargin()
        )
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.blankline.NoBlankLineBetweenWhenEntriesRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/blankline/NoBlankLineBetweenWhenEntriesRuleTest.kt
git commit -m "test: add edge case tests for NoBlankLineBetweenWhenEntriesRule"
```

---

### Task 14: Add tests for CollapseSupertypeListRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseSupertypeListRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should collapse single generic supertype`() {
    // language=kotlin
    val code = """
        |class Foo
        |    : Comparable<Foo> {
        |    override fun compareTo(other: Foo): Int = 0
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 5, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo : Comparable<Foo> {
            |    override fun compareTo(other: Foo): Int = 0
            |}
            """.trimMargin()
        )
}

@Test
fun `should not collapse when supertype list with generics exceeds collapse max`() {
    // language=kotlin
    val code = """
        |class VeryLongClassNameForTestingPurposesThatTakesUpManyCharactersOnTheLine
        |    : AbstractSuperLongBaseClassNameWithGenerics<String, Int, List<Map<String, Any>>> {
        |    override fun toString(): String = ""
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should collapse multiple interfaces`() {
    // language=kotlin
    val code = """
        |class Foo
        |    : Serializable, Comparable<Foo> {
        |    override fun compareTo(other: Foo): Int = 0
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 5, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo : Serializable, Comparable<Foo> {
            |    override fun compareTo(other: Foo): Int = 0
            |}
            """.trimMargin()
        )
}

@Test
fun `should collapse supertype with delegation`() {
    // language=kotlin
    val code = """
        |class Foo(list: List<Int>)
        |    : List<Int> by list
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 5, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo(list: List<Int>) : List<Int> by list
            """.trimMargin()
        )
}

@Test
fun `should not collapse when already on same line`() {
    // language=kotlin
    val code = """
        |class Foo : Serializable
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.collapse.CollapseSupertypeListRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseSupertypeListRuleTest.kt
git commit -m "test: add edge case tests for CollapseSupertypeListRule"
```

---

### Task 15: Add tests for AlignWhenBranchArrowRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/AlignWhenBranchArrowRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should align arrows with is-checks of different length`() {
    // language=kotlin
    val code = """
        |when (obj) {
        |    is String -> println("string")
        |    is Int -> println("int")
        |    is LongClassName -> println("long")
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (obj) {
            |    is String        -> println("string")
            |    is Int           -> println("int")
            |    is LongClassName -> println("long")
            |}
            """.trimMargin()
        )
}

@Test
fun `should align arrows with range checks`() {
    // language=kotlin
    val code = """
        |when (x) {
        |    in 1..10 -> "small"
        |    in 11..100 -> "medium"
        |    else -> "large"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (x) {
            |    in 1..10   -> "small"
            |    in 11..100 -> "medium"
            |    else       -> "large"
            |}
            """.trimMargin()
        )
}

@Test
fun `should not align when single entry`() {
    // language=kotlin
    val code = """
        |when (x) {
        |    else -> "only"
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should align enum value checks`() {
    // language=kotlin
    val code = """
        |when (color) {
        |    Color.RED -> "red"
        |    Color.GREEN -> "green"
        |    Color.BLUE -> "blue"
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |when (color) {
            |    Color.RED   -> "red"
            |    Color.GREEN -> "green"
            |    Color.BLUE  -> "blue"
            |}
            """.trimMargin()
        )
}

@Test
fun `should not align already aligned arrows`() {
    // language=kotlin
    val code = """
        |when (x) {
        |    1    -> "one"
        |    2    -> "two"
        |    else -> "other"
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.formatting.AlignWhenBranchArrowRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/AlignWhenBranchArrowRuleTest.kt
git commit -m "test: add edge case tests for AlignWhenBranchArrowRule"
```

---

### Task 16: Add tests for NoTrailingCommaRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/NoTrailingCommaRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should remove trailing comma in destructuring declaration`() {
    // language=kotlin
    val code = """
        |val (x, y,) = pair
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(1, 11, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val (x, y) = pair
            """.trimMargin()
        )
}

@Test
fun `should remove trailing comma in enum entries`() {
    // language=kotlin
    val code = """
        |enum class Color {
        |    RED,
        |    GREEN,
        |    BLUE,
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(4, 9, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |enum class Color {
            |    RED,
            |    GREEN,
            |    BLUE
            |}
            """.trimMargin()
        )
}

@Test
fun `should remove trailing comma in type parameters`() {
    // language=kotlin
    val code = """
        |class Foo<
        |    T : Any,
        |    U : Comparable<U>,
        |>
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 23, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |class Foo<
            |    T : Any,
            |    U : Comparable<U>
            |>
            """.trimMargin()
        )
}

@Test
fun `should remove trailing comma in annotation parameters`() {
    // language=kotlin
    val code = """
        |@Suppress(
        |    "unused",
        |    "unchecked",
        |)
        |class Foo
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(3, 16, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |@Suppress(
            |    "unused",
            |    "unchecked"
            |)
            |class Foo
            """.trimMargin()
        )
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.formatting.NoTrailingCommaRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/NoTrailingCommaRuleTest.kt
git commit -m "test: add edge case tests for NoTrailingCommaRule"
```

---

### Task 17: Add tests for CollapseShortKdocRule

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseShortKdocRuleTest.kt`

- [ ] **Step 1: Add edge case tests**

```kotlin
@Test
fun `should not collapse kdoc with @param tag`() {
    // language=kotlin
    val code = """
        |/**
        | * @param x the value
        | */
        |fun foo(x: Int) {}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not collapse kdoc with @return tag`() {
    // language=kotlin
    val code = """
        |/**
        | * @return the result
        | */
        |fun foo(): Int = 1
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not collapse kdoc with @see tag`() {
    // language=kotlin
    val code = """
        |/**
        | * @see OtherClass
        | */
        |class Foo
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}

@Test
fun `should not collapse already single-line kdoc`() {
    // language=kotlin
    val code = """
        |/** Short description. */
        |fun foo() {}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.collapse.CollapseShortKdocRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseShortKdocRuleTest.kt
git commit -m "test: add edge case tests for CollapseShortKdocRule"
```

---

### Task 18: Add tests for remaining thin rules (CollapseEnumEntriesRule, ChainAfterLambdaIndentRule)

**Files:**
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseEnumEntriesRuleTest.kt`
- Modify: `zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/ChainAfterLambdaIndentRuleTest.kt`

- [ ] **Step 1: Add tests for CollapseEnumEntriesRule**

```kotlin
@Test
fun `should collapse enum implementing interface`() {
    // language=kotlin
    val code = """
        |enum class Color : Serializable {
        |    RED,
        |    GREEN,
        |    BLUE
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(2, 5, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |enum class Color : Serializable {
            |    RED, GREEN, BLUE
            |}
            """.trimMargin()
        )
}

@Test
fun `should not collapse enum with constructor args on entries`() {
    // language=kotlin
    val code = """
        |enum class Color(val rgb: Int) {
        |    RED(0xFF0000),
        |    GREEN(0x00FF00),
        |    BLUE(0x0000FF)
        |}
    """.trimMargin()
    ruleAssertThat(code).hasNoLintViolations()
}
```

- [ ] **Step 2: Add tests for ChainAfterLambdaIndentRule**

```kotlin
@Test
fun `should fix deeply nested chain after lambda`() {
    // language=kotlin
    val code = """
        |fun foo() {
        |    listOf(1, 2, 3)
        |        .filter {
        |            it > 0
        |        }
        |            .map { it * 2 }
        |            .toList()
        |}
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(6, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |fun foo() {
            |    listOf(1, 2, 3)
            |        .filter {
            |            it > 0
            |        }
            |        .map { it * 2 }
            |        .toList()
            |}
            """.trimMargin()
        )
}

@Test
fun `should fix chain after lambda with parameters`() {
    // language=kotlin
    val code = """
        |val result = items
        |    .groupBy { item ->
        |        item.category
        |    }
        |        .mapValues { it.value.size }
    """.trimMargin()
    ruleAssertThat(code)
        .hasLintViolation(5, 1, violationMessage)
        .isFormattedAs(
            // language=kotlin
            """
            |val result = items
            |    .groupBy { item ->
            |        item.category
            |    }
            |    .mapValues { it.value.size }
            """.trimMargin()
        )
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew :zenhelix-ktlint-rules-core:test --tests "io.github.zenhelix.ktlint.rules.collapse.CollapseEnumEntriesRuleTest" --tests "io.github.zenhelix.ktlint.rules.formatting.ChainAfterLambdaIndentRuleTest" --no-build-cache`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/collapse/CollapseEnumEntriesRuleTest.kt \
       zenhelix-ktlint-rules-core/src/test/kotlin/io/github/zenhelix/ktlint/rules/formatting/ChainAfterLambdaIndentRuleTest.kt
git commit -m "test: add edge case tests for CollapseEnumEntriesRule and ChainAfterLambdaIndentRule"
```

---

## Phase 5: Final Verification

### Task 19: Run full build and verify everything works

- [ ] **Step 1: Run full build with tests**

Run: `./gradlew clean build --no-build-cache`
Expected: BUILD SUCCESSFUL with all tests passing

- [ ] **Step 2: Verify test count increased**

Run: `./gradlew test --no-build-cache 2>&1 | grep -E "tests? (completed|passed|failed)"`
Expected: Significantly more tests than before (~388 → ~460+)

- [ ] **Step 3: Commit any remaining changes**

If any files were missed, add and commit them.
