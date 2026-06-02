package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.ValueObjectError

interface RuleManagementEventStore {
    fun load(draftId: RuleSetDraftId): Outcome<RuleManagementEventStoreError, RuleManagementEventStream>

    fun append(
        expectedVersion: RuleManagementExpectedStreamVersion,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementEventStoreError, RuleManagementStreamVersion>
}

data class RuleManagementEventStream(
    val draftId: RuleSetDraftId,
    val version: RuleManagementStreamVersion?,
    val events: Sequence<RuleManagementEvent>,
)

@JvmInline
value class RuleManagementStreamVersion private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): Outcome<ValueObjectError, RuleManagementStreamVersion> =
            if (value > 0) {
                Outcome.Ok(RuleManagementStreamVersion(value))
            } else {
                Outcome.Err(ValueObjectError.NotPositive("ruleManagementStreamVersion", value))
            }
    }

    override fun toString(): String = value.toString()
}

sealed interface RuleManagementExpectedStreamVersion {
    data object NoStream : RuleManagementExpectedStreamVersion

    data class Exact(
        val version: RuleManagementStreamVersion,
    ) : RuleManagementExpectedStreamVersion
}

sealed interface RuleManagementEventStoreError {
    data class StreamVersionConflict(
        val draftId: RuleSetDraftId,
        val expected: RuleManagementExpectedStreamVersion,
        val actual: RuleManagementStreamVersion?,
    ) : RuleManagementEventStoreError

    data class StorageFailure(
        val message: String,
    ) : RuleManagementEventStoreError
}
