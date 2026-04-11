package io.github.zenhelix.ktlint.rules

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens

/** Common member declaration types used across ordering and blank-line rules. */
public val MEMBER_TYPES: TokenSet = TokenSet.create(
    KtNodeTypes.FUN,
    KtNodeTypes.PROPERTY,
    KtNodeTypes.CLASS,
    KtNodeTypes.OBJECT_DECLARATION,
    KtNodeTypes.SECONDARY_CONSTRUCTOR,
    KtNodeTypes.TYPEALIAS,
)

/** Same as [MEMBER_TYPES] but includes [KtNodeTypes.CLASS_INITIALIZER]. */
public val MEMBER_TYPES_WITH_INITIALIZER: TokenSet = TokenSet.create(
    KtNodeTypes.FUN,
    KtNodeTypes.PROPERTY,
    KtNodeTypes.CLASS,
    KtNodeTypes.OBJECT_DECLARATION,
    KtNodeTypes.SECONDARY_CONSTRUCTOR,
    KtNodeTypes.TYPEALIAS,
    KtNodeTypes.CLASS_INITIALIZER,
)

/** Returns true if this OBJECT_DECLARATION node has the `companion` keyword. */
public fun ASTNode.isCompanionObject(): Boolean = elementType == KtNodeTypes.OBJECT_DECLARATION &&
        findChildByType(KtNodeTypes.MODIFIER_LIST)
            ?.getChildren(null)
            ?.any { it.elementType == KtTokens.COMPANION_KEYWORD }
        ?: false

/** Returns true if this whitespace node contains a blank line (2+ newlines). */
public fun ASTNode.hasBlankLine(): Boolean = text.count { it == '\n' } >= 2

/**
 * Returns the text from the start of the current line up to (not including) this [ASTNode].
 * Walks left and up through the AST.
 */
public fun ASTNode.linePrefix(): String {
    val result = StringBuilder()
    var current: ASTNode? = this
    outer@ while (current != null) {
        var sibling = current.treePrev
        while (sibling != null) {
            val text = sibling.text
            val nlIndex = text.lastIndexOf('\n')
            if (nlIndex >= 0) {
                result.insert(0, text.substring(nlIndex + 1))
                break@outer
            }
            result.insert(0, text)
            sibling = sibling.treePrev
        }
        current = current.treeParent
    }
    return result.toString()
}

/** Returns the 0-based column of this [ASTNode] by walking left and up through the AST. */
public fun ASTNode.columnOf(): Int {
    var col = 0
    var current: ASTNode? = this
    while (current != null) {
        var sibling = current.treePrev
        while (sibling != null) {
            val text = sibling.text
            val nlIndex = text.lastIndexOf('\n')
            if (nlIndex >= 0) {
                return col + text.length - nlIndex - 1
            }
            col += text.length
            sibling = sibling.treePrev
        }
        current = current.treeParent
    }
    return col
}

/**
 * Estimates the length of text that would follow `)` on the same line after collapsing.
 * Walks up through parent nodes collecting tokens like `: SuperType`, `where T : Bound`,
 * stopping at `{` or genuine block boundaries.
 */
public fun ASTNode.suffixLengthAfterCollapse(): Int {
    var length = 0
    var current: ASTNode? = this
    while (current != null) {
        var sibling = current.treeNext
        while (sibling != null) {
            if (sibling.elementType == TokenType.WHITE_SPACE) {
                length += 1
            } else {
                val text = sibling.text
                if (text.startsWith("{")) return length
                val nlIndex = text.indexOf('\n')
                if (nlIndex >= 0) {
                    length += nlIndex
                    return length
                }
                length += text.length
            }
            sibling = sibling.treeNext
        }
        current = current.treeParent
    }
    return length
}

/**
 * Collapses whitespace inside a parenthesized list node:
 * - Removes whitespace after `(`
 * - Removes whitespace before `)`
 * - Collapses whitespace after `,` to a single space
 */
public fun ASTNode.collapseParenthesizedWhitespace() {
    getChildren(null)
        .filter { it.elementType == TokenType.WHITE_SPACE }
        .toList()
        .forEach { ws ->
            val prev = ws.treePrev
            val next = ws.treeNext
            when {
                prev?.elementType == KtTokens.LPAR  -> {
                    (ws as LeafPsiElement).rawReplaceWithText("")
                }

                next?.elementType == KtTokens.RPAR  -> {
                    (ws as LeafPsiElement).rawReplaceWithText("")
                }

                prev?.elementType == KtTokens.COMMA -> {
                    (ws as LeafPsiElement).rawReplaceWithText(" ")
                }
            }
        }
}

/**
 * Checks if this VALUE_PARAMETER_LIST belongs to a data class constructor.
 */
public fun ASTNode.isDataClassConstructor(): Boolean {
    val parent = treeParent ?: return false
    if (parent.elementType != KtNodeTypes.PRIMARY_CONSTRUCTOR) return false
    val classNode = parent.treeParent ?: return false
    if (classNode.elementType != KtNodeTypes.CLASS) return false
    val modifierList = classNode.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return false
    return modifierList.getChildren(null).any { it.text == "data" }
}

/** Checks if any VALUE_PARAMETER child has a KDoc comment. */
public fun ASTNode.hasDocumentedParameter(): Boolean =
    getChildren(null).any { child ->
        child.elementType == KtNodeTypes.VALUE_PARAMETER &&
                child.findChildByType(KDocTokens.KDOC) != null
    }

/** Checks if any VALUE_PARAMETER child has an annotation entry. */
public fun ASTNode.hasAnnotatedParameter(): Boolean =
    getChildren(PARAM_TOKEN_SET).any { param ->
        param.findChildByType(KtNodeTypes.MODIFIER_LIST)
            ?.getChildren(null)
            ?.any { it.elementType == KtNodeTypes.ANNOTATION_ENTRY }
            ?: false
    }

/** Checks if any VALUE_PARAMETER child has a block annotation (on its own line, not inline). */
public fun ASTNode.hasBlockAnnotatedParameter(): Boolean =
    getChildren(PARAM_TOKEN_SET).any { param ->
        val modifierList = param.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return@any false
        val hasAnnotation = modifierList.getChildren(null).any { it.elementType == KtNodeTypes.ANNOTATION_ENTRY }
        if (!hasAnnotation) return@any false
        // Block annotation: newline within modifier list (multiple annotations) or between modifier list and val keyword
        modifierList.text.contains('\n') ||
            modifierList.treeNext?.let { it.elementType == TokenType.WHITE_SPACE && it.text.contains('\n') } == true
    }

public val PARAM_TOKEN_SET: TokenSet = TokenSet.create(KtNodeTypes.VALUE_PARAMETER)

/** Standard indent (4 spaces). */
public const val INDENT: String = "    "

/** Returns the leading whitespace (spaces/tabs) of the line containing this node. */
public fun ASTNode.lineIndent(): String = linePrefix().takeWhile { it == ' ' || it == '\t' }

/**
 * Checks whether this parenthesized list (collapsed to [collapsedText]) would fit
 * on one line within [maxLineLength], accounting for column offset and suffix after `)`.
 */
public fun ASTNode.fitsOnOneLine(collapsedText: String, maxLineLength: Int): Boolean {
    val column = columnOf()
    val suffixLength = suffixLengthAfterCollapse()
    return column + collapsedText.length + suffixLength <= maxLineLength
}

/** Returns true if any whitespace child after a comma contains a newline. */
public fun ASTNode.hasNewlineAfterComma(): Boolean = getChildren(null).any { child ->
    child.elementType == TokenType.WHITE_SPACE &&
            child.treePrev?.elementType == KtTokens.COMMA &&
            child.text.contains('\n')
}

/** Returns true if any whitespace child contains a newline. */
public fun ASTNode.hasNewlineInChildren(): Boolean = getChildren(null).any { child ->
    child.elementType == TokenType.WHITE_SPACE &&
            child.text.contains('\n')
}

/**
 * Returns true if whitespace nodes after commas have a mix of newlines and no-newlines,
 * indicating manual grouping of items.
 */
public fun ASTNode.hasPartialNewlinesAfterComma(): Boolean {
    val interItemWhitespace = getChildren(null).filter { ws ->
        ws.elementType == TokenType.WHITE_SPACE &&
                ws.treePrev?.elementType == KtTokens.COMMA
    }
    if (interItemWhitespace.isEmpty()) return false
    val withNewlines = interItemWhitespace.count { it.text.contains('\n') }
    return withNewlines > 0 && withNewlines < interItemWhitespace.size
}

/**
 * Checks if this node belongs to an enum class.
 * Works for CLASS_BODY, PRIMARY_CONSTRUCTOR, and VALUE_PARAMETER_LIST nodes.
 */
public fun ASTNode.isEnumClass(): Boolean {
    val classNode = when (elementType) {
        KtNodeTypes.CLASS_BODY           -> treeParent
        KtNodeTypes.PRIMARY_CONSTRUCTOR  -> treeParent
        KtNodeTypes.VALUE_PARAMETER_LIST -> treeParent?.takeIf {
            it.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR
        }?.treeParent

        else                             -> return false
    } ?: return false
    if (classNode.elementType != KtNodeTypes.CLASS) return false
    val modifierList = classNode.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return false
    return modifierList.getChildren(null).any { it.text == "enum" }
}

/** Returns true if the condition part of this WHEN_ENTRY (everything before `->`) contains a newline. */
public fun ASTNode.hasMultilineConditionBeforeArrow(): Boolean {
    if (elementType != KtNodeTypes.WHEN_ENTRY) return false
    val arrow = findChildByType(KtTokens.ARROW) ?: return false
    var child = firstChildNode
    while (child != null && child != arrow) {
        if (child.text.contains('\n')) return true
        child = child.treeNext
    }
    return false
}

/** Collects all WHITE_SPACE leaf nodes in this subtree. If [requireNewline] is true, only nodes containing '\n' are included. */
public fun ASTNode.collectWhitespace(requireNewline: Boolean = false): List<ASTNode> {
    val result = mutableListOf<ASTNode>()

    fun walk(n: ASTNode) {
        if (n.elementType == TokenType.WHITE_SPACE && (!requireNewline || n.text.contains('\n'))) {
            result.add(n)
        }
        var child = n.firstChildNode
        while (child != null) {
            walk(child)
            child = child.treeNext
        }
    }
    walk(this)
    return result
}

/** Shifts the indent (after the last newline) of this WHITE_SPACE node by [shift] spaces. */
public fun ASTNode.shiftIndent(shift: Int) {
    val text = text
    val lastNl = text.lastIndexOf('\n')
    if (lastNl < 0) return
    val oldIndent = text.substring(lastNl + 1)
    val newLen = maxOf(0, oldIndent.length + shift)
    val newText = text.substring(0, lastNl + 1) + " ".repeat(newLen)
    if (newText != text) {
        (this as LeafPsiElement).rawReplaceWithText(newText)
    }
}

/** Replaces a whitespace node containing a blank line with a single newline + indent. */
public fun ASTNode.replaceBlankLineWithSingleNewline() {
    val indent = text.substringAfterLast('\n')
    (this as LeafPsiElement).rawReplaceWithText("\n$indent")
}

/**
 * Returns the ratio of blank lines between parameters (0.0 to 1.0).
 * Counts whitespace nodes after commas that contain blank lines vs total.
 * Returns 0.0 if there are no transitions (0 or 1 parameters).
 */
public fun ASTNode.blankLineRatioBetweenParams(): Double {
    val afterCommaWhitespace = getChildren(null).filter {
        it.elementType == TokenType.WHITE_SPACE &&
                it.treePrev?.elementType == KtTokens.COMMA
    }
    if (afterCommaWhitespace.isEmpty()) return 0.0
    val withBlankLine = afterCommaWhitespace.count { it.hasBlankLine() }
    return withBlankLine.toDouble() / afterCommaWhitespace.size
}

/** Finds the first child with BLOCK element type. */
public fun ASTNode.findBlock(): ASTNode? {
    var child = firstChildNode
    while (child != null) {
        if (child.elementType == KtNodeTypes.BLOCK) return child
        child = child.treeNext
    }
    return null
}

/**
 * Returns the single non-whitespace statement in this block node,
 * or null if there are zero or more than one statements.
 */
public fun ASTNode.singleStatementInBlock(): ASTNode? {
    val statements = mutableListOf<ASTNode>()
    var child = firstChildNode
    while (child != null) {
        if (child.elementType != TokenType.WHITE_SPACE) {
            statements.add(child)
        }
        child = child.treeNext
    }
    if (statements.size != 1) return null
    val statement = statements.first()
    if (statement.elementType == KtTokens.EOL_COMMENT || statement.elementType == KtTokens.BLOCK_COMMENT) return null
    return statement
}

/** Returns true if any direct child is a whitespace node containing a newline. */
public fun ASTNode.hasNewlineInDirectChildren(): Boolean {
    var child = firstChildNode
    while (child != null) {
        if (child.elementType == TokenType.WHITE_SPACE && child.text.contains('\n')) {
            return true
        }
        child = child.treeNext
    }
    return false
}

/** Returns text after this node on the same line (stops at newline), walking up the tree. */
public fun ASTNode.textAfterNodeOnSameLine(): String {
    val sb = StringBuilder()
    var current: ASTNode? = this
    while (current != null) {
        var sibling = current.treeNext
        while (sibling != null) {
            val text = sibling.text
            val nlIndex = text.indexOf('\n')
            if (nlIndex >= 0) {
                sb.append(text.substring(0, nlIndex))
                return sb.toString()
            }
            sb.append(text)
            sibling = sibling.treeNext
        }
        current = current.treeParent
    }
    return sb.toString()
}

/** Checks if this VALUE_PARAMETER_LIST belongs to a primary constructor. */
public fun ASTNode.isConstructorParameterList(): Boolean {
    val parent = treeParent ?: return false
    return parent.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR
}

/**
 * Checks if the function owning this VALUE_PARAMETER_LIST has no body
 * (no block `{ }` and no expression body `=`).
 * Navigates through PRIMARY_CONSTRUCTOR if needed.
 */
public fun ASTNode.isBodylessFunction(): Boolean {
    val funNode = treeParent?.let {
        if (it.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR) it.treeParent else it
    } ?: return false
    if (funNode.elementType != KtNodeTypes.FUN) return false
    return funNode.findChildByType(KtNodeTypes.BLOCK) == null &&
        funNode.findChildByType(KtTokens.EQ) == null
}

/**
 * Returns the simple name of this ANNOTATION_ENTRY node.
 * Handles both `@Foo("/path")` (with CONSTRUCTOR_CALLEE) and `@Foo` (bare TYPE_REFERENCE).
 */
public fun ASTNode.annotationSimpleName(): String? {
    val callee = findChildByType(KtNodeTypes.CONSTRUCTOR_CALLEE)
    if (callee != null) return callee.text
    return findChildByType(KtNodeTypes.TYPE_REFERENCE)?.text
}
