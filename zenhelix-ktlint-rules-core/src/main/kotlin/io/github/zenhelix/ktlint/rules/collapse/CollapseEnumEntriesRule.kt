package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import io.github.zenhelix.ktlint.rules.LineLengthSettings
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.columnOf
import io.github.zenhelix.ktlint.rules.isEnumClass

private const val ENUM_ENTRY_TYPE_NAME: String = "ENUM_ENTRY"

/**
 * Collapses multiline enum entries to a single line when they are simple
 * (no body, no annotations) and fit within max line length.
 *
 * Before:
 * ```kotlin
 * enum class Direction {
 *     NORTH,
 *     SOUTH,
 *     EAST,
 *     WEST
 * }
 * ```
 *
 * After:
 * ```kotlin
 * enum class Direction {
 *     NORTH, SOUTH, EAST, WEST
 * }
 * ```
 */
public class CollapseEnumEntriesRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-enum-entries"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) return
        if (!node.isEnumClass()) return

        val entries = collectEnumEntries(node)
        if (entries.size < 2) return
        if (entries.size > MAX_COLLAPSIBLE_ENTRIES) return
        if (!allEntriesSimple(entries)) return
        if (!hasNewlineBetweenEntries(node)) return

        val collapsedEntries = entries.joinToString(", ") { it.text.trimEnd().removeSuffix(",") }
        val column = entries.first().columnOf()
        if (column + collapsedEntries.length > LineLengthSettings.COLLAPSE_MAX_LINE_LENGTH) return

        emitAndCorrect(emit, node.startOffset, "Enum entries fit on a single line") {
            collapseWhitespaceBetweenEntries(node)
        }
    }

    private fun isEnumEntry(node: ASTNode): Boolean = node.elementType.toString() == ENUM_ENTRY_TYPE_NAME

    private fun collectEnumEntries(classBody: ASTNode): List<ASTNode> {
        val entries = mutableListOf<ASTNode>()
        var child = classBody.firstChildNode
        while (child != null) {
            if (isEnumEntry(child)) {
                entries.add(child)
            }
            child = child.treeNext
        }
        return entries
    }

    private fun allEntriesSimple(entries: List<ASTNode>): Boolean = entries.all { entry ->
        val text = entry.text.trimEnd().removeSuffix(",")
        !text.contains('\n') && !text.contains('(') && text.length <= MAX_ENTRY_NAME_LENGTH
    }

    private fun hasNewlineBetweenEntries(classBody: ASTNode): Boolean {
        var child = classBody.firstChildNode
        while (child != null) {
            val prev = child.treePrev
            val next = child.treeNext
            if (child.elementType == TokenType.WHITE_SPACE &&
                child.text.contains('\n') &&
                prev != null && isEnumEntry(prev) &&
                next != null && isEnumEntry(next)
            ) {
                return true
            }
            child = child.treeNext
        }
        return false
    }

    private fun collapseWhitespaceBetweenEntries(classBody: ASTNode) {
        val toCollapse = mutableListOf<ASTNode>()
        var child = classBody.firstChildNode
        while (child != null) {
            if (child.elementType == TokenType.WHITE_SPACE) {
                val prev = child.treePrev
                val next = child.treeNext
                if (prev != null && isEnumEntry(prev) && next != null && isEnumEntry(next)) {
                    toCollapse.add(child)
                }
            }
            child = child.treeNext
        }
        toCollapse.forEach { ws ->
            (ws as LeafPsiElement).rawReplaceWithText(" ")
        }
    }

    private companion object {

        private const val MAX_COLLAPSIBLE_ENTRIES = 5
        private const val MAX_ENTRY_NAME_LENGTH = 15
    }
}
