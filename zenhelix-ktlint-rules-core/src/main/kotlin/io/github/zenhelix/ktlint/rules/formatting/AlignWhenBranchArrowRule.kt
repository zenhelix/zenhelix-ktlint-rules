package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.hasMultilineConditionBeforeArrow

/**
 * Aligns `->` arrows in `when` branches to the same column,
 * matching IntelliJ's `ij_kotlin_align_in_columns_case_branch` behavior.
 *
 * Before:
 * ```kotlin
 * val x = when {
 *     condition1 -> "a"
 *     longerCondition2 -> "b"
 *     else -> "c"
 * }
 * ```
 *
 * After:
 * ```kotlin
 * val x = when {
 *     condition1       -> "a"
 *     longerCondition2 -> "b"
 *     else             -> "c"
 * }
 * ```
 */
public class AlignWhenBranchArrowRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:align-when-branch-arrow"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.WHEN) return

        val entries = node.getChildren(null).filter { it.elementType == KtNodeTypes.WHEN_ENTRY }
        if (entries.size < 2) return

        // Only align if all entries are single-line conditions
        if (entries.any { it.hasMultilineConditionBeforeArrow() }) return

        // Find the max condition width (text before ->)
        val arrowInfos = entries.mapNotNull { entry -> arrowInfo(entry) }
        if (arrowInfos.size != entries.size) return

        val maxConditionWidth = arrowInfos.maxOf { it.conditionWidth }

        for (info in arrowInfos) {
            val needed = maxConditionWidth - info.conditionWidth + 1 // +1 for at least one space
            val currentSpaces = info.spacesBeforeArrow

            if (currentSpaces != needed) {
                emitAndCorrect(emit, info.whitespaceNode.startOffset, "When branch arrows should be aligned") {
                    (info.whitespaceNode as LeafPsiElement).rawReplaceWithText(" ".repeat(needed))
                }
            }
        }
    }

    private fun arrowInfo(entry: ASTNode): ArrowInfo? {
        val arrow = entry.findChildByType(KtTokens.ARROW) ?: return null
        val wsBeforeArrow = arrow.treePrev ?: return null
        if (wsBeforeArrow.elementType != TokenType.WHITE_SPACE) return null

        // Condition width = all text before the whitespace before ->
        var conditionWidth = 0
        var child = entry.firstChildNode
        while (child != null && child != wsBeforeArrow) {
            conditionWidth += child.textLength
            child = child.treeNext
        }

        return ArrowInfo(
            conditionWidth = conditionWidth,
            spacesBeforeArrow = wsBeforeArrow.textLength,
            whitespaceNode = wsBeforeArrow,
        )
    }

    private data class ArrowInfo(
        val conditionWidth: Int,
        val spacesBeforeArrow: Int,
        val whitespaceNode: ASTNode,
    )
}
