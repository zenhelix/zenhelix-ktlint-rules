package io.github.zenhelix.ktlint.rules.collapse

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
import io.github.zenhelix.ktlint.rules.WHITESPACE_REGEX
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collectWhitespace
import io.github.zenhelix.ktlint.rules.lineIndent
import io.github.zenhelix.ktlint.rules.linePrefix
import io.github.zenhelix.ktlint.rules.shiftIndent

/**
 * When a function expression body starts on a new line after `=`,
 * collapses it onto the same line if the first line of the expression fits.
 *
 * Before:
 * ```
 * fun foo(): Bar =
 *     doSomething(a, b)
 * ```
 *
 * After:
 * ```
 * fun foo(): Bar = doSomething(a, b)
 * ```
 *
 * For multiline expressions, collapses only the `=` with the first line:
 * ```
 * // Before:
 * fun foo(): Bar =
 *     doSomething(
 *         a, b
 *     )
 *
 * // After:
 * fun foo(): Bar = doSomething(
 *     a, b
 * )
 * ```
 */
public class CollapseExpressionBodyRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-expression-body"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_FUNCTION_EXPRESSION_BODY_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.FUN && node.elementType != KtNodeTypes.PROPERTY) return

        val eq = node.findChildByType(KtTokens.EQ) ?: return
        val wsAfterEq = eq.treeNext ?: return
        if (wsAfterEq.elementType != TokenType.WHITE_SPACE) return

        if (wsAfterEq.text.contains('\n')) {
            collapseEqToSameLine(node, eq, wsAfterEq, emit)
        } else {
            fixIndentAfterExternalCollapse(node, eq, wsAfterEq, emit)
        }
    }

    private fun collapseEqToSameLine(
        node: ASTNode,
        eq: ASTNode,
        wsAfterEq: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val expression = wsAfterEq.treeNext ?: return

        val eqLinePrefix = eq.linePrefix()
        val isMultilineExpression = expression.text.contains('\n')
        val maxLength = lineLengthSettings.hard

        // Try 1: collapse ENTIRE multiline expression to one line (e.g., elvis `expr ?: fallback`)
        if (isMultilineExpression && tryCollapseEntireExpression(eqLinePrefix, expression, wsAfterEq, emit)) {
            return
        }

        // Try 2: collapse just `=` to same line as first line of expression
        val expressionFirstLine = expression.text.substringBefore('\n')
        val collapsedLength = eqLinePrefix.length + "= ".length + expressionFirstLine.length
        if (collapsedLength > maxLength) return

        emitAndCorrect(emit, wsAfterEq.startOffset, "Expression body should start on the same line as '='") {
            (wsAfterEq as LeafPsiElement).rawReplaceWithText(" ")
            if (isMultilineExpression) {
                reindentAfterEq(node, eq, eqLinePrefix)
            }
        }
    }

    /**
     * Tries to collapse an entire multiline expression to one line.
     * Useful for short multiline expressions like `expr ?: fallback` or `expr.let { x }`.
     * Returns true if collapse was performed.
     */
    private fun tryCollapseEntireExpression(
        eqLinePrefix: String,
        expression: ASTNode,
        wsAfterEq: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ): Boolean {
        // Don't collapse block expressions (if/when/try with braces) — they need vertical format
        if (expression.text.contains('{')) return false

        val collapsedText = expression.text.replace(WHITESPACE_REGEX, " ")
        val fullLength = eqLinePrefix.length + "= ".length + collapsedText.length
        if (fullLength > lineLengthSettings.hard) return false

        emitAndCorrect(emit, wsAfterEq.startOffset, "Expression body should start on the same line as '='") {
            (wsAfterEq as LeafPsiElement).rawReplaceWithText(" ")
            expression.collectWhitespace(requireNewline = true).forEach { ws ->
                val prev = ws.treePrev
                val next = ws.treeNext
                val replacement = when {
                    prev?.elementType == KtTokens.LPAR -> ""
                    next?.elementType == KtTokens.RPAR -> ""
                    // No space before `.` or `?.` (chained calls)
                    next?.elementType == KtTokens.DOT -> ""
                    next?.elementType == KtTokens.SAFE_ACCESS -> ""
                    else -> " "
                }
                (ws as LeafPsiElement).rawReplaceWithText(replacement)
            }
        }
        return true
    }

    /**
     * Fixes indentation when `standard:function-expression-body` already collapsed
     * `{ return expr }` → `= expr` but didn't reindent the multiline expression.
     */
    private fun fixIndentAfterExternalCollapse(
        node: ASTNode,
        eq: ASTNode,
        wsAfterEq: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val expression = wsAfterEq.treeNext ?: return
        if (!expression.text.contains('\n')) return

        val baseIndent = eq.lineIndent()
        val expectedContentIndent = baseIndent + INDENT

        val representativeWs = findRepresentativeWhitespace(expression) ?: return
        val currentIndent = representativeWs.text.substringAfterLast('\n')
        if (currentIndent == expectedContentIndent) return

        val shift = expectedContentIndent.length - currentIndent.length
        emitAndCorrect(emit, expression.startOffset, "Fix indentation of expression body") {
            reindentWithShift(node, eq, shift)
        }
    }

    /**
     * Finds the whitespace node that best represents the block body indentation.
     *
     * For block expressions (if/when/try), the first whitespace in the expression tree
     * might be inside a multiline condition or argument list, which has its own indentation
     * unrelated to the block body. This method looks inside the actual block body instead.
     */
    private fun findRepresentativeWhitespace(expression: ASTNode): ASTNode? {
        when (expression.elementType) {
            KtNodeTypes.IF -> {
                // IF → THEN → BLOCK (the then-branch is wrapped in a THEN node)
                val thenNode = expression.findChildByType(KtNodeTypes.THEN)
                val block = thenNode?.findChildByType(KtNodeTypes.BLOCK)
                if (block != null) {
                    val ws = block.collectWhitespace(requireNewline = true).firstOrNull()
                    if (ws != null) return ws
                }
            }

            KtNodeTypes.TRY -> {
                val block = expression.findChildByType(KtNodeTypes.BLOCK)
                if (block != null) {
                    val ws = block.collectWhitespace(requireNewline = true).firstOrNull()
                    if (ws != null) return ws
                }
            }

            KtNodeTypes.WHEN -> {
                val lbrace = expression.findChildByType(KtTokens.LBRACE)
                if (lbrace != null) {
                    val ws = lbrace.treeNext
                    if (ws?.elementType == TokenType.WHITE_SPACE && ws.text.contains('\n')) {
                        return ws
                    }
                }
            }
        }
        return expression.collectWhitespace(requireNewline = true).firstOrNull()
    }

    private fun reindentAfterEq(funNode: ASTNode, eq: ASTNode, eqLinePrefix: String) {
        reindentWithShift(funNode, eq, -INDENT.length)
    }

    private fun reindentWithShift(funNode: ASTNode, eq: ASTNode, shift: Int) {
        val eqOffset = eq.startOffset
        funNode.collectWhitespace(requireNewline = true)
            .filter { it.startOffset > eqOffset }
            .forEach { it.shiftIndent(shift) }
    }

    private companion object {
        val STANDARD_FUNCTION_EXPRESSION_BODY_RULE_ID = RuleId("standard:function-expression-body")
    }
}
