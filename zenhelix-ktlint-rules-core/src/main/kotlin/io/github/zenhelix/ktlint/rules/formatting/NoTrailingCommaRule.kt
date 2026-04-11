package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule

/**
 * Removes trailing commas from all lists: parameter lists, argument lists,
 * destructuring declarations, collection literals, type parameter/argument lists,
 * when entries, etc.
 */
public class NoTrailingCommaRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-trailing-comma"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtTokens.COMMA) return

        val next = nextNonWhitespaceNode(node) ?: return

        if (next.elementType !in CLOSING_TOKENS) return

        emitAndCorrect(emit, node.startOffset, "Trailing comma is not allowed") {
            removeCommaAndSurroundingWhitespace(node)
        }
    }

    private fun nextNonWhitespaceNode(node: ASTNode): ASTNode? {
        var current = node.treeNext
        while (current != null && current.elementType == TokenType.WHITE_SPACE) {
            current = current.treeNext
        }
        return current
    }

    private fun removeCommaAndSurroundingWhitespace(comma: ASTNode) {
        val parent = comma.treeParent
        val next = comma.treeNext

        // Remove whitespace between comma and closing token
        if (next != null && next.elementType == TokenType.WHITE_SPACE) {
            val afterWhitespace = next.treeNext
            // Keep whitespace only if closing token is on a new line
            if (afterWhitespace != null && afterWhitespace.elementType in CLOSING_TOKENS) {
                if (!next.text.contains('\n')) {
                    parent.removeChild(next)
                }
            }
        }

        parent.removeChild(comma)
    }

    private companion object {

        val CLOSING_TOKENS: TokenSet = TokenSet.create(
            KtTokens.RPAR,
            KtTokens.RBRACKET,
            KtTokens.GT,
            KtNodeTypes.WHEN_ENTRY,
        )
    }
}
