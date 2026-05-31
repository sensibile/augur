@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.ValueObjectError
import kotlin.uuid.Uuid

@JvmInline
value class RuleSetDraftId private constructor(
    val value: Uuid,
) {
    companion object {
        fun of(value: Uuid): Outcome<ValueObjectError, RuleSetDraftId> = value.requireUuidV7("ruleSetDraftId", ::RuleSetDraftId)
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class PublishedRuleSetId private constructor(
    val value: Uuid,
) {
    companion object {
        fun of(value: Uuid): Outcome<ValueObjectError, PublishedRuleSetId> = value.requireUuidV7("publishedRuleSetId", ::PublishedRuleSetId)
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class RuleManagementEventId private constructor(
    val value: Uuid,
) {
    companion object {
        fun of(value: Uuid): Outcome<ValueObjectError, RuleManagementEventId> =
            value.requireUuidV7("ruleManagementEventId", ::RuleManagementEventId)
    }

    override fun toString(): String = value.toString()
}

interface RuleSetDraftIdGenerator {
    fun nextRuleSetDraftId(): RuleSetDraftId
}

interface PublishedRuleSetIdGenerator {
    fun nextPublishedRuleSetId(): PublishedRuleSetId
}

interface RuleManagementEventIdGenerator {
    fun nextRuleManagementEventId(): RuleManagementEventId
}

private const val UUID_VERSION_7 = 7
private const val UUID_VERSION_BYTE_INDEX = 6
private const val UUID_VERSION_SHIFT = 4
private const val UUID_VERSION_MASK = 0x0f

private inline fun <A> Uuid.requireUuidV7(
    field: String,
    wrap: (Uuid) -> A,
): Outcome<ValueObjectError, A> =
    if (version == UUID_VERSION_7) {
        Outcome.Ok(wrap(this))
    } else {
        Outcome.Err(ValueObjectError.UnsupportedUuidVersion(field, version))
    }

private val Uuid.version: Int
    get() = (toByteArray()[UUID_VERSION_BYTE_INDEX].toInt() ushr UUID_VERSION_SHIFT) and UUID_VERSION_MASK
