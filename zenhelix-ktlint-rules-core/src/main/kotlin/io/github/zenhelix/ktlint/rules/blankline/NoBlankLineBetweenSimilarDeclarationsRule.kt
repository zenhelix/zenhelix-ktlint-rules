package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.blankline.NoBlankLineBetweenSimilarDeclarationsRule.Companion.MAX_SMALL_DECLARATION_LINES
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.replaceBlankLineWithSingleNewline

private val STANDARD_BLANK_LINE_RULE_ID = RuleId("standard:blank-line-before-declaration")

private val DECLARATION_TYPES = TokenSet.create(
    KtNodeTypes.FUN,
    KtNodeTypes.CLASS,
    KtNodeTypes.OBJECT_DECLARATION,
    KtNodeTypes.TYPEALIAS,
)

/** Type declarations that can be grouped together (e.g. sealed subclasses mixing `data class` and `object`). */
private val TYPE_DECLARATION_TYPES = TokenSet.create(
    KtNodeTypes.CLASS,
    KtNodeTypes.OBJECT_DECLARATION,
)

/**
 * Removes blank lines between consecutive small declarations of the same kind.
 *
 * Runs after `standard:blank-line-before-declaration` which adds blank lines before every
 * declaration. This rule selectively removes them when declarations are small and similar,
 * keeping them visually grouped.
 *
 * A declaration is considered "small" if it fits within [MAX_SMALL_DECLARATION_LINES] lines.
 *
 * Before:
 * ```kotlin
 * data class Tin(val tin: String) : TinOrPinfl(id = tin)
 *
 * data class Pinfl(val pinfl: String) : TinOrPinfl(id = pinfl)
 * ```
 *
 * After:
 * ```kotlin
 * data class Tin(val tin: String) : TinOrPinfl(id = tin)
 * data class Pinfl(val pinfl: String) : TinOrPinfl(id = pinfl)
 * ```
 */
public class NoBlankLineBetweenSimilarDeclarationsRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-blank-line-between-similar-declarations"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_BLANK_LINE_RULE_ID,
            mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != TokenType.WHITE_SPACE) return
        if (node.treeParent?.elementType != KtNodeTypes.CLASS_BODY) return
        if (!node.hasBlankLine()) return

        val prev = node.treePrev ?: return
        val next = node.treeNext ?: return

        // Both must be declarations of the same kind (or related type declarations like class + object)
        if (prev.elementType !in DECLARATION_TYPES) return
        val sameKind = prev.elementType == next.elementType
        val relatedTypeDeclarations = prev.elementType in TYPE_DECLARATION_TYPES && next.elementType in TYPE_DECLARATION_TYPES
        if (!sameKind && !relatedTypeDeclarations) return

        // Check if this pair can be grouped:
        // - For classes/objects: at least one must be single-line, the other must be small (≤ 3 lines).
        //   When both are multiline, brackets break reading flow and require visual separation.
        // - For functions/typealiases: both must be single-line.
        if (!canGroupPair(prev, next)) return

        // Only collapse when ALL related declarations in the class body are small.
        // In classes with a mix of small and large declarations,
        // blank lines between small declarations aid navigation.
        val classBody = node.treeParent ?: return
        if (!allRelatedDeclarationsSmall(classBody, prev.elementType, next.elementType)) return

        emitAndCorrect(emit, node.startOffset, "Blank line between consecutive small declarations of the same kind should be removed") {
            node.replaceBlankLineWithSingleNewline()
        }
    }

    private fun canGroupPair(prev: ASTNode, next: ASTNode): Boolean = when {
        prev.elementType in TYPE_DECLARATION_TYPES && next.elementType in TYPE_DECLARATION_TYPES -> {
            val prevSingleLine = isSingleLine(prev)
            val nextSingleLine = isSingleLine(next)
            (prevSingleLine || nextSingleLine) && isSmall(prev) && isSmall(next)
        }

        else                                                                                    -> {
            isSingleLine(prev) && isSingleLine(next)
        }
    }

    private fun allRelatedDeclarationsSmall(
        classBody: ASTNode,
        type1: IElementType,
        type2: IElementType,
    ): Boolean = classBody.getChildren(TokenSet.create(type1, type2)).all { isSmall(it) }

    private fun isSingleLine(declaration: ASTNode): Boolean = !declaration.text.contains('\n')

    private fun isSmall(declaration: ASTNode): Boolean = when (declaration.elementType) {
        KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION -> declaration.text.count { it == '\n' } < MAX_SMALL_DECLARATION_LINES
        else                                              -> isSingleLine(declaration)
    }

    private companion object {

        /** Declarations with fewer than this many newlines are considered "small" (3 = up to 3 lines). */
        private const val MAX_SMALL_DECLARATION_LINES = 3
    }
}
