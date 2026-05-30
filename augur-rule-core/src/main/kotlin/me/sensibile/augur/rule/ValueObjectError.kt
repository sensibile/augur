package me.sensibile.augur.rule

sealed interface ValueObjectError {
    val field: String

    data class Blank(
        override val field: String,
    ) : ValueObjectError

    data class InvalidFormat(
        override val field: String,
        val value: String,
    ) : ValueObjectError

    data class NotPositive(
        override val field: String,
        val value: Long,
    ) : ValueObjectError

    data class NotFinite(
        override val field: String,
        val value: Double,
    ) : ValueObjectError

    data class UnsupportedUuidVersion(
        override val field: String,
        val version: Int,
    ) : ValueObjectError
}
