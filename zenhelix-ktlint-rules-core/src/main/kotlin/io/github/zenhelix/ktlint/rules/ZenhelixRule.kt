package io.github.zenhelix.ktlint.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY

public abstract class ZenhelixRule(
    ruleId: RuleId,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
    usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : Rule(
    ruleId = ruleId,
    about = ZENHELIX_ABOUT,
    visitorModifiers = visitorModifiers,
    usesEditorConfigProperties = usesEditorConfigProperties + MAX_LINE_LENGTH_PROPERTY,
),
    RuleAutocorrectApproveHandler {

    protected var lineLengthSettings: LineLengthSettings = LineLengthSettings()
        private set

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        val maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
        lineLengthSettings = if (maxLineLength == Int.MAX_VALUE) {
            LineLengthSettings()
        } else {
            LineLengthSettings(maxLineLength)
        }
    }

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
