package me.sensibile.augur.rule

sealed interface RuleValue {
    companion object {
        fun string(value: String): StringValue = StringValue(value)

        fun number(value: Double): Outcome<ValueObjectError, NumberValue> = NumberValue.of(value)

        fun boolean(value: Boolean): BooleanValue = BooleanValue(value)

        fun list(values: List<RuleValue>): ListValue = ListValue(values)

        fun nullValue(): NullValue = NullValue
    }

    data class StringValue(
        val value: String,
    ) : RuleValue

    @ConsistentCopyVisibility
    data class NumberValue internal constructor(
        val value: Double,
    ) : RuleValue {
        companion object {
            fun of(value: Double): Outcome<ValueObjectError, NumberValue> =
                if (value.isFinite()) {
                    Outcome.Ok(NumberValue(value))
                } else {
                    Outcome.Err(ValueObjectError.NotFinite("ruleValue", value))
                }
        }
    }

    data class BooleanValue(
        val value: Boolean,
    ) : RuleValue

    data class ListValue(
        val values: List<RuleValue>,
    ) : RuleValue

    data object NullValue : RuleValue
}
