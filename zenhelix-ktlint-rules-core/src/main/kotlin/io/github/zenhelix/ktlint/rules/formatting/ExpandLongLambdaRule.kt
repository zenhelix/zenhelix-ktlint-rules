package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.INDENT
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.findBlock
import io.github.zenhelix.ktlint.rules.hasNewlineInDirectChildren
import io.github.zenhelix.ktlint.rules.lineIndent
import io.github.zenhelix.ktlint.rules.linePrefix
import io.github.zenhelix.ktlint.rules.singleStatementInBlock
import io.github.zenhelix.ktlint.rules.textAfterNodeOnSameLine

/**
 * Expands single-line lambdas to multiple lines when the line exceeds hard max line length.
 *
 * Before (line > 160 chars):
 * ```kotlin
 * onHttpError = { statusCode, _, meta -> HttpResult.failure(HttpError.UnexpectedError("HTTP $statusCode"), meta) }
 * ```
 *
 * After:
 * ```kotlin
 * onHttpError = { statusCode, _, meta ->
 *     HttpResult.failure(HttpError.UnexpectedError("HTTP $statusCode"), meta)
 * }
 * ```
 */
public class ExpandLongLambdaRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:expand-long-lambda"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.FUNCTION_LITERAL) return
        if (node.hasNewlineInDirectChildren()) return

        val lbrace = node.findChildByType(KtTokens.LBRACE) ?: return
        val rbrace = node.findChildByType(KtTokens.RBRACE) ?: return
        val block = node.findBlock() ?: return
        if (block.singleStatementInBlock() == null) return

        val prefixLength = lbrace.linePrefix().length
        val lineSuffix = node.textAfterNodeOnSameLine()
        val lineLength = prefixLength + node.textLength + lineSuffix.length
        if (lineLength <= lineLengthSettings.hard) return

        val baseIndent = lbrace.lineIndent()
        val bodyIndent = baseIndent + INDENT

        emitAndCorrect(emit, node.startOffset, "Lambda body should be expanded when line exceeds max length") {
            expandLambda(node, lbrace, rbrace, bodyIndent, baseIndent)
        }
    }

    private fun expandLambda(
        functionLiteral: ASTNode,
        lbrace: ASTNode,
        rbrace: ASTNode,
        bodyIndent: String,
        baseIndent: String,
    ) {
        val arrow = functionLiteral.findChildByType(KtTokens.ARROW)
        val anchor = arrow ?: lbrace
        val wsAfterAnchor = anchor.treeNext
        if (wsAfterAnchor != null && wsAfterAnchor.elementType == TokenType.WHITE_SPACE) {
            (wsAfterAnchor as LeafPsiElement).rawReplaceWithText("\n$bodyIndent")
        }

        val wsBeforeRbrace = rbrace.treePrev
        if (wsBeforeRbrace != null && wsBeforeRbrace.elementType == TokenType.WHITE_SPACE) {
            (wsBeforeRbrace as LeafPsiElement).rawReplaceWithText("\n$baseIndent")
        }
    }

}
