package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.PARAM_TOKEN_SET
import io.github.zenhelix.ktlint.rules.collapseParenthesizedWhitespace
import io.github.zenhelix.ktlint.rules.fitsOnOneLine
import io.github.zenhelix.ktlint.rules.hasBlockAnnotatedParameter
import io.github.zenhelix.ktlint.rules.hasDocumentedParameter
import io.github.zenhelix.ktlint.rules.hasNewlineInChildren
import io.github.zenhelix.ktlint.rules.hasPartialNewlinesAfterComma
import io.github.zenhelix.ktlint.rules.isBodylessFunction
import io.github.zenhelix.ktlint.rules.isDataClassConstructor
import io.github.zenhelix.ktlint.rules.isEnumClass

/**
 * Collapses multiline parameter lists to a single line when they fit within max line length.
 * Skips parameter lists where parameters have KDoc comments.
 */
private val FUNCTION_EXPRESSION_BODY_RULE_ID = RuleId("standard:function-expression-body")

public class CollapseParameterListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-parameter-list"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = FUNCTION_EXPRESSION_BODY_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.VALUE_PARAMETER_LIST) return
        if (node.isDataClassConstructor()) return
        if (node.isEnumClass()) return
        if (node.hasDocumentedParameter()) return
        if (node.hasBlockAnnotatedParameter()) return

        val params = node.getChildren(PARAM_TOKEN_SET)
        if (params.isEmpty()) return
        if (params.size >= 4) return
        if (!node.hasNewlineInChildren()) return
        if (params.size >= 2) {
            if (node.hasPartialNewlinesAfterComma()) return
            if (hasMixedModifiers(params)) return
        }

        val collapsedParams = params.joinToString(", ") { it.text.trim() }
        val collapsedText = "($collapsedParams)"

        val maxLength = when {
            hasFunctionalParameter(params) -> lineLengthSettings.collapseFunctional
            node.isBodylessFunction() -> lineLengthSettings.hard
            else -> lineLengthSettings.collapse
        }
        if (!node.fitsOnOneLine(collapsedText, maxLength)) return

        emitAndCorrect(emit, node.startOffset, "Parameter list fits on a single line") {
            node.collapseParenthesizedWhitespace()
        }
    }

    private fun hasFunctionalParameter(params: Array<ASTNode>): Boolean =
        params.any { it.text.contains("->") }

    private fun hasMixedModifiers(params: Array<ASTNode>): Boolean {
        if (params.size < 2) return false
        val signatures = params.map { paramModifierSignature(it) }.toSet()
        return signatures.size > 1
    }

    private fun paramModifierSignature(param: ASTNode): String {
        val parts = mutableListOf<String>()

        // Visibility and other modifiers from MODIFIER_LIST
        val modifierList = param.findChildByType(KtNodeTypes.MODIFIER_LIST)
        if (modifierList != null) {
            modifierList.getChildren(null)
                .filter { it.elementType in MODIFIER_KEYWORDS }
                .forEach { parts.add(it.text) }
        }

        // val/var are direct children of VALUE_PARAMETER, not in MODIFIER_LIST
        if (param.findChildByType(KtTokens.VAL_KEYWORD) != null) parts.add("val")
        if (param.findChildByType(KtTokens.VAR_KEYWORD) != null) parts.add("var")

        return parts.joinToString(" ")
    }

    private companion object {

        val MODIFIER_KEYWORDS: TokenSet = TokenSet.create(
            KtTokens.PUBLIC_KEYWORD,
            KtTokens.PRIVATE_KEYWORD,
            KtTokens.INTERNAL_KEYWORD,
            KtTokens.PROTECTED_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD,
            KtTokens.VARARG_KEYWORD,
        )
    }
}
