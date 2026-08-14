package com.mealoo.app.pricing

import com.mealoo.app.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Step(
    @Serializable(with = BigDecimalSerializer::class)
    val threshold: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val percent: BigDecimal? = null,

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null
)

@Serializable
sealed class EventRule {
    @Serializable
    data class PercentOffEach(
        @Serializable(with = BigDecimalSerializer::class) val percent: BigDecimal
    ) : EventRule()

    @Serializable
    data class AmountOffEach(
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal
    ) : EventRule()

    @Serializable
    object None : EventRule()

    @Serializable
    data class SecondPercent(
        @Serializable(with = BigDecimalSerializer::class) val percent: BigDecimal
    ) : EventRule()

    @Serializable
    data class SecondMinus(
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal
    ) : EventRule()

    @Serializable
    data class BuyXGetY(
        @Serializable(with = BigDecimalSerializer::class) val buyQty: BigDecimal,
        @Serializable(with = BigDecimalSerializer::class) val getQty: BigDecimal,
        @Serializable(with = BigDecimalSerializer::class) val priceOfGetPercent: BigDecimal = BigDecimal.ZERO
    ) : EventRule()

    @Serializable
    data class BundleFixedPrice(
        @Serializable(with = BigDecimalSerializer::class) val bundleQty: BigDecimal,
        @Serializable(with = BigDecimalSerializer::class) val bundlePrice: BigDecimal
    ) : EventRule()

    @Serializable
    data class TierByQty(val steps: List<Step>) : EventRule()

    @Serializable
    data class TierBySpend(val steps: List<Step>) : EventRule()

    @Serializable
    data class Conditional(
        val eligiblePaymentMethods: Set<String>? = null,
        val eligibleStores: Set<String>? = null,
        val stackable: Boolean = true
    ) : EventRule()

    @Serializable
    object FreeShipping : EventRule()
}

// 扩展函数：验证规则
fun requiredItemRange(rule: EventRule): IntRange = when (rule) {
    is EventRule.None,
    is EventRule.FreeShipping,
    is EventRule.Conditional -> 0..Int.MAX_VALUE
    is EventRule.BuyXGetY    -> 1..2
    is EventRule.BundleFixedPrice -> 2..Int.MAX_VALUE
    is EventRule.PercentOffEach,
    is EventRule.AmountOffEach,
    is EventRule.SecondPercent,
    is EventRule.SecondMinus,
    is EventRule.TierByQty,
    is EventRule.TierBySpend -> 1..Int.MAX_VALUE
}
