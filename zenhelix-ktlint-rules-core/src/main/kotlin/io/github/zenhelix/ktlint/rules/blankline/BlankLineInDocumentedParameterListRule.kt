package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.BLANK_LINE_RATIO_THRESHOLD
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.PARAM_TOKEN_SET
import io.github.zenhelix.ktlint.rules.blankLineRatioBetweenParams
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.hasBlockAnnotatedParameter
import io.github.zenhelix.ktlint.rules.hasDocumentedParameter
import io.github.zenhelix.ktlint.rules.isConstructorParameterList
import io.github.zenhelix.ktlint.rules.isDataClassConstructor

/**
 * When a constructor parameter list contains parameters with KDoc comments
 * or block annotations with sufficient visual separation, ensures blank lines
 * after `(` (and before `)` for KDoc or supertype) for visual clarity.
 *
 * Block annotation trigger requires all of:
 * - At least 2 parameters
 * - Has block annotations (on separate lines)
 * - >= 1/3 of transitions between params have blank lines
 */
public class BlankLineInDocumentedParameterListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:blank-line-in-documented-parameter-list"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.VALUE_PARAMETER_LIST) return
        if (!node.isConstructorParameterList()) return

        val hasKDoc = node.hasDocumentedParameter() && !node.isDataClassConstructor()
        val paramCount = node.getChildren(PARAM_TOKEN_SET).size
        val hasBlockAnnotation = node.isDataClassConstructor() &&
                paramCount >= MIN_PARAMS &&
                node.hasBlockAnnotatedParameter() &&
                node.blankLineRatioBetweenParams() >= BLANK_LINE_RATIO_THRESHOLD
        if (!hasKDoc && !hasBlockAnnotation) return

        val lpar = node.findChildByType(KtTokens.LPAR) ?: return

        checkAfterLpar(lpar, emit)

        if (hasKDoc || (hasBlockAnnotation && hasSuperTypeAfterConstructor(node))) {
            val rpar = node.findChildByType(KtTokens.RPAR) ?: return
            checkBeforeRpar(rpar, emit)
        }
    }

    /** Returns true if the class has a supertype list (`: SuperType`). */
    private fun hasSuperTypeAfterConstructor(parameterList: ASTNode): Boolean {
        val constructor = parameterList.treeParent ?: return false
        val classNode = constructor.treeParent ?: return false
        return classNode.findChildByType(KtNodeTypes.SUPER_TYPE_LIST) != null
    }

    private fun checkAfterLpar(
        lpar: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = lpar.treeNext ?: return
        if (next.elementType != TokenType.WHITE_SPACE) return
        if (next.hasBlankLine()) return

        emitAndCorrect(emit, next.startOffset, "Expected blank line after '(' in documented parameter list") {
            (next as LeafPsiElement).rawReplaceWithText("\n" + next.text)
        }
    }

    private fun checkBeforeRpar(
        rpar: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prev = rpar.treePrev ?: return
        if (prev.elementType != TokenType.WHITE_SPACE) return
        if (prev.hasBlankLine()) return

        emitAndCorrect(emit, prev.startOffset, "Expected blank line before ')' in documented parameter list") {
            val indent = prev.text.substringAfterLast('\n')
            (prev as LeafPsiElement).rawReplaceWithText("\n\n" + indent)
        }
    }

    private companion object {
        const val MIN_PARAMS = 2
    }
}
