package io.github.zenhelix.ktlint.rules.formatting

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
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.collectWhitespace
import io.github.zenhelix.ktlint.rules.lineIndent
import io.github.zenhelix.ktlint.rules.shiftIndent

/**
 * Fixes indentation of `when` entries and closing `}` relative to the `when` keyword.
 *
 * Before:
 * ```kotlin
 * when (x) {
 * is Foo -> doFoo()
 * is Bar -> doBar()
 * }
 * ```
 *
 * After:
 * ```kotlin
 * when (x) {
 *     is Foo -> doFoo()
 *     is Bar -> doBar()
 * }
 * ```
 */
public class FixWhenBodyIndentRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:fix-when-body-indent"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_INDENT_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.WHEN) return

        val lbrace = node.findChildByType(KtTokens.LBRACE) ?: return
        val rbrace = node.findChildByType(KtTokens.RBRACE) ?: return
        val whenIndent = node.lineIndent()
        val expectedEntryIndent = whenIndent + INDENT

        var child = lbrace.treeNext
        while (child != null && child != rbrace) {
            if (child.elementType == TokenType.WHITE_SPACE && child.text.contains('\n')) {
                fixEntryIndent(child, rbrace, whenIndent, expectedEntryIndent, emit)
            }
            child = child.treeNext
        }
    }

    private fun fixEntryIndent(
        ws: ASTNode,
        rbrace: ASTNode,
        whenIndent: String,
        expectedEntryIndent: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = ws.treeNext ?: return
        if (next.elementType != KtNodeTypes.WHEN_ENTRY && next != rbrace) return

        val currentIndent = ws.text.substringAfterLast('\n')
        val expectedIndent = if (next == rbrace) whenIndent else expectedEntryIndent
        if (currentIndent == expectedIndent) return

        val shift = expectedIndent.length - currentIndent.length
        val prefix = ws.text.substringBeforeLast('\n')

        emitAndCorrect(emit, ws.startOffset, "Wrong indentation inside when body") {
            (ws as LeafPsiElement).rawReplaceWithText("$prefix\n$expectedIndent")

            if (next.elementType == KtNodeTypes.WHEN_ENTRY) {
                next.collectWhitespace(requireNewline = true).forEach { it.shiftIndent(shift) }
            }
        }
    }

    private companion object {
        val STANDARD_INDENT_RULE_ID = RuleId("standard:indent")
    }
}
