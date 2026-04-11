package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule

/**
 * Collapses short KDoc comments (single paragraph, no tags) into a single line.
 *
 * Before:
 * ```
 * /**
 *  * Short description
 *  */
 * ```
 *
 * After:
 * ```
 * /** Short description */
 * ```
 */
public class CollapseShortKdocRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-short-kdoc"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // Visit the parent of KDOC, not KDOC itself — replacing a child is safe for tree traversal
        val kdoc = node.findChildByType(KDocTokens.KDOC) ?: return

        val text = kdoc.text
        val content = extractContent(text) ?: return

        if (content.contains('\n')) return
        if (KDOC_TAG_PATTERN.containsMatchIn(content)) return

        val collapsed = "/** $content */"
        if (text == collapsed) return

        emitAndCorrect(emit, kdoc.startOffset, "Short KDoc should be on a single line") {
            node.replaceChild(kdoc, LeafPsiElement(KDocTokens.KDOC, collapsed))
        }
    }

    private companion object {
        // Matches KDoc tags like @param, @return — but not @ in the middle of text
        val KDOC_TAG_PATTERN: Regex = Regex("""(^|\s)@[a-zA-Z]""")
    }

    private fun extractContent(kdoc: String): String? {
        val lines = kdoc.lines()
        if (lines.size < 2) return null

        val contentLines = lines
            .drop(1)
            .dropLast(1)
            .map { it.trimStart().removePrefix("* ").removePrefix("*") }
            .filter { it.isNotBlank() }

        if (contentLines.size != 1) return null

        return contentLines.first().trim()
    }
}
