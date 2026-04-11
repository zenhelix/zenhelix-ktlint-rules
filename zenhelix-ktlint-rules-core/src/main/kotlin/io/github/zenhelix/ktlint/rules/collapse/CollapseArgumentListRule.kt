package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collapseParenthesizedWhitespace
import io.github.zenhelix.ktlint.rules.fitsOnOneLine
import io.github.zenhelix.ktlint.rules.hasNewlineInChildren
import io.github.zenhelix.ktlint.rules.hasPartialNewlinesAfterComma

/**
 * Collapses multiline argument lists in function calls to a single line
 * when they fit within max line length.
 *
 * Before:
 * ```kotlin
 * header(
 *     HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()
 * )
 * ```
 *
 * After:
 * ```kotlin
 * header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
 * ```
 */
public class CollapseArgumentListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-argument-list"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_ARGUMENT_LIST_WRAPPING_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.VALUE_ARGUMENT_LIST) return

        val args = node.getChildren(ARG_TOKEN_SET)
        if (args.isEmpty()) return
        if (args.size >= 4) return
        if (!node.hasNewlineInChildren()) return
        if (node.hasPartialNewlinesAfterComma()) return
        if (hasLambdaArg(args)) return
        if (allQualifiedReferences(args)) return

        val collapsedArgs = args.joinToString(", ") { it.text.trim() }
        val collapsedText = "($collapsedArgs)"

        // Single-argument lists have no readability benefit from wrapping,
        // so allow collapsing up to HARD_MAX. Multi-arg lists use COLLAPSE_MAX.
        val maxLength = if (args.size == 1) {
            lineLengthSettings.hard
        } else {
            lineLengthSettings.collapse
        }
        if (!node.fitsOnOneLine(collapsedText, maxLength)) return

        emitAndCorrect(emit, node.startOffset, "Argument list fits on a single line") {
            node.collapseParenthesizedWhitespace()
        }
    }

    /**
     * All args are constant/enum references like `Feature.VALUE` — a config list that reads better vertically.
     * Detects DOT_QUALIFIED_EXPRESSION ending with REFERENCE_EXPRESSION (not CALL_EXPRESSION).
     */
    private fun allQualifiedReferences(args: Array<ASTNode>): Boolean {
        if (args.size < 2) return false
        return args.all { arg -> isConstantReference(arg) }
    }

    private fun isConstantReference(arg: ASTNode): Boolean {
        val dotExpr = arg.getChildren(null)
            .firstOrNull { it.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION }
            ?: return false
        val lastChild = dotExpr.lastChildNode ?: return false
        return lastChild.elementType == KtNodeTypes.REFERENCE_EXPRESSION
    }

    private fun hasLambdaArg(args: Array<ASTNode>): Boolean = args.any { arg ->
            arg.getChildren(null).any {
                it.elementType == KtNodeTypes.LAMBDA_EXPRESSION
            }
        }

    private companion object {
        val STANDARD_ARGUMENT_LIST_WRAPPING_RULE_ID = RuleId("standard:argument-list-wrapping")
        val ARG_TOKEN_SET: TokenSet = TokenSet.create(KtNodeTypes.VALUE_ARGUMENT)
    }
}
