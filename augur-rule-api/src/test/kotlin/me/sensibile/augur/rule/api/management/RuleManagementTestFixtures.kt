@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import kotlin.uuid.Uuid

internal const val TEST_DRAFT_ID = "018ff7c1-9354-7b02-b021-76d2791d6a21"
internal const val TEST_EVENT_ID = "018ff7c1-9354-7b02-b021-76d2791d6a22"
internal const val TEST_RULE_ID = "018ff7c1-9354-7b02-b021-76d2791d6a23"
internal const val TEST_PUBLISHED_RULE_SET_ID = "018ff7c1-9354-7b02-b021-76d2791d6a25"

internal fun draftState(
    flags: Map<FlagKey, Flag> = emptyMap(),
    status: RuleSetDraftStatus = RuleSetDraftStatus.Draft,
): RuleSetDraftState =
    RuleSetDraftState(
        draftId = draftId(),
        ruleSetVersion = version(1),
        flags = flags,
        status = status,
    )

internal fun flag(
    key: String,
    enabled: Boolean = true,
    defaultValue: RuleValue = RuleValue.boolean(false),
    rules: List<Rule> = emptyList(),
): Flag =
    Flag(
        key = flagKey(key),
        enabled = enabled,
        defaultValue = defaultValue,
        rules = rules,
    )

internal fun rule(
    id: String = TEST_RULE_ID,
    serve: RuleValue = RuleValue.boolean(true),
): Rule =
    Rule(
        id = ruleId(id),
        condition = condition("country", Operator.Eq, RuleValue.string("KR")),
        serve = serve,
    )

internal fun condition(
    attributeKey: String,
    operator: Operator,
    value: RuleValue,
): Condition =
    Condition.Predicate(
        attributeKey = attributeKey(attributeKey),
        operator = operator,
        value = value,
    )

internal fun draftId(value: String = TEST_DRAFT_ID): RuleSetDraftId =
    when (val draftId = RuleSetDraftId.of(Uuid.parse(value))) {
        is Outcome.Err -> error("Invalid test draft id: ${draftId.error}")
        is Outcome.Ok -> draftId.value
    }

internal fun eventId(value: String = TEST_EVENT_ID): RuleManagementEventId =
    when (val eventId = RuleManagementEventId.of(Uuid.parse(value))) {
        is Outcome.Err -> error("Invalid test event id: ${eventId.error}")
        is Outcome.Ok -> eventId.value
    }

internal fun publishedRuleSetId(value: String = TEST_PUBLISHED_RULE_SET_ID): PublishedRuleSetId =
    when (val publishedRuleSetId = PublishedRuleSetId.of(Uuid.parse(value))) {
        is Outcome.Err -> error("Invalid test published rule set id: ${publishedRuleSetId.error}")
        is Outcome.Ok -> publishedRuleSetId.value
    }

internal fun version(value: Long): RuleSetVersion =
    when (val version = RuleSetVersion.of(value)) {
        is Outcome.Err -> error("Invalid test version: ${version.error}")
        is Outcome.Ok -> version.value
    }

internal fun flagKey(value: String): FlagKey =
    when (val key = FlagKey.of(value)) {
        is Outcome.Err -> error("Invalid test flag key: ${key.error}")
        is Outcome.Ok -> key.value
    }

internal fun attributeKey(value: String): AttributeKey =
    when (val key = AttributeKey.of(value)) {
        is Outcome.Err -> error("Invalid test attribute key: ${key.error}")
        is Outcome.Ok -> key.value
    }

internal fun ruleId(value: String = TEST_RULE_ID): RuleId =
    when (val ruleId = RuleId.of(Uuid.parse(value))) {
        is Outcome.Err -> error("Invalid test rule id: ${ruleId.error}")
        is Outcome.Ok -> ruleId.value
    }
