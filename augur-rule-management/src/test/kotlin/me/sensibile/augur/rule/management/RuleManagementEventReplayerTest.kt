package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementEventReplayerTest {
    @Test
    fun `returns no state when event stream is empty`() {
        val actual = RuleManagementEventReplayer.replay(emptyList())

        assertEquals(Outcome.Ok(null), actual)
    }

    @Test
    fun `replays sequence event stream`() {
        val draftId = draftId()
        val events =
            sequenceOf(
                RuleSetDraftCreated(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
            )

        val actual = RuleManagementEventReplayer.replay(events)

        val state = (actual as Outcome.Ok).value
        requireNotNull(state)
        assertEquals(draftId, state.draftId)
    }

    @Test
    fun `replays draft publish lifecycle events`() {
        val draftId = draftId()
        val flag = flag("new_checkout")
        val rule = rule()
        val events =
            listOf(
                RuleSetDraftCreated(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
                FlagAdded(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"),
                    draftId = draftId,
                    flag = flag,
                ),
                RuleAdded(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a27"),
                    draftId = draftId,
                    flagKey = flag.key,
                    rule = rule,
                ),
                RuleSetDraftValidated(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a28"),
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
                RuleSetPublished(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a29"),
                    draftId = draftId,
                    publishedRuleSetId = publishedRuleSetId(),
                    ruleSetVersion = version(1),
                ),
            )

        val actual = RuleManagementEventReplayer.replay(events)

        val state = (actual as Outcome.Ok).value
        requireNotNull(state)
        assertEquals(draftId, state.draftId)
        assertEquals(RuleSetDraftStatus.Published, state.status)
        assertEquals(listOf(rule), state.flags.getValue(flag.key).rules)
    }

    @Test
    fun `stops when an event cannot be applied`() {
        val draftId = draftId()
        val missingFlagKey = flagKey("missing_flag")
        val events =
            listOf(
                RuleSetDraftCreated(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
                RuleAdded(
                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a27"),
                    draftId = draftId,
                    flagKey = missingFlagKey,
                    rule = rule(),
                ),
            )

        val actual = RuleManagementEventReplayer.replay(events)

        assertEquals(
            Outcome.Err(RuleManagementEventApplyError.FlagNotFound(draftId, missingFlagKey)),
            actual,
        )
    }
}
