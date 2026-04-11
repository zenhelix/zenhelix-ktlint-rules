# zenhelix-ktlint-rules

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zenhelix/zenhelix-ktlint-rules-core)](https://central.sonatype.com/artifact/io.github.zenhelix/zenhelix-ktlint-rules-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Opinionated KtLint rule set for compact, readable Kotlin code that ktlint doesn't enforce out of the box.

## Philosophy

These rules target the gap between what ktlint provides and what a consistent, dense codebase needs:

- **Collapse** — fold short constructs onto one line when they fit (parameters, lambdas, KDoc, enums, etc.)
- **Ordering** — enforce deterministic member order (visibility, properties before functions, companion last)
- **Blank lines** — remove noise between similar declarations, add breathing room in documented parameter lists
- **Formatting** — align when-arrows, enforce braces, remove trailing commas, shorten qualified names

All rules have autocorrect. Line length limits are hardcoded: standard=120, collapse=130, hard=160.

## Installation

### Gradle (ktlint-gradle plugin)

```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "<version>"
}

dependencies {
    ktlintRuleSet("io.github.zenhelix:zenhelix-ktlint-rules-core:<version>")
    // Optional: Spring rules
    ktlintRuleSet("io.github.zenhelix:zenhelix-ktlint-rules-spring:<version>")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.zenhelix</groupId>
    <artifactId>zenhelix-ktlint-rules-core</artifactId>
    <version>${zenhelix-ktlint-rules.version}</version>
</dependency>
```

### KtLint CLI

```bash
ktlint --ruleset=io.github.zenhelix:zenhelix-ktlint-rules-core:<version>
```

## Rules Overview

| # | Rule ID | Category | Description |
|---|---------|----------|-------------|
| 1 | `zenhelix:blank-line-in-documented-parameter-list` | Blank Line | Adds blank lines around documented/annotated parameter lists |
| 2 | `zenhelix:blank-line-inside-class-body` | Blank Line | Enforces blank lines after `{` and before `}` in mixed class bodies |
| 3 | `zenhelix:no-blank-line-between-consecutive-properties` | Blank Line | Removes blank lines between properties with same modifier signature |
| 4 | `zenhelix:no-blank-line-between-similar-declarations` | Blank Line | Removes blank lines between small same-kind declarations |
| 5 | `zenhelix:no-blank-line-between-when-entries` | Blank Line | Removes blank lines between single-line when entries |
| 6 | `zenhelix:no-blank-line-in-undocumented-parameter-list` | Blank Line | Removes blank lines in parameter lists without KDoc |
| 7 | `zenhelix:collapse-argument-list` | Collapse | Collapses short argument lists to one line |
| 8 | `zenhelix:collapse-constructor-annotation` | Collapse | Keeps constructor annotations on same line as class |
| 9 | `zenhelix:collapse-enum-entries` | Collapse | Collapses simple enum entries to one line |
| 10 | `zenhelix:collapse-expression-body` | Collapse | Joins `=` with expression body when it fits |
| 11 | `zenhelix:collapse-if-condition` | Collapse | Collapses or restructures multiline if-conditions |
| 12 | `zenhelix:collapse-method-chain` | Collapse | Collapses single-step method chain to receiver line |
| 13 | `zenhelix:collapse-parameter-list` | Collapse | Collapses short parameter lists to one line |
| 14 | `zenhelix:collapse-short-kdoc` | Collapse | Collapses single-line KDoc to `/** ... */` |
| 15 | `zenhelix:collapse-short-lambda` | Collapse | Collapses short lambdas to one line |
| 16 | `zenhelix:collapse-supertype-list` | Collapse | Collapses supertype list onto class line |
| 17 | `zenhelix:collapse-when-entry` | Collapse | Removes unnecessary braces from when entry bodies |
| 18 | `zenhelix:align-when-branch-arrow` | Formatting | Aligns `->` arrows in when branches |
| 19 | `zenhelix:chain-after-lambda-indent` | Formatting | Fixes indent of chained calls after lambda `}` |
| 20 | `zenhelix:expand-long-lambda` | Formatting | Expands single-line lambdas exceeding hard limit |
| 21 | `zenhelix:expand-long-parameter-list` | Formatting | Expands parameter list when line exceeds limit |
| 22 | `zenhelix:fix-when-body-indent` | Formatting | Fixes indentation of when entries |
| 23 | `zenhelix:if-braces` | Formatting | Enforces braces on single-statement if without else |
| 24 | `zenhelix:no-trailing-comma` | Formatting | Removes trailing commas from all lists |
| 25 | `zenhelix:shorten-qualified-name` | Formatting | Replaces FQN with import + simple name |
| 26 | `zenhelix:companion-object-last` | Ordering | Companion object must be last in class |
| 27 | `zenhelix:init-block-before-function` | Ordering | Init blocks before functions |
| 28 | `zenhelix:no-member-interleaving` | Ordering | Groups same-category members together |
| 29 | `zenhelix:property-before-function` | Ordering | Properties declared before functions |
| 30 | `zenhelix:visibility-order` | Ordering | Members ordered public -> internal -> protected -> private |
| 31 | `zenhelix-spring:spring-endpoint-explicit-return-type` | Spring | Explicit return type on controller endpoints |

## Rules Reference

### Blank Line Rules

#### `zenhelix:blank-line-in-documented-parameter-list`

Adds blank lines after `(` and before `)` in constructor parameter lists that have KDoc comments or block annotations.

```kotlin
// Before
data class User(
    /** User name */
    val name: String,
    /** User age */
    val age: Int
)

// After
data class User(

    /** User name */
    val name: String,
    /** User age */
    val age: Int

)
```

#### `zenhelix:blank-line-inside-class-body`

Enforces blank lines after `{` and before `}` in class bodies with mixed member types. Removes them for compact bodies.

```kotlin
// Before
class MyClass {
    val name: String = ""

    fun doSomething() {}
}

// After
class MyClass {

    val name: String = ""

    fun doSomething() {}

}
```

#### `zenhelix:no-blank-line-between-consecutive-properties`

Removes blank lines between consecutive properties with the same modifier signature.

```kotlin
// Before
@PublishedApi internal var record: REC? = null

@PublishedApi internal var conflictFields: List<Field<*>> = emptyList()

// After
@PublishedApi internal var record: REC? = null
@PublishedApi internal var conflictFields: List<Field<*>> = emptyList()
```

#### `zenhelix:no-blank-line-between-similar-declarations`

Removes blank lines between consecutive small same-kind declarations (functions, classes, type aliases).

```kotlin
// Before
data class Tin(val tin: String) : TinOrPinfl(id = tin)

data class Pinfl(val pinfl: String) : TinOrPinfl(id = pinfl)

// After
data class Tin(val tin: String) : TinOrPinfl(id = tin)
data class Pinfl(val pinfl: String) : TinOrPinfl(id = pinfl)
```

#### `zenhelix:no-blank-line-between-when-entries`

Removes blank lines between single-line when entries.

```kotlin
// Before
when {
    condition1 -> result1

    condition2 -> result2

    else       -> default
}

// After
when {
    condition1 -> result1
    condition2 -> result2
    else       -> default
}
```

#### `zenhelix:no-blank-line-in-undocumented-parameter-list`

Removes blank lines in parameter lists without KDoc documentation.

```kotlin
// Before
fun process(

    param1: String,
    param2: Int

)

// After
fun process(
    param1: String,
    param2: Int
)
```

### Collapse Rules

#### `zenhelix:collapse-argument-list`

Collapses multiline argument lists to one line when they fit. Skips lists with 4+ arguments or lambdas.

```kotlin
// Before
header(
    HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()
)

// After
header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
```

#### `zenhelix:collapse-constructor-annotation`

Keeps constructor annotations on the same line as the class declaration when they fit.

```kotlin
// Before
public class Foo<T>
    @PublishedApi
    internal constructor()

// After
public class Foo<T> @PublishedApi internal constructor()
```

#### `zenhelix:collapse-enum-entries`

Collapses simple enum entries to one line (up to 5 entries, max 15 character names).

```kotlin
// Before
enum class Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST
}

// After
enum class Direction {
    NORTH, SOUTH, EAST, WEST
}
```

#### `zenhelix:collapse-expression-body`

Joins `=` with expression body on the same line when it fits.

```kotlin
// Before
fun foo(): Bar =
    doSomething(a, b)

// After
fun foo(): Bar = doSomething(a, b)
```

#### `zenhelix:collapse-if-condition`

Collapses or restructures multiline if-conditions for readability.

```kotlin
// Before
fun foo() = if (hasLength(
        arg
    ) && other
) {
    doSomething()
}

// After
fun foo() = if (hasLength(arg) && other) {
    doSomething()
}
```

#### `zenhelix:collapse-method-chain`

Collapses single-step method chain onto the receiver line.

```kotlin
// Before
orderSecurityService
    .validateBankOrderAccess(orderId, details.bankId)

// After
orderSecurityService.validateBankOrderAccess(orderId, details.bankId)
```

#### `zenhelix:collapse-parameter-list`

Collapses short parameter lists to one line. Skips data classes and lists with 4+ parameters.

```kotlin
// Before
fun foo(
    a: Int,
    b: String
) { }

// After
fun foo(a: Int, b: String) { }
```

#### `zenhelix:collapse-short-kdoc`

Collapses single-paragraph KDoc to `/** ... */`.

```kotlin
// Before
/**
 * Short description
 */

// After
/** Short description */
```

#### `zenhelix:collapse-short-lambda`

Collapses short single-expression lambdas to one line.

```kotlin
// Before
onError = { error ->
    throw error.toException()
}

// After
onError = { error -> throw error.toException() }
```

#### `zenhelix:collapse-supertype-list`

Collapses supertype list onto the class declaration line when it fits.

```kotlin
// Before
class ReaderException(val reasons: Set<ReaderErrorView>) :
    RuntimeException("Errors while read file")

// After
class ReaderException(val reasons: Set<ReaderErrorView>) : RuntimeException("Errors while read file")
```

#### `zenhelix:collapse-when-entry`

Removes unnecessary braces from single-expression when entry bodies.

```kotlin
// Before
is Success -> { doSomething() }

// After
is Success -> doSomething()
```

### Formatting Rules

#### `zenhelix:align-when-branch-arrow`

Aligns `->` arrows in when branches to the same column.

```kotlin
// Before
val x = when {
    condition1 -> "a"
    longerCondition2 -> "b"
    else -> "c"
}

// After
val x = when {
    condition1       -> "a"
    longerCondition2 -> "b"
    else             -> "c"
}
```

#### `zenhelix:chain-after-lambda-indent`

Fixes indent of chained calls after lambda closing brace.

```kotlin
// Before
post().uri {
    ...
}
    .header(AUTHORIZATION, authorization)

// After
post().uri {
    ...
}
.header(AUTHORIZATION, authorization)
```

#### `zenhelix:expand-long-lambda`

Expands single-line lambdas to multiple lines when exceeding hard limit (160 chars).

```kotlin
// Before (line > 160 chars)
onHttpError = { statusCode, _, meta -> HttpResult.failure(HttpError.UnexpectedError("HTTP $statusCode"), meta) }

// After
onHttpError = { statusCode, _, meta ->
    HttpResult.failure(HttpError.UnexpectedError("HTTP $statusCode"), meta)
}
```

#### `zenhelix:expand-long-parameter-list`

Expands parameter list to multiple lines when function signature exceeds line limit.

```kotlin
// Before (line > 160 chars)
public fun createVeryLongFunctionName(param1: VeryLongType1, param2: VeryLongType2): ReturnType = doSomething()

// After
public fun createVeryLongFunctionName(
    param1: VeryLongType1, param2: VeryLongType2
): ReturnType = doSomething()
```

#### `zenhelix:fix-when-body-indent`

Fixes indentation of when entries and closing brace.

```kotlin
// Before
when (x) {
is Foo -> doFoo()
is Bar -> doBar()
}

// After
when (x) {
    is Foo -> doFoo()
    is Bar -> doBar()
}
```

#### `zenhelix:if-braces`

Enforces braces on single-statement if without else clause.

```kotlin
// Before
if (!x.isNullOrEmpty()) return x

// After
if (!x.isNullOrEmpty()) {
    return x
}
```

#### `zenhelix:no-trailing-comma`

Removes trailing commas from all lists.

```kotlin
// Before
val list = listOf(
    1,
    2,
    3,
)

// After
val list = listOf(
    1,
    2,
    3
)
```

#### `zenhelix:shorten-qualified-name`

Replaces fully-qualified names with imports + simple names when there's no conflict.

```kotlin
// Before
fun foo(cb: io.github.resilience4j.circuitbreaker.CircuitBreaker)

// After
import io.github.resilience4j.circuitbreaker.CircuitBreaker

fun foo(cb: CircuitBreaker)
```

### Ordering Rules

#### `zenhelix:companion-object-last`

Companion object must appear after all other class members.

```kotlin
// Before
class MyClass {
    companion object { const val C = "" }
    val property: String = ""
}

// After
class MyClass {
    val property: String = ""
    companion object { const val C = "" }
}
```

#### `zenhelix:init-block-before-function`

Init blocks must appear before function declarations.

```kotlin
// Before
class MyClass {
    fun method() {}
    init { println("initialized") }
}

// After
class MyClass {
    init { println("initialized") }
    fun method() {}
}
```

#### `zenhelix:no-member-interleaving`

Same-category members must be grouped together.

```kotlin
// Before
class MyClass {
    val a: String = ""
    fun method1() {}
    val b: String = ""
}

// After
class MyClass {
    val a: String = ""
    val b: String = ""
    fun method1() {}
}
```

#### `zenhelix:property-before-function`

Property declarations must appear before functions.

```kotlin
// Before
class MyClass {
    fun doSomething() {}
    val name: String = ""
}

// After
class MyClass {
    val name: String = ""
    fun doSomething() {}
}
```

#### `zenhelix:visibility-order`

Members ordered by visibility: public -> internal -> protected -> private.

```kotlin
// Before
class MyClass {
    private val a: String = ""
    public val b: String = ""
}

// After
class MyClass {
    public val b: String = ""
    private val a: String = ""
}
```

### Spring Rules

#### `zenhelix-spring:spring-endpoint-explicit-return-type`

Public Spring controller endpoints must declare explicit return type.

```kotlin
// Before
@RestController
class UserController {
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long) = userService.find(id)
}

// After
@RestController
class UserController {
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): User = userService.find(id)
}
```

## Compatibility

| Dependency | Version |
|------------|---------|
| KtLint | 1.8.0+ |
| Kotlin | 2.1.20+ |
| JDK | 17+ |

## Integration Guide

### KtLint CLI

```bash
# Download the rule set JAR
curl -sSLO https://repo1.maven.org/maven2/io/github/zenhelix/zenhelix-ktlint-rules-core/<version>/zenhelix-ktlint-rules-core-<version>.jar

# Run with custom rules
ktlint --ruleset=zenhelix-ktlint-rules-core-<version>.jar "src/**/*.kt"

# Autocorrect
ktlint --ruleset=zenhelix-ktlint-rules-core-<version>.jar --format "src/**/*.kt"
```

### ktlint-gradle plugin

```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "<version>"
}

dependencies {
    ktlintRuleSet("io.github.zenhelix:zenhelix-ktlint-rules-core:<version>")
}

// All rules are enabled by default. To disable specific rules:
// .editorconfig
// [*.kt]
// ktlint_zenhelix_collapse-short-lambda = disabled
```

### IntelliJ IDEA (ktlint plugin)

1. Install the [ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) for IntelliJ IDEA
2. Add the rule set JAR to your project dependencies (as shown in Installation)
3. The plugin will automatically pick up custom rule sets from the classpath
4. Run inspections via **Code > Inspect Code** or on-the-fly

### Disabling Rules

Individual rules can be disabled via `.editorconfig`:

```ini
[*.kt]
# Disable a specific rule
ktlint_zenhelix_no-trailing-comma = disabled

# Disable entire rule set
ktlint_zenhelix = disabled

# Disable spring rules
ktlint_zenhelix-spring = disabled
```

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
