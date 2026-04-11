package io.github.zenhelix.ktlint.rules.formatting

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory
import io.github.zenhelix.ktlint.rules.ZenhelixRule

private val IMPORT_ORDERING_RULE_ID = RuleId("standard:import-ordering")
private val NO_UNUSED_IMPORTS_RULE_ID = RuleId("standard:no-unused-imports")

/**
 * Replaces fully-qualified class names in type references with simple names
 * and adds the corresponding import statement.
 *
 * Only shortens when there is no naming conflict (no other class with the
 * same simple name already imported or used with a different package).
 *
 * Must run after `import-ordering` and `no-unused-imports` because this rule
 * adds new import directives. If those rules run after this one, they may
 * remove the newly added imports before the next formatting pass recognizes them.
 *
 * Before:
 * ```kotlin
 * fun foo(cb: io.github.resilience4j.circuitbreaker.CircuitBreaker)
 * ```
 *
 * After:
 * ```kotlin
 * import io.github.resilience4j.circuitbreaker.CircuitBreaker
 *
 * fun foo(cb: CircuitBreaker)
 * ```
 */
public class ShortenQualifiedNameRule : ZenhelixRule(
    ruleId = RuleId("zenhelix:shorten-qualified-name"),
    visitorModifiers = setOf(
        VisitorModifier.RunAfterRule(
            ruleId = IMPORT_ORDERING_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
        VisitorModifier.RunAfterRule(
            ruleId = NO_UNUSED_IMPORTS_RULE_ID,
            mode = REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED,
        ),
    ),
) {

    private val existingImports = mutableSetOf<String>()
    private val simpleNameToFqns = mutableMapOf<String, MutableSet<String>>()
    private val importsToAdd = mutableSetOf<String>()

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (node.elementType) {
            KtNodeTypes.FILE      -> {
                existingImports.clear()
                simpleNameToFqns.clear()
                importsToAdd.clear()
                prescan(node)
            }

            KtNodeTypes.USER_TYPE -> {
                handleUserType(node, emit)
            }

            KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                handleDotQualifiedExpression(node, emit)
            }
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == KtNodeTypes.FILE && importsToAdd.isNotEmpty()) {
            addImportsToFile(node)
        }
    }

    private fun prescan(fileNode: ASTNode) {
        val importList = fileNode.findChildByType(KtNodeTypes.IMPORT_LIST)
        if (importList != null) {
            for (child in importList.getChildren(null)) {
                if (child.elementType == KtNodeTypes.IMPORT_DIRECTIVE) {
                    val path = extractImportPath(child) ?: continue
                    existingImports.add(path)
                    val simpleName = path.substringAfterLast('.')
                    if (simpleName != "*") {
                        simpleNameToFqns.getOrPut(simpleName) { mutableSetOf() }.add(path)
                    }
                }
            }
        }
        collectFqcns(fileNode)
    }

    private fun collectFqcns(node: ASTNode) {
        if (!isInsideImportOrPackage(node)) {
            when {
                node.elementType == KtNodeTypes.USER_TYPE &&
                    node.treeParent?.elementType != KtNodeTypes.USER_TYPE -> {
                    extractFqcn(node)?.let { registerFqcn(it) }
                }

                node.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION &&
                    node.treeParent?.elementType != KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                    extractFqcnFromDotQualified(node)?.let { registerFqcn(it) }
                }
            }
        }
        node.getChildren(null).forEach { collectFqcns(it) }
    }

    private fun registerFqcn(fqcn: String) {
        val simpleName = fqcn.substringAfterLast('.')
        simpleNameToFqns.getOrPut(simpleName) { mutableSetOf() }.add(fqcn)
    }

    private fun handleUserType(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.treeParent?.elementType == KtNodeTypes.USER_TYPE) return
        handleQualifiedNode(node, emit, ::extractFqcn, ::shortenType)
    }

    private fun handleDotQualifiedExpression(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.treeParent?.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION) return
        handleQualifiedNode(node, emit, ::extractFqcnFromDotQualified, ::shortenDotQualified)
    }

    private fun handleQualifiedNode(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        extractFqcn: (ASTNode) -> String?,
        shorten: (ASTNode) -> Unit,
    ) {
        if (isInsideImportOrPackage(node)) return

        val fqcn = extractFqcn(node) ?: return
        val simpleName = fqcn.substringAfterLast('.')

        val fqns = simpleNameToFqns[simpleName] ?: return
        if (fqns.size > 1) return

        emitAndCorrect(emit, node.startOffset, "Fully qualified name '$fqcn' should use import") {
            shorten(node)
            if (fqcn !in existingImports && !isImportedByStar(fqcn)) {
                importsToAdd.add(fqcn)
                existingImports.add(fqcn)
            }
        }
    }

    /**
     * Extracts the fully-qualified name from a DOT_QUALIFIED_EXPRESSION, or null
     * if the expression is not a package-qualified class reference.
     *
     * Handles both plain references (`uz.orient.Foo`) and constructor calls (`uz.orient.Foo(...)`).
     */
    private fun extractFqcnFromDotQualified(node: ASTNode): String? {
        val segments = mutableListOf<String>()
        if (!collectDotSegments(node, segments)) return null
        if (segments.size < 3) return null
        if (!segments.first().first().isLowerCase()) return null
        if (!segments.last().first().isUpperCase()) return null
        val middleSegments = segments.subList(1, segments.size - 1)
        if (middleSegments.any { !it.first().isLowerCase() }) return null
        return segments.joinToString(".")
    }

    private fun collectDotSegments(node: ASTNode, segments: MutableList<String>): Boolean = when (node.elementType) {
        KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
            node.getChildren(null).all { child ->
                child.elementType == KtTokens.DOT || child.psi is PsiWhiteSpaceImpl || collectDotSegments(child, segments)
            }
        }
        KtNodeTypes.REFERENCE_EXPRESSION -> {
            segments.add(node.text)
            true
        }
        KtNodeTypes.CALL_EXPRESSION -> {
            val ref = node.findChildByType(KtNodeTypes.REFERENCE_EXPRESSION) ?: return false
            segments.add(ref.text)
            true
        }
        else -> false
    }

    /** Replaces the DOT_QUALIFIED_EXPRESSION with its selector (the part after the last dot). */
    private fun shortenDotQualified(node: ASTNode) {
        val parent = node.treeParent ?: return
        val children = node.getChildren(null)
        val lastDotIndex = children.indexOfLast { it.elementType == KtTokens.DOT }
        if (lastDotIndex < 0 || lastDotIndex >= children.size - 1) return
        val selector = children[lastDotIndex + 1]
        parent.replaceChild(node, selector.clone() as ASTNode)
    }

    /** Extracts the fully-qualified name from a USER_TYPE, or null if not a FQCN. */
    private fun extractFqcn(userType: ASTNode): String? {
        // Must have a qualifier (nested USER_TYPE) to be a qualified name
        if (userType.findChildByType(KtNodeTypes.USER_TYPE) == null) return null
        // Strip type arguments (<T>) for analysis
        val text = userType.text.substringBefore('<').trim()
        if (text.isEmpty() || !text.contains('.')) return null
        // Skip qualified nested classes like Map.Entry (first segment is uppercase)
        if (text.first().isUpperCase()) return null
        return text
    }

    /** Removes the qualifier USER_TYPE and DOT children, keeping REFERENCE_EXPRESSION and TYPE_ARGUMENT_LIST. */
    private fun shortenType(node: ASTNode) {
        val toRemove = node.getChildren(null).filter { child ->
            child.elementType == KtNodeTypes.USER_TYPE || child.elementType == KtTokens.DOT
        }
        toRemove.forEach { node.removeChild(it) }
    }

    private fun isImportedByStar(fqcn: String): Boolean {
        val packagePart = fqcn.substringBeforeLast('.')
        return "$packagePart.*" in existingImports
    }

    /**
     * Adds new import directives as proper IMPORT_DIRECTIVE AST nodes.
     *
     * Uses [KtPsiFactory] to parse a minimal Kotlin file containing the
     * import statements, then extracts the IMPORT_DIRECTIVE nodes and
     * inserts them into the real import list. This ensures they are
     * proper AST nodes that survive restructuring by other rules
     * (e.g. `import-ordering`).
     */
    private fun addImportsToFile(fileNode: ASTNode) {
        val importList = fileNode.findChildByType(KtNodeTypes.IMPORT_LIST) ?: return
        var lastImportNode: ASTNode = importList.getChildren(null)
            .lastOrNull { it.elementType == KtNodeTypes.IMPORT_DIRECTIVE }
            ?: return

        val psiFactory = KtPsiFactory(fileNode.psi.project)

        for (fqcn in importsToAdd.sorted()) {
            val dummyFile = psiFactory.createFile("import $fqcn\n")
            val dummyImportList = dummyFile.node.findChildByType(KtNodeTypes.IMPORT_LIST) ?: continue
            val newImportDirective = dummyImportList.findChildByType(KtNodeTypes.IMPORT_DIRECTIVE) ?: continue

            // Add whitespace (newline) and proper IMPORT_DIRECTIVE node after the last import
            val anchor = lastImportNode.treeNext
            importList.addChild(PsiWhiteSpaceImpl("\n"), anchor)
            val clonedNode = newImportDirective.clone() as ASTNode
            importList.addChild(clonedNode, anchor)
            lastImportNode = clonedNode
        }
    }

    private fun extractImportPath(importDirective: ASTNode): String? {
        val text = importDirective.text.trim()
        if (!text.startsWith("import ")) return null
        return text.removePrefix("import ").trim().substringBefore(" as ").trim()
    }

    private fun isInsideImportOrPackage(node: ASTNode): Boolean {
        var current = node.treeParent
        while (current != null) {
            val type = current.elementType
            if (type == KtNodeTypes.IMPORT_DIRECTIVE || type == KtNodeTypes.PACKAGE_DIRECTIVE) return true
            current = current.treeParent
        }
        return false
    }
}
