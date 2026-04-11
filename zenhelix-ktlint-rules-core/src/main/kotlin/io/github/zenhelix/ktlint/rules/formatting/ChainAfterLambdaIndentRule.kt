package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.INDENT
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.lineIndent

/**
 * Fixes continuation indent for chained calls after lambda closing brace.
 *
 * Before (continuation indent — 12 spaces):
 * ```
 *     post().uri {
 *         ...
 *     }
 *         .header(AUTHORIZATION, authorization)
 *         .exchangeToHttpResult<...>()
 * ```
 *
 * After (aligned with the closing brace — 8 spaces):
 * ```
 *     post().uri {
 *         ...
 *     }
 *     .header(AUTHORIZATION, authorization)
 *     .exchangeToHttpResult<...>()
 * ```
 */
public class ChainAfterLambdaIndentRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:chain-after-lambda-indent"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_INDENT_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtTokens.DOT && node.elementType != KtTokens.SAFE_ACCESS) return

        val whitespace = node.treePrev ?: return
        if (whitespace.elementType != TokenType.WHITE_SPACE) return
        if (!whitespace.text.contains('\n')) return

        val reference = findIndentReference(whitespace) ?: return

        val expectedIndent = if (reference.text.contains('\n')) {
            // Multiline lambda: align with closing brace indent
            indentOfLastLine(reference)
        } else {
            // Inline lambda: use line indent + one indent level
            reference.lineIndent() + INDENT
        }

        val dotIndent = whitespace.text.substringAfterLast('\n')

        if (dotIndent == expectedIndent) return

        emitAndCorrect(emit, node.startOffset, "Chained call after lambda should be aligned with closing brace") {
            val newWhitespace = whitespace.text.substringBeforeLast('\n') + "\n" + expectedIndent
            (whitespace as LeafPsiElement).rawReplaceWithText(newWhitespace)
        }
    }

    private fun findIndentReference(whitespace: ASTNode): ASTNode? {
        var current = whitespace.treePrev
        while (current != null && current.elementType == TokenType.WHITE_SPACE) {
            current = current.treePrev
        }
        if (current == null) return null

        // Direct: follows a closing brace
        if (current.text.endsWith("}") || current.elementType == KtTokens.RBRACE) {
            return current
        }

        // Indirect: follows a call chain that itself comes from a closing brace.
        // Walk down the leftmost child of the DOT_QUALIFIED_EXPRESSION chain.
        if (current.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION ||
            current.elementType == KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) {
            val chainRoot = findChainRoot(current)
            if (chainRoot != null && (chainRoot.text.endsWith("}") || chainRoot.elementType == KtTokens.RBRACE)) {
                return chainRoot
            }
        }

        return null
    }

    private fun findChainRoot(expr: ASTNode): ASTNode? {
        var current: ASTNode = expr
        while (current.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION ||
            current.elementType == KtNodeTypes.SAFE_ACCESS_EXPRESSION
        ) {
            val left = current.firstChildNode ?: return null
            if (left.text.endsWith("}") || left.elementType == KtTokens.RBRACE) {
                return left
            }
            current = left
        }
        return null
    }

    /**
     * Returns the indent of the LAST line of [node]'s text.
     *
     * For multiline lambdas the last line is the closing `}`, so the result
     * is the indent of `}` — used to align subsequent chained calls with the brace.
     *
     * For inline lambdas (everything on one line, e.g. `.foo { bar() }`) the
     * last line IS the only line, so the result is the indent of that call —
     * used to keep subsequent chained calls at the same continuation indent.
     */
    private fun indentOfLastLine(node: ASTNode): String {
        val nodeText = node.text
        val lastNl = nodeText.lastIndexOf('\n')
        if (lastNl >= 0) {
            // Multiline node: take the leading whitespace of its last line.
            return nodeText.substring(lastNl + 1).takeWhile { it == ' ' || it == '\t' }
        }
        // Single-line node: fall back to walking the tree for the enclosing line indent.
        val result = StringBuilder()
        var current: ASTNode? = node
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
        return result.toString().takeWhile { it == ' ' || it == '\t' }
    }

    private companion object {
        val STANDARD_INDENT_RULE_ID = RuleId("standard:indent")
    }
}
