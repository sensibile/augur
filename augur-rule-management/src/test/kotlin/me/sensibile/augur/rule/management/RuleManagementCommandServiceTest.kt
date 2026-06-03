package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleManagementCommandServiceTest {
    @Test
    fun `loads empty stream processes command and appends event`() {
        val draftId = draftId()
        val store = FakeRuleManagementEventStore(stream = emptyEventStream(draftId))
        val service = RuleManagementCommandService(store)
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = service.handle(command = command, eventId = eventId())

        val result = (actual as Outcome.Ok).value
        assertIs<RuleSetDraftCreated>(result.event)
        assertEquals(RuleSetDraftStatus.Draft, result.state.status)
        assertEquals(streamVersion(1), result.streamVersion)
        assertEquals(RuleManagementExpectedStreamVersion.NoStream, store.appendedExpectedVersion)
        assertEquals(result.event, store.appendedEvent)
    }

    @Test
    fun `replays existing stream before processing command`() {
        val draftId = draftId()
        val flag = flag("new_checkout")
        val store =
            FakeRuleManagementEventStore(
                stream =
                    RuleManagementEventStream(
                        draftId = draftId,
                        version = streamVersion(1),
                        events =
                            sequenceOf(
                                RuleSetDraftCreated(
                                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
                                    draftId = draftId,
                                    ruleSetVersion = version(1),
                                ),
                            ),
                    ),
                nextVersion = streamVersion(2),
            )
        val service = RuleManagementCommandService(store)
        val command = AddFlag(draftId = draftId, flag = flag)

        val actual = service.handle(command = command, eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"))

        val result = (actual as Outcome.Ok).value
        assertIs<FlagAdded>(result.event)
        assertEquals(mapOf(flag.key to flag), result.state.flags)
        assertEquals(streamVersion(2), result.streamVersion)
        assertEquals(RuleManagementExpectedStreamVersion.Exact(streamVersion(1)), store.appendedExpectedVersion)
    }

    @Test
    fun `rejects command when required stream version does not match loaded stream`() {
        val draftId = draftId()
        val store =
            FakeRuleManagementEventStore(
                stream =
                    RuleManagementEventStream(
                        draftId = draftId,
                        version = streamVersion(2),
                        events =
                            sequenceOf(
                                RuleSetDraftCreated(
                                    eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
                                    draftId = draftId,
                                    ruleSetVersion = version(1),
                                ),
                            ),
                    ),
            )
        val service = RuleManagementCommandService(store)
        val expectedVersion = RuleManagementExpectedStreamVersion.Exact(streamVersion(1))
        val command = AddFlag(draftId = draftId, flag = flag("new_checkout"))

        val actual =
            service.handle(
                command = command,
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"),
                expectedVersion = expectedVersion,
            )

        assertEquals(
            Outcome.Err(
                RuleManagementCommandServiceError.EventStoreAppendFailed(
                    RuleManagementEventStoreError.StreamVersionConflict(
                        draftId = draftId,
                        expected = expectedVersion,
                        actual = streamVersion(2),
                    ),
                ),
            ),
            actual,
        )
        assertEquals(null, store.appendedEvent)
    }

    @Test
    fun `returns load failure without appending event`() {
        val draftId = draftId()
        val error = RuleManagementEventStoreError.StorageFailure("load failed")
        val store = FakeRuleManagementEventStore(loadResult = Outcome.Err(error))
        val service = RuleManagementCommandService(store)
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = service.handle(command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandServiceError.EventStoreLoadFailed(error)), actual)
        assertEquals(null, store.appendedEvent)
    }

    @Test
    fun `returns replay failure without appending event`() {
        val draftId = draftId()
        val flag = flag("new_checkout")
        val store =
            FakeRuleManagementEventStore(
                stream =
                    RuleManagementEventStream(
                        draftId = draftId,
                        version = streamVersion(1),
                        events = sequenceOf(FlagAdded(eventId = eventId(), draftId = draftId, flag = flag)),
                    ),
            )
        val service = RuleManagementCommandService(store)
        val command = AddFlag(draftId = draftId, flag = flag)

        val actual = service.handle(command = command, eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"))

        assertEquals(
            Outcome.Err(
                RuleManagementCommandServiceError.EventReplayFailed(
                    RuleManagementEventApplyError.DraftNotFound(draftId),
                ),
            ),
            actual,
        )
        assertEquals(null, store.appendedEvent)
    }

    @Test
    fun `returns process failure without appending event`() {
        val draftId = draftId()
        val store = FakeRuleManagementEventStore(stream = emptyEventStream(draftId))
        val service = RuleManagementCommandService(store)
        val command = AddFlag(draftId = draftId, flag = flag("new_checkout"))

        val actual = service.handle(command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(
                RuleManagementCommandServiceError.CommandProcessFailed(
                    RuleManagementProcessError.CommandRejected(
                        RuleManagementCommandError.DraftNotFound(draftId),
                    ),
                ),
            ),
            actual,
        )
        assertEquals(null, store.appendedEvent)
    }

    @Test
    fun `returns append failure after processing command`() {
        val draftId = draftId()
        val error =
            RuleManagementEventStoreError.StreamVersionConflict(
                draftId = draftId,
                expected = RuleManagementExpectedStreamVersion.NoStream,
                actual = streamVersion(1),
            )
        val store = FakeRuleManagementEventStore(stream = emptyEventStream(draftId), appendResult = Outcome.Err(error))
        val service = RuleManagementCommandService(store)
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = service.handle(command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandServiceError.EventStoreAppendFailed(error)), actual)
        assertIs<RuleSetDraftCreated>(store.appendedEvent)
    }
}
