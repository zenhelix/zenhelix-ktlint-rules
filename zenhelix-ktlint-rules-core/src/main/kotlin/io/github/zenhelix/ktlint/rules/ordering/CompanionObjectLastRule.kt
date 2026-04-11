package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import io.github.zenhelix.ktlint.rules.MEMBER_TYPES
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.isCompanionObject

public class CompanionObjectLastRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:companion-object-last"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) return

        val members = node.getChildren(MEMBER_TYPES).toList()
        val companionIndex = members.indexOfFirst { it.isCompanionObject() }

        if (companionIndex == -1) return

        val hasMembersAfterCompanion = members.drop(companionIndex + 1).any {
            !it.isCompanionObject()
        }

        if (hasMembersAfterCompanion) {
            emit(
                members[companionIndex].startOffset,
                "Companion object should be declared after all other class members",
                false
            )
        }
    }

}
