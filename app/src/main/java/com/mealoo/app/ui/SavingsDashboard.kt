package com.mealoo.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mealoo.app.R
import com.mealoo.app.data.repository.SubscriptionRepository
import com.mealoo.app.ui.design.ProFeatureDialog
import com.mealoo.app.data.DiscountEventDataStore
import com.mealoo.app.data.SavingsRecord
import com.mealoo.app.db.SavingsRepository
import com.mealoo.app.db.ShopRepository
import com.mealoo.app.model.AppCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mealoo.app.ui.design.MealooTechColors
import com.mealoo.app.ui.design.mealooTechGlassBorder

private enum class TrendWindow(val days: Int) {
    DAYS_7(7),
    DAYS_30(30)
}
data class MonthlyKpiData(
    val currency: String,
    val cost: BigDecimal,
    val savings: BigDecimal,
    val rate: Double
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsDashboard(
    savingsRepo: SavingsRepository,
    shopRepo: ShopRepository,
    eventStore: DiscountEventDataStore,
    subscriptionRepo: SubscriptionRepository,
    onBackToMenu: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isPro by subscriptionRepo.isPro.collectAsState()
    var showProDialog by remember { mutableStateOf(false) }

    val savingsState by savingsRepo.savingsState.collectAsState()
    val records = savingsState.records

    val shops by shopRepo.shopsFlow().collectAsState(initial = emptyList())
    val events by eventStore.eventsFlow().collectAsState(initial = emptyList())
    val shopNameById = remember(shops) { shops.associate { it.id to it.name } }
    val eventTitleById = remember(events) { events.associate { it.id to it.title } }

    val zone = remember { ZoneId.of("Asia/Taipei") }
    val monthPrefix = YearMonth.now(zone).toString() // yyyy-MM
    val today = LocalDate.now(zone)

    var window by rememberSaveable { mutableStateOf(TrendWindow.DAYS_7) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (!isPro) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.savings_dashboard_title), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackToMenu) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = MealooTechColors.DeepBackground
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MealooTechColors.AccentCyan,
                    modifier = Modifier.size(80.dp).padding(bottom = 24.dp)
                )
                Text(
                    text = "Pro Feature",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Upgrade to Pro to access detailed savings analytics, tracking, and insights.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = { showProDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MealooTechColors.AccentCyan.copy(alpha = 0.2f),
                        contentColor = MealooTechColors.AccentCyan
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                        .mealooTechGlassBorder(CircleShape, MealooTechColors.AccentCyan.copy(alpha = 0.5f))
                ) {
                    Text("Unlock with Pro", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showProDialog) {
            ProFeatureDialog(
                onDismiss = { showProDialog = false },
                onUpgrade = {
                    showProDialog = false
                    // Ideally we should navigate, but we don't have navController here.
                    // The user can go back and navigate to settings.
                }
            )
        }
        return
    }

    // --- Currency Logic ---
    // 检测是否有多种币别
    val currencySet = remember(records) { records.mapNotNull { it.currency.takeIf { c -> c.isNotBlank() } }.toSet() }
    val isMixedCurrency = currencySet.size > 1
    val primaryCurrency = currencySet.firstOrNull() ?: AppCurrency.USD.code

    fun fmtMoney(amount: BigDecimal, currency: String? = null): String {
        val validCurrency = currency?.takeIf { it.isNotBlank() }
        val cur = validCurrency ?: if (isMixedCurrency) "MIXED" else primaryCurrency
        val v = amount.setScale(2, RoundingMode.HALF_UP)
        val symbol = if (cur == "MIXED") "MIXED" else AppCurrency.fromCode(cur).symbol
        return "$symbol ${v.toPlainString()}"
    }

    // --- KPI Calculations (依幣別獨立計算本月數據) ---
    val monthlyKpis = remember(records, monthPrefix, primaryCurrency) {
        val currentMonthRecords = records.filter { it.date.startsWith(monthPrefix) }
        val grouped = currentMonthRecords.groupBy {
            it.currency.takeIf { c -> c.isNotBlank() } ?: primaryCurrency
        }

        grouped.map { (currency, recs) ->
            val totalOriginal = recs.fold(BigDecimal.ZERO) { acc, r -> acc + r.original }
            val totalSavings = recs.fold(BigDecimal.ZERO) { acc, r -> acc + r.savings }
            val totalCost = recs.fold(BigDecimal.ZERO) { acc, r -> acc + r.afterDiscount }
            val rate = if (totalOriginal > BigDecimal.ZERO) {
                totalSavings.divide(totalOriginal, 6, RoundingMode.HALF_UP).toDouble()
            } else 0.0

            MonthlyKpiData(currency, totalCost, totalSavings, rate)
        }.sortedByDescending { it.cost } // 依花費金額降冪排列
    }

    // --- Trend Data ---
    val dateFmt = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val trendDates = remember(window, today, dateFmt) {
        val n = window.days
        (0 until n).map { idx ->
            today.minusDays((n - 1 - idx).toLong()).format(dateFmt)
        }
    }

    // 將資料依據「幣別」分組，再依據「日期」展開為多組趨勢線
    val trendDataByCurrency = remember(records, trendDates) {
        val groupedByCurrency = records.groupBy { it.currency.takeIf { c -> c.isNotBlank() } ?: primaryCurrency }
        groupedByCurrency.mapValues { (_, curRecords) ->
            val byDate = curRecords.groupBy { it.date }
            trendDates.map { d ->
                byDate[d]?.fold(BigDecimal.ZERO) { acc, r -> acc + r.afterDiscount }?.toDouble() ?: 0.0
            }
        }
    }

    // 取得全局最大值作為 Y 軸基準
    val globalTrendMax = remember(trendDataByCurrency) {
        trendDataByCurrency.values.flatten().maxOrNull()?.takeIf { it > 0 } ?: 0.0
    }

    // --- Rankings ---
    val topShops = remember(records) {
        records
            .filter { it.shopId != null }
            .groupBy { it.shopId!! }
            .map { (id, rs) -> id to rs.fold(BigDecimal.ZERO) { acc, r -> acc + r.afterDiscount } }
            .sortedByDescending { it.second }
            .take(5)
    }
    val topEvents = remember(records) {
        records
            .filter { it.eventId != null }
            .groupBy { it.eventId!! }
            .map { (id, rs) -> id to rs.fold(BigDecimal.ZERO) { acc, r -> acc + r.afterDiscount } }
            .sortedByDescending { it.second }
            .take(5)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MealooTechColors.DeepBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.savings_dashboard_title), color = Color.White) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Empty State
            if (records.isEmpty()) {
                EmptySavingsCard()
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .mealooTechGlassBorder(RoundedCornerShape(20.dp))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MealooTechColors.AccentCyan)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.savings_trend_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrendChip(
                                selected = window == TrendWindow.DAYS_7,
                                label = stringResource(R.string.savings_last_7_days),
                                onClick = { window = TrendWindow.DAYS_7 }
                            )
                            TrendChip(
                                selected = window == TrendWindow.DAYS_30,
                                label = stringResource(R.string.savings_last_30_days),
                                onClick = { window = TrendWindow.DAYS_30 }
                            )
                        }
                    }

                    FinancialLineChart(
                        dates = trendDates,
                        dataByCurrency = trendDataByCurrency,
                        globalMax = globalTrendMax
                    )
                }
            }
            if (monthlyKpis.isNotEmpty()) {
                MonthlyKpiCard(kpis = monthlyKpis)
            }

            Text(
                text = stringResource(R.string.recent_records),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            records.sortedByDescending { it.date }.forEach { record ->
                SavingsRecordRow(
                    record = record,
                    shopName = record.shopId?.let { shopNameById[it] },
                    eventName = record.eventId?.let { eventTitleById[it] },
                    onDelete = { scope.launch { savingsRepo.deleteRecord(record.id) } },
                    formattedCost = fmtMoney(record.afterDiscount, record.currency)
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.clear_all_records_title)) },
            text = { Text(stringResource(R.string.clear_all_records_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { savingsRepo.clearAll() }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// --- Components ---

@Composable
private fun EmptySavingsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .mealooTechGlassBorder(RoundedCornerShape(20.dp))
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Event, null, modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.5f))
            Text(
                text = stringResource(R.string.savings_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.savings_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
private val BD_100 = BigDecimal.valueOf(100)
@Composable
private fun SavingsRecordRow(
    record: SavingsRecord,
    shopName: String?,
    eventName: String?,
    formattedCost: String,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .mealooTechGlassBorder(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    if (record.itemCount > 0) {
                        Text(
                            text = " · ${record.itemCount} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    text = shopName ?: eventName ?: "未知來源",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedCost,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MealooTechColors.AccentCyan
                )
                // Rate
                if (record.savingsRate.compareTo(BigDecimal.ZERO) > 0){
                    val pctText = record.savingsRate
                        .multiply(BD_100)
                        .setScale(1, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString()

                    Text(
                        text = "-$pctText%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MealooTechColors.AccentMagenta
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthlyKpiCard(kpis: List<MonthlyKpiData>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .mealooTechGlassBorder(RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 標題
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = MealooTechColors.AccentCyan
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.savings_kpi_monthly_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 各幣別獨立數據
            kpis.forEachIndexed { index, kpi ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MealooTechColors.TechBorder.copy(alpha = 0.5f)
                    )
                }

                val symbol = AppCurrency.fromCode(kpi.currency).symbol
                val fmtCost = "$symbol ${kpi.cost.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
                val fmtSavings = "$symbol ${kpi.savings.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
                val ratePct = (kpi.rate * 100).toBigDecimal().setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 幣別徽章
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MealooTechColors.AccentCyan.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MealooTechColors.AccentCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = kpi.currency,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MealooTechColors.AccentCyan
                        )
                    }

                    // 數據區塊
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.savings_kpi_total_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = fmtCost,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.savings_kpi_total_saved),
                                style = MaterialTheme.typography.labelSmall,
                                color = MealooTechColors.AccentMagenta.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = fmtSavings,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MealooTechColors.AccentMagenta
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "($ratePct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MealooTechColors.AccentMagenta.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendChip(selected: Boolean, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MealooTechColors.AccentCyan.copy(alpha = 0.15f) else Color.Transparent,
            labelColor = if (selected) MealooTechColors.AccentCyan else Color.White.copy(alpha = 0.5f),
            trailingIconContentColor = if (selected) MealooTechColors.AccentCyan else Color.White.copy(alpha = 0.5f)
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = if (selected) MealooTechColors.AccentCyan.copy(alpha = 0.5f) else MealooTechColors.TechBorder.copy(alpha = 0.5f)
        ),
        shape = CircleShape
    )
}

@Composable
private fun FinancialLineChart(
    dates: List<String>,
    dataByCurrency: Map<String, List<Double>>,
    globalMax: Double
) {
    val colors = listOf(
        MealooTechColors.AccentCyan,
        MealooTechColors.AccentMagenta,
        MealooTechColors.NeonP,
        Color(0xFFBB86FC), // Light Purple
        Color(0xFFFFB74D)  // Orange
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
        // 1. 多幣別圖例 (Legend)
        if (dataByCurrency.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                dataByCurrency.keys.forEachIndexed { index, currency ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors[index % colors.size])
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 2. 趨勢折線圖 (Canvas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val xStep = if (dates.size > 1) width / (dates.size - 1) else width

                // 繪製背景水平網格線 (財報視覺感)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height - (height * (i.toFloat() / gridLines))
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // 繪製各幣別的折線與資料點
                dataByCurrency.entries.forEachIndexed { index, entry ->
                    val color = colors[index % colors.size]
                    val values = entry.value

                    val path = Path()
                    values.forEachIndexed { i, value ->
                        val x = i * xStep
                        val normalizedY = if (globalMax > 0) (value / globalMax).toFloat() else 0f
                        val y = height - (normalizedY * height * 0.85f) - (height * 0.05f)

                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    // 畫線
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round
                        )
                    )
                    if (dates.size <= 7) {
                        val textPaint = android.graphics.Paint().apply {
                            this.color = color.toArgb() // 文字顏色與折線一致
                            this.textSize = 32f // 文字大小
                            this.textAlign = android.graphics.Paint.Align.CENTER
                            this.isAntiAlias = true
                        }

                        values.forEachIndexed { i, value ->
                            val x = i * xStep
                            val normalizedY = if (globalMax > 0) (value / globalMax).toFloat() else 0f
                            val y = height - (normalizedY * height * 0.85f) - (height * 0.05f)

                            // 將數值轉為小數點後1位並移除多餘的0 (例如: 150.0 變 150)
                            val text = value.toBigDecimal()
                                .setScale(1, java.math.RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                                .toPlainString()

                            drawContext.canvas.nativeCanvas.drawText(
                                text,
                                x,
                                y - 20f, // 往上微調，讓文字浮在折線上方
                                textPaint
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (dates.isNotEmpty()) {
                Text(
                    text = dates.first().takeLast(5), // MM-dd
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
                if (dates.size > 2) {
                    Text(
                        text = dates[dates.size / 2].takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                if (dates.size > 1) {
                    Text(
                        text = dates.last().takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}