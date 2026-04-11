package io.github.zenhelix.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId

public abstract class ZenhelixRule(
    ruleId: RuleId,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
) : Rule(
    ruleId = ruleId,
    about = ZENHELIX_ABOUT,
    visitorModifiers = visitorModifiers,
),
    RuleAutocorrectApproveHandler {

    protected inline fun emitAndCorrect(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        offset: Int,
        errorMessage: String,
        correct: () -> Unit,
    ) {
        val decision = emit(offset, errorMessage, true)
        if (decision == AutocorrectDecision.ALLOW_AUTOCORRECT) {
            correct()
        }
    }
}
