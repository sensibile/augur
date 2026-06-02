package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleManagementCommandServiceTest {
    @Test
    fun `loads empty stream processes command and appends event`() {
        val draftId = draftId()
        val store = FakeRuleManagementEventStore(stream = emptyStream(draftId))
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
        val store = FakeRuleManagementEventStore(stream = emptyStream(draftId))
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
        val store = FakeRuleManagementEventStore(stream = emptyStream(draftId), appendResult = Outcome.Err(error))
        val service = RuleManagementCommandService(store)
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = service.handle(command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandServiceError.EventStoreAppendFailed(error)), actual)
        assertIs<RuleSetDraftCreated>(store.appendedEvent)
    }

    private fun emptyStream(draftId: RuleSetDraftId): RuleManagementEventStream =
        RuleManagementEventStream(
            draftId = draftId,
            version = null,
            events = emptySequence(),
        )

    private class FakeRuleManagementEventStore(
        stream: RuleManagementEventStream =
            RuleManagementEventStream(
                draftId = draftId(),
                version = null,
                events = emptySequence(),
            ),
        private val loadResult: Outcome<RuleManagementEventStoreError, RuleManagementEventStream> = Outcome.Ok(stream),
        private val appendResult: Outcome<RuleManagementEventStoreError, RuleManagementStreamVersion>? = null,
        private val nextVersion: RuleManagementStreamVersion = streamVersion(1),
    ) : RuleManagementEventStore {
        var appendedExpectedVersion: RuleManagementExpectedStreamVersion? = null
            private set
        var appendedEvent: RuleManagementEvent? = null
            private set

        override fun load(draftId: RuleSetDraftId): Outcome<RuleManagementEventStoreError, RuleManagementEventStream> = loadResult

        override fun append(
            expectedVersion: RuleManagementExpectedStreamVersion,
            event: RuleManagementEvent,
        ): Outcome<RuleManagementEventStoreError, RuleManagementStreamVersion> {
            appendedExpectedVersion = expectedVersion
            appendedEvent = event
            return appendResult ?: Outcome.Ok(nextVersion)
        }
    }
}
