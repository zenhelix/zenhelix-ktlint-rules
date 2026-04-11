package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.INDENT
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collectWhitespace
import io.github.zenhelix.ktlint.rules.columnOf
import io.github.zenhelix.ktlint.rules.shiftIndent

/**
 * Removes unnecessary braces from single-expression when entry bodies.
 *
 * For single-line expressions:
 * ```kotlin
 * // Before:
 * is Success -> { doSomething() }
 * // After:
 * is Success -> doSomething()
 * ```
 *
 * For multiline expressions (e.g. inner `when`):
 * ```kotlin
 * // Before:
 * is HttpError.Business -> {
 *     when (error.error) {
 *         is BlockedByOneId -> result
 *     }
 * }
 * // After:
 * is HttpError.Business -> when (error.error) {
 *     is BlockedByOneId -> result
 * }
 * ```
 */
public class CollapseWhenEntryRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-when-entry"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_WHEN_ENTRY_BRACING_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.WHEN_ENTRY) return

        val arrow = node.findChildByType(KtTokens.ARROW) ?: return
        val wsAfterArrow = arrow.treeNext ?: return
        if (wsAfterArrow.elementType != TokenType.WHITE_SPACE) return

        val body = wsAfterArrow.treeNext ?: return
        if (body.elementType != KtNodeTypes.BLOCK) return

        val statement = singleStatement(body) ?: return
        val statementText = statement.text.trim()

        if (statementText.contains('\n') && statement.elementType == KtNodeTypes.WHEN) {
            collapseMultilineStatement(arrow, wsAfterArrow, body, statement, statementText, emit)
        } else if (!statementText.contains('\n')) {
            collapseSingleLineStatement(arrow, wsAfterArrow, body, statement, statementText, emit)
        }
    }

    private fun collapseSingleLineStatement(
        arrow: ASTNode,
        wsAfterArrow: ASTNode,
        body: ASTNode,
        statement: ASTNode,
        statementText: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val arrowColumn = arrow.columnOf()
        val collapsedLength = arrowColumn + "-> ".length + statementText.length
        if (collapsedLength > lineLengthSettings.standard) return

        emitAndCorrect(emit, body.startOffset, "When entry body with single expression does not need braces") {
            replaceBlockWithStatementNode(wsAfterArrow, body, statement)
        }
    }

    private fun collapseMultilineStatement(
        arrow: ASTNode,
        wsAfterArrow: ASTNode,
        body: ASTNode,
        statement: ASTNode,
        statementText: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val firstLine = statementText.substringBefore('\n')
        val arrowColumn = arrow.columnOf()
        val collapsedFirstLineLength = arrowColumn + "-> ".length + firstLine.length
        if (collapsedFirstLineLength > lineLengthSettings.standard) return

        if (hasMultilineWhenEntries(statement)) return

        emitAndCorrect(emit, body.startOffset, "When entry body with single expression does not need braces") {
            replaceBlockWithStatementNode(wsAfterArrow, body, statement)
        }
    }

    /** Returns true if a WHEN node has any entry with a multiline body (after `->`). */
    private fun hasMultilineWhenEntries(whenNode: ASTNode): Boolean {
        var child = whenNode.firstChildNode
        while (child != null) {
            if (child.elementType == KtNodeTypes.WHEN_ENTRY) {
                val entryArrow = child.findChildByType(KtTokens.ARROW)
                if (entryArrow != null) {
                    var sibling = entryArrow.treeNext
                    while (sibling != null) {
                        if (sibling.text.contains('\n')) return true
                        sibling = sibling.treeNext
                    }
                }
            }
            child = child.treeNext
        }
        return false
    }

    /** Moves the statement node out of the block, preserving AST structure (imports stay valid). */
    private fun replaceBlockWithStatementNode(wsAfterArrow: ASTNode, block: ASTNode, statement: ASTNode) {
        (wsAfterArrow as LeafPsiElement).rawReplaceWithText(" ")

        val parent = block.treeParent
        parent.addChild(statement, block)
        parent.removeChild(block)

        statement.collectWhitespace(requireNewline = true).forEach { it.shiftIndent(-INDENT.length) }
    }

    private fun singleStatement(block: ASTNode): ASTNode? {
        var found: ASTNode? = null
        var child = block.firstChildNode
        while (child != null) {
            if (child.elementType != KtTokens.LBRACE &&
                child.elementType != KtTokens.RBRACE &&
                child.elementType != TokenType.WHITE_SPACE
            ) {
                if (found != null) return null
                found = child
            }
            child = child.treeNext
        }
        // Don't collapse blocks that contain only comments (no real statements)
        if (found != null && found.elementType in COMMENT_TYPES) return null
        return found
    }

    private companion object {
        val STANDARD_WHEN_ENTRY_BRACING_RULE_ID = RuleId("standard:when-entry-bracing")

        val COMMENT_TYPES = TokenSet.create(
            KtTokens.EOL_COMMENT,
            KtTokens.BLOCK_COMMENT,
        )
    }
}
