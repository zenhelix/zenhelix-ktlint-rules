package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.LineLengthSettings
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.columnOf
import io.github.zenhelix.ktlint.rules.findBlock
import io.github.zenhelix.ktlint.rules.hasNewlineInDirectChildren
import io.github.zenhelix.ktlint.rules.singleStatementInBlock
import io.github.zenhelix.ktlint.rules.suffixLengthAfterCollapse

/**
 * Collapses short multiline lambdas with a single expression to one line.
 *
 * Before:
 * ```kotlin
 * onError = { error ->
 *     throw error.toException()
 * }
 * ```
 *
 * After:
 * ```kotlin
 * onError = { error -> throw error.toException() }
 * ```
 *
 * Also handles parameterless lambdas:
 * ```kotlin
 * // Before:
 * run {
 *     doSomething()
 * }
 *
 * // After:
 * run { doSomething() }
 * ```
 */
public class CollapseShortLambdaRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-short-lambda"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.FUNCTION_LITERAL) return

        val lbrace = node.findChildByType(KtTokens.LBRACE) ?: return
        val rbrace = node.findChildByType(KtTokens.RBRACE) ?: return

        val block = node.findBlock() ?: return
        val statement = block.singleStatementInBlock() ?: return
        val statementText = statement.text.trim()
        if (statementText.contains('\n')) return
        if (statementText.contains('{')) return
        if (isTrailingLambdaAfterMultilineArgs(node)) return

        if (!node.hasNewlineInDirectChildren()) return

        val collapsedText = buildCollapsedText(node, lbrace, statementText)
        val column = lbrace.columnOf()
        val suffixLength = rbrace.suffixLengthAfterCollapse()
        if (column + collapsedText.length + suffixLength > LineLengthSettings.STANDARD_MAX_LINE_LENGTH) return

        emitAndCorrect(emit, node.startOffset, "Short lambda fits on a single line") {
            collapseWhitespace(node, lbrace, rbrace)
        }
    }

    /**
     * Trailing lambda after multiline argument list: `foo(\n...\n) { ... }`
     * Should not be collapsed — the `{` belongs on its own line after `)`.
     */
    private fun isTrailingLambdaAfterMultilineArgs(functionLiteral: ASTNode): Boolean {
        // FUNCTION_LITERAL -> LAMBDA_EXPRESSION -> (LAMBDA_ARGUMENT ->) CALL_EXPRESSION
        var current = functionLiteral.treeParent ?: return false
        if (current.elementType == KtNodeTypes.LAMBDA_EXPRESSION) {
            current = current.treeParent ?: return false
        }
        // May be wrapped in LAMBDA_ARGUMENT
        if (current.elementType.toString() == "LAMBDA_ARGUMENT") {
            current = current.treeParent ?: return false
        }
        if (current.elementType != KtNodeTypes.CALL_EXPRESSION) return false
        val argList = current.findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST) ?: return false
        return argList.text.contains('\n')
    }

    private fun buildCollapsedText(
        functionLiteral: ASTNode,
        lbrace: ASTNode,
        statementText: String,
    ): String {
        val sb = StringBuilder("{ ")
        val arrow = functionLiteral.findChildByType(KtTokens.ARROW)
        if (arrow != null) {
            var child = lbrace.treeNext
            while (child != null && child != arrow) {
                if (child.elementType != TokenType.WHITE_SPACE) {
                    sb.append(child.text)
                }
                child = child.treeNext
            }
            sb.append(" -> ")
        }
        sb.append(statementText)
        sb.append(" }")
        return sb.toString()
    }

    private fun collapseWhitespace(functionLiteral: ASTNode, lbrace: ASTNode, rbrace: ASTNode) {
        val toCollapse = mutableListOf<Pair<ASTNode, String>>()
        var child = functionLiteral.firstChildNode
        while (child != null) {
            if (child.elementType == TokenType.WHITE_SPACE && child.text.contains('\n')) {
                toCollapse.add(child to " ")
            }
            child = child.treeNext
        }
        toCollapse.forEach { (ws, replacement) ->
            (ws as LeafPsiElement).rawReplaceWithText(replacement)
        }
    }
}
