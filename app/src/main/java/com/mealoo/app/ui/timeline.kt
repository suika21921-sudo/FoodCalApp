package com.mealoo.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import com.mealoo.app.model.DrinkEntry
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import com.mealoo.app.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrinkTimeline(
    drinks: List<DrinkEntry>,
    isToday: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    if (drinks.isEmpty()) return

    // 1) 按时间排序（最新在上）
    val sorted = remember(drinks) {
        drinks.sortedByDescending { it.timestamp ?: 0L }
    }

    // 2) 顶部概览（可选）
    SummaryRowForDrinks(sorted)

    // 3) 列表本体
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(sorted, key = { _, e -> e.id }) { index, entry ->
            val prev = sorted.getOrNull(index - 1)
            DrinkTimelineItem(
                entry = entry,
                showTopConnector = index != 0,
                showBottomConnector = index != sorted.lastIndex,
                intervalFromPrev = intervalLabel(prev?.timestamp, entry.timestamp),
                isNowLine = isToday && shouldShowNowLineBetween(prev?.timestamp, entry.timestamp, nowMillis),
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
private fun SummaryRowForDrinks(drinks: List<DrinkEntry>) {
    val totalMl = drinks.sumOf { (it.waterML ?: 0.0) }.toInt()
    val totalCaf = drinks.sumOf { (it.caffeine ?: 0.0) }.toInt()
    val totalKcal = drinks.sumOf { (it.kcal) }.toInt()
    // 新：糖合计（使用 sugarGrams 字段）
    val totalSugar = drinks.sumOf { (it.sugarGrams ?: 0.0) }.toInt()
    val hasAlcohol = drinks.any { (it.alcoholPercent ?: 0f) > 0f || (it.alcoholVolumeML ?: 0.0) > 0.0 }

    Text(
        text = buildString {
            append(stringResource(R.string.water))
            append("${totalMl}ml")
            if (totalCaf > 0) append(" · ☕${totalCaf}mg")
            if (totalSugar > 0) append(" · 糖${totalSugar}g")
            if (hasAlcohol) append(" · 🍺")
            append(" · ${totalKcal} kcal")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DrinkTimelineItem(
    entry: DrinkEntry,
    showTopConnector: Boolean,
    showBottomConnector: Boolean,
    intervalFromPrev: String?,
    isNowLine: Boolean,
    modifier: Modifier = Modifier
) {
    val colorOutline = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ){

        // 左：竖线 + 圆点（保留你的判色逻辑）
        Box(
            modifier = Modifier
                .width(32.dp)
                .padding(vertical = 2.dp)
                .drawBehind {
                    val cx = size.width / 2f
                    val top = if (showTopConnector) 0f else size.height / 2f
                    val bottom = if (showBottomConnector) size.height else size.height / 2f
                    drawLine(
                        color = colorOutline,
                        start = Offset(cx, top),
                        end = Offset(cx, bottom),
                        strokeWidth = 2f
                    )
                }
        ) {
            val dotSize = 10.dp
            val dotColor = when {
                (entry.alcoholPercent ?: 0f) > 0f || (entry.alcoholVolumeML ?: 0.0) > 0.0 ->
                    MaterialTheme.colorScheme.error
                (entry.caffeine ?: 0.0) > 0.0 ->
                    MaterialTheme.colorScheme.primary
                else ->
                    MaterialTheme.colorScheme.secondary
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.Center)
                    .background(dotColor, shape = CircleShape)
            )
        }

        // 右：时间 + 内容
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp) // 原 12.dp -> 8.dp 更紧凑
        ) {
            // “现在”横线与小标签（仅今日）
            if (isNowLine) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.now), // 若无此字符串，可改成 "Now"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            if (!intervalFromPrev.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.interval_fmt, intervalFromPrev),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(2.dp))
            }

            val time = entry.timestamp?.let { formatTimeHHmm(it) } ?: "??:??"
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(48.dp) // 固定列宽，保证对齐
                )

                val ml   = (entry.waterML ?: 0.0).toInt()
                val caf  = (entry.caffeine ?: 0.0).toInt()
                val sug  = (entry.sugarGrams ?: 0.0).toInt()
                val kcal = entry.kcal.toInt()
                val displayName = entry.name.ifBlank { null }

                Column {
                    // 主行：名称（有才显示；单行省略）
                    displayName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // 新：用 Chip 展示关键信息（替换原来的 -xxx 文本行）
                    StatChips(kcal = kcal, ml = ml, sugarG = sug, cafMg = caf)
                }
            }
        }
    }
}
// --- 新增：小胶囊 Chip ---
@Composable
private fun StatChip(text: String) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatChips(
    kcal: Int, ml: Int, sugarG: Int, cafMg: Int
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (kcal > 0) StatChip("${kcal} kcal")
        if (ml   > 0) StatChip("${ml} ml")
        if (sugarG > 0) StatChip(text=("${sugarG} g"+stringResource(R.string.sugar)))
        if (cafMg > 0) StatChip("${cafMg} mg ☕")
    }
}

private fun formatTimeHHmm(millis: Long): String {
    val df = SimpleDateFormat("HH:mm", Locale.getDefault())
    return df.format(Date(millis))
}

private fun intervalLabel(prev: Long?, curr: Long?): String? {
    if (prev == null || curr == null) return null
    val delta = kotlin.math.abs(curr - prev)
    val h = (delta / 3600000L).toInt()
    val m = ((delta % 3600000L) / 60000L).toInt()
    return when {
        h == 0 && m == 0 -> null
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h${m}m"
    }
}

private fun shouldShowNowLineBetween(prev: Long?, curr: Long?, now: Long): Boolean {
    if (curr == null) return false
    val upper = prev ?: Long.MAX_VALUE
    return now in (curr + 1) .. upper
}
