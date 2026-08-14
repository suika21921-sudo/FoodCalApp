@file:Suppress("MemberVisibilityCanBePrivate")
package com.mealoo.app.util

import com.mealoo.app.data.CartItem
import com.mealoo.app.pricing.EventRule
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pricing Blocks (BigDecimal Edition) — Robust/Compliance Upgrade
 *
 * Pipeline (fixed order):
 *  ITEM_DISCOUNTS (grouped by exclusiveGroup -> pick best per group -> apply groups in order) ->
 *  CHANNEL_PROMO -> FEES -> TAXES (multiple, ordered; item/group aware) -> PAYMENT_BONUS (instant -> future)
 *
 * Highlights in this revision:
 *  - BigDecimal (scale=2, HALF_UP) everywhere.
 *  - Item discounts now processed by global grouping (exclusiveGroup) using groupBy.
 *  - calculate() returns Result<PricingResult, PricingError> with structured errors and logs.
 *  - Multi-tax support: PricingInput.taxes: List<Tax>. Taxes can target CART or ITEM_GROUP via taxGroup and applyAfterFees.
 *  - CartItem supports taxGroup for per-item/segment taxation.
 */

// --------------------------------------------------
// BigDecimal helpers
// --------------------------------------------------
private val BD_100 = BigDecimal("100").setScale(2, RoundingMode.HALF_UP)

private val BD_ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
private val BD_ONE = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP)
private val BD_HUNDRED = BigDecimal("100").setScale(2, RoundingMode.HALF_UP)

fun bd(value: String): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
fun bd(value: Int): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
fun bd(value: Long): BigDecimal = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
fun bd(value: Double): BigDecimal =
    BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP)


fun BigDecimal.round2(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
fun BigDecimal.max0(): BigDecimal = if (this < BD_ZERO) BD_ZERO else this
fun BigDecimal.min(other: BigDecimal): BigDecimal = if (this <= other) this else other
fun BigDecimal.max(other: BigDecimal): BigDecimal = if (this >= other) this else other

fun BigDecimal.mul(other: BigDecimal): BigDecimal = this.multiply(other).setScale(2, RoundingMode.HALF_UP)
fun BigDecimal.div2(other: BigDecimal): BigDecimal = this.divide(other, 2, RoundingMode.HALF_UP)

// --------------------------------------------------
// Result / Error types
// --------------------------------------------------

sealed class PricingError(open val message: String, open val logs: List<String> = emptyList()) {
    data class InvalidInput(override val message: String, override val logs: List<String> = emptyList()) : PricingError(message, logs)
    data class CalculationError(override val message: String, val cause: Throwable? = null, override val logs: List<String> = emptyList()) : PricingError(message, logs)
}

sealed class Result<out V, out E> {
    data class Ok<V>(val value: V) : Result<V, Nothing>()
    data class Err<E>(val error: E) : Result<Nothing, E>()
}

// --------------------------------------------------
// Item-level discounts (grouped by exclusiveGroup; best per group; groups applied in order)
// --------------------------------------------------

interface ItemDiscount {
    /** Discounts with the same exclusiveGroup are mutually exclusive choices. */
    val exclusiveGroup: String?
    /** Group order hint. Engine sorts groups by the min order within each group. */
    val order: Int get() = 0
    /** Compute subtotal for (unitPrice, qty) if ONLY this discount applied. */
    fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult
}

data class ItemDiscountResult(
    val subtotal: BigDecimal,
    val explanation: String
)

// ---- Item discount types ----

data class MemberPrice(
    val memberUnitPrice: BigDecimal,
    override val exclusiveGroup: String? = null,
    override val order: Int = 0
) : ItemDiscount {
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val q = qty.max0()
        val u = memberUnitPrice.max0()
        val s = u.mul(q)
        return ItemDiscountResult(s, "MemberPrice: unit=$u x qty=$q = $s")
    }
}

data class AmountOffEach(
    val offEach: BigDecimal,
    val floorUnitPrice: BigDecimal? = null,
    override val exclusiveGroup: String? = null,
    override val order: Int = 10
) : ItemDiscount {
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val q = qty.max0()
        var u = unitPrice.max0().minus(offEach.max0())
        if (floorUnitPrice != null) u = u.max(floorUnitPrice)
        if (u < BD_ZERO) u = BD_ZERO
        val s = u.mul(q)
        return ItemDiscountResult(s, "AmountOffEach: (unit=$unitPrice - ${offEach.round2()}) -> $u; subtotal=$s")
    }
}

data class PercentOffEach(
    /** percentOff in [0,1], e.g. 0.2 = 20% */
    val percentOff: BigDecimal,
    val floorUnitPrice: BigDecimal? = null,
    override val exclusiveGroup: String? = null,
    override val order: Int = 11
) : ItemDiscount {
    init { require(percentOff >= BD_ZERO && percentOff <= BD_ONE) { "percentOff must be in [0,1]" } }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val q = qty.max0()
        var u = unitPrice.max0().mul(BD_ONE.minus(percentOff))
        if (floorUnitPrice != null) u = u.max(floorUnitPrice)
        if (u < BD_ZERO) u = BD_ZERO
        val s = u.mul(q)
        return ItemDiscountResult(s, "PercentOffEach: unit=$unitPrice*(1-${percentOff.round2()}) -> $u; subtotal=$s")
    }
}

data class NthPiecePercent(
    val n: Int,
    /** percentForNth in [0,1], e.g. 0.5 = 50% price for nth piece */
    val percentForNth: BigDecimal,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 20
) : ItemDiscount {
    init {
        require(n >= 2) { "n must be >= 2" }
        require(percentForNth >= BD_ZERO && percentForNth <= BD_ONE) { "percentForNth must be in [0,1]" }
    }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val q = qty.max0()
        val qInt = q.toInt()
        val fullGroups = qInt / n
        val remainder = qInt % n
        val priceGroup = u.mul(bd(n - 1)).plus(u.mul(percentForNth))
        var sum = priceGroup.mul(bd(fullGroups)).plus(u.mul(bd(remainder)))
        val frac = q.minus(bd(qInt))
        if (frac > BD_ZERO) sum = sum.plus(u.mul(frac))
        sum = sum.round2()
        return ItemDiscountResult(sum, "NthPiecePercent: every $n-th at ${percentForNth*BD_HUNDRED}% price; subtotal=$sum")
    }
}

data class BuyNGetM(
    val n: Int,
    val m: Int,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 21
) : ItemDiscount {
    init { require(n > 0 && m >= 0) { "n>0, m>=0" } }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val q = qty.max0()
        val qInt = q.toInt()
        val group = n + m
        val full = if (group > 0) qInt / group else 0
        val rem = if (group > 0) qInt % group else qInt
        val payable = full * n + kotlin.math.min(rem, n)
        var sum = u.mul(bd(payable))
        val frac = q.minus(bd(qInt))
        if (frac > BD_ZERO) sum = sum.plus(u.mul(frac))
        sum = sum.round2()
        return ItemDiscountResult(sum, "BuyNGetM: n=$n,m=$m -> pay=$payable; subtotal=$sum")
    }
}

data class KForY(
    val k: Int,
    val y: BigDecimal,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 22
) : ItemDiscount {
    init { require(k > 0) { "k must be > 0" } }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val q = qty.max0()
        val qInt = q.toInt()
        val bundles = qInt / k
        val remainder = qInt % k
        var sum = y.max0().mul(bd(bundles)).plus(u.mul(bd(remainder)))
        val frac = q.minus(bd(qInt))
        if (frac > BD_ZERO) sum = sum.plus(u.mul(frac))
        sum = sum.round2()
        return ItemDiscountResult(sum, "KForY: k=$k for y=$y -> bundles=$bundles, rem=$remainder; subtotal=$sum")
    }
}

data class TieredUnitPrice(
    val tiers: List<Tier>,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 23
) : ItemDiscount {
    data class Tier(val thresholdQty: BigDecimal, val unitPrice: BigDecimal)
    init { require(tiers.isNotEmpty()) { "tiers must not be empty" } }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val q = qty.max0()
        var applied = unitPrice.max0()
        for (t in tiers.sortedBy { it.thresholdQty }) {
            if (q >= t.thresholdQty) applied = t.unitPrice
        }
        val s = applied.mul(q).round2()
        return ItemDiscountResult(s, "TieredUnitPrice: unit=$applied at qty=$q; subtotal=$s")
    }
}

data class SurchargeAfter(
    val thresholdQty: BigDecimal,
    val surchargeEach: BigDecimal,
    override val exclusiveGroup: String? = null,
    override val order: Int = 90
) : ItemDiscount {
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val q = qty.max0()
        val base = u.mul(q)
        val extraQty = (q.minus(thresholdQty.max0())).max0()
        val extra = surchargeEach.max0().mul(extraQty)
        val s = base.plus(extra).round2()
        return ItemDiscountResult(s, "SurchargeAfter: threshold=$thresholdQty, +${surchargeEach.round2()} each after; subtotal=$s")
    }
}

// --------------------------------------------------
// Channel promo (cart-level)
// --------------------------------------------------

interface ChannelPromo {
    /** Returns discount (>=0) to subtract from cartSubtotalAfterItems. */
    fun discount(cartSubtotalAfterItems: BigDecimal, totalQty: BigDecimal): ChannelPromoResult
}

data class ChannelPromoResult(
    val discount: BigDecimal,
    val explanation: String,
    val repeatTimes: Int = 0
)

data class CartThresholdPercentOff(
    val thresholdAmount: BigDecimal,
    /** percent in [0,1] */
    val percentOff: BigDecimal,
    val cap: BigDecimal? = null,
    val repeatable: Boolean = false
) : ChannelPromo {
    init { require(percentOff >= BD_ZERO && percentOff <= BD_ONE) { "percentOff must be in [0,1]" } }
    override fun discount(cartSubtotalAfterItems: BigDecimal, totalQty: BigDecimal): ChannelPromoResult {
        val base = cartSubtotalAfterItems.max0()
        if (base < thresholdAmount) return ChannelPromoResult(BD_ZERO, "PercentOff: below threshold")
        return if (repeatable) {
            val times = base.div2(thresholdAmount).toInt()
            val perBlock = thresholdAmount.mul(percentOff)
            val raw = perBlock.mul(bd(times))
            val applied = cap?.let { raw.min(it) } ?: raw
            ChannelPromoResult(applied.round2(), "PercentOff repeatable: $times blocks x $percentOff of $thresholdAmount", times)
        } else {
            val raw = base.mul(percentOff)
            val applied = cap?.let { raw.min(it) } ?: raw
            ChannelPromoResult(applied.round2(), "PercentOff once: ${percentOff*BD_HUNDRED}% of $base -> ${applied.round2()}")
        }
    }
}

data class CartThresholdFixedOff(
    val thresholdAmount: BigDecimal,
    val offAmount: BigDecimal,
    val repeatable: Boolean = false
) : ChannelPromo {
    override fun discount(cartSubtotalAfterItems: BigDecimal, totalQty: BigDecimal): ChannelPromoResult {
        val base = cartSubtotalAfterItems.max0()
        if (base < thresholdAmount) return ChannelPromoResult(BD_ZERO, "FixedOff: below threshold")
        return if (repeatable) {
            val times = base.div2(thresholdAmount).toInt()
            val d = offAmount.max0().mul(bd(times))
            ChannelPromoResult(d.round2(), "FixedOff repeatable: ${offAmount.round2()} x $times", times)
        } else {
            val d = offAmount.max0()
            ChannelPromoResult(d.round2(), "FixedOff once: -${d.round2()}", 1)
        }
    }
}

// --------------------------------------------------
// Fees
// --------------------------------------------------

data class Fees(val shipping: BigDecimal = BD_ZERO, val service: BigDecimal = BD_ZERO) {
    val total: BigDecimal get() = shipping.max0().plus(service.max0()).round2()
}

// --------------------------------------------------
// Tax (multi)
// --------------------------------------------------

enum class TaxScope { CART, ITEM_GROUP }

sealed interface Tax {
    /** Calculate tax amount given a base; engine constructs the right base by scope/group. */
    fun amount(taxBase: BigDecimal): BigDecimal
    /** When true, fees are included into tax base; when false, fees are excluded. */
    val applyAfterFees: Boolean get() = true
    /** CART or ITEM_GROUP level taxation */
    val scope: TaxScope get() = TaxScope.CART
    /** For ITEM_GROUP scope only: which group to target (e.g., "food"). */
    val taxGroup: String? get() = null
    /** Optional label for logs. */
    val label: String get() = this::class.simpleName ?: "Tax"
}

data class PercentTax(
    /** rate in [0,1] */
    val rate: BigDecimal,
    override val applyAfterFees: Boolean = true,
    override val scope: TaxScope = TaxScope.CART,
    override val taxGroup: String? = null,
    override val label: String = "PercentTax"
) : Tax {
    init { require(rate >= BD_ZERO && rate <= BD_ONE) { "tax rate must be in [0,1]" } }
    override fun amount(taxBase: BigDecimal): BigDecimal = taxBase.max0().mul(rate).round2()
}

data class FixedTax(
    val amountFixed: BigDecimal,
    override val applyAfterFees: Boolean = true,
    override val scope: TaxScope = TaxScope.CART,
    override val taxGroup: String? = null,
    override val label: String = "FixedTax"
) : Tax {
    override fun amount(taxBase: BigDecimal): BigDecimal = amountFixed.max0().round2()
}

// --------------------------------------------------
// Payment bonus (single payment method chosen; components ordered)
// --------------------------------------------------

interface PayComponent {
    val order: Int get() = 0
    fun apply(base: BigDecimal): Cashback
}

data class Cashback(val instantOff: BigDecimal = BD_ZERO, val futureRebate: BigDecimal = BD_ZERO)

data class PercentRebate(
    /** percent in [0,1] */
    val percent: BigDecimal,
    val isInstant: Boolean = false,
    val cap: BigDecimal? = null,
    val minSpend: BigDecimal = BD_ZERO,
    override val order: Int = 0
) : PayComponent {
    init { require(percent >= BD_ZERO) { "percent >= 0" } }
    override fun apply(base: BigDecimal): Cashback {
        if (base < minSpend) return Cashback()
        val raw = base.mul(percent)
        val rebate = cap?.let { raw.min(it) } ?: raw
        return if (isInstant) Cashback(instantOff = rebate.round2()) else Cashback(futureRebate = rebate.round2())
    }
}

data class FixedAmount(
    val amount: BigDecimal,
    val isInstant: Boolean = true,
    val minSpend: BigDecimal = BD_ZERO,
    override val order: Int = 0
) : PayComponent {
    override fun apply(base: BigDecimal): Cashback {
        if (base < minSpend) return Cashback()
        val a = amount.max0().round2()
        return if (isInstant) Cashback(instantOff = a) else Cashback(futureRebate = a)
    }
}

/** A payment method may combine multiple components. Components are executed by ascending order. */
data class PaymentMethod(
    val id: String,
    val label: String,
    val components: List<PayComponent>,
    val enabled: Boolean = true
) {
    fun evaluate(payableBeforeBonus: BigDecimal): PaymentEvaluation {
        var base = payableBeforeBonus.max0()
        var instant = BD_ZERO
        var future = BD_ZERO
        val steps = mutableListOf<String>()
        for (c in components.sortedBy { it.order }) {
            val cb = c.apply(base)
            if (cb.instantOff > BD_ZERO) {
                instant = instant.plus(cb.instantOff).round2()
                base = (base.minus(cb.instantOff)).max0().round2()
                steps += "Instant -${cb.instantOff} => new base=$base"
            }
            if (cb.futureRebate > BD_ZERO) {
                future = future.plus(cb.futureRebate).round2()
                steps += "Future +${cb.futureRebate}"
            }
        }
        return PaymentEvaluation(
            methodId = id,
            label = label,
            instantOff = instant,
            futureRebate = future,
            explanation = steps
        )
    }
}
private fun EventRule.toItemDiscounts(): List<ItemDiscount> = when (this) {
    is EventRule.None -> emptyList()

    is EventRule.PercentOffEach ->
        // percent 是 20.0 (代表 20%)，需要除以 100 轉為 0.2
        listOf(PercentOffEach(percentOff = this.percent.divide(BD_100, 6, RoundingMode.HALF_UP)))

    is EventRule.AmountOffEach ->
        listOf(AmountOffEach(offEach = this.amount)) // amount 已經是 BigDecimal

    is EventRule.SecondPercent ->
        listOf(NthPiecePercent(n = 2, percentForNth = this.percent.divide(BD_100, 6, RoundingMode.HALF_UP)))

    is EventRule.SecondMinus ->
        listOf(NthPieceMinus(n = 2, minusForNth = this.amount))

    is EventRule.BuyXGetY -> {
        val x = this.buyQty.toInt().coerceAtLeast(1)
        val y = this.getQty.toInt().coerceAtLeast(0)
        // 贈品計價比率：0% 表示免費，100% 表示原價
        val p = this.priceOfGetPercent.divide(BD_100, 6, RoundingMode.HALF_UP).max0().min(BD_ONE)

        if (p.compareTo(BD_ZERO) == 0) listOf(BuyNGetM(n = x, m = y))
        else listOf(BuyXGetYPercent(x = x, y = y, percentForGift = p))
    }

    is EventRule.BundleFixedPrice ->
        listOf(KForY(k = this.bundleQty.toInt().coerceAtLeast(1), y = this.bundlePrice))

    is EventRule.TierByQty ->
        listOf(TierByQtyRule(steps = this.steps))

    is EventRule.TierBySpend,
    is EventRule.Conditional,
    is EventRule.FreeShipping -> emptyList()
}
data class NthPieceMinus(
    val n: Int,
    val minusForNth: BigDecimal,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 20
) : ItemDiscount {
    init { require(n >= 2) { "n must be >= 2" } }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val qInt = qty.max0().toInt()
        val groups = qInt / n
        var sum = u.mul(bd(qInt)).minus(minusForNth.max0().mul(bd(groups))).max0().round2()
        // 分数件照原价计
        val frac = qty.minus(bd(qInt))
        if (frac > BD_ZERO) sum = sum.plus(u.mul(frac)).round2()
        return ItemDiscountResult(sum, "NthPieceMinus: every ${n}th -${minusForNth.round2()} ; subtotal=$sum")
    }
}

data class BuyXGetYPercent(
    val x: Int,
    val y: Int,
    /** 送的那 Y 件的计价比例 [0,1]；0 表示免费 */
    val percentForGift: BigDecimal,
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 21
) : ItemDiscount {
    init { require(x > 0 && y >= 0) }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val u = unitPrice.max0()
        val qInt = qty.max0().toInt()
        val group = x + y
        if (group == 0) return ItemDiscountResult(BD_ZERO, "BuyXGetYPercent: degenerate")
        val full = qInt / group
        val rem  = qInt % group
        // 每组：x 件全价 + y 件按 percentForGift
        val perGroup = u.mul(bd(x)).plus(u.mul(percentForGift).mul(bd(y)))
        var sum = perGroup.mul(bd(full))
        // 余数优先填满 x，再把落在赠送区间的余数按 percent 计算
        val payRem = kotlin.math.min(rem, x)
        val giftRem = (rem - payRem).coerceAtLeast(0)
        sum = sum.plus(u.mul(bd(payRem))).plus(u.mul(percentForGift).mul(bd(giftRem))).round2()
        // 分数件照原价
        val frac = qty.minus(bd(qInt))
        if (frac > BD_ZERO) sum = sum.plus(u.mul(frac)).round2()
        return ItemDiscountResult(sum, "BuyXGetYPercent: $x+$y with gift @${percentForGift*BD_HUNDRED}%; subtotal=$sum")
    }
}

data class TierByQtyRule(
    val steps: List<com.mealoo.app.pricing.Step>, // Step 內的 threshold/percent/amount 都是 BD
    override val exclusiveGroup: String? = "ITEM_SHAPE",
    override val order: Int = 23
) : ItemDiscount {
    init { require(steps.isNotEmpty()) }
    override fun subtotal(unitPrice: BigDecimal, qty: BigDecimal): ItemDiscountResult {
        val q = qty.max0()
        // threshold 已經是 BD，直接比較
        val matched = steps.sortedBy { it.threshold }.lastOrNull { q >= it.threshold }
            ?: return ItemDiscountResult(unitPrice.max0().mul(q).round2(), "TierByQty: no step matched")

        val explain: String
        val s: BigDecimal = when {
            matched.percent != null -> {
                // percent / 100
                val rate = matched.percent.divide(BD_100, 6, RoundingMode.HALF_UP).max0().min(BD_ONE)
                explain = "percent ${matched.percent}%@qty≥${matched.threshold}"
                unitPrice.max0().mul(BD_ONE.minus(rate)).mul(q).round2()
            }
            matched.amount != null -> {
                val offEach = matched.amount.max0()
                explain = "amount -$offEach each@qty≥${matched.threshold}"
                unitPrice.max0().minus(offEach).max0().mul(q).round2()
            }
            else -> {
                explain = "no-op step"
                unitPrice.max0().mul(q).round2()
            }
        }
        return ItemDiscountResult(s, "TierByQty: $explain -> $s")
    }
}

data class PaymentEvaluation(
    val methodId: String,
    val label: String,
    val instantOff: BigDecimal,
    val futureRebate: BigDecimal,
    val explanation: List<String>
)

// --------------------------------------------------
// Engine I/O
// --------------------------------------------------

data class PricingInput(
    val items: List<CartItem>,
    val channelPromo: ChannelPromo? = null,
    val fees: Fees = Fees(),
    val taxes: List<Tax> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val pointsValue: BigDecimal = BD_ONE,
    val currency: String = "TWD",
    val wasteCostFactor: BigDecimal = BD_ONE
)

data class ItemBreakdown(
    val sku: String,
    val name: String?,
    val qty: BigDecimal,
    val needQty: BigDecimal,
    val baseSubtotal: BigDecimal,
    val afterItemDiscountSubtotal: BigDecimal,
    val wasteQty: BigDecimal,
    val taxGroup: String?,
    val notes: List<String>,
)

data class PricingResult(
    val items: List<ItemBreakdown>,
    val rawSubtotal: BigDecimal,
    val cartSubtotalAfterItems: BigDecimal,
    val channelDiscount: BigDecimal,
    val feesTotal: BigDecimal,
    val taxAmount: BigDecimal,
    val payableBeforeBonus: BigDecimal,
    val chosenPayment: PaymentEvaluation?,
    val finalPayable: BigDecimal,
    val effectiveCost: BigDecimal,
    val effectiveUnitCostBySku: Map<String, BigDecimal>,
    val messages: List<String>,
    val detailedLogs: List<String>
)

// --------------------------------------------------
// Engine implementation
// --------------------------------------------------

object PricingEngine {

    fun calculate(input: PricingInput): Result<PricingResult, PricingError> {
        val logs = mutableListOf<String>()
        return try {
            require(input.items.isNotEmpty()) { "items required" }

            val itemBreakdowns = input.items.mapIndexed { idx, item ->
                val sku = item.name?.takeIf { it.isNotBlank() } ?: "item-$idx"
                val q = (item.Qty ?: BD_ONE).max0().round2()
                require(q >= BD_ZERO) { "Invalid quantity for $sku" }
                val base = item.unitPrice.max0().mul(q).round2()

                val discounts = item.discount?.toItemDiscounts() ?: emptyList()
                val (afterSubtotal, notes) = applyItemDiscountsGrouped(
                    unitPrice = item.unitPrice,
                    qty = q,
                    discounts = discounts,
                    logs = logs
                )

                // 由于没有“采购量/需求量”区分，waste 记 0；如将来引入 purchaseQty，再恢复计算
                val need = q
                val waste = BD_ZERO

                ItemBreakdown(
                    sku = sku,
                    name = item.name,
                    qty = q,
                    needQty = need,
                    baseSubtotal = base,
                    afterItemDiscountSubtotal = afterSubtotal,
                    wasteQty = waste,
                    taxGroup = null,
                    notes = notes
                )
            }

            val rawSubtotal = itemBreakdowns.sumOfBD { it.baseSubtotal }.round2()
            val cartAfterItems = itemBreakdowns.sumOfBD { it.afterItemDiscountSubtotal }.round2()
            logs += "Raw subtotal=$rawSubtotal; After items=$cartAfterItems"

            val totalQty = itemBreakdowns.sumOfBD { it.qty }.round2()
            val channelRes = input.channelPromo?.discount(cartAfterItems, totalQty)
                ?: ChannelPromoResult(BD_ZERO, "No channel promo")

            val channelDiscount = channelRes.discount.max0().min(cartAfterItems).round2()
            logs += "Channel promo: ${channelRes.explanation}; discount=$channelDiscount"

            val feesTotal = input.fees.total.round2()

            // 2) Taxes (multiple, ordered): build base per tax depending on scope/applyAfterFees
            val sumAfterItems = cartAfterItems
            val baseBeforeFees = (sumAfterItems.minus(channelDiscount)).round2()
            val feeBase = feesTotal

            var totalTax = BD_ZERO
            for (tax in input.taxes) {
                val base = when (tax.scope) {
                    TaxScope.CART -> if (tax.applyAfterFees) baseBeforeFees.plus(feeBase).round2() else baseBeforeFees
                    TaxScope.ITEM_GROUP -> {
                        val group = tax.taxGroup
                        val groupSum = itemBreakdowns
                            .filter { it.taxGroup == group }
                            .sumOfBD { it.afterItemDiscountSubtotal }
                            .round2()
                        val share = if (sumAfterItems > BD_ZERO) groupSum.div2(sumAfterItems) else BD_ZERO
                        val groupBaseBeforeFees = baseBeforeFees.mul(share).round2()
                        val groupFeeShare = if (tax.applyAfterFees) feeBase.mul(share).round2() else BD_ZERO
                        groupBaseBeforeFees.plus(groupFeeShare).round2()
                    }
                }
                val t = tax.amount(base).round2()
                totalTax = totalTax.plus(t).round2()
                logs += "Tax ${tax.label}: scope=${tax.scope}, group=${tax.taxGroup}, base=$base -> $t"
            }

            val payableBeforeBonus = (cartAfterItems.minus(channelDiscount).plus(feesTotal).plus(totalTax)).round2()

            // 3) Payment evaluation (single best method)
            val methods = input.paymentMethods.filter { it.enabled }.map { it.evaluate(payableBeforeBonus) }
            val noBonus = PaymentEvaluation("none", "No bonus", BD_ZERO, BD_ZERO, emptyList())
            val candidates = if (methods.isEmpty()) listOf(noBonus) else methods + noBonus

            val best = candidates.minByOrNull { eval ->
                val finalPayable = (payableBeforeBonus.minus(eval.instantOff)).max0().round2()
                finalPayable.minus(input.pointsValue.mul(eval.futureRebate)).round2()
            }

            val chosen = best
            val finalPayable = (payableBeforeBonus.minus(chosen?.instantOff ?: BD_ZERO)).max0().round2()
            val effectiveCostNoWaste = (finalPayable.minus(input.pointsValue.mul(chosen?.futureRebate ?: BD_ZERO))).round2()

            // 4) Waste adjustment
            val totalUsed = itemBreakdowns.sumOfBD { it.qty.min(it.needQty) }.round2()
            val totalQtyAll = itemBreakdowns.sumOfBD { it.qty }.round2()
            val wasteRatio = if (totalQtyAll > BD_ZERO) (totalQtyAll.minus(totalUsed)).div2(totalQtyAll) else BD_ZERO
            val effectiveCost = (effectiveCostNoWaste.minus(
                effectiveCostNoWaste.mul(wasteRatio).mul(BD_ONE.minus(input.wasteCostFactor))
            )).round2()

            // 5) Effective unit cost allocation
            val effUnitBySku = mutableMapOf<String, BigDecimal>()
            if (sumAfterItems > BD_ZERO) {
                for (b in itemBreakdowns) {
                    val share = b.afterItemDiscountSubtotal.div2(sumAfterItems)
                    val assigned = effectiveCost.mul(share).round2()
                    val used = b.qty.min(b.needQty).max(bd(0))
                    val unit = if (used > BD_ZERO) assigned.div2(used) else BD_ZERO
                    effUnitBySku[b.sku] = unit.round2()
                }
            } else {
                val usedTotal = itemBreakdowns.sumOfBD { it.qty.min(it.needQty) }.max(bd(0))
                for (b in itemBreakdowns) {
                    val used = b.qty.min(b.needQty).max(bd(0))
                    val share = if (usedTotal > BD_ZERO) used.div2(usedTotal) else BD_ZERO
                    val unit = if (used > BD_ZERO) effectiveCost.mul(share).div2(used) else BD_ZERO
                    effUnitBySku[b.sku] = unit.round2()
                }
            }

            val msgs = buildList {
                if (channelRes.discount > BD_ZERO) add("Channel promo: ${channelRes.explanation}")
                if (feesTotal > BD_ZERO) add("Fees: shipping=${input.fees.shipping}, service=${input.fees.service}")
                if (input.taxes.isNotEmpty()) add("Taxes total=$totalTax")
                if (chosen != null && chosen.methodId != "none") {
                    add("Payment: ${chosen.label} -> instant -${chosen.instantOff}, future +${chosen.futureRebate}")
                    addAll(chosen.explanation)
                }
            }
            logs += msgs

            Result.Ok(
                PricingResult(
                    items = itemBreakdowns,
                    rawSubtotal = rawSubtotal,
                    cartSubtotalAfterItems = cartAfterItems,
                    channelDiscount = channelDiscount,
                    feesTotal = feesTotal,
                    taxAmount = totalTax,
                    payableBeforeBonus = payableBeforeBonus,
                    chosenPayment = chosen,
                    finalPayable = finalPayable,
                    effectiveCost = effectiveCost,
                    effectiveUnitCostBySku = effUnitBySku,
                    messages = msgs,
                    detailedLogs = logs
                )
            )
        } catch (e: IllegalArgumentException) {
            logs += "Invalid input: ${e.message}"
            Result.Err(PricingError.InvalidInput(e.message ?: "Invalid input", logs))
        } catch (t: Throwable) {
            logs += "Calculation error: ${t.message}"
            Result.Err(PricingError.CalculationError(t.message ?: "Calculation error", t, logs))
        }
    }

    /** Global grouping: group by exclusiveGroup, pick best inside each group, then apply groups in ascending group-order.
     *  For null exclusiveGroup, treat each as a singleton group (so they don't fuse).
     */
    private fun applyItemDiscountsGrouped(
        unitPrice: BigDecimal,
        qty: BigDecimal,
        discounts: List<ItemDiscount>,
        logs: MutableList<String>
    ): Pair<BigDecimal, List<String>> {
        if (discounts.isEmpty()) return unitPrice.mul(qty).round2() to listOf("No item discount")

        // Make stable keys so that null groups don't merge; preserve declaration order per group for tie-breaks
        val withIndex = discounts.withIndex()
        val groups: Map<String, List<IndexedValue<ItemDiscount>>> = withIndex.groupBy { iv ->
            iv.value.exclusiveGroup ?: "_SINGLE_${iv.index}"
        }

        // Determine group order by min(order) of members; tie-breaker by first index
        val orderedGroups = groups.entries
            .map { (gk, members) ->
                val minOrder = members.minOf { it.value.order }
                val firstIndex = members.minOf { it.index }
                Triple(gk, members.sortedBy { it.value.order }, Pair(minOrder, firstIndex))
            }
            .sortedWith(compareBy<Triple<String, List<IndexedValue<ItemDiscount>>, Pair<Int, Int>>>({ it.third.first }, { it.third.second }))

        var currentSubtotal = unitPrice.mul(qty).round2()
        val notes = mutableListOf<String>()

        for ((groupKey, members, meta) in orderedGroups) {
            // Evaluate all members independently against the ORIGINAL base (unitPrice, qty)
            var best: ItemDiscountResult? = null
            for (m in members) {
                val r = m.value.subtotal(unitPrice, qty)
                if (best == null || r.subtotal < best!!.subtotal) best = r
            }
            if (best != null) {
                // Sequentially apply the group's best result by taking the better (lower) subtotal
                val newSubtotal = best!!.subtotal.min(currentSubtotal).round2()
                notes += if (groupKey.startsWith("_SINGLE_")) best!!.explanation else "Group[$groupKey] -> ${best!!.explanation}"
                logs += "Item group $groupKey chose: ${best!!.explanation}; subtotal from $currentSubtotal -> $newSubtotal"
                currentSubtotal = newSubtotal
            }
        }
        return currentSubtotal.round2() to notes
    }
}

// --------------------------------------------------
// Collection helpers
// --------------------------------------------------

private inline fun <T> Iterable<T>.sumOfBD(selector: (T) -> BigDecimal): BigDecimal {
    var sum = BD_ZERO
    for (e in this) {
        sum = sum.plus(selector(e)).round2()
    }
    return sum
}