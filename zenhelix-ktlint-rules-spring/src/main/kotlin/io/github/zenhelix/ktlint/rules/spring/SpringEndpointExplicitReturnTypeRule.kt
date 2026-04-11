package io.github.zenhelix.ktlint.rules.spring

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.annotationSimpleName

public class SpringEndpointExplicitReturnTypeRule : ZenhelixRule(
    ruleId = RuleId("zenhelix-spring:spring-endpoint-explicit-return-type")
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.FUN) return

        val modifierList = node.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return
        if (modifierList.findChildByType(KtTokens.PRIVATE_KEYWORD) != null) return
        if (!modifierList.hasSpringMappingAnnotation()) return
        if (node.hasExplicitReturnType()) return

        emit(
            node.startOffset,
            "Controller endpoint function should have explicit return type (including Unit for void methods)",
            false,
        )
    }

    private fun ASTNode.hasSpringMappingAnnotation(): Boolean {
        var child = firstChildNode
        while (child != null) {
            if (child.elementType == KtNodeTypes.ANNOTATION_ENTRY) {
                val name = child.annotationSimpleName()
                if (name != null && (name in MAPPING_ANNOTATIONS || MAPPING_ANNOTATIONS.any { name.endsWith(".$it") })) {
                    return true
                }
            }
            child = child.treeNext
        }
        return false
    }

    private fun ASTNode.hasExplicitReturnType(): Boolean {
        var afterParamList = false
        var child = firstChildNode
        while (child != null) {
            when (child.elementType) {
                KtNodeTypes.VALUE_PARAMETER_LIST -> afterParamList = true
                KtNodeTypes.TYPE_REFERENCE -> if (afterParamList) return true
            }
            child = child.treeNext
        }
        return false
    }

    private companion object {

        val MAPPING_ANNOTATIONS: Set<String> = setOf(
            "RequestMapping",
            "GetMapping",
            "PostMapping",
            "PutMapping",
            "DeleteMapping",
            "PatchMapping",
        )
    }
}
