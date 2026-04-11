# EditorConfig Integration, Self-Sufficiency & Test Coverage

## Problem

The zenhelix-ktlint-rules library was extracted from a project where rules worked in conjunction with a comprehensive `.editorconfig` that disabled ~20 conflicting standard KtLint rules. Without that `.editorconfig`, the library has:

1. **Hardcoded line length limits** (`LineLengthSettings.kt`: 120/130/120/160) not configurable via `.editorconfig`
2. **Conflicts with standard rules** — trailing commas, wrapping, blank lines, indentation rules clash
3. **Thin test coverage** on 14 of 31 rules (4-8 tests each)
4. **No standalone validation** — no tests proving rules work correctly without `.editorconfig`

## Design

### 1. EditorConfig Integration

**Goal:** `max_line_length` from `.editorconfig` becomes the base parameter; all limits scale proportionally.

**Formula (percentage-based):**

| Limit | Formula | Default (M=160) |
|-------|---------|-----------------|
| HARD | M | 160 |
| COLLAPSE | ceil(M * 0.81) | 130 |
| STANDARD | floor(M * 0.75) | 120 |
| COLLAPSE_FUNCTIONAL | floor(M * 0.75) | 120 |

**Default:** If `max_line_length` is not set or `off` — use `160`.

**Changes:**

- `LineLengthSettings` — convert from `object` with constants to `data class` instantiated from `max_line_length`:

```kotlin
public data class LineLengthSettings(val maxLineLength: Int = 160) {
    val hard: Int = maxLineLength
    val collapse: Int = ceil(maxLineLength * 0.81).toInt()
    val standard: Int = (maxLineLength * 0.75).toInt()
    val collapseFunctional: Int = (maxLineLength * 0.75).toInt()
}
```

- `ZenhelixRule` — add `MAX_LINE_LENGTH_PROPERTY` to `usesEditorConfigProperties`, read value in `beforeFirstNode()`, expose computed `LineLengthSettings` to subclasses
- All rules using line length — switch from static constants to dynamic values from base class

### 2. Self-Sufficiency via RunAfterRule

**Goal:** Even if user forgets to disable a conflicting standard rule, zenhelix rule runs after it and corrects the result.

**Already implemented (6 rules):**
- `collapse-constructor-annotation` → after `standard:annotation`
- `collapse-parameter-list` → after `standard:function-expression-body`
- `no-blank-line-between-consecutive-properties` → after `standard:spacing-between-declarations-with-comments`
- `no-blank-line-between-similar-declarations` → after `standard:blank-line-before-declaration`
- `shorten-qualified-name` → after `standard:import-ordering`, `standard:no-unused-imports`

**To add (16 rules):**
- `no-trailing-comma` → after `standard:trailing-comma-on-declaration-site`, `standard:trailing-comma-on-call-site`
- `collapse-argument-list` → after `standard:argument-list-wrapping`
- `collapse-expression-body` → after `standard:function-expression-body`
- `collapse-method-chain` → after `standard:chain-method-continuation`
- `collapse-if-condition` → after `standard:multiline-expression-wrapping`
- `collapse-short-lambda` → after `standard:function-literal`
- `collapse-when-entry` → after `standard:when-entry-bracing`
- `collapse-supertype-list` → after `standard:wrapping`
- `expand-long-parameter-list` → after `standard:parameter-list-wrapping`
- `expand-long-lambda` → after `standard:function-literal`
- `blank-line-inside-class-body` → after `standard:no-empty-first-line-in-class-body`
- `blank-line-in-documented-parameter-list` → after `standard:no-blank-line-in-list`
- `no-blank-line-in-undocumented-parameter-list` → after `standard:no-blank-line-in-list`
- `no-blank-line-between-when-entries` → after `standard:blank-line-between-when-conditions`
- `chain-after-lambda-indent` → after `standard:indent`
- `fix-when-body-indent` → after `standard:indent`

All with `mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED`.

### 3. EditorConfig Template

File: `docs/editorconfig-template.txt` — ready-to-copy `.editorconfig` snippet.

Also documented in README "Getting Started" section.

**20 standard rules to disable:**

```ini
[*.{kt,kts}]
max_line_length = 160

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

### 4. Test Coverage Enhancement

#### 4.1 Strengthen thin tests (~78 new tests)

| Rule | Current | Add | Focus |
|------|---------|-----|-------|
| `InitBlockBeforeFunctionRule` | 4 | +8 | Multiple init blocks, init with companion, init in nested class, enum with init |
| `CollapseConstructorAnnotationRule` | 4 | +8 | Multiple annotations, annotations with params, generics, long annotations beyond HARD |
| `CompanionObjectLastRule` | 5 | +7 | Named companion, companion in enum, nested class, interface companion |
| `PropertyBeforeFunctionRule` | 5 | +7 | Delegated properties, abstract properties, extension properties in companion |
| `MemberInterleavingRule` | 5 | +7 | Interleaving with KDoc, annotations, in enum, in object |
| `FixWhenBodyIndentRule` | 5 | +7 | When as expression in return/assign, nested when, sealed class |
| `ExpandLongLambdaRule` | 6 | +6 | Nested lambdas, lambda with receiver, destructuring, chain after lambda |
| `NoBlankLineBetweenWhenEntriesRule` | 6 | +6 | Comments between entries, KDoc before when, block bodies with nested when |
| `CollapseSupertypeListRule` | 7 | +5 | Generic supertypes, delegation (by), multiple interfaces + class |
| `AlignWhenBranchArrowRule` | 7 | +5 | Unicode in conditions, enum entries, ranges, is-checks |
| `NoTrailingCommaRule` | 8 | +4 | Destructuring, enum entries, type parameters, annotation params |
| `CollapseShortKdocRule` | 8 | +4 | KDoc with @param/@return/@see, long single-line KDoc beyond limit |
| `CollapseEnumEntriesRule` | 13 | +2 | Enum with generics, enum implementing interface |
| `ChainAfterLambdaIndentRule` | 10 | +2 | Deeply nested chains, chain after lambda with parameters |

#### 4.2 Standalone integration tests

New test class `StandaloneRuleIntegrationTest`:
- All collapse rules use correct computed limits with default max_line_length (160)
- Expand rules trigger at correct thresholds
- Rules don't crash when max_line_length = off

#### 4.3 LineLengthSettings tests

`LineLengthSettingsTest` — verify proportional computation for max_line_length values: 120, 140, 160, 200, Int.MAX_VALUE (off).

## Out of Scope

- New rules
- Changing logic of existing rules
- Refactoring `AstNodeExtensions.kt`

## Risks

- Changing `ZenhelixRule` affects all 31 rules — careful with backward compatibility of `beforeFirstNode()`
- Rules that override `beforeFirstNode()` must call `super`
