package com.mealoo.app.util

import com.mealoo.app.model.InventoryItem
import java.math.BigDecimal
import java.math.RoundingMode

enum class AlertType {
    THAW_ALERT,
    LOW_STOCK_ALERT
}

data class InventoryAlert(
    val type: AlertType,
    val itemId: String,
    val priority: Int,
    val title: String,
    val subtitle: String
)

object InventoryAlertEngine {
    private val lowThresholds = mapOf(
        "pcs" to BigDecimal.ONE,
        "g" to BigDecimal("100"),
        "ml" to BigDecimal("100")
    )

    fun computeAlerts(
        items: List<InventoryItem>,
        requirements: Map<String, Double> = emptyMap()
    ): List<InventoryAlert> {
        val nameById = items.associate { it.id to it.name }
        val alerts = items.flatMap { item ->
            alertsForItem(item, nameById, requirements[item.id])
        }
        return alerts.sortedWith(
            compareByDescending<InventoryAlert> { it.priority }
                .thenBy { it.title }
        )
    }

    fun alertsForItem(
        item: InventoryItem,
        nameById: Map<String, String> = emptyMap(),
        requiredAmount: Double? = null
    ): List<InventoryAlert> {
        val alerts = mutableListOf<InventoryAlert>()
        buildThawAlert(item, nameById)?.let { alerts += it }
        buildLowAlert(item, requiredAmount)?.let { alerts += it }
        return alerts
    }

    fun lowThresholdFor(item: InventoryItem): BigDecimal? {
        return lowThresholds[item.unit.lowercase()]
    }

    private fun buildThawAlert(
        item: InventoryItem,
        nameById: Map<String, String>
    ): InventoryAlert? {
        val hasTag = item.note?.hasThawTag() == true
        val isFridge = item.location.equals("Fridge", ignoreCase = true)
        if (!hasTag || !isFridge) return null

        val fromName = item.note?.extractThawFrom()
            ?: item.parentId?.let { nameById[it] }
        val subtitle = if (fromName.isNullOrBlank()) {
            "THAW: From: -"
        } else {
            "THAW: From: $fromName"
        }

        return InventoryAlert(
            type = AlertType.THAW_ALERT,
            itemId = item.id,
            priority = 2000 + thawAgePriority(item),
            title = "THAW: ${item.name}",
            subtitle = subtitle
        )
    }

    private fun buildLowAlert(item: InventoryItem, requiredAmount: Double?): InventoryAlert? {
        // 如果沒有需求，或需求 <= 0，不視為低庫存
        if (requiredAmount == null || requiredAmount <= 0.0) return null

        val reqBD = BigDecimal.valueOf(requiredAmount)

        // 如果庫存足夠，不發警告
        if (item.quantity >= reqBD) return null

        // 計算缺口比例 (用於優先級排序)
        val ratio = if (reqBD > BigDecimal.ZERO) {
            item.quantity.divide(reqBD, 3, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // 缺口越大 (ratio 越小)，優先級越高
        val ratioScore = (ratio.toDouble() * 100).toInt().coerceIn(0, 100)
        val priority = 1000 - ratioScore

        // 顯示資訊：計畫需要 > 現有庫存
        val subtitle = "缺貨: 計畫需要 ${formatQty(reqBD)} ${item.unit} > 現有 ${formatQty(item.quantity)}"

        return InventoryAlert(
            type = AlertType.LOW_STOCK_ALERT,
            itemId = item.id,
            priority = priority,
            title = "LOW: ${item.name}",
            subtitle = subtitle
        )
    }

    private fun thawAgePriority(item: InventoryItem): Int {
        val createdAt = item.createdAt
        if (createdAt <= 0L) return 0
        val ageHours = ((System.currentTimeMillis() - createdAt) / (60 * 60 * 1000)).toInt()
        return ageHours.coerceAtMost(72)
    }

    private fun formatQty(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }
}
