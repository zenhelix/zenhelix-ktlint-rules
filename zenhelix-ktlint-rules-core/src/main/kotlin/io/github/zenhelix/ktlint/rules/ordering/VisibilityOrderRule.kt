package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.MEMBER_TYPES
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.isCompanionObject

public class VisibilityOrderRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:visibility-order"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) return

        val maxByCategory = mutableMapOf<MemberCategory, Visibility>()

        for (child in node.getChildren(MEMBER_TYPES)) {
            if (child.isCompanionObject()) continue

            val category = child.memberCategory()
            val visibility = child.resolveVisibility()
            val maxVisibility = maxByCategory[category] ?: Visibility.PUBLIC

            if (visibility < maxVisibility) {
                emit(
                    child.startOffset,
                    "'${visibility.label}' member should be declared before '${maxVisibility.label}' members (expected order: public -> internal -> protected -> private)",
                    false
                )
            }

            if (visibility > maxVisibility) {
                maxByCategory[category] = visibility
            }
        }
    }

    private enum class MemberCategory {
        PROPERTY,
        FUNCTION,
        CONSTRUCTOR,
        TYPE,
    }

    private enum class Visibility(val label: String) {
        PUBLIC("public"),
        INTERNAL("internal"),
        PROTECTED("protected"),
        PRIVATE("private"),
    }

    private companion object {

        val VISIBILITY_MAP: Map<IElementType, Visibility> = mapOf(
            KtTokens.PUBLIC_KEYWORD to Visibility.PUBLIC,
            KtTokens.INTERNAL_KEYWORD to Visibility.INTERNAL,
            KtTokens.PROTECTED_KEYWORD to Visibility.PROTECTED,
            KtTokens.PRIVATE_KEYWORD to Visibility.PRIVATE,
        )

        fun ASTNode.memberCategory(): MemberCategory = when (elementType) {
            KtNodeTypes.PROPERTY -> MemberCategory.PROPERTY
            KtNodeTypes.FUN -> MemberCategory.FUNCTION
            KtNodeTypes.SECONDARY_CONSTRUCTOR -> MemberCategory.CONSTRUCTOR
            else -> MemberCategory.TYPE
        }

        fun ASTNode.resolveVisibility(): Visibility {
            val modifierList = findChildByType(KtNodeTypes.MODIFIER_LIST)
                ?: return Visibility.PUBLIC

            for (modifier in modifierList.getChildren(null)) {
                VISIBILITY_MAP[modifier.elementType]?.let { return it }
            }

            return Visibility.PUBLIC
        }
    }
}
