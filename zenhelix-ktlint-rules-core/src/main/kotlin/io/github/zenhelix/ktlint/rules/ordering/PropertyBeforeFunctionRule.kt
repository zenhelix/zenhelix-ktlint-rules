package io.github.zenhelix.ktlint.rules.ordering

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import io.github.zenhelix.ktlint.rules.ZenhelixRule

public class PropertyBeforeFunctionRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:property-before-function"),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS_BODY) {
            return
        }

        var functionSeen = false

        for (child in node.getChildren(RELEVANT_TYPES)) {
            when (child.elementType) {
                KtNodeTypes.FUN -> {
                    functionSeen = true
                }

                KtNodeTypes.PROPERTY -> {
                    if (functionSeen) {
                        emit(
                            child.startOffset,
                            "Property should be declared before function declarations",
                            false,
                        )
                    }
                }
            }
        }
    }

    private companion object {

        val RELEVANT_TYPES: TokenSet = TokenSet.create(
            KtNodeTypes.PROPERTY,
            KtNodeTypes.FUN,
        )
    }
}
