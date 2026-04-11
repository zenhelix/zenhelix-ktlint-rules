package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.INDENT
import io.github.zenhelix.ktlint.rules.WHITESPACE_REGEX
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collectWhitespace
import io.github.zenhelix.ktlint.rules.lineIndent
import io.github.zenhelix.ktlint.rules.linePrefix

/**
 * Reformats multiline `if` conditions that have awkward wrapping (e.g., function call args
 * split across lines) into a cleaner format.
 *
 * **Collapse** — when the full condition fits on one line:
 * ```kotlin
 * // Before:
 * fun foo() = if (hasLength(
 *         arg
 *     ) && other
 * ) {
 *
 * // After:
 * fun foo() = if (hasLength(arg) && other) {
 * ```
 *
 * **Break at operators** — when the condition is too long for one line:
 * ```kotlin
 * // Before:
 * fun foo() = if (!StringUtils.hasLength(parsedBankBranch.bankBranchAddress) && !StringUtils.hasLength(
 *         uzBankBranch?.bankBranchAddress
 *     )
 * ) {
 *
 * // After:
 * fun foo() = if (
 *     !StringUtils.hasLength(parsedBankBranch.bankBranchAddress)
 *     && !StringUtils.hasLength(uzBankBranch?.bankBranchAddress)
 * ) {
 * ```
 */
public class CollapseIfConditionRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-if-condition"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.IF) return

        val lpar = node.findChildByType(KtTokens.LPAR) ?: return
        val rpar = node.findChildByType(KtTokens.RPAR) ?: return

        val conditionNode = findConditionNode(lpar, rpar) ?: return
        // Unwrap CONDITION wrapper (used by Kotlin PSI for if/while conditions)
        val condition = if (conditionNode.elementType == KtNodeTypes.CONDITION) {
            conditionNode.firstChildNode ?: return
        } else {
            conditionNode
        }
        if (!condition.text.contains('\n')) return

        // Skip if condition contains intentionally expanded structures (4+ arg lists)
        if (containsExpandedArgumentList(condition)) return

        // Condition is multiline — try to fix it
        val collapsedCondition = condition.text.replace(WHITESPACE_REGEX, " ").trim()
        val ifPrefix = lpar.linePrefix() + "("
        val collapsedLine = ifPrefix + collapsedCondition + ")"
        val suffixLength = estimateSuffix(rpar)

        if (collapsedLine.length + suffixLength <= lineLengthSettings.hard) {
            collapseCondition(node, lpar, rpar, condition, emit)
        } else {
            breakConditionAtOperators(node, lpar, rpar, condition, emit)
        }
    }

    private fun findConditionNode(lpar: ASTNode, rpar: ASTNode): ASTNode? {
        var child = lpar.treeNext
        while (child != null && child != rpar) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                return child
            }
            child = child.treeNext
        }
        return null
    }

    private fun estimateSuffix(rpar: ASTNode): Int {
        // After `)` of if condition, expect ` {` or ` {\n`
        val next = rpar.treeNext ?: return 0
        return if (next.elementType == TokenType.WHITE_SPACE) {
            val afterWs = next.treeNext ?: return 0
            1 + afterWs.text.substringBefore('\n').length
        } else {
            next.text.substringBefore('\n').length
        }
    }

    /**
     * Collapses all whitespace inside the condition to single spaces,
     * removing spaces adjacent to parentheses.
     */
    private fun collapseCondition(
        node: ASTNode,
        lpar: ASTNode,
        rpar: ASTNode,
        condition: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emitAndCorrect(emit, condition.startOffset, "If condition fits on a single line") {
            // Collapse all multiline whitespace inside the condition
            collapseWhitespaceInExpression(condition)
            // Collapse whitespace between ( and condition
            val wsAfterLpar = lpar.treeNext
            if (wsAfterLpar?.elementType == TokenType.WHITE_SPACE && wsAfterLpar.text.contains('\n')) {
                (wsAfterLpar as LeafPsiElement).rawReplaceWithText("")
            }
            // Collapse whitespace between condition and )
            val wsBeforeRpar = rpar.treePrev
            if (wsBeforeRpar?.elementType == TokenType.WHITE_SPACE && wsBeforeRpar.text.contains('\n')) {
                (wsBeforeRpar as LeafPsiElement).rawReplaceWithText("")
            }
        }
    }

    private fun collapseWhitespaceInExpression(node: ASTNode) {
        node.collectWhitespace(requireNewline = true).forEach { ws ->
            val prev = ws.treePrev
            val next = ws.treeNext
            val replacement = when {
                prev?.elementType == KtTokens.LPAR -> ""
                next?.elementType == KtTokens.RPAR -> ""
                else -> " "
            }
            (ws as LeafPsiElement).rawReplaceWithText(replacement)
        }
    }

    /**
     * Reformats the condition to break at top-level `&&`/`||` operators:
     * ```
     * if (
     *     condition1
     *     && condition2
     * ) {
     * ```
     */
    private fun breakConditionAtOperators(
        ifNode: ASTNode,
        lpar: ASTNode,
        rpar: ASTNode,
        condition: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Only reformat if the condition has a top-level binary operator
        if (condition.elementType != KtNodeTypes.BINARY_EXPRESSION) return

        // Check if already cleanly formatted (operators at line start)
        if (isAlreadyCleanlyFormatted(condition, lpar)) return

        // Skip if any operand is itself multiline (e.g., setOf(...) with expanded arguments)
        // — collapsing would destroy intentional formatting
        if (hasMultilineOperand(condition)) return

        val baseIndent = lpar.lineIndent()
        val contentIndent = baseIndent + INDENT

        emitAndCorrect(emit, condition.startOffset, "Reformat if condition at operators") {
            // 1. First collapse all whitespace in the condition to single spaces
            collapseWhitespaceInExpression(condition)

            // 2. Set whitespace after `(` — newline + content indent
            val wsAfterLpar = lpar.treeNext
            if (wsAfterLpar?.elementType == TokenType.WHITE_SPACE) {
                (wsAfterLpar as LeafPsiElement).rawReplaceWithText("\n$contentIndent")
            } else {
                // No whitespace between ( and condition — embed into ( itself
                (lpar as LeafPsiElement).rawReplaceWithText("(\n$contentIndent")
            }

            // 3. Insert newlines before each top-level `&&`/`||` operator
            insertNewlinesBeforeOperators(condition, contentIndent)

            // 4. Set whitespace before `)` — newline + base indent
            val wsBeforeRpar = rpar.treePrev
            if (wsBeforeRpar?.elementType == TokenType.WHITE_SPACE) {
                (wsBeforeRpar as LeafPsiElement).rawReplaceWithText("\n$baseIndent")
            } else {
                // No whitespace between condition and ) — embed into ) itself
                (rpar as LeafPsiElement).rawReplaceWithText("\n$baseIndent)")
            }
        }
    }

    private fun insertNewlinesBeforeOperators(binaryExpr: ASTNode, indent: String) {
        // Walk the binary expression tree to find top-level operators
        // For `a && b || c`, the AST is:
        //   BINARY_EXPRESSION(BINARY_EXPRESSION(a, &&, b), ||, c)
        // We want to break at ALL operators, not just the top-level one.
        val operators = mutableListOf<ASTNode>()
        collectAllOperators(binaryExpr, operators)

        for (op in operators) {
            val wsBefore = op.treePrev
            if (wsBefore?.elementType == TokenType.WHITE_SPACE) {
                (wsBefore as LeafPsiElement).rawReplaceWithText("\n$indent")
            }
        }
    }

    private fun collectAllOperators(node: ASTNode, operators: MutableList<ASTNode>) {
        if (node.elementType != KtNodeTypes.BINARY_EXPRESSION) return

        var child = node.firstChildNode
        while (child != null) {
            when (child.elementType) {
                KtNodeTypes.BINARY_EXPRESSION -> collectAllOperators(child, operators)
                KtNodeTypes.OPERATION_REFERENCE -> {
                    val opText = child.text
                    if (opText == "&&" || opText == "||") {
                        operators.add(child)
                    }
                }
            }
            child = child.treeNext
        }
    }

    /**
     * Checks if any operand contains an intentionally expanded argument list (4+ args).
     * Single-arg or 2-3 arg multiline lists are just contextual wrapping and can be collapsed.
     * But `setOf(A, B, C, D)` with 4+ expanded args should be preserved.
     */
    private fun hasMultilineOperand(binaryExpr: ASTNode): Boolean {
        var child = binaryExpr.firstChildNode
        while (child != null) {
            when (child.elementType) {
                TokenType.WHITE_SPACE, KtNodeTypes.OPERATION_REFERENCE -> { /* skip */ }
                KtNodeTypes.BINARY_EXPRESSION -> {
                    if (hasMultilineOperand(child)) return true
                }
                else -> {
                    if (containsExpandedArgumentList(child)) return true
                }
            }
            child = child.treeNext
        }
        return false
    }

    private fun containsExpandedArgumentList(node: ASTNode): Boolean {
        if (node.elementType == KtNodeTypes.VALUE_ARGUMENT_LIST) {
            val args = node.getChildren(TokenSet.create(KtNodeTypes.VALUE_ARGUMENT))
            if (args.size >= 4 && node.text.contains('\n')) return true
        }
        var child = node.firstChildNode
        while (child != null) {
            if (containsExpandedArgumentList(child)) return true
            child = child.treeNext
        }
        return false
    }

    /**
     * Checks if the condition is already formatted with operators at line starts.
     * This avoids re-triggering on already clean code.
     */
    private fun isAlreadyCleanlyFormatted(condition: ASTNode, lpar: ASTNode): Boolean {
        // Check: is there a newline after `(`?
        val wsAfterLpar = lpar.treeNext ?: return false
        if (wsAfterLpar.elementType != TokenType.WHITE_SPACE) return false
        if (!wsAfterLpar.text.contains('\n')) return false

        // Check: does the condition still have newlines only before operators?
        val operators = mutableListOf<ASTNode>()
        collectAllOperators(condition, operators)
        if (operators.isEmpty()) return false

        return operators.all { op ->
            val wsBefore = op.treePrev
            wsBefore?.elementType == TokenType.WHITE_SPACE && wsBefore.text.contains('\n')
        }
    }

}
