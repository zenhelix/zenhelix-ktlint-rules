package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.blankLineRatioBetweenParams
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.hasBlockAnnotatedParameter
import io.github.zenhelix.ktlint.rules.hasDocumentedParameter
import io.github.zenhelix.ktlint.rules.isConstructorParameterList
import io.github.zenhelix.ktlint.rules.isDataClassConstructor
import io.github.zenhelix.ktlint.rules.replaceBlankLineWithSingleNewline

/**
 * Removes blank lines after `(` and before `)` in constructor parameter lists
 * that do not have KDoc-documented parameters and are not visually grouped
 * (block annotations with high blank line ratio).
 */
public class NoBlankLineInUndocumentedParameterListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-blank-line-in-undocumented-parameter-list"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.VALUE_PARAMETER_LIST) return
        if (!node.isConstructorParameterList()) return
        if (node.hasDocumentedParameter()) return

        // Preserve blank lines only in data class constructors with sufficient visual grouping
        if (node.isDataClassConstructor() &&
            node.hasBlockAnnotatedParameter() &&
            node.blankLineRatioBetweenParams() >= BLANK_LINE_RATIO_THRESHOLD
        ) {
            return
        }

        val lpar = node.findChildByType(KtTokens.LPAR) ?: return
        val rpar = node.findChildByType(KtTokens.RPAR) ?: return

        checkAfterLpar(lpar, emit)
        checkBeforeRpar(rpar, emit)
    }

    private fun checkAfterLpar(
        lpar: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = lpar.treeNext ?: return
        if (next.elementType != TokenType.WHITE_SPACE) return
        if (!next.hasBlankLine()) return

        emitAndCorrect(emit, next.startOffset, "Unexpected blank line after '(' in parameter list") {
            next.replaceBlankLineWithSingleNewline()
        }
    }

    private fun checkBeforeRpar(
        rpar: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prev = rpar.treePrev ?: return
        if (prev.elementType != TokenType.WHITE_SPACE) return
        if (!prev.hasBlankLine()) return

        emitAndCorrect(emit, prev.startOffset, "Unexpected blank line before ')' in parameter list") {
            prev.replaceBlankLineWithSingleNewline()
        }
    }

    private companion object {
        const val BLANK_LINE_RATIO_THRESHOLD = 1.0 / 3.0
    }
}
