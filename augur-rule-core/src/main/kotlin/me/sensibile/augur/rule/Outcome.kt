package me.sensibile.augur.rule

sealed interface Outcome<out E, out A> {
    data class Err<out E>(
        val error: E,
    ) : Outcome<E, Nothing>

    data class Ok<out A>(
        val value: A,
    ) : Outcome<Nothing, A>
}

inline fun <E, A, B> Outcome<E, A>.map(transform: (A) -> B): Outcome<E, B> =
    when (this) {
        is Outcome.Err -> this
        is Outcome.Ok -> Outcome.Ok(transform(value))
    }

inline fun <E, A, B> Outcome<E, A>.flatMap(transform: (A) -> Outcome<E, B>): Outcome<E, B> =
    when (this) {
        is Outcome.Err -> this
        is Outcome.Ok -> transform(value)
    }
