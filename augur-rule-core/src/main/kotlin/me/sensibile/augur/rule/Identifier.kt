@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule

import kotlin.uuid.Uuid

@JvmInline
value class FlagKey private constructor(
    val value: String,
) {
    companion object {
        private val pattern = Regex("[a-z][a-z0-9_\\-.]{0,127}")

        fun of(value: String): Outcome<ValueObjectError, FlagKey> {
            val normalized = value.trim()
            return when {
                normalized.isEmpty() -> Outcome.Err(ValueObjectError.Blank("flagKey"))
                !pattern.matches(normalized) -> Outcome.Err(ValueObjectError.InvalidFormat("flagKey", value))
                else -> Outcome.Ok(FlagKey(normalized))
            }
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class AttributeKey private constructor(
    val value: String,
) {
    companion object {
        private val pattern = Regex("[a-zA-Z_][a-zA-Z0-9_\\-.]{0,127}")

        fun of(value: String): Outcome<ValueObjectError, AttributeKey> {
            val normalized = value.trim()
            return when {
                normalized.isEmpty() -> Outcome.Err(ValueObjectError.Blank("attributeKey"))
                !pattern.matches(normalized) -> Outcome.Err(ValueObjectError.InvalidFormat("attributeKey", value))
                else -> Outcome.Ok(AttributeKey(normalized))
            }
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class TargetKey private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): Outcome<ValueObjectError, TargetKey> {
            val normalized = value.trim()
            return when {
                normalized.isEmpty() -> Outcome.Err(ValueObjectError.Blank("targetKey"))
                else -> Outcome.Ok(TargetKey(normalized))
            }
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class RuleId private constructor(
    val value: Uuid,
) {
    companion object {
        private const val UUID_VERSION_7 = 7
        private const val UUID_VERSION_BYTE_INDEX = 6
        private const val UUID_VERSION_SHIFT = 4
        private const val UUID_VERSION_MASK = 0x0f

        fun of(value: Uuid): Outcome<ValueObjectError, RuleId> =
            if (value.version == UUID_VERSION_7) {
                Outcome.Ok(RuleId(value))
            } else {
                Outcome.Err(ValueObjectError.UnsupportedUuidVersion("ruleId", value.version))
            }

        fun generate(): RuleId = RuleId(Uuid.generateV7())

        private val Uuid.version: Int
            get() = (toByteArray()[UUID_VERSION_BYTE_INDEX].toInt() ushr UUID_VERSION_SHIFT) and UUID_VERSION_MASK
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class RuleSetVersion private constructor(
    val value: Long,
) {
    companion object {
        fun of(value: Long): Outcome<ValueObjectError, RuleSetVersion> =
            if (value > 0) {
                Outcome.Ok(RuleSetVersion(value))
            } else {
                Outcome.Err(ValueObjectError.NotPositive("ruleSetVersion", value))
            }
    }

    override fun toString(): String = value.toString()
}

interface RuleIdGenerator {
    fun nextRuleId(): RuleId
}
