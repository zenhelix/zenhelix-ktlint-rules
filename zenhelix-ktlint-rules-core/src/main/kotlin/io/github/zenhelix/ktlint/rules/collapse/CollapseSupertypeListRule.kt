package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.linePrefix
import io.github.zenhelix.ktlint.rules.textAfterNodeOnSameLine

/**
 * When a class supertype list starts on a new line after `:`,
 * collapses it onto the same line if the result fits within max line length.
 *
 * Before:
 * ```
 * class ReaderException(val reasons: Set<ReaderErrorView>) :
 *     RuntimeException("Errors while read file")
 * ```
 *
 * After:
 * ```
 * class ReaderException(val reasons: Set<ReaderErrorView>) : RuntimeException("Errors while read file")
 * ```
 */
public class CollapseSupertypeListRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-supertype-list"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.SUPER_TYPE_LIST) return

        val parent = node.treeParent ?: return
        if (parent.elementType != KtNodeTypes.CLASS) return

        val wsBefore = node.treePrev ?: return
        if (wsBefore.elementType != TokenType.WHITE_SPACE) return
        if (!wsBefore.text.contains('\n')) return

        // Only collapse single-line supertype lists
        if (node.text.contains('\n')) return

        val colon = wsBefore.treePrev ?: return
        if (colon.elementType != KtTokens.COLON) return

        val colonLinePrefix = colon.linePrefix()
        val supertypeText = node.text.trim()
        val textAfterSupertype = node.textAfterNodeOnSameLine()
        val collapsedLength = colonLinePrefix.length + ": ".length + supertypeText.length + textAfterSupertype.length

        if (collapsedLength > lineLengthSettings.collapse) return

        emitAndCorrect(emit, wsBefore.startOffset, "Supertype list fits on the same line as class declaration") {
            (wsBefore as LeafPsiElement).rawReplaceWithText(" ")
        }
    }

}
