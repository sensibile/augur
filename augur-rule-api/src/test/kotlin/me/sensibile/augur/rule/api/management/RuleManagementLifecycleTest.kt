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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RuleManagementLifecycleTest {
    @Test
    fun `handles and applies draft publish lifecycle`() {
        val draftId = draftId()
        val flag = flag("new_checkout")
        val rule = rule()
        val publishedRuleSetId = publishedRuleSetId()
        var state: RuleSetDraftState? = null

        val created =
            handleAndApply(
                state = state,
                command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1)),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
            )
        assertIs<RuleSetDraftCreated>(created.event)
        state = created.state

        val flagAdded =
            handleAndApply(
                state = state,
                command = AddFlag(draftId = draftId, flag = flag),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"),
            )
        assertIs<FlagAdded>(flagAdded.event)
        state = flagAdded.state

        val ruleAdded =
            handleAndApply(
                state = state,
                command = AddRule(draftId = draftId, flagKey = flag.key, rule = rule),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a27"),
            )
        assertIs<RuleAdded>(ruleAdded.event)
        state = ruleAdded.state

        val validated =
            handleAndApply(
                state = state,
                command = ValidateRuleSetDraft(draftId = draftId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a28"),
            )
        assertIs<RuleSetDraftValidated>(validated.event)
        state = validated.state

        val published =
            handleAndApply(
                state = state,
                command = PublishRuleSet(draftId = draftId, publishedRuleSetId = publishedRuleSetId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a29"),
            )
        assertIs<RuleSetPublished>(published.event)
        state = published.state

        val archived =
            handleAndApply(
                state = state,
                command = ArchiveRuleSet(draftId = draftId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a2a"),
            )
        assertIs<RuleSetArchived>(archived.event)
        state = archived.state

        val finalState = requireNotNull(state)
        assertEquals(RuleSetDraftStatus.Archived, finalState.status)
        assertEquals(listOf(rule), finalState.flags.getValue(flag.key).rules)
    }

    private fun handleAndApply(
        state: RuleSetDraftState?,
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
    ): AppliedCommand {
        val event =
            when (val handled = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)) {
                is Outcome.Err -> error("Command failed: ${handled.error}")
                is Outcome.Ok -> handled.value
            }
        val nextState =
            when (val applied = RuleManagementEventApplier.apply(state = state, event = event)) {
                is Outcome.Err -> error("Event apply failed: ${applied.error}")
                is Outcome.Ok -> applied.value
            }
        return AppliedCommand(event = event, state = nextState)
    }

    private data class AppliedCommand(
        val event: RuleManagementEvent,
        val state: RuleSetDraftState,
    )

    private fun flag(key: String): Flag =
        Flag(
            key = flagKey(key),
            enabled = true,
            defaultValue = RuleValue.boolean(false),
            rules = emptyList(),
        )

    private fun rule(): Rule =
        Rule(
            id = ruleId(),
            condition =
                Condition.Predicate(
                    attributeKey = attributeKey("country"),
                    operator = Operator.Eq,
                    value = RuleValue.string("KR"),
                ),
            serve = RuleValue.boolean(true),
        )

    private fun draftId(): RuleSetDraftId =
        when (val draftId = RuleSetDraftId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21"))) {
            is Outcome.Err -> error("Invalid test draft id: ${draftId.error}")
            is Outcome.Ok -> draftId.value
        }

    private fun eventId(value: String): RuleManagementEventId =
        when (val eventId = RuleManagementEventId.of(Uuid.parse(value))) {
            is Outcome.Err -> error("Invalid test event id: ${eventId.error}")
            is Outcome.Ok -> eventId.value
        }

    private fun publishedRuleSetId(): PublishedRuleSetId =
        when (val publishedRuleSetId = PublishedRuleSetId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a25"))) {
            is Outcome.Err -> error("Invalid test published rule set id: ${publishedRuleSetId.error}")
            is Outcome.Ok -> publishedRuleSetId.value
        }

    private fun version(value: Long): RuleSetVersion =
        when (val version = RuleSetVersion.of(value)) {
            is Outcome.Err -> error("Invalid test version: ${version.error}")
            is Outcome.Ok -> version.value
        }

    private fun flagKey(value: String): FlagKey =
        when (val key = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val key = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun ruleId(): RuleId =
        when (val ruleId = RuleId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a23"))) {
            is Outcome.Err -> error("Invalid test rule id: ${ruleId.error}")
            is Outcome.Ok -> ruleId.value
        }
}
