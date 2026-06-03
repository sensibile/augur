@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.eventsourcing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.json.RuleJson
import me.sensibile.augur.rule.management.FlagAdded
import me.sensibile.augur.rule.management.FlagDisabled
import me.sensibile.augur.rule.management.FlagEnabled
import me.sensibile.augur.rule.management.PublishedRuleSetId
import me.sensibile.augur.rule.management.RuleAdded
import me.sensibile.augur.rule.management.RuleConditionChanged
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.augur.rule.management.RuleManagementEventId
import me.sensibile.augur.rule.management.RuleRemoved
import me.sensibile.augur.rule.management.RuleServeValueChanged
import me.sensibile.augur.rule.management.RuleSetArchived
import me.sensibile.augur.rule.management.RuleSetDraftCreated
import me.sensibile.augur.rule.management.RuleSetDraftValidated
import me.sensibile.augur.rule.management.RuleSetPublished
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import kotlin.uuid.Uuid

object JsonRuleManagementEventCodec : RuleManagementEventCodec {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = false
        }

    override fun encode(event: RuleManagementEvent): EventStoreEvent =
        EventStoreEvent(
            id = event.eventId.value.toString(),
            eventType = event.eventType,
            eventVersion = 1,
            payloadJson = event.toPayloadJson(),
        )

    @Suppress("LongMethod")
    override fun decode(event: StoredEvent): Outcome<RuleManagementEventCodecError, RuleManagementEvent> =
        event.streamId.toDraftIdFromStreamId().flatMap { draftId ->
            event.id.toEventId().flatMap { eventId ->
                when (event.eventType) {
                    RULE_SET_DRAFT_CREATED -> {
                        decodePayload<RuleSetDraftCreatedPayload>(event).flatMap { payload ->
                            payload.ruleSetVersion.toRuleSetVersion(event.eventType).map { ruleSetVersion ->
                                RuleSetDraftCreated(eventId = eventId, draftId = draftId, ruleSetVersion = ruleSetVersion)
                            }
                        }
                    }

                    FLAG_ADDED -> {
                        decodePayload<FlagAddedPayload>(event).flatMap { payload ->
                            RuleJson.decodeFlag(payload.flagJson).mapCodecError(event.eventType).map { flag ->
                                FlagAdded(eventId = eventId, draftId = draftId, flag = flag)
                            }
                        }
                    }

                    FLAG_ENABLED -> {
                        decodePayload<FlagKeyPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).map { flagKey ->
                                FlagEnabled(eventId = eventId, draftId = draftId, flagKey = flagKey)
                            }
                        }
                    }

                    FLAG_DISABLED -> {
                        decodePayload<FlagKeyPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).map { flagKey ->
                                FlagDisabled(eventId = eventId, draftId = draftId, flagKey = flagKey)
                            }
                        }
                    }

                    RULE_ADDED -> {
                        decodePayload<RuleAddedPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).flatMap { flagKey ->
                                RuleJson.decodeRule(payload.ruleJson).mapCodecError(event.eventType).map { rule ->
                                    RuleAdded(eventId = eventId, draftId = draftId, flagKey = flagKey, rule = rule)
                                }
                            }
                        }
                    }

                    RULE_CONDITION_CHANGED -> {
                        decodePayload<RuleConditionChangedPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).flatMap { flagKey ->
                                payload.ruleId.toRuleId(event.eventType).flatMap { ruleId ->
                                    RuleJson.decodeCondition(payload.conditionJson).mapCodecError(event.eventType).map { condition ->
                                        RuleConditionChanged(
                                            eventId = eventId,
                                            draftId = draftId,
                                            flagKey = flagKey,
                                            ruleId = ruleId,
                                            condition = condition,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    RULE_SERVE_VALUE_CHANGED -> {
                        decodePayload<RuleServeValueChangedPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).flatMap { flagKey ->
                                payload.ruleId.toRuleId(event.eventType).flatMap { ruleId ->
                                    RuleJson.decodeRuleValue(payload.serveJson).mapCodecError(event.eventType).map { serve ->
                                        RuleServeValueChanged(
                                            eventId = eventId,
                                            draftId = draftId,
                                            flagKey = flagKey,
                                            ruleId = ruleId,
                                            serve = serve,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    RULE_REMOVED -> {
                        decodePayload<RuleRemovedPayload>(event).flatMap { payload ->
                            payload.flagKey.toFlagKey(event.eventType).flatMap { flagKey ->
                                payload.ruleId.toRuleId(event.eventType).map { ruleId ->
                                    RuleRemoved(eventId = eventId, draftId = draftId, flagKey = flagKey, ruleId = ruleId)
                                }
                            }
                        }
                    }

                    RULE_SET_DRAFT_VALIDATED -> {
                        decodePayload<RuleSetDraftValidatedPayload>(event).flatMap { payload ->
                            payload.ruleSetVersion.toRuleSetVersion(event.eventType).map { ruleSetVersion ->
                                RuleSetDraftValidated(eventId = eventId, draftId = draftId, ruleSetVersion = ruleSetVersion)
                            }
                        }
                    }

                    RULE_SET_PUBLISHED -> {
                        decodePayload<RuleSetPublishedPayload>(event).flatMap { payload ->
                            payload.publishedRuleSetId.toPublishedRuleSetId(event.eventType).flatMap { publishedRuleSetId ->
                                payload.ruleSetVersion.toRuleSetVersion(event.eventType).map { ruleSetVersion ->
                                    RuleSetPublished(
                                        eventId = eventId,
                                        draftId = draftId,
                                        publishedRuleSetId = publishedRuleSetId,
                                        ruleSetVersion = ruleSetVersion,
                                    )
                                }
                            }
                        }
                    }

                    RULE_SET_ARCHIVED -> {
                        decodePayload<RuleSetArchivedPayload>(event).map {
                            RuleSetArchived(eventId = eventId, draftId = draftId)
                        }
                    }

                    else -> {
                        Outcome.Err(RuleManagementEventCodecError.UnsupportedEventType(event.eventType))
                    }
                }
            }
        }

    private fun RuleManagementEvent.toPayloadJson(): String =
        when (this) {
            is RuleSetDraftCreated -> {
                json.encodeToString(RuleSetDraftCreatedPayload(ruleSetVersion = ruleSetVersion.value))
            }

            is FlagAdded -> {
                json.encodeToString(FlagAddedPayload(flagJson = RuleJson.encodeFlag(flag)))
            }

            is FlagEnabled -> {
                json.encodeToString(FlagKeyPayload(flagKey = flagKey.value))
            }

            is FlagDisabled -> {
                json.encodeToString(FlagKeyPayload(flagKey = flagKey.value))
            }

            is RuleAdded -> {
                json.encodeToString(RuleAddedPayload(flagKey = flagKey.value, ruleJson = RuleJson.encodeRule(rule)))
            }

            is RuleConditionChanged -> {
                json.encodeToString(
                    RuleConditionChangedPayload(
                        flagKey = flagKey.value,
                        ruleId = ruleId.value.toString(),
                        conditionJson = RuleJson.encodeCondition(condition),
                    ),
                )
            }

            is RuleServeValueChanged -> {
                json.encodeToString(
                    RuleServeValueChangedPayload(
                        flagKey = flagKey.value,
                        ruleId = ruleId.value.toString(),
                        serveJson = RuleJson.encodeRuleValue(serve),
                    ),
                )
            }

            is RuleRemoved -> {
                json.encodeToString(RuleRemovedPayload(flagKey = flagKey.value, ruleId = ruleId.value.toString()))
            }

            is RuleSetDraftValidated -> {
                json.encodeToString(RuleSetDraftValidatedPayload(ruleSetVersion = ruleSetVersion.value))
            }

            is RuleSetPublished -> {
                json.encodeToString(
                    RuleSetPublishedPayload(
                        publishedRuleSetId = publishedRuleSetId.value.toString(),
                        ruleSetVersion = ruleSetVersion.value,
                    ),
                )
            }

            is RuleSetArchived -> {
                json.encodeToString(RuleSetArchivedPayload)
            }
        }

    private inline fun <reified A> decodePayload(event: StoredEvent): Outcome<RuleManagementEventCodecError, A> =
        try {
            Outcome.Ok(json.decodeFromString<A>(event.payloadJson))
        } catch (exception: SerializationException) {
            Outcome.Err(RuleManagementEventCodecError.InvalidPayload(event.eventType, exception.message.orEmpty()))
        } catch (exception: IllegalArgumentException) {
            Outcome.Err(RuleManagementEventCodecError.InvalidPayload(event.eventType, exception.message.orEmpty()))
        }

    private fun String.toEventId(): Outcome<RuleManagementEventCodecError, RuleManagementEventId> =
        parseUuidV7(field = "ruleManagementEventId", value = this, parse = RuleManagementEventId::of)

    private fun String.toPublishedRuleSetId(eventType: String): Outcome<RuleManagementEventCodecError, PublishedRuleSetId> =
        parseUuidV7(field = eventType, value = this, parse = PublishedRuleSetId::of)

    private fun String.toRuleId(eventType: String): Outcome<RuleManagementEventCodecError, RuleId> =
        parseUuidV7(field = eventType, value = this, parse = RuleId::of)

    private fun String.toFlagKey(eventType: String): Outcome<RuleManagementEventCodecError, FlagKey> =
        when (val flagKey = FlagKey.of(this)) {
            is Outcome.Err -> Outcome.Err(RuleManagementEventCodecError.InvalidPayload(eventType, flagKey.error.toString()))
            is Outcome.Ok -> Outcome.Ok(flagKey.value)
        }

    private fun Long.toRuleSetVersion(eventType: String): Outcome<RuleManagementEventCodecError, RuleSetVersion> =
        when (val version = RuleSetVersion.of(this)) {
            is Outcome.Err -> Outcome.Err(RuleManagementEventCodecError.InvalidPayload(eventType, version.error.toString()))
            is Outcome.Ok -> Outcome.Ok(version.value)
        }

    private fun <A> Outcome<me.sensibile.augur.rule.json.RuleJsonError, A>.mapCodecError(
        eventType: String,
    ): Outcome<RuleManagementEventCodecError, A> =
        when (this) {
            is Outcome.Err -> Outcome.Err(RuleManagementEventCodecError.InvalidPayload(eventType, error.toString()))
            is Outcome.Ok -> Outcome.Ok(value)
        }
}

private val RuleManagementEvent.eventType: String
    get() =
        when (this) {
            is RuleSetDraftCreated -> RULE_SET_DRAFT_CREATED
            is FlagAdded -> FLAG_ADDED
            is FlagEnabled -> FLAG_ENABLED
            is FlagDisabled -> FLAG_DISABLED
            is RuleAdded -> RULE_ADDED
            is RuleConditionChanged -> RULE_CONDITION_CHANGED
            is RuleServeValueChanged -> RULE_SERVE_VALUE_CHANGED
            is RuleRemoved -> RULE_REMOVED
            is RuleSetDraftValidated -> RULE_SET_DRAFT_VALIDATED
            is RuleSetPublished -> RULE_SET_PUBLISHED
            is RuleSetArchived -> RULE_SET_ARCHIVED
        }

private const val RULE_SET_DRAFT_CREATED = "rule-set-draft.created"
private const val FLAG_ADDED = "flag.added"
private const val FLAG_ENABLED = "flag.enabled"
private const val FLAG_DISABLED = "flag.disabled"
private const val RULE_ADDED = "rule.added"
private const val RULE_CONDITION_CHANGED = "rule.condition-changed"
private const val RULE_SERVE_VALUE_CHANGED = "rule.serve-value-changed"
private const val RULE_REMOVED = "rule.removed"
private const val RULE_SET_DRAFT_VALIDATED = "rule-set-draft.validated"
private const val RULE_SET_PUBLISHED = "rule-set.published"
private const val RULE_SET_ARCHIVED = "rule-set.archived"

@Serializable
private data class RuleSetDraftCreatedPayload(
    val ruleSetVersion: Long,
)

@Serializable
private data class FlagAddedPayload(
    val flagJson: String,
)

@Serializable
private data class FlagKeyPayload(
    val flagKey: String,
)

@Serializable
private data class RuleAddedPayload(
    val flagKey: String,
    val ruleJson: String,
)

@Serializable
private data class RuleConditionChangedPayload(
    val flagKey: String,
    val ruleId: String,
    val conditionJson: String,
)

@Serializable
private data class RuleServeValueChangedPayload(
    val flagKey: String,
    val ruleId: String,
    val serveJson: String,
)

@Serializable
private data class RuleRemovedPayload(
    val flagKey: String,
    val ruleId: String,
)

@Serializable
private data class RuleSetDraftValidatedPayload(
    val ruleSetVersion: Long,
)

@Serializable
private data class RuleSetPublishedPayload(
    val publishedRuleSetId: String,
    val ruleSetVersion: Long,
)

@Serializable
@SerialName("rule-set-archived")
private data object RuleSetArchivedPayload

private inline fun <E, A, B> Outcome<E, A>.map(transform: (A) -> B): Outcome<E, B> =
    when (this) {
        is Outcome.Err -> this
        is Outcome.Ok -> Outcome.Ok(transform(value))
    }

private inline fun <E, A, B> Outcome<E, A>.flatMap(transform: (A) -> Outcome<E, B>): Outcome<E, B> =
    when (this) {
        is Outcome.Err -> this
        is Outcome.Ok -> transform(value)
    }
