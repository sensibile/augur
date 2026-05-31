package me.sensibile.augur.rule.api

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.json.RuleJson
import org.springframework.stereotype.Service

@Service
class RuleSetValidationService : RuleSetValidatorPort {
    override fun validateCanonicalJson(value: String): RuleSetValidationResult =
        when (val decoded = RuleJson.decodeRuleSetSnapshot(value)) {
            is Outcome.Err -> RuleSetValidationResult.Invalid(decoded.error.toValidationError())
            is Outcome.Ok -> RuleSetValidationResult.Valid(decoded.value.toSummary())
        }
}
