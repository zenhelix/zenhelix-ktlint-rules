package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import io.github.zenhelix.ktlint.rules.MEMBER_TYPES_WITH_INITIALIZER
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.isCompanionObject

public class MemberInterleavingRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-member-interleaving"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) {
            return
        }

        val seenAndLeft = mutableSetOf<MemberCategory>()
        var currentCategory: MemberCategory? = null

        for (child in node.getChildren(MEMBER_TYPES_WITH_INITIALIZER)) {
            if (child.isCompanionObject()) continue

            val category = child.memberCategory() ?: continue

            if (category != currentCategory) {
                if (currentCategory != null) {
                    seenAndLeft.add(currentCategory)
                }
                if (category in seenAndLeft) {
                    emit(
                        child.startOffset,
                        "${category.label} should be grouped together, not interleaved with other members",
                        false,
                    )
                }
                currentCategory = category
            }
        }
    }

    private enum class MemberCategory(val label: String) {
        PROPERTY("Properties"),
        INITIALIZER("Initializer blocks"),
        CONSTRUCTOR("Secondary constructors"),
        FUNCTION("Functions"),
        NESTED_TYPE("Nested types"),
    }

    private companion object {

        val CATEGORY_MAP: Map<IElementType, MemberCategory> = mapOf(
            KtNodeTypes.PROPERTY to MemberCategory.PROPERTY,
            KtNodeTypes.CLASS_INITIALIZER to MemberCategory.INITIALIZER,
            KtNodeTypes.SECONDARY_CONSTRUCTOR to MemberCategory.CONSTRUCTOR,
            KtNodeTypes.FUN to MemberCategory.FUNCTION,
            KtNodeTypes.CLASS to MemberCategory.NESTED_TYPE,
            KtNodeTypes.OBJECT_DECLARATION to MemberCategory.NESTED_TYPE,
            KtNodeTypes.TYPEALIAS to MemberCategory.NESTED_TYPE,
        )

        fun ASTNode.memberCategory(): MemberCategory? = CATEGORY_MAP[elementType]
    }
}
