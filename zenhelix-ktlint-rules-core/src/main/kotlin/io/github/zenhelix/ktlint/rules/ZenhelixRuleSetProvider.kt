package io.github.zenhelix.ktlint.rules

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import io.github.zenhelix.ktlint.rules.blankline.BlankLineInDocumentedParameterListRule
import io.github.zenhelix.ktlint.rules.blankline.BlankLineInsideClassBodyRule
import io.github.zenhelix.ktlint.rules.blankline.NoBlankLineBetweenConsecutivePropertiesRule
import io.github.zenhelix.ktlint.rules.blankline.NoBlankLineBetweenSimilarDeclarationsRule
import io.github.zenhelix.ktlint.rules.blankline.NoBlankLineBetweenWhenEntriesRule
import io.github.zenhelix.ktlint.rules.blankline.NoBlankLineInUndocumentedParameterListRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseArgumentListRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseConstructorAnnotationRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseEnumEntriesRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseExpressionBodyRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseIfConditionRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseMethodChainRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseParameterListRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseShortKdocRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseShortLambdaRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseSupertypeListRule
import io.github.zenhelix.ktlint.rules.collapse.CollapseWhenEntryRule
import io.github.zenhelix.ktlint.rules.formatting.AlignWhenBranchArrowRule
import io.github.zenhelix.ktlint.rules.formatting.ChainAfterLambdaIndentRule
import io.github.zenhelix.ktlint.rules.formatting.ExpandLongLambdaRule
import io.github.zenhelix.ktlint.rules.formatting.ExpandLongParameterListRule
import io.github.zenhelix.ktlint.rules.formatting.FixWhenBodyIndentRule
import io.github.zenhelix.ktlint.rules.formatting.IfBracesRule
import io.github.zenhelix.ktlint.rules.formatting.NoTrailingCommaRule
import io.github.zenhelix.ktlint.rules.formatting.ShortenQualifiedNameRule
import io.github.zenhelix.ktlint.rules.ordering.CompanionObjectLastRule
import io.github.zenhelix.ktlint.rules.ordering.InitBlockBeforeFunctionRule
import io.github.zenhelix.ktlint.rules.ordering.MemberInterleavingRule
import io.github.zenhelix.ktlint.rules.ordering.PropertyBeforeFunctionRule
import io.github.zenhelix.ktlint.rules.ordering.VisibilityOrderRule

public class ZenhelixRuleSetProvider : RuleSetProviderV3(id = RuleSetId("zenhelix")) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { VisibilityOrderRule() },
        RuleProvider { CompanionObjectLastRule() },
        RuleProvider { PropertyBeforeFunctionRule() },
        RuleProvider { InitBlockBeforeFunctionRule() },
        RuleProvider { MemberInterleavingRule() },
        RuleProvider { BlankLineInsideClassBodyRule() },
        RuleProvider { BlankLineInDocumentedParameterListRule() },
        RuleProvider { CollapseShortKdocRule() },
        RuleProvider { NoTrailingCommaRule() },
        RuleProvider { CollapseParameterListRule() },
        RuleProvider { CollapseExpressionBodyRule() },
        RuleProvider { ChainAfterLambdaIndentRule() },
        RuleProvider { ExpandLongParameterListRule() },
        RuleProvider { IfBracesRule() },
        RuleProvider { CollapseConstructorAnnotationRule() },
        RuleProvider { NoBlankLineBetweenConsecutivePropertiesRule() },
        RuleProvider { ShortenQualifiedNameRule() },
        RuleProvider { AlignWhenBranchArrowRule() },
        RuleProvider { NoBlankLineBetweenSimilarDeclarationsRule() },
        RuleProvider { NoBlankLineBetweenWhenEntriesRule() },
        RuleProvider { CollapseArgumentListRule() },
        RuleProvider { CollapseIfConditionRule() },
        RuleProvider { CollapseMethodChainRule() },
        RuleProvider { CollapseSupertypeListRule() },
        RuleProvider { CollapseEnumEntriesRule() },
        RuleProvider { CollapseWhenEntryRule() },
        RuleProvider { CollapseShortLambdaRule() },
        RuleProvider { ExpandLongLambdaRule() },
        RuleProvider { NoBlankLineInUndocumentedParameterListRule() },
        RuleProvider { FixWhenBodyIndentRule() },
    )

}
