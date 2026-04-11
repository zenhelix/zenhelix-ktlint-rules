package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.LineLengthSettings
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.PARAM_TOKEN_SET
import io.github.zenhelix.ktlint.rules.hasNewlineAfterComma
import io.github.zenhelix.ktlint.rules.isBodylessFunction
import io.github.zenhelix.ktlint.rules.linePrefix
import io.github.zenhelix.ktlint.rules.textAfterNodeOnSameLine

/**
 * Expands a single-line parameter list to multiple lines when the function
 * signature line exceeds max line length.
 *
 * Before (line > 160 chars):
 * ```
 * public fun foo(param1: Type1, param2: Type2): ReturnType = ...
 * ```
 *
 * After:
 * ```
 * public fun foo(
 *     param1: Type1, param2: Type2
 * ): ReturnType = ...
 * ```
 */
public class ExpandLongParameterListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:expand-long-parameter-list"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.VALUE_PARAMETER_LIST) return

        val params = node.getChildren(PARAM_TOKEN_SET)
        if (params.isEmpty()) return

        // Already expanded: each param on its own line (newlines after commas)
        if (node.hasNewlineAfterComma()) return

        // Check triggers:
        // 1. Expanding params would allow collapsing a wrapped expression body (any param count)
        // 2. Line exceeds max length (2+ params) or hard max (1 param)
        val benefitsFromExpansion = wouldBenefitFromExpansion(node)
        if (!benefitsFromExpansion) {
            if (params.size <= 1 && !hasLongParamLine(node, LineLengthSettings.HARD_MAX_LINE_LENGTH)) return
            if (!hasLongParamLine(node)) return

            // Abstract/interface methods have no body — the signature IS the declaration.
            // Only expand if exceeds HARD_MAX (160) to keep declarations compact.
            if (node.isBodylessFunction() && !hasLongParamLine(node, LineLengthSettings.HARD_MAX_LINE_LENGTH)) return

            // Don't expand params when the signature (up to '= ') fits —
            // expression body length is not the params' responsibility.
            // Exception: always expand when line exceeds hard max (160)
            if (!hasLongParamLine(node, LineLengthSettings.HARD_MAX_LINE_LENGTH) && signatureFitsWithoutExpressionBody(node)) return
        }

        emitAndCorrect(emit, node.startOffset, "Parameter list should be expanded to multiple lines") {
            val lpar = node.findChildByType(KtTokens.LPAR) ?: return@emitAndCorrect
            val linePrefixBeforeLpar = lpar.linePrefix()
            expandParameters(node, linePrefixBeforeLpar)
        }
    }

    /**
     * Detects: `fun foo(params): ReturnType =\n    body`
     * If expanding params would produce `): ReturnType = body` that fits,
     * the expansion is worthwhile (avoids ugly `=` continuation).
     */
    private fun wouldBenefitFromExpansion(node: ASTNode): Boolean {
        val funNode = node.treeParent?.let {
            if (it.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR) it.treeParent else it
        } ?: return false
        if (funNode.elementType != KtNodeTypes.FUN) return false

        val eq = funNode.findChildByType(KtTokens.EQ) ?: return false
        val wsAfterEq = eq.treeNext ?: return false
        // Expression body must be on the next line (= at end of line)
        if (wsAfterEq.elementType != TokenType.WHITE_SPACE || !wsAfterEq.text.contains('\n')) return false

        val expression = wsAfterEq.treeNext ?: return false

        // Multiline expressions (chains with ?., ?:, etc.) won't fully collapse onto the `)` line,
        // so expansion doesn't produce a cleaner result
        if (expression.text.contains('\n')) return false

        val expressionFirstLine = expression.text

        // After expansion, `)` would be on its own line. Check if `): ReturnType = body` fits.
        val lpar = node.findChildByType(KtTokens.LPAR) ?: return false
        val baseIndent = lpar.linePrefix().takeWhile { it == ' ' || it == '\t' }
        // Text between `)` and `=`: return type etc.
        val textBetweenRparAndEq = buildString {
            var sibling = node.treeNext
            while (sibling != null && sibling != eq) {
                append(sibling.text.replace('\n', ' ').replace(Regex("\\s+"), " "))
                sibling = sibling.treeNext
            }
        }
        val collapsedLine = "$baseIndent)$textBetweenRparAndEq= $expressionFirstLine"
        return collapsedLine.length <= LineLengthSettings.STANDARD_MAX_LINE_LENGTH
    }

    /**
     * Returns true if the function signature (up to and including `= `) fits within the limit.
     * When it fits, the line is only long because of the expression body — params should not be expanded.
     */
    private fun signatureFitsWithoutExpressionBody(node: ASTNode): Boolean {
        val funNode = node.treeParent?.let {
            if (it.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR) it.treeParent else it
        } ?: return false
        if (funNode.elementType != KtNodeTypes.FUN) return false

        val eq = funNode.findChildByType(KtTokens.EQ) ?: return false

        val lpar = node.findChildByType(KtTokens.LPAR) ?: return false
        val prefixLength = lpar.linePrefix().length

        // Calculate length from start of line to '= ' (inclusive)
        var lengthToEq = prefixLength
        var sibling: ASTNode? = node
        while (sibling != null && sibling != eq) {
            val text = sibling.text
            val nlIndex = text.lastIndexOf('\n')
            if (nlIndex >= 0) {
                lengthToEq = text.length - nlIndex - 1
            } else {
                lengthToEq += text.length
            }
            sibling = sibling.treeNext
        }
        lengthToEq += "= ".length

        return lengthToEq <= LineLengthSettings.STANDARD_MAX_LINE_LENGTH
    }

    private fun hasLongParamLine(node: ASTNode): Boolean =
        hasLongParamLine(node, LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH)

    private fun hasLongParamLine(node: ASTNode, maxLength: Int): Boolean {
        val lpar = node.findChildByType(KtTokens.LPAR) ?: return false
        val wsAfterLpar = lpar.treeNext
        val parensOnSeparateLines = wsAfterLpar?.elementType == TokenType.WHITE_SPACE &&
                wsAfterLpar.text.contains('\n')

        return if (parensOnSeparateLines) {
            val indent = wsAfterLpar.text.substringAfterLast('\n')
            val paramsOneLine = node.getChildren(PARAM_TOKEN_SET).joinToString(", ") { it.text.trim() }
            indent.length + paramsOneLine.length > maxLength
        } else {
            val linePrefixBeforeLpar = lpar.linePrefix()
            val paramsText = node.text
            val afterParens = node.textAfterNodeOnSameLine()
            linePrefixBeforeLpar.length + paramsText.length + afterParens.length > maxLength
        }
    }

    private fun expandParameters(node: ASTNode, linePrefixBeforeLpar: String) {
        val baseIndent = linePrefixBeforeLpar.takeWhile { it == ' ' || it == '\t' }
        val paramIndent = "$baseIndent    "

        val lpar = node.findChildByType(KtTokens.LPAR) ?: return
        val rpar = node.findChildByType(KtTokens.RPAR) ?: return

        val params = node.getChildren(PARAM_TOKEN_SET)
        val allParamsOneLine = params.joinToString(", ") { it.text.trim() }
        val allFitOnOneLine = paramIndent.length + allParamsOneLine.length <= LineLengthSettings.STANDARD_MAX_LINE_LENGTH

        // Ensure newline + indent after LPAR
        lpar.upsertWhitespaceAfterMe("\n$paramIndent")

        // Ensure newline + base indent before RPAR
        rpar.upsertWhitespaceBeforeMe("\n$baseIndent")

        // Collapse or split comma whitespace
        node.getChildren(null)
            .filter { it.elementType == TokenType.WHITE_SPACE && it.treePrev?.elementType == KtTokens.COMMA }
            .forEach { ws ->
                val replacement = if (allFitOnOneLine) " " else "\n$paramIndent"
                (ws as LeafPsiElement).rawReplaceWithText(replacement)
            }
    }
}
