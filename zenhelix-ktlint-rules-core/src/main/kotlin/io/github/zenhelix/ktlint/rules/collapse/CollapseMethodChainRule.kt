package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collectWhitespace
import io.github.zenhelix.ktlint.rules.lineIndent
import io.github.zenhelix.ktlint.rules.linePrefix
import io.github.zenhelix.ktlint.rules.shiftIndent

/**
 * Collapses a single-step method chain where the `.method(` call starts on a new line
 * but can fit on the same line as the receiver.
 *
 * Before:
 * ```kotlin
 * orderSecurityService
 *     .validateBankOrderAccess(
 *         orderId,
 *         details.bankId,
 *     )
 * ```
 *
 * After:
 * ```kotlin
 * orderSecurityService.validateBankOrderAccess(
 *     orderId,
 *     details.bankId,
 * )
 * ```
 *
 * Skips chains where the receiver ends with `}` (lambda chains) —
 * those are handled by [ChainAfterLambdaIndentRule].
 */
public class CollapseMethodChainRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-method-chain"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.DOT_QUALIFIED_EXPRESSION &&
            node.elementType != KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) {
            return
        }

        val dot = node.findChildByType(KtTokens.DOT)
            ?: node.findChildByType(KtTokens.SAFE_ACCESS)
            ?: return

        val wsBefore = dot.treePrev ?: return
        if (wsBefore.elementType != TokenType.WHITE_SPACE) return
        if (!wsBefore.text.contains('\n')) return

        val receiver = wsBefore.treePrev ?: return

        // Skip if a line comment precedes the dot — collapsing would absorb code into the comment
        if (receiver.elementType == KtTokens.EOL_COMMENT) return

        // Skip if this is an expression body of a function — preserve builder pattern formatting
        // e.g. `fun foo(): T = dsl\n.fetchExists(...)` — the `= receiver\n.method()` is intentional
        if (isFunctionExpressionBody(node)) return

        // Skip lambda chains (receiver ends with `}`) — handled by ChainAfterLambdaIndentRule
        if (receiver.text.trimEnd().endsWith("}")) return

        // Skip multi-step chains: only collapse isolated `receiver.method()`, not `a.b.c()`
        if (isPartOfChain(node, receiver)) return

        // Skip if receiver itself is a DOT_QUALIFIED chain call (not a simple reference)
        // e.g. `a.b()\n.c()` — the receiver `a.b()` is a DQE, so this is a multi-step chain
        if (receiver.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION ||
            receiver.elementType == KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) {
            return
        }

        val afterDot = dot.treeNext ?: return

        // Skip if method call contains a lambda — preserves functional chain readability
        // e.g. `?.let { ... }`, `.apply { ... }`, `.map { ... }`
        if (afterDot.text.contains('{')) return

        // Calculate collapsed first line length
        val receiverPrefix = receiver.linePrefix()
        val receiverLastLine = receiver.text.substringAfterLast('\n', receiver.text)
        val dotText = dot.text // "." or "?."
        val methodFirstLine = afterDot.text.substringBefore('\n', afterDot.text)

        val collapsedLength = receiverPrefix.length + receiverLastLine.length + dotText.length + methodFirstLine.length
        if (collapsedLength > lineLengthSettings.hard) return

        val dotIndent = wsBefore.text.substringAfterLast('\n')
        val receiverIndent = receiver.lineIndent()
        val shift = receiverIndent.length - dotIndent.length

        emitAndCorrect(emit, dot.startOffset, "Method chain fits on one line with receiver") {
            // Collapse the whitespace before dot
            (wsBefore as LeafPsiElement).rawReplaceWithText("")

            // Reindent multiline content after the dot
            if (shift != 0) {
                node.collectWhitespace(requireNewline = true)
                    .filter { it.startOffset > dot.startOffset }
                    .forEach { it.shiftIndent(shift) }
            }
        }
    }

    /**
     * Checks if this DOT_QUALIFIED_EXPRESSION is the expression body of a function declaration.
     * e.g. `fun foo(): T = receiver\n.method(...)` — preserve the vertical builder pattern.
     */
    private fun isFunctionExpressionBody(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false
        if (parent.elementType != KtNodeTypes.FUN) return false
        var prev = node.treePrev
        while (prev != null) {
            if (prev.elementType == KtTokens.EQ) return true
            if (prev.elementType != TokenType.WHITE_SPACE) return false
            prev = prev.treePrev
        }
        return false
    }

    /**
     * Checks if this DOT_QUALIFIED_EXPRESSION is part of a longer method chain.
     * Returns true if the parent is also a DOT_QUALIFIED/SAFE_ACCESS expression
     * and this node is its receiver (left-hand side), meaning there are more chain steps after.
     */
    private fun isPartOfChain(node: ASTNode, receiver: ASTNode): Boolean {
        val parent = node.treeParent ?: return false
        if (parent.elementType != KtNodeTypes.DOT_QUALIFIED_EXPRESSION &&
            parent.elementType != KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) {
            return false
        }

        // Check if our node is the receiver (left side) of the parent chain
        // If so, there are more steps after us → we're in a chain
        val parentFirstChild = parent.firstChildNode
        return parentFirstChild === node
    }
}
