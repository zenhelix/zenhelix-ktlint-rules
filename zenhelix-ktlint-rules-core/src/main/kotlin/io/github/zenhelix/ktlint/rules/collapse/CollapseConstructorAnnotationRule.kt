package io.github.zenhelix.ktlint.rules.collapse

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.linePrefix

/**
 * When the primary constructor has an annotation (e.g. `@PublishedApi internal constructor()`),
 * keeps it on the same line as the class declaration instead of wrapping to a new line.
 *
 * Before:
 * ```kotlin
 * public class Foo<T>
 *     @PublishedApi
 *     internal constructor()
 * ```
 *
 * After:
 * ```kotlin
 * public class Foo<T> @PublishedApi internal constructor()
 * ```
 */
public class CollapseConstructorAnnotationRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:collapse-constructor-annotation"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_ANNOTATION_RULE_ID,
            mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != KtNodeTypes.CLASS) return

        val primaryConstructor = node.findChildByType(KtNodeTypes.PRIMARY_CONSTRUCTOR) ?: return

        // Only when constructor has annotations in its modifier list
        val modifierList = primaryConstructor.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return
        val hasAnnotation = modifierList.getChildren(null).any {
            it.elementType == KtNodeTypes.ANNOTATION_ENTRY
        }
        if (!hasAnnotation) return

        // Find whitespace between class header and PRIMARY_CONSTRUCTOR
        val wsBeforeConstructor = primaryConstructor.treePrev
            ?.takeIf { it.elementType == TokenType.WHITE_SPACE && it.text.contains('\n') }
            ?: return

        // Compute the collapsed line length to ensure it fits within the limit
        val lineBeforeWs = wsBeforeConstructor.linePrefix()
        val constructorSingleLine = primaryConstructor.text.replace(Regex("[ \t]*\n[ \t]*"), " ")
        if (lineBeforeWs.length + 1 + constructorSingleLine.length > lineLengthSettings.hard) return

        emitAndCorrect(emit, wsBeforeConstructor.startOffset, "Primary constructor annotation should be on the same line as the class declaration") {
            // Collapse whitespace before PRIMARY_CONSTRUCTOR to a single space
            (wsBeforeConstructor as LeafPsiElement).rawReplaceWithText(" ")

            // Collapse any newlines within PRIMARY_CONSTRUCTOR's modifier list
            // (e.g. newline between @PublishedApi and internal)
            modifierList.getChildren(null)
                .filter { it.elementType == TokenType.WHITE_SPACE && it.text.contains('\n') }
                .forEach { ws -> (ws as LeafPsiElement).rawReplaceWithText(" ") }
        }
    }

    private companion object {
        val STANDARD_ANNOTATION_RULE_ID = RuleId("standard:annotation")
    }
}
