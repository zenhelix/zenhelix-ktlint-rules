package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule

/**
 * Enforces curly braces around single-statement `if` bodies when there is no `else` clause.
 * The built-in `if-else-bracing` rule only handles `if-else` pairs.
 *
 * Before:
 * ```kotlin
 * if (!x.isNullOrEmpty()) return x
 * ```
 *
 * After:
 * ```kotlin
 * if (!x.isNullOrEmpty()) {
 *     return x
 * }
 * ```
 */
public class IfBracesRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:if-braces"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.IF) return

        // Skip if there's an else clause — handled by built-in if-else-bracing rule
        if (node.findChildByType(KtTokens.ELSE_KEYWORD) != null) return

        val then = node.findChildByType(KtNodeTypes.THEN) ?: return

        // Skip if then body is already a block
        if (then.findChildByType(KtNodeTypes.BLOCK) != null) return

        // Get the actual then body (skip leading whitespace)
        val thenBody = then.firstChildNode?.let { first ->
            if (first.elementType == TokenType.WHITE_SPACE) first.treeNext else first
        } ?: return

        // Skip if the body itself is another if (nested if without else — would be handled recursively)
        if (thenBody.elementType == KtNodeTypes.IF) return

        // Skip if used as an expression (e.g., val x = if (...) y  or  return if (...) y)
        val parent = node.treeParent
        if (parent?.elementType == KtNodeTypes.PROPERTY) return
        if (parent?.elementType == KtNodeTypes.BINARY_EXPRESSION) return
        if (parent?.elementType == KtNodeTypes.RETURN) return
        if (parent?.elementType == KtNodeTypes.VALUE_ARGUMENT) return

        emit(node.startOffset, "Missing braces around if body", false)
    }
}
