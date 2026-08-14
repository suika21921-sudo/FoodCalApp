package com.mealoo.app.data

import java.math.BigDecimal

sealed class ThawError(open val message: String) {
    data class NotFound(val id: String) : ThawError("找不到库存项目")
    data class InvalidQuantity(val raw: Double) : ThawError("数量不合法")
    data class InsufficientQuantity(val available: BigDecimal, val requested: BigDecimal) : ThawError("库存数量不足")
}
