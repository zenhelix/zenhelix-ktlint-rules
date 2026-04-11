package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.hasMultilineConditionBeforeArrow
import io.github.zenhelix.ktlint.rules.replaceBlankLineWithSingleNewline

/**
 * Removes blank lines between `when` entries whose conditions are single-line.
 *
 * **Why this improves readability:**
 *
 * `when` branches are structurally parallel — each follows the same `condition -> result` pattern.
 * Blank lines break this visual parallelism, making it harder to scan all alternatives at a glance.
 * The `->` arrow (especially when aligned by [AlignWhenBranchArrowRule]) already provides clear
 * visual separation between entries. Compact `when` blocks read like a decision table, which is
 * their purpose.
 *
 * Before:
 * ```kotlin
 * when {
 *     condition1 -> result1
 *
 *     condition2 -> result2
 *
 *     else       -> default
 * }
 * ```
 *
 * After:
 * ```kotlin
 * when {
 *     condition1 -> result1
 *     condition2 -> result2
 *     else       -> default
 * }
 * ```
 *
 * Blank lines are preserved when either adjacent entry has a multiline condition
 * (newline before `->` within the entry), as those require visual breathing room.
 */
public class NoBlankLineBetweenWhenEntriesRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-blank-line-between-when-entries"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != TokenType.WHITE_SPACE) return
        if (node.treeParent?.elementType != KtNodeTypes.WHEN) return
        if (!node.hasBlankLine()) return

        val prev = node.treePrev ?: return
        val next = node.treeNext ?: return
        if (prev.elementType != KtNodeTypes.WHEN_ENTRY) return
        if (next.elementType != KtNodeTypes.WHEN_ENTRY) return

        // Keep blank line if either entry has a multiline condition (before ->)
        if (prev.hasMultilineConditionBeforeArrow() || next.hasMultilineConditionBeforeArrow()) return

        emitAndCorrect(emit, node.startOffset, "Blank line between when entries with single-line conditions should be removed") {
            node.replaceBlankLineWithSingleNewline()
        }
    }
}
