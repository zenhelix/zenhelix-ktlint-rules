package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.MEMBER_TYPES_WITH_INITIALIZER
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.isEnumClass
import io.github.zenhelix.ktlint.rules.replaceBlankLineWithSingleNewline

public class BlankLineInsideClassBodyRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:blank-line-inside-class-body"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_NO_EMPTY_FIRST_LINE_IN_CLASS_BODY_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) return
        if (node.isEnumClass()) return

        val lbrace = node.findChildByType(KtTokens.LBRACE) ?: return
        val rbrace = node.findChildByType(KtTokens.RBRACE) ?: return

        val members = node.getChildren(MEMBER_TYPES_WITH_INITIALIZER)
        if (members.isEmpty()) return
        if (hasOnlyProperties(node)) return
        // Single companion object or nested class — the inner body handles its own blank lines
        if (members.size == 1 && isCompanionOrClass(members.first())) return
        if (isCompactBody(node, members)) {
            // Compact body: remove blank lines after { and before } if present
            removeBlankLineAfterLbrace(lbrace, emit)
            removeBlankLineBeforeRbrace(rbrace, emit)
            return
        }

        checkAfterLbrace(lbrace, emit)
        checkBeforeRbrace(rbrace, emit)
    }

    /** All members are single-line and of the same declaration type (e.g. all single-line funs). */
    private fun hasOnlyHomogeneousSingleLineMembers(classBody: ASTNode): Boolean {
        val members = classBody.getChildren(MEMBER_TYPES_WITH_INITIALIZER)
        if (members.isEmpty()) return false
        val firstType = members.first().elementType
        return members.all { it.elementType == firstType && !it.text.contains('\n') }
    }

    /** Compact body: homogeneous single-line members OR anonymous object with single member. */
    private fun isCompactBody(classBody: ASTNode, members: Array<ASTNode>): Boolean {
        if (hasOnlyHomogeneousSingleLineMembers(classBody)) return true
        // Anonymous object with single member — e.g. `object : Interface { override fun ... }`
        if (members.size == 1 && isAnonymousObject(classBody)) return true
        return false
    }

    private fun isAnonymousObject(classBody: ASTNode): Boolean {
        val parent = classBody.treeParent ?: return false
        return parent.elementType == KtNodeTypes.OBJECT_DECLARATION &&
            parent.treeParent?.elementType == KtNodeTypes.OBJECT_LITERAL
    }

    private fun isCompanionOrClass(member: ASTNode): Boolean =
        member.elementType == KtNodeTypes.OBJECT_DECLARATION ||
            member.elementType == KtNodeTypes.CLASS

    private fun hasOnlyProperties(classBody: ASTNode): Boolean {
        val members = classBody.getChildren(MEMBER_TYPES_WITH_INITIALIZER)
        return members.isNotEmpty() && members.all { it.elementType == KtNodeTypes.PROPERTY }
    }

    private fun removeBlankLineAfterLbrace(
        lbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = lbrace.treeNext ?: return
        if (next.elementType != TokenType.WHITE_SPACE) return
        if (!next.hasBlankLine()) return

        emitAndCorrect(emit, next.startOffset, "Unexpected blank line after opening brace in compact class body") {
            next.replaceBlankLineWithSingleNewline()
        }
    }

    private fun removeBlankLineBeforeRbrace(
        rbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prev = rbrace.treePrev ?: return
        if (prev.elementType != TokenType.WHITE_SPACE) return
        if (!prev.hasBlankLine()) return

        emitAndCorrect(emit, prev.startOffset, "Unexpected blank line before closing brace in compact class body") {
            prev.replaceBlankLineWithSingleNewline()
        }
    }

    private fun checkAfterLbrace(
        lbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val next = lbrace.treeNext ?: return
        if (next.elementType != TokenType.WHITE_SPACE) return
        if (next.hasBlankLine()) return

        emitAndCorrect(emit, next.startOffset, "Expected blank line after opening brace of class body") {
            (next as LeafPsiElement).rawReplaceWithText("\n" + next.text)
        }
    }

    private fun checkBeforeRbrace(
        rbrace: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prev = rbrace.treePrev ?: return
        if (prev.elementType != TokenType.WHITE_SPACE) return
        if (prev.hasBlankLine()) return

        emitAndCorrect(emit, prev.startOffset, "Expected blank line before closing brace of class body") {
            val indent = prev.text.substringAfterLast('\n')
            (prev as LeafPsiElement).rawReplaceWithText("\n\n" + indent)
        }
    }

    private companion object {
        val STANDARD_NO_EMPTY_FIRST_LINE_IN_CLASS_BODY_RULE_ID = RuleId("standard:no-empty-first-line-in-class-body")
    }
}
