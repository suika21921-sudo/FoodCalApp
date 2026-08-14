@file:UseSerializers(BigDecimalAsStringSerializer::class)

package com.mealoo.app.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode
import android.content.Context
import com.mealoo.app.data.CartItem
import com.mealoo.app.data.DiscountEvent
import com.mealoo.app.data.DiscountEventDataStore
import com.mealoo.app.data.DiscountItem as DS_DiscountItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.mealoo.app.data.DiscountRepository
import com.mealoo.app.pricing.EventRule

/* ---------------------------
   BigDecimal <-> String 序列化
   --------------------------- */
object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        // 用 plain string，避免科学计数法
        encoder.encodeString(value.stripTrailingZeros().toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val s = decoder.decodeString().trim()
        return if (s.isEmpty()) BigDecimal.ZERO else BigDecimal(s)
    }
}

/* ==========
   基本规则
   ========== */
const val SCALE_MONEY = 2
const val SCALE_QTY = 3
val RM: RoundingMode = RoundingMode.HALF_UP
fun BigDecimal.percentToRatio(): BigDecimal =
    this.divide(BigDecimal("100"), 6, RM)

/* =========================
   折扣活动（应用存储侧 DTO）
   ========================= */

@Serializable
data class DiscountEventDTO(
    val id: String,
    val title: String,
    val items: List<DiscountEventItemDTO>,
    val channelPromo: ChannelPromoDTO? = null,
    val fees: FeesDTO? = null,
    val taxes: List<TaxDTO> = emptyList(),
    val paymentMethods: List<PaymentMethodDTO> = emptyList(),
    val wasteCostFactor: BigDecimal = BigDecimal.ZERO
)

@Serializable
data class DiscountEventItemDTO(
    val sku: String,
    val name: String? = null,
    val unitPrice: BigDecimal,
    val qty: BigDecimal,
    val needQty: BigDecimal? = null,
    // 三选一：memberPrice > amountOffEach > percentOff
    val memberPrice: BigDecimal? = null,
    val amountOffEach: BigDecimal? = null,
    val percentOff: BigDecimal? = null,
    val exclusiveGroup: String? = "PRICE",
    val order: Int = 0
)

/* =======================
   整单/费用/税/付款法 DTO
   ======================= */

@Serializable
data class ChannelPromoDTO(
    val type: ChannelPromoType,
    val threshold: BigDecimal,
    val percentOff: BigDecimal? = null,
    val fixedOff: BigDecimal? = null,
    val repeatable: Boolean = false
)
enum class ChannelPromoType { PERCENT_OFF, FIXED_OFF }

@Serializable
data class FeesDTO(
    val shipping: BigDecimal = BigDecimal.ZERO,
    val service: BigDecimal = BigDecimal.ZERO
)

@Serializable
data class TaxDTO(
    val type: TaxType,
    val rateOrAmount: BigDecimal,
    val scope: TaxScope = TaxScope.CART,
    val includeFees: Boolean = true
)
enum class TaxType { PERCENT, FIXED }

@Serializable
data class PaymentMethodDTO(
    val name: String,
    val components: List<PaymentComponentDTO> = emptyList()
)

@Serializable
data class PaymentComponentDTO(
    val type: PaymentComponentType,
    val value: BigDecimal,
    val isInstant: Boolean
)
enum class PaymentComponentType { PERCENT, FIXED }

/* ==========================
   DTO → 引擎模型：单品项映射
   ========================== */

fun DiscountEventItemDTO.toCartItem(): CartItem {
    val base = unitPrice.money()
    val qNeed = (needQty ?: qty).qty()

    // [Refactor] 這裡建構 EventRule 時直接傳入 BigDecimal
    val rule: EventRule? = when {
        // 會員價：轉為 AmountOffEach
        memberPrice != null -> {
            val mp = memberPrice.money()
            val offEach = base.minus(mp).max(BigDecimal.ZERO)
            if (offEach > BigDecimal.ZERO) EventRule.AmountOffEach(offEach) // ✅ 傳入 BD
            else EventRule.None
        }
        // 每件減固定金額
        amountOffEach != null -> {
            val off = amountOffEach.money()
            if (off > BigDecimal.ZERO) EventRule.AmountOffEach(off) // ✅ 傳入 BD
            else EventRule.None
        }
        // 每件打折
        percentOff != null && percentOff > BigDecimal.ZERO -> {
            EventRule.PercentOffEach(percentOff) // ✅ 傳入 BD
        }
        else -> null
    }

    return CartItem(
        image = null,
        name = name ?: sku,
        unitPrice = base,
        Qty = qNeed,
        discount = rule
    )
}
private fun BigDecimal?.safeMoney(): BigDecimal =
    (this ?: BigDecimal.ZERO).setScale(SCALE_MONEY, RM)

private fun BigDecimal?.safeQty(): BigDecimal =
    (this ?: BigDecimal.ONE).setScale(SCALE_QTY, RM)
/* ==========================
   DTO → 引擎输入：PricingInput
   ========================== */

fun DiscountEventDTO.toPricingInput(): PricingInput = PricingInput(
    items = items.map { it.toCartItem() },
    channelPromo = channelPromo.toEngine(),
    fees = fees.toEngine(),
    taxes = taxes.toEngineTaxes(),
    paymentMethods = paymentMethods.toEnginePaymentMethods(),
    wasteCostFactor = wasteCostFactor
)
private fun DS_DiscountItem.toDto(): DiscountEventItemDTO =
    DiscountEventItemDTO(
        sku = (name.takeIf { it.isNotBlank() } ?: "item-$id").trim(),
        name = name,
        // [Fix] 這裡原本調用 .bdMoney() (for Double?)，改用 safeMoney() (for BigDecimal?)
        unitPrice = this.unitPrice.safeMoney(),
        qty = this.qty.safeQty(),
        needQty = null,
        memberPrice = null,
        amountOffEach = null,
        percentOff = null,
        exclusiveGroup = "PRICE",
        order = 0
    )

private fun DiscountEvent.toDto(): DiscountEventDTO =
    DiscountEventDTO(
        id = id,
        title = title,
        items = items.map { it.toDto() },
        channelPromo = null,
        fees = null,
        taxes = emptyList(),
        paymentMethods = emptyList(),
        wasteCostFactor = BigDecimal.ZERO
    )

suspend fun buildPricingInputFromEventId(context: Context, eventId: String): PricingInput {
    val repo = DiscountRepository(
        eventStore = DiscountEventDataStore(context)
    )
    val event = repo.getById(eventId)
        ?: return PricingInput(items = emptyList(), fees = Fees(), taxes = emptyList(), paymentMethods = emptyList())
    val dto = event.toDto()
    return dto.toPricingInput()
}

// 便捷：由「購物清單已選中的活動 id」構建（若沒選就空）
fun buildPricingInputFromSelectedEventBlocking(context: Context, cart: com.mealoo.app.data.ShoppingCartStore): PricingInput =
    runBlocking {
        val selected = cart.selectedEventIdFlow().first()
        if (selected == null) PricingInput(items = emptyList()) else buildPricingInputFromEventId(context, selected)
    }

object PricingFacade {
    suspend fun calculateByEventId(context: Context, eventId: String): Result<PricingResult, PricingError> {
        val repo = DiscountRepository(
            eventStore = DiscountEventDataStore(context)
        )
        val event = repo.getById(eventId)
            ?: return Result.Err(PricingError.InvalidInput("Discount event not found: $eventId"))

        val dto = try {
            event.toDto()
        } catch (e: Exception) {
            return Result.Err(PricingError.InvalidInput("Failed to map Event -> DTO: ${e.message}"))
        }

        val input = try {
            dto.toPricingInput()
        } catch (e: Exception) {
            return Result.Err(PricingError.InvalidInput("Failed to map DTO -> PricingInput: ${e.message}"))
        }

        return try {
            PricingEngine.calculate(input)
        } catch (e: Throwable) {
            Result.Err(PricingError.CalculationError("Engine failure: ${e.message}", e))
        }
    }
}
/* ==========================
   DTO → 引擎模型：整单/费用/税/付款法
   ========================== */



fun FeesDTO?.toEngine(): Fees = this?.let {
    Fees(
        shipping = shipping.money(),
        service = service.money()
    )
} ?: Fees()
// --- Channel promo (CART級滿減/打折) ---
fun ChannelPromoDTO?.toEngine(): ChannelPromo? = this?.let { p ->
    when (p.type) {
        ChannelPromoType.PERCENT_OFF -> {
            // 滿額百分比折扣；可重複套用（repeatable）
            CartThresholdPercentOff(
                thresholdAmount = p.threshold.money(),
                percentOff = (p.percentOff ?: BigDecimal.ZERO).percentToRatio(),
                cap = null,
                repeatable = p.repeatable
            )
        }
        ChannelPromoType.FIXED_OFF -> {
            // 滿額固定金額折；可重複套用（repeatable）
            CartThresholdFixedOff(
                thresholdAmount = p.threshold.money(),
                offAmount = (p.fixedOff ?: BigDecimal.ZERO).money(),
                repeatable = p.repeatable
            )
        }
    }
}

// --- 税 (支持 CART / ITEM_GROUP；是否含費用等行為由引擎處理) ---
fun List<TaxDTO>.toEngineTaxes(): List<Tax> = this.map { t ->
    when (t.type) {
        TaxType.PERCENT -> PercentTax(
            rate = t.rateOrAmount.percentToRatio(),
            applyAfterFees = t.includeFees,
            scope = t.scope,          // 直接沿用你在 DTO 中的 scope（同名枚舉）
            taxGroup = null,
            label = "PercentTax"
        )
        TaxType.FIXED -> FixedTax(
            amountFixed = t.rateOrAmount.money(),
            applyAfterFees = t.includeFees,
            scope = t.scope,
            taxGroup = null,
            label = "FixedTax"
        )
    }
}

// --- 支付法 (即時/回饋分離；引擎會自動挑最佳方案) ---
fun List<PaymentMethodDTO>.toEnginePaymentMethods(): List<PaymentMethod> = this.map { pm ->
    val comps: List<PayComponent> = pm.components.mapIndexed { idx, c ->
        when (c.type) {
            PaymentComponentType.PERCENT -> PercentRebate(
                percent = c.value.percentToRatio(),
                isInstant = c.isInstant,
                cap = null,
                minSpend = BigDecimal.ZERO.money(),
                order = idx
            )
            PaymentComponentType.FIXED -> FixedAmount(
                amount = c.value.money(),
                isInstant = c.isInstant,
                minSpend = BigDecimal.ZERO.money(),
                order = idx
            )
        }
    }
    PaymentMethod(
        id = pm.name,
        label = pm.name,
        components = comps,
        enabled = true
    )
}
