package io.github.zenhelix.ktlint.rules.spring

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

public class ZenhelixSpringRuleSetProvider : RuleSetProviderV3(id = RuleSetId("zenhelix-spring")) {

    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { SpringEndpointExplicitReturnTypeRule() },
    )

}
