package io.github.zenhelix.ktlint.rules.blankline

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import io.github.zenhelix.ktlint.rules.ZenhelixRule
import io.github.zenhelix.ktlint.rules.hasBlankLine
import io.github.zenhelix.ktlint.rules.replaceBlankLineWithSingleNewline

private val STANDARD_SPACING_RULE_ID = RuleId("standard:spacing-between-declarations-with-comments")

private val SIGNATURE_KEYWORDS = TokenSet.create(
    KtTokens.PUBLIC_KEYWORD,
    KtTokens.PRIVATE_KEYWORD,
    KtTokens.INTERNAL_KEYWORD,
    KtTokens.PROTECTED_KEYWORD,
    KtTokens.ABSTRACT_KEYWORD,
    KtTokens.CONST_KEYWORD,
    KtTokens.OVERRIDE_KEYWORD,
    KtTokens.LATEINIT_KEYWORD,
)

/**
 * Removes blank lines between consecutive property declarations in a class body
 * when neither property has a block annotation (multiline annotation on its own line).
 *
 * Keeps blank lines when a property has a multiline annotation, as those require visual separation.
 *
 * Before:
 * ```kotlin
 * @PublishedApi internal var record: REC? = null
 *
 * @PublishedApi internal var conflictFields: List<Field<*>> = emptyList()
 * ```
 *
 * After:
 * ```kotlin
 * @PublishedApi internal var record: REC? = null
 * @PublishedApi internal var conflictFields: List<Field<*>> = emptyList()
 * ```
 */
public class NoBlankLineBetweenConsecutivePropertiesRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:no-blank-line-between-consecutive-properties"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = STANDARD_SPACING_RULE_ID,
            mode = VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    private val mixedPatternCache = HashMap<ASTNode, Boolean>()

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != TokenType.WHITE_SPACE) return
        val classBody = node.treeParent ?: return
        if (classBody.elementType != KtNodeTypes.CLASS_BODY) return
        if (!node.hasBlankLine()) return

        val prev = node.treePrev ?: return
        val next = node.treeNext ?: return
        if (prev.elementType != KtNodeTypes.PROPERTY) return
        if (next.elementType != KtNodeTypes.PROPERTY) return

        // Keep blank line if either property has a block annotation or trailing comment
        if (hasBlockAnnotation(prev)) return
        if (hasBlockAnnotation(next)) return
        if (hasEolComment(prev)) return

        // Keep blank line if either property is multiline (e.g. lambda initializer)
        if (isMultilineProperty(prev)) return
        if (isMultilineProperty(next)) return

        // Different modifier signatures (e.g. abstract val vs val, public const val vs private val)
        if (modifierSignature(prev) != modifierSignature(next)) return

        // For object declarations: respect intentional grouping (mixed blank-line pattern)
        if (isObjectBody(classBody)) {
            val isMixed = mixedPatternCache.getOrPut(classBody) { hasMixedBlankLinePattern(classBody) }
            if (isMixed) return
        }

        emitAndCorrect(emit, node.startOffset, "Blank line between consecutive properties without block annotations should be removed") {
            node.replaceBlankLineWithSingleNewline()
        }
    }

    private fun hasEolComment(property: ASTNode): Boolean {
        val next = property.treeNext ?: return false
        if (next.elementType == KtTokens.EOL_COMMENT) return true
        if (next.elementType == TokenType.WHITE_SPACE && !next.text.contains('\n')) {
            return next.treeNext?.elementType == KtTokens.EOL_COMMENT
        }
        return false
    }

    private fun isObjectBody(classBody: ASTNode): Boolean = classBody.treeParent?.elementType == KtNodeTypes.OBJECT_DECLARATION

    private fun hasMixedBlankLinePattern(classBody: ASTNode): Boolean {
        var hasBlankLine = false
        var hasNoBlankLine = false

        var child = classBody.firstChildNode
        while (child != null) {
            if (child.elementType == TokenType.WHITE_SPACE) {
                val prev = child.treePrev
                val next = child.treeNext
                if (prev?.elementType == KtNodeTypes.PROPERTY && next?.elementType == KtNodeTypes.PROPERTY) {
                    if (!hasBlockAnnotation(prev) && !hasBlockAnnotation(next) &&
                        !hasEolComment(prev) &&
                        !isMultilineProperty(prev) && !isMultilineProperty(next)
                    ) {
                        if (child.hasBlankLine()) {
                            hasBlankLine = true
                        } else {
                            hasNoBlankLine = true
                        }
                    }
                }
            }
            child = child.treeNext
        }

        return hasBlankLine && hasNoBlankLine
    }

    private fun modifierSignature(property: ASTNode): String {
        val modifierList = property.findChildByType(KtNodeTypes.MODIFIER_LIST)
            ?: return "public"
        val keywords = modifierList.getChildren(null)
            .filter { it.elementType in SIGNATURE_KEYWORDS }
            .joinToString(" ") { it.text }
        return keywords.ifEmpty { "public" }
    }

    private fun isMultilineProperty(property: ASTNode): Boolean {
        // Check if the property body (from val/var keyword onwards) spans multiple lines.
        // Excludes leading annotations/KDoc from the check.
        val valOrVar = property.findChildByType(KtTokens.VAL_KEYWORD)
            ?: property.findChildByType(KtTokens.VAR_KEYWORD)
            ?: return false
        var child: ASTNode? = valOrVar.treeNext
        while (child != null) {
            if (child.text.contains('\n')) return true
            child = child.treeNext
        }
        return false
    }

    private fun hasBlockAnnotation(property: ASTNode): Boolean {
        val modifierList = property.findChildByType(KtNodeTypes.MODIFIER_LIST) ?: return false
        return modifierList.text.contains('\n')
    }
}
