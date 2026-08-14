@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.mealoo.app.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealoo.app.R
import com.mealoo.app.data.DailyMealConfig
import com.mealoo.app.data.DailyMealConfigStore
import com.mealoo.app.data.MealDefsStore
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.data.UserPreference
import com.mealoo.app.data.repository.SubscriptionRepository
import com.mealoo.app.ui.design.ProFeatureDialog
import com.mealoo.app.model.MealKind
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.MenuPlanSummary
import com.mealoo.app.model.SelectedFood
import com.mealoo.app.model.UNCLASSIFIED_MEAL_ID
import com.mealoo.app.ui.design.MealooDimens
import com.mealoo.app.ui.design.MealooTheme
import com.mealoo.app.util.stripBrackets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.UUID

private object SandboxTechColors {
    val DeepBackground = Color(0xFF030508)
    val GlassCard = Color(0xFF0D1117).copy(alpha = 0.85f)
    val AccentCyan = Color(0xFF00D2FF)
    val AccentMagenta = Color(0xFFFF2D55)
    val TechBorder = Color(0xFF30363D)
    val CardGlow = Color(0xFF00D2FF).copy(alpha = 0.1f)
    val NeonP = Color(0xFF00FF88) // Protein
    val NeonC = Color(0xFF00D2FF) // Carbs
    val NeonF = Color(0xFFFF9F0A) // Fats
}

@Composable
private fun Modifier.techGlassBorder(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    borderColor: Color = SandboxTechColors.TechBorder
) = this.border(1.dp, borderColor, shape)

@Composable
private fun Modifier.techGlow(
    color: Color = SandboxTechColors.AccentCyan,
    alpha: Float = 0.15f
) = this.background(
    androidx.compose.ui.graphics.Brush.radialGradient(
        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
        radius = 800f
    )
)


private enum class SandboxTool { SCALE, RATIO, COMPARE }
private enum class SnapshotSlot { A, B }

enum class SimulationStrategy {
    REPEAT_DRAFT,
    CALENDAR_DATA
}

data class SimulationResult(
    val days: Int,
    val totalInputKcal: Double,
    val totalTargetKcal: Double,
    val avgDailyP: Double,
    val avgDailyC: Double,
    val avgDailyF: Double,
    val weightChangeRange: ClosedRange<Double>,
    val strategy: SimulationStrategy
)
private data class MealTypeOption(
    val key: String,
    val label: String,
    val color: Color
)

private data class PlanChange(
    val planId: String,
    val mealType: String?,
    val recipeName: String,
    val beforeServings: Double?,
    val afterServings: Double?,
    val beforeKcal: Double?,
    val afterKcal: Double?
)

private data class KcalSummary(
    val totalKcal: Double,
    val perMealKcal: Map<String, Double>
)

private data class MealRatioRow(
    val mealLabel: String,
    val mealTypeKey: String?,
    val ratioPercent: Double,
    val currentKcal: Double,
    val targetKcal: Double
)

private data class SandboxWorkspaceState(
    val selectedDate: String,
    val selectedMealType: String? = null,
    val planSource: List<MealPlanEntry> = emptyList(),
    val draftPlans: List<MealPlanEntry> = emptyList(),
    val dailyMealConfig: DailyMealConfig? = null,
    val isDirty: Boolean = false,
    val changeLog: List<PlanChange> = emptyList(),
    val history: List<List<MealPlanEntry>> = emptyList(),
    val targetKcal: Double = 2000.0,
    val targetKcalInput: String = "2000",
    val snapshotA: List<MealPlanEntry>? = null,
    val snapshotB: List<MealPlanEntry>? = null,
    val simulationResult: SimulationResult? = null,
    val isSimulating: Boolean = false,
    val rangeData: Map<String, List<MealPlanEntry>> = emptyMap(),
    val simulationStart: LocalDate = LocalDate.now(),
    val simulationEnd: LocalDate = LocalDate.now().plusDays(6)
)

private class SandboxWorkspaceViewModel(
    private val planStore: MealPlanRepository,
    private val scope: CoroutineScope,
    initialDate: String,
    initialTargetKcal: Double
) {
    var state by mutableStateOf(
        SandboxWorkspaceState(
            selectedDate = initialDate,
            targetKcal = initialTargetKcal,
            targetKcalInput = initialTargetKcal.roundToInt().toString()
        )
    )
        private set

    init {
        fetchRangeData()
    }

    fun onDateSelected(date: String) {
        val cachedBaseline = state.rangeData[date] ?: emptyList()
        state = state.copy(
            selectedDate = date,
            planSource = cachedBaseline
        )
    }
    fun applyPlan(plan: MenuPlanSummary) {
        val startDate = state.simulationStart
        val planDays = plan.dayCount.coerceAtLeast(1)
        val newEnd = startDate.plusDays((planDays - 1).toLong()) // 根據方案天數計算新的結束日

        // 1. 將方案內的相對天數 (Day 1, 2...) 轉為絕對日期 (YYYY-MM-DD)
        val newEntries = plan.entries.mapNotNull { entry ->
            val dayOffset = (entry.date.toIntOrNull() ?: 1) - 1
            if (dayOffset < 0 || dayOffset >= planDays) return@mapNotNull null
            val actualDateStr = startDate.plusDays(dayOffset.toLong()).toString()

            entry.copy(
                id = UUID.randomUUID().toString(), // 重新給予 UUID 避免衝突
                date = actualDateStr
            )
        }

        // 2. 清除該範圍內舊的 Draft 資料 (Overwrite 邏輯)
        val targetDates = (0 until planDays).map { startDate.plusDays(it.toLong()).toString() }.toSet()
        val cleanDraft = state.draftPlans.filterNot { it.date in targetDates }

        // 3. 更新狀態：延展模擬範圍，並寫入新的 Draft
        state = state.copy(simulationEnd = newEnd)
        setDraft(cleanDraft + newEntries, pushHistory = true)

        // 4. 重新拉取 Baseline 資料以符合新的日期範圍
        fetchRangeData()
    }

    fun setSimulationRange(start: LocalDate, end: LocalDate) {
        if (end.isBefore(start)) return
        state = state.copy(simulationStart = start, simulationEnd = end)
        fetchRangeData()
    }

    private fun fetchRangeData() {
        scope.launch {
            val start = state.simulationStart
            val end = state.simulationEnd
            val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
            val newData = mutableMapOf<String, List<MealPlanEntry>>()

            for (i in 0 until days) {
                val dateStr = start.plusDays(i.toLong()).toString()
                val entries = planStore.entriesFlow(dateStr).first()
                newData[dateStr] = entries
            }
            state = state.copy(rangeData = newData)
        }
    }

    fun addDirectMacroToDraft(name: String, kcal: Double, p: Double, c: Double, f: Double, dateOverride: String? = null) {
        val targetDate = dateOverride ?: state.selectedDate
        val resolvedMealId = if (targetDate == state.selectedDate) state.selectedMealType else null
        val mealId = resolvedMealId ?: UNCLASSIFIED_MEAL_ID
        val entry = MealPlanEntry(
            id = UUID.randomUUID().toString(),
            date = targetDate,
            mealId = mealId,
            recipeId = "",
            name = name,
            servings = 1.0,
            kcal = kcal,
            protein = p,
            carbs = c,
            fats = f,
            cholesterol = 0.0
        )

        if (targetDate == state.selectedDate) {
            val updatedDraft = state.draftPlans + entry
            setDraft(updatedDraft, pushHistory = true)
        } else {
            scope.launch {
                val existing = planStore.entriesFlow(targetDate).first()
                planStore.replaceEntries(targetDate, existing + entry)
                fetchRangeData()
            }
        }
    }

    fun addFoodsToDraft(foods: List<SelectedFood>, mealTypeOverride: String?, dateOverride: String? = null) {
        val targetDate = dateOverride ?: state.selectedDate
        val resolvedMealId = mealTypeOverride ?: (if (targetDate == state.selectedDate) state.selectedMealType else null)
        val mealId = resolvedMealId ?: UNCLASSIFIED_MEAL_ID
        val newEntries = foods.map { food ->
            MealPlanEntry(
                id = UUID.randomUUID().toString(),
                date = targetDate,
                mealId = mealId,
                recipeId = if (food.source == "RECIPE") "" else food.fdcId.toString(),
                name = food.name,
                servings = 1.0, // Servings 保持 1.0，我們主要看 grams
                kcal = food.standardNutrients["Energy"] ?: 0.0,
                protein = food.standardNutrients["Protein"] ?: 0.0,
                carbs = food.standardNutrients["Available carbohydrate"] ?: food.standardNutrients["Carbohydrate, by difference"] ?: 0.0,
                fats = food.standardNutrients["Total lipid (fat)"] ?: 0.0,
                cholesterol = 0.0,
                grams = food.grams
            )
        }

        if (targetDate == state.selectedDate) {
            val updatedDraft = state.draftPlans + newEntries
            setDraft(updatedDraft, pushHistory = true)
        } else {
            scope.launch {
                val existing = planStore.entriesFlow(targetDate).first()
                planStore.replaceEntries(targetDate, existing + newEntries)
                fetchRangeData()
            }
        }
    }

    fun updateDailyConfig(cfg: DailyMealConfig?) {
        state = state.copy(dailyMealConfig = cfg)
    }

    fun updateTargetKcal(input: String) {
        val cleaned = input.filter { it.isDigit() || it == '.' }
        val parsed = cleaned.toDoubleOrNull()
        val next = when {
            cleaned.isBlank() -> 0.0
            parsed != null -> parsed
            else -> state.targetKcal
        }
        state = state.copy(targetKcalInput = cleaned, targetKcal = next)
    }

    fun setMealType(mealType: String?) {
        state = state.copy(selectedMealType = mealType)
    }

    fun importPlans(plans: List<MealPlanEntry>, dailyCfg: DailyMealConfig?) {
        val baseline = copyPlans(plans)

        // 取出畫布中「不屬於當前選取日」的其他多日資料
        val otherDaysDraft = state.draftPlans.filterNot { it.date == state.selectedDate }
        // 將其他日資料與新傳入的單日資料合併
        val newDraft = otherDaysDraft + copyPlans(plans)

        state = state.copy(
            planSource = baseline,
            draftPlans = newDraft,
            dailyMealConfig = dailyCfg,
            history = emptyList(),
            changeLog = emptyList(),
            isDirty = false,
            snapshotA = null,
            snapshotB = null
        )
    }

    fun applyScale(multiplier: Double, mealType: String?) {
        if (multiplier <= 0.0) return
        val scaled = scalePlans(state.draftPlans, multiplier, mealType)
        setDraft(scaled, pushHistory = true)
    }

    fun applySnapshot(slot: SnapshotSlot) {
        val snapshot = when (slot) {
            SnapshotSlot.A -> state.snapshotA
            SnapshotSlot.B -> state.snapshotB
        } ?: return
        setDraft(copyPlans(snapshot), pushHistory = true)
    }

    fun saveSnapshot(slot: SnapshotSlot) {
        val snapshot = copyPlans(state.draftPlans)
        state = when (slot) {
            SnapshotSlot.A -> state.copy(snapshotA = snapshot)
            SnapshotSlot.B -> state.copy(snapshotB = snapshot)
        }
    }

    fun undo() {
        val history = state.history
        if (history.isEmpty()) return
        val previous = history.last()
        setDraft(copyPlans(previous), newHistory = history.dropLast(1))
    }

    fun resetDraft() {
        // Start from Baseline
        setDraft(copyPlans(state.planSource), newHistory = emptyList())
    }

    // ✅ Added: Clear draft (Start Empty)
    fun clearDraft() {
        setDraft(emptyList(), pushHistory = true)
    }

    fun commitOverwrite(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        val plans = copyPlans(state.draftPlans)
        val start = state.simulationStart
        val end = state.simulationEnd

        scope.launch {
            try {
                // 1. 找出所有受影響的日期 (包含模擬範圍，以及草稿中可能出現的其他日期)
                val daysCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
                val rangeDates = (0 until daysCount).map { start.plusDays(it.toLong()).toString() }.toSet()
                val allAffectedDates = rangeDates + plans.map { it.date }.toSet()

                // 2. 將草稿按實際的 date 分組
                val groupedPlans = plans.groupBy { it.date }

                // 3. 逐日儲存 (若該日在草稿中被清空，對應存入 emptyList，達到正確的 Delete 效果)
                allAffectedDates.forEach { dateStr ->
                    val dailyPlans = groupedPlans[dateStr] ?: emptyList()
                    planStore.replaceEntries(dateStr, dailyPlans)
                }

                // 4. 更新狀態
                state = state.copy(
                    planSource = copyPlans(plans.filter { it.date == state.selectedDate }),
                    draftPlans = copyPlans(plans),
                    history = emptyList(),
                    changeLog = emptyList(),
                    isDirty = false
                )
                onSuccess()
                fetchRangeData()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun runSimulation(
        strategy: SimulationStrategy,
        userPref: UserPreference?
    ) {
        if (userPref == null) return
        val start = state.simulationStart
        val end = state.simulationEnd
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        if (days <= 0) return

        scope.launch {
            state = state.copy(isSimulating = true)
            val dailyTDEE = userPref.dailyKcalTarget.takeIf { it > 0 } ?: 2000.0

            var totalInputKcal = 0.0
            var totalP = 0.0
            var totalC = 0.0
            var totalF = 0.0

            when (strategy) {
                SimulationStrategy.REPEAT_DRAFT -> {
                    // [Fix] 判斷 Draft 模式：是「單日模板 (Template)」還是「多日規劃 (Schedule)」?

                    // 1. 檢查 Draft 中是否有任何日期落在模擬範圍內
                    val plansInRange = state.draftPlans.filter {
                        val d = LocalDate.parse(it.date)
                        !d.isBefore(start) && !d.isAfter(end)
                    }

                    if (plansInRange.isNotEmpty()) {
                        // [Scenario A: Schedule Mode]
                        // Draft 包含具體日期的規劃。我們只計算範圍內的這些項目總和。
                        // (未規劃的日期視為 0 kcal，或者你可以選擇 fallback 到 TDEE，這裡先視為 0)
                        totalInputKcal = plansInRange.sumOf { it.kcal }
                        totalP = plansInRange.sumOf { it.protein }
                        totalC = plansInRange.sumOf { it.carbs }
                        totalF = plansInRange.sumOf { it.fats }

                        // 注意：如果使用者只規劃了 7 天中的 3 天，其他 4 天是 0，這會拉低平均值。
                        // 這通常是預期的（沒吃就是斷食），除非你要做 auto-fill。
                    } else {
                        // [Scenario B: Template Mode]
                        // Draft 的日期都在範圍外 (例如剛從 Baseline 載入的過去日期)。
                        // 將整份 Draft 視為「每一天都吃這樣」。
                        val dailyKcal = state.draftPlans.sumOf { it.kcal }
                        val dailyP = state.draftPlans.sumOf { it.protein }
                        val dailyC = state.draftPlans.sumOf { it.carbs }
                        val dailyF = state.draftPlans.sumOf { it.fats }

                        totalInputKcal = dailyKcal * days
                        totalP = dailyP * days
                        totalC = dailyC * days
                        totalF = dailyF * days
                    }
                }

                SimulationStrategy.CALENDAR_DATA -> {
                    // 保持原有的 Baseline 讀取邏輯
                    for (i in 0 until days) {
                        val dateStr = start.plusDays(i.toLong()).toString()
                        val entries = state.rangeData[dateStr] ?: planStore.entriesFlow(dateStr).first()
                        if (entries.isNotEmpty()) {
                            totalInputKcal += entries.sumOf { it.kcal }
                            totalP += entries.sumOf { it.protein }
                            totalC += entries.sumOf { it.carbs }
                            totalF += entries.sumOf { it.fats }
                        }
                    }
                }
            }

            val totalTargetKcal = dailyTDEE * days
            val netKcal = totalInputKcal - totalTargetKcal

            // Weight Change Calculation
            val baseChange = netKcal / 7700.0
            val deviation = baseChange * 0.15 // ±15% deviation
            val val1 = baseChange - deviation
            val val2 = baseChange + deviation
            val range = (if (val1 < val2) val1 else val2)..(if (val1 < val2) val2 else val1)

            state = state.copy(
                isSimulating = false,
                simulationResult = SimulationResult(
                    days = days,
                    totalInputKcal = totalInputKcal,
                    totalTargetKcal = totalTargetKcal,
                    avgDailyP = totalP / days,
                    avgDailyC = totalC / days,
                    avgDailyF = totalF / days,
                    weightChangeRange = range,
                    strategy = strategy
                )
            )
        }
    }

    private fun setDraft(
        newDraft: List<MealPlanEntry>,
        pushHistory: Boolean = false,
        newHistory: List<List<MealPlanEntry>>? = null
    ) {
        val history = when {
            newHistory != null -> newHistory
            pushHistory -> (state.history + listOf(copyPlans(state.draftPlans))).takeLast(20)
            else -> state.history
        }
        val changeLog = buildPlanChanges(state.planSource, newDraft)
        state = state.copy(
            draftPlans = newDraft,
            history = history,
            changeLog = changeLog,
            isDirty = changeLog.isNotEmpty()
        )
    }
    // [New] 更新單一項目
    fun updateEntry(entry: MealPlanEntry) {
        val index = state.draftPlans.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            val newDraft = state.draftPlans.toMutableList()
            newDraft[index] = entry
            setDraft(newDraft, pushHistory = true)
        }
    }

    // [New] 刪除單一項目
    fun deleteEntry(entryId: String) {
        val newDraft = state.draftPlans.filterNot { it.id == entryId }
        setDraft(newDraft, pushHistory = true)
    }

    // [New] 清空特定日期的所有餐點
    fun clearDay(dateStr: String) {
        val newDraft = state.draftPlans.filterNot { it.date == dateStr }
        setDraft(newDraft, pushHistory = true)
    }

    // [New] 核心功能：將某一日的菜單複製到當前模擬範圍內的其他所有日期
    fun copyDayToRange(sourceDate: String) {
        val rangeStart = state.simulationStart
        val rangeEnd = state.simulationEnd

        // 1. 取得來源日的計畫
        val sourcePlans = state.draftPlans.filter { it.date == sourceDate }
        if (sourcePlans.isEmpty()) return

        // 2. 計算範圍內的所有日期字串
        val days = ChronoUnit.DAYS.between(rangeStart, rangeEnd).toInt() + 1
        val targetDates = (0 until days)
            .map { rangeStart.plusDays(it.toLong()).toString() }
            .filter { it != sourceDate } // 排除來源日自己

        // 3. 移除目標日期原本的資料 (Overwrite 邏輯)
        val cleanDraft = state.draftPlans.filter { it.date == sourceDate || it.date !in targetDates }

        // 4. 產生複製項目 (必須重新生成 UUID)
        val newEntries = targetDates.flatMap { targetDate ->
            sourcePlans.map { plan ->
                plan.copy(
                    id = UUID.randomUUID().toString(),
                    date = targetDate
                )
            }
        }

        setDraft(cleanDraft + newEntries, pushHistory = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandBoxFeature(
    userPref: UserPreference?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subRepo = remember { SubscriptionRepository() }
    val isPro by subRepo.isPro.collectAsState()
    var showProDialog by remember { mutableStateOf(false) }
    val planStore = remember(context) { MealPlanRepository.getInstance(context) }
    val dailyStore = remember(context) { DailyMealConfigStore(context) }
    val initialTarget = remember(userPref) {
        (userPref?.dailyKcalTarget ?: 2000.0).takeIf { it > 0 } ?: 2000.0
    }
    val initialDate = remember { LocalDate.now().toString() }

    val viewModel = remember {
        SandboxWorkspaceViewModel(
            planStore = planStore,
            scope = scope,
            initialDate = initialDate,
            initialTargetKcal = initialTarget
        )
    }
    val state = viewModel.state
    val runWithProCheck: (() -> Unit) -> Unit = { action ->
        if (isPro) action() else showProDialog = true
    }
    val initialPlans = remember(state.selectedDate, state.rangeData) {
        state.rangeData[state.selectedDate] ?: emptyList()
    }
    val planFlow = remember(state.selectedDate) { planStore.entriesFlow(state.selectedDate) }
    val planSource by planFlow.collectAsState(initial = initialPlans)
    val cfgFlow = remember(state.selectedDate) { dailyStore.flow(state.selectedDate) }
    val dailyCfg by cfgFlow.collectAsState(initial = null)

    LaunchedEffect(planSource, state.selectedDate, state.isDirty) {
        if (!state.isDirty) {
            viewModel.importPlans(planSource, dailyCfg)
        }
    }
    LaunchedEffect(dailyCfg) {
        viewModel.updateDailyConfig(dailyCfg)
    }

    var activePickerDate by remember { mutableStateOf<String?>(null) }
    var activePickerMealType by remember { mutableStateOf<String?>(null) }
    var showMealPicker by remember { mutableStateOf(false) }
    var showDirectMacroDialog by remember { mutableStateOf(false) }
    var showPlanPicker by remember { mutableStateOf(false) }
    val savedPlans by planStore.getAllPlansFlow().collectAsState(initial = emptyList())

    val mealTypeOptions = rememberMealTypeOptions()
    val mealLabelByKey = remember(mealTypeOptions) { mealTypeOptions.associate { it.key to it.label } }
    val fallbackLabel = stringResource(R.string.unclassified)

    fun labelForMealType(key: String?): String {
        return when {
            key.isNullOrBlank() -> fallbackLabel
            key == UNCLASSIFIED_MEAL_ID -> fallbackLabel
            else -> mealLabelByKey[key] ?: key
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = SandboxTechColors.DeepBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp) // 标题和下方内容的间距
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SandboxTechColors.AccentCyan
                    )
                }

                Text(
                    text = stringResource(R.string.diet_sandbox),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SandboxTechColors.AccentCyan
                )
            }

            // 1. Reference / Baseline
            DateSpace(
                state = state,
                mealTypeOptions = mealTypeOptions,
                labelForMealType = ::labelForMealType,
                onDateSelected = viewModel::onDateSelected,
                onUpdateSimulationRange = viewModel::setSimulationRange
            )

            // 2. Range Projection (Read Only)
            RangeSimulationPanel(
                state = state,
                userPref = userPref,
                mealTypeOptions = mealTypeOptions,
                labelForMealType = ::labelForMealType,
                onRunSimulation = { strategy ->
                    viewModel.runSimulation(strategy, userPref)
                }
            )

            // 3. Experiment Canvas
            ExperimentCanvas(
                state = state,
                mealTypeOptions = mealTypeOptions,
                labelForMealType = ::labelForMealType,
                onStartFromBaseline = viewModel::resetDraft,
                onStartEmpty = viewModel::clearDraft,
                onApplySnapshot = viewModel::applySnapshot,
                onSetMealType = viewModel::setMealType,
                onPickMeal = { dateOverride, mealTypeOverride ->
                    activePickerDate = dateOverride
                    activePickerMealType = mealTypeOverride // 鎖定餐別
                    showMealPicker = true
                },
                onUpdateEntry = viewModel::updateEntry,
                onDeleteEntry = viewModel::deleteEntry,
                onCopyDay = viewModel::copyDayToRange,
                onClearDay = viewModel::clearDay,
                onImportPlanClick = {
                    runWithProCheck { showPlanPicker = true }
                },
                onSave = {
                    runWithProCheck {
                        viewModel.commitOverwrite(
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.sandbox_save_success), Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, context.getString(R.string.sandbox_save_error, it.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
            Spacer(Modifier.height(116.dp))
        }
    }
    if (showPlanPicker) {
        AlertDialog(
            onDismissRequest = { showPlanPicker = false },
            title = { Text(stringResource(R.string.import_menu_plan)) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedPlans.isEmpty()) {
                        item { Text(stringResource(R.string.no_saved_plans), color = Color.Gray) }
                    }
                    items(savedPlans) { plan ->
                        OutlinedCard(
                            onClick = {
                                viewModel.applyPlan(plan)
                                showPlanPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.plan_details_fmt, plan.dayCount, plan.entries.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlanPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showMealPicker) {
        MealPickerDialog(
            onDismiss = {
                showMealPicker = false
                activePickerDate = null
                activePickerMealType = null
            },
            onConfirm = { foods, _ ->
                // [Mod] 優先使用 activePickerMealType，若無則回退到 selectedMealType
                val targetMealType = activePickerMealType ?: state.selectedMealType
                viewModel.addFoodsToDraft(foods, targetMealType, activePickerDate)

                showMealPicker = false
                activePickerDate = null
                activePickerMealType = null // Reset
            }
        )
    }
    if (showDirectMacroDialog) {
        val quickMacroName = stringResource(R.string.quick_macro)
        DirectMacroInputDialog(
            onDismiss = { showDirectMacroDialog = false; activePickerDate = null },
            onConfirm = { kcal, p, c, f ->
                viewModel.addDirectMacroToDraft(quickMacroName, kcal, p, c, f, activePickerDate)
                showDirectMacroDialog = false
                activePickerDate = null
            }
        )
    }
    if (showProDialog) {
        ProFeatureDialog(
            onDismiss = { showProDialog = false },
            onUpgrade = {
                showProDialog = false
            }
        )
    }
}

private enum class CanvasViewMode { AGGREGATE, DAILY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperimentCanvas(
    state: SandboxWorkspaceState,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String,
    onStartFromBaseline: () -> Unit,
    onStartEmpty: () -> Unit,
    onApplySnapshot: (SnapshotSlot) -> Unit,
    onSetMealType: (String?) -> Unit,
    onPickMeal: (String?, String?) -> Unit,
    onUpdateEntry: (MealPlanEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onCopyDay: (String) -> Unit,
    onClearDay: (String) -> Unit,
    onImportPlanClick: () -> Unit,
    onSave: () -> Unit
) {
    var viewMode by rememberSaveable { mutableStateOf(CanvasViewMode.DAILY) }
    var editingEntry by remember { mutableStateOf<MealPlanEntry?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth().techGlassBorder(RoundedCornerShape(20.dp)),
        color = SandboxTechColors.GlassCard,
        shape = RoundedCornerShape(20.dp)
    ){
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Header: Title & View Switch ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SandboxTechColors.AccentCyan)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sandbox_canvas), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SandboxTechColors.AccentCyan)
                    Text(stringResource(R.string.sandbox_drafting_area), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }


                // View Mode Switcher
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.widthIn(min = 160.dp)
                ) {
                    SegmentedButton(
                        selected = viewMode == CanvasViewMode.DAILY,
                        onClick = { viewMode = CanvasViewMode.DAILY },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                        modifier = Modifier.weight(1f),
                        icon = {}
                    ) {
                        Text(
                            text = stringResource(R.string.sandbox_view_daily),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    SegmentedButton(
                        selected = viewMode == CanvasViewMode.AGGREGATE,
                        onClick = { viewMode = CanvasViewMode.AGGREGATE },
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.weight(1f),
                        icon = {}
                    ) {
                        Text(
                            text = stringResource(R.string.sandbox_view_total),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            // --- Controls (Start/Snapshot) ---
            // Only show strictly necessary controls to save space
            if (viewMode == CanvasViewMode.AGGREGATE) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = onStartFromBaseline, label = { Text(stringResource(R.string.reset)) })
                    AssistChip(onClick = onStartEmpty, label = { Text(stringResource(R.string.clear_all)) })
                    state.snapshotA?.let { AssistChip(onClick = { onApplySnapshot(SnapshotSlot.A) }, label = { Text(stringResource(R.string.sandbox_load_a)) }) }
                    state.snapshotB?.let { AssistChip(onClick = { onApplySnapshot(SnapshotSlot.B) }, label = { Text(stringResource(R.string.sandbox_load_b)) }) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // --- Content Area ---
            when (viewMode) {
                CanvasViewMode.AGGREGATE -> {
                    AggregateViewContent(
                        draftPlans = state.draftPlans,
                        selectedMealType = state.selectedMealType,
                        mealTypeOptions = mealTypeOptions,
                        labelForMealType = labelForMealType,
                        onSetMealType = onSetMealType,
                        onPickMeal = { onPickMeal(null, null) },
                        onEditEntry = { editingEntry = it },
                        onDeleteEntry = onDeleteEntry
                    )
                }
                CanvasViewMode.DAILY -> {
                    DailyViewContent(
                        state = state, // Need full state for range
                        mealTypeOptions = mealTypeOptions,
                        labelForMealType = labelForMealType,
                        onCopyDay = onCopyDay,
                        onClearDay = onClearDay,
                        onEditEntry = { editingEntry = it },
                        onDeleteEntry = onDeleteEntry,
                        onPickMeal = onPickMeal
                    )
                    // Note: In Daily View, adding meals usually requires setting the context date.
                    // The original `onPickMeal` relied on `activePickerDate` in the parent.
                    // Ideally, pass `onPickMeal(Date)` up.
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), // 設定間距與靠右
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isDirty) {
                        Text(
                            text = stringResource(R.string.unsaved_changes_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onImportPlanClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MealooDimens.FieldHeight),
                            shape = RoundedCornerShape(MealooDimens.RadiusMedium),
                            border = BorderStroke(1.dp, SandboxTechColors.AccentCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SandboxTechColors.AccentCyan)
                        ) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.import_plan),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onSave,
                            enabled = state.draftPlans.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MealooDimens.FieldHeight),
                            shape = RoundedCornerShape(MealooDimens.RadiusMedium),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SandboxTechColors.AccentCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sandbox_save_draft),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        }

                    }
                }
            }
        }
    }

    // Edit Dialog
    if (editingEntry != null) {
        EditEntryDialog(
            entry = editingEntry!!,
            onDismiss = { editingEntry = null },
            onConfirm = { updated ->
                onUpdateEntry(updated)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun AggregateViewContent(
    draftPlans: List<MealPlanEntry>,
    selectedMealType: String?,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String,
    onSetMealType: (String?) -> Unit,
    onPickMeal: () -> Unit, // 雖然傳進來了，但在這裡我們不再調用它
    onEditEntry: (MealPlanEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val groupedByMealType = draftPlans.groupBy { it.mealId }
    val allKeys = mealTypeOptions.map { it.key }
    val customKeys = groupedByMealType.keys.filterNotNull().filter { it !in allKeys }.sorted()
    val displayKeys = allKeys + customKeys
    val mealMap = remember(mealTypeOptions) { mealTypeOptions.associateBy { it.key } }

    displayKeys.forEach { key ->
        val plansInType = groupedByMealType[key] ?: emptyList()
        val label = labelForMealType(key)
        val color = mealMap[key]?.color ?: MaterialTheme.colorScheme.outline
        val isSelected = selectedMealType == key

        // [New] 核心邏輯：按食物名稱聚合 (Aggregation by Name)
        // 這樣 "Mon: Oatmeal" 和 "Tue: Oatmeal" 會被合併成一條 "Oatmeal"
        val aggregatedItems = remember(plansInType) {
            plansInType.groupBy { it.name }.mapValues { (_, items) ->
                // 計算加總
                val totalGrams = items.sumOf { it.grams }
                val totalKcal = items.sumOf { it.kcal }
                val count = items.size
                // 回傳聚合對象 (這裡用一個簡單的 Triple 或 Data Class 表示)
                Triple(totalGrams, totalKcal, count)
            }.toList()
                .sortedByDescending { it.second.second } // 按總熱量排序
        }

        Surface(
            modifier = Modifier.fillMaxWidth().techGlassBorder(RoundedCornerShape(12.dp)),
            color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(1.dp, color) else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    // 點擊卡片切換選中狀態 (用於 Scale 工具的作用域)
                    .clickable { onSetMealType(if (isSelected) null else key) }
            ) {
                // --- Section Header ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(color, CircleShape).techGlow(color))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)

                    Spacer(Modifier.weight(1f))
                    if (plansInType.isNotEmpty()) {
                        Text(
                            text = "${formatKcal(plansInType.sumOf { it.kcal })} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (plansInType.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))

                    // --- Aggregated List ---
                    aggregatedItems.forEach { (name, stats) ->
                        val (totalGrams, totalKcal, count) = stats

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Count Badge (顯示這項食物出現了幾次/幾天)
                            if (count > 1) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${count}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            // 2. Name
                            Text(
                                text = stripBrackets(name),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )


                            // 3. Total Grams & Kcal
                            Column(horizontalAlignment = Alignment.End) {
                                if (totalGrams > 0) {
                                    Text(
                                        text = stringResource(R.string.common_grams_value_desc, "%.0f".format(totalGrams)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                } else {
                                    // Fallback if grams are missing (old data)
                                    Text(stringResource(R.string.common_empty_placeholder), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }

                                Text(
                                    text = stringResource(R.string.kcal_val_fmt, "", totalKcal),
                                    style = MaterialTheme.typography.labelSmall, // smaller font for kcal in this view
                                    color = Color.Gray
                                )

                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.2f)
                        )
                    }
                } else {
                    // Empty State
                    Text(
                        text = stringResource(R.string.no_items),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyViewContent(
    state: SandboxWorkspaceState,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String,
    onCopyDay: (String) -> Unit,
    onClearDay: (String) -> Unit,
    onEditEntry: (MealPlanEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onPickMeal: (String, String) -> Unit
) {
    // Generate dates from range
    val days = remember(state.simulationStart, state.simulationEnd) {
        val count = ChronoUnit.DAYS.between(state.simulationStart, state.simulationEnd).toInt() + 1
        (0 until count).map { state.simulationStart.plusDays(it.toLong()) }
    }

    // [Optimization] 使用 State 記錄當前選中的日期索引，而非列出所有日期
    var selectedIndex by remember { mutableIntStateOf(0) }
    // 確保索引不越界 (當範圍改變時)
    val safeIndex = selectedIndex.coerceIn(days.indices)
    val selectedDate = days.getOrNull(safeIndex)

    if (selectedDate == null) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. 水平日期選單 (Horizontal Date Selector)
        ScrollableTabRow(
            selectedTabIndex = safeIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                if (safeIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[safeIndex]),
                        color = SandboxTechColors.AccentCyan
                    )

                }
            }
        ) {
            days.forEachIndexed { index, date ->
                val isSelected = index == safeIndex
                Tab(
                    selected = isSelected,
                    onClick = { selectedIndex = index },
                    text = {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MM/dd")),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SandboxTechColors.AccentCyan else Color.Gray
                        )

                    }
                )
            }
        }

        // 2. 單日內容卡片 (Single Day Card)
        val dateStr = selectedDate.toString()
        val dailyPlans = state.draftPlans.filter { it.date == dateStr }
        val dayKcal = dailyPlans.sumOf { it.kcal }
        val mealMap = remember(mealTypeOptions) { mealTypeOptions.associateBy { it.key } }

        Surface(
            modifier = Modifier.fillMaxWidth().techGlassBorder(RoundedCornerShape(12.dp)),
            color = SandboxTechColors.GlassCard.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {

            Column(Modifier.padding(12.dp)) {
                // Day Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.total_kcal_fmt_simple, formatKcal(dayKcal)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SandboxTechColors.AccentCyan
                        )

                    }
                    Spacer(Modifier.weight(1f))

                    // [Removed] 這裡原本的全域 Add 按鈕已移除，因為它無法分類

                    // Smart Actions (Copy / Clear)
                    IconButton(onClick = { onCopyDay(dateStr) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_to_all), modifier = Modifier.size(18.dp), tint = SandboxTechColors.AccentCyan)
                    }
                    IconButton(onClick = { onClearDay(dateStr) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_day), modifier = Modifier.size(20.dp), tint = SandboxTechColors.AccentMagenta)
                    }

                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Group by Meal Type for this day
                val dailyGrouped = dailyPlans.groupBy { it.mealId }
                val keys = mealTypeOptions.map { it.key }

                keys.forEach { mType ->
                    val mPlans = dailyGrouped[mType] ?: emptyList()
                    val mLabel = labelForMealType(mType)
                    val mColor = mealMap[mType]?.color ?: Color.Gray

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(mColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(Modifier.size(6.dp).background(mColor, CircleShape).techGlow(mColor))
                        Spacer(Modifier.width(6.dp))
                        Text(mLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = mColor)


                        Spacer(Modifier.weight(1f))

                        // [New] 將 Add 按鈕移到這裡，直接綁定 mType
                        IconButton(
                            onClick = { onPickMeal(dateStr, mType) }, // 傳入日期與餐別
                            modifier = Modifier.size(24.dp) // 小一點的按鈕
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_to_meal, mLabel), modifier = Modifier.size(16.dp), tint = mColor)
                        }


                        if (mPlans.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Text("${formatKcal(mPlans.sumOf { it.kcal })}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (mPlans.isEmpty()) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
                        )
                    } else {
                        mPlans.forEach { plan ->
                            DraftItemRow(
                                plan = plan,
                                onEdit = { onEditEntry(plan) },
                                onDelete = { onDeleteEntry(plan.id) }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftItemRow(
    plan: MealPlanEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stripBrackets(plan.name),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )

            Text(
                text = stringResource(R.string.servings_kcal_fmt, formatServings(plan.servings), formatKcal(plan.kcal)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete_item), tint = SandboxTechColors.AccentMagenta.copy(alpha = 0.7f))
        }

    }
}

@Composable
private fun EditEntryDialog(
    entry: MealPlanEntry,
    onDismiss: () -> Unit,
    onConfirm: (MealPlanEntry) -> Unit
) {
    var name by remember { mutableStateOf(entry.name) }
    var servings by remember { mutableStateOf(formatServings(entry.servings)) }
    var kcal by remember { mutableStateOf(formatKcal(entry.kcal)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SandboxTechColors.DeepBackground,
        titleContentColor = SandboxTechColors.AccentCyan,
        title = { Text(stringResource(R.string.edit_item), fontWeight = FontWeight.Bold) },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text(stringResource(R.string.servings)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = kcal,
                        onValueChange = { kcal = it },
                        label = { Text(stringResource(R.string.kcal)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = servings.toDoubleOrNull() ?: entry.servings
                val k = kcal.toDoubleOrNull() ?: entry.kcal
                // Note: Ideally scale Macros here too, but simple Kcal override for now
                val ratio = if (entry.kcal > 0) k / entry.kcal else 1.0
                onConfirm(entry.copy(
                    name = name,
                    servings = s,
                    kcal = k,
                    protein = entry.protein * ratio,
                    carbs = entry.carbs * ratio,
                    fats = entry.fats * ratio
                ))
            }) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }

        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DateSpace(
    state: SandboxWorkspaceState,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String,
    onDateSelected: (String) -> Unit,
    onUpdateSimulationRange: (LocalDate, LocalDate) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val mealMap = remember(mealTypeOptions) { mealTypeOptions.associateBy { it.key } }

    // State for Date Picker Dialog
    var showRangePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) } // true=start, false=end

    if (showRangePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (if (pickingStart) state.simulationStart else state.simulationEnd)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            if (pickingStart) {
                                val newEnd = if (selected.isAfter(state.simulationEnd)) selected.plusDays(1) else state.simulationEnd
                                onUpdateSimulationRange(selected, newEnd)
                            } else {
                                val newStart = if (selected.isBefore(state.simulationStart)) selected else state.simulationStart
                                onUpdateSimulationRange(newStart, selected)
                            }
                        }
                        showRangePicker = false
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().techGlassBorder(RoundedCornerShape(20.dp)),
        color = SandboxTechColors.GlassCard,
        shape = RoundedCornerShape(20.dp)
    ){
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // --- Header (Collapsible) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sandbox_baseline_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SandboxTechColors.AccentCyan)
                    Text(
                        text = stringResource(R.string.sandbox_baseline_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.toggle),
                        tint = SandboxTechColors.AccentCyan
                    )
                }

            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // ==========================================
                    // BLOCK 1: Range & Timeline
                    // ==========================================
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1.1 Timeline (Weekly View)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SandboxTechColors.AccentCyan
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sandbox_active_date_range),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SandboxTechColors.AccentCyan
                            )

                        }

                        DateStrip(
                            selectedDate = state.selectedDate,
                            startDate = state.simulationStart,
                            endDate = state.simulationEnd,
                            onDateSelected = onDateSelected
                        )

                        // 1.2 Simulation Range Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { pickingStart = true; showRangePicker = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.from), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(state.simulationStart.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.to), modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(2.dp))

                            OutlinedButton(
                                onClick = { pickingStart = false; showRangePicker = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.to), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(state.simulationEnd.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // 1.3 Explanatory Hint
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sandbox_range_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // ==========================================
                    // BLOCK 2: Planned Meal Plan (Baseline)
                    // ==========================================
                    val displayPlans = state.rangeData[state.selectedDate] ?: state.planSource

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.sandbox_planned_baseline), // Renamed from "Current Draft Plans"
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            // Show date context
                            Text(
                                text = stringResource(R.string.sandbox_for_date, state.selectedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            if (displayPlans.isNotEmpty()){
                                Text(
                                    text = stringResource(R.string.sandbox_items_count, state.planSource.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Content List (Using planSource instead of draftPlans)
                        if (displayPlans.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .techGlassBorder(MaterialTheme.shapes.small),
                                color = Color.White.copy(alpha = 0.05f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Box(
                                    modifier = Modifier.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.sandbox_no_planned_meals),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // [Optimization] 限制最大高度，避免佔據過多版面
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp) // 設定最大高度
                                    .verticalScroll(rememberScrollState()) // 允許內部捲動
                            ) {
                                val grouped = displayPlans.groupBy { it.mealId }
                                val knownKeys = mealTypeOptions.map { it.key }
                                val orderedKeys = (knownKeys + grouped.keys.filterNot { it in knownKeys }.sortedBy { it }).distinct()

                                orderedKeys.forEach { key ->
                                    val plans = grouped[key] ?: return@forEach
                                    Surface(

                                        color = MaterialTheme.colorScheme.surfaceContainerLow,

                                        shape = MaterialTheme.shapes.small

                                    ) {

                                        Column(Modifier.padding(8.dp)) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                val color = mealMap[key]?.color ?: MaterialTheme.colorScheme.outline

                                                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))

                                                Spacer(Modifier.width(8.dp))

                                                Text(

                                                    text = labelForMealType(key),

                                                    style = MaterialTheme.typography.labelMedium,

                                                    fontWeight = FontWeight.Bold

                                                )

                                            }

                                            Spacer(Modifier.height(4.dp))



                                            plans.forEach { plan ->

                                                Row(

                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),

                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {

                                                    Text(

                                                        text = stripBrackets(plan.name),

                                                        style = MaterialTheme.typography.bodyMedium,

                                                        modifier = Modifier.weight(1f),

                                                        maxLines = 1,

                                                        overflow = TextOverflow.Ellipsis,

                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Muted text for read-only

                                                    )

                                                    Spacer(Modifier.width(8.dp))

                                                    Text(

                                                        text = "${formatKcal(plan.kcal)}",

                                                        style = MaterialTheme.typography.bodySmall,

                                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                                    )

                                                }

                                            }

                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun RangeSimulationPanel(
    state: SandboxWorkspaceState,
    userPref: UserPreference?,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String,
    onRunSimulation: (SimulationStrategy) -> Unit
){
    var selectedStrategy by remember { mutableStateOf(SimulationStrategy.REPEAT_DRAFT) }
    var showDailyDetails by remember { mutableStateOf(false) }
    val result = state.simulationResult
    val themeColors = MealooTheme.colors

    val days = remember(state.simulationStart, state.simulationEnd) {
        val count = ChronoUnit.DAYS.between(state.simulationStart, state.simulationEnd).toInt() + 1
        (0 until count).map { state.simulationStart.plusDays(it.toLong()) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().techGlassBorder(RoundedCornerShape(20.dp)),
        color = SandboxTechColors.GlassCard,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Header Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = SandboxTechColors.AccentMagenta
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sandbox_impact_projection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SandboxTechColors.AccentMagenta
                    )
                }


                // Source Selection Toggle
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clip(CircleShape)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val strategies = listOf(
                            SimulationStrategy.REPEAT_DRAFT to stringResource(R.string.sandbox_use_draft),
                            SimulationStrategy.CALENDAR_DATA to stringResource(R.string.sandbox_use_baseline)
                        )
                        strategies.forEach { (strat, label) ->
                            val isSelected = selectedStrategy == strat
                            val bgColor = if (isSelected) SandboxTechColors.AccentCyan else Color.Transparent
                            val textColor = if (isSelected) Color.Black else Color.Gray

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable { selectedStrategy = strat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }


                    Spacer(Modifier.height(4.dp))

                    val sourceText = if (selectedStrategy == SimulationStrategy.REPEAT_DRAFT) stringResource(R.string.sandbox_source_draft) else stringResource(R.string.sandbox_source_baseline)
                    Text(
                        text = stringResource(R.string.sandbox_current_source, sourceText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9
                    )
                }
            }

            // --- Info & Action Row ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration Info
                Column {
                    Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.days_count, days.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))

                // [Fix] 調整 Button padding 避免文字換行
                Button(
                    onClick = { onRunSimulation(selectedStrategy) },
                    enabled = !state.isSimulating,
                    contentPadding = PaddingValues(horizontal = 16.dp), // 縮小 Padding
                    modifier = Modifier.height(36.dp)
                ) {
                    if (state.isSimulating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.calculate))
                    }
                }
            }

            // --- Results Display ---
            AnimatedVisibility(visible = result != null) {
                result?.let { res ->
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Row 1: Weight Change & Net Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top // 對齊頂部
                        ) {
                            // Left: Weight Trend
                            Column {
                                Text(stringResource(R.string.sandbox_est_trend), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val min = res.weightChangeRange.start
                                val max = res.weightChangeRange.endInclusive
                                val isLoss = max < 0
                                val isGain = min > 0
                                val color = if (isLoss) SandboxTechColors.AccentCyan else if (isGain) SandboxTechColors.AccentMagenta else Color.White

                                val sign = if (isLoss) "-" else if (isGain) "+" else "~"
                                Text(
                                    text = "$sign%.1f ~ %.1f kg".format(abs(min), abs(max)),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = color,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Text(
                                    text = if(isLoss) stringResource(R.string.sandbox_projected_loss) else if (isGain) stringResource(R.string.sandbox_projected_gain) else stringResource(R.string.sandbox_maintenance),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color.copy(alpha = 0.8f)
                                )
                            }

                            // Right: Net Balance
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.sandbox_net_balance_day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val dailyDiff = (res.totalInputKcal - res.totalTargetKcal) / res.days
                                Text(
                                    text = "${if(dailyDiff>0)"+" else ""}${"%.0f".format(dailyDiff)} kcal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.target_simple, "%.0f".format(userPref?.dailyKcalTarget)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Row 2: Macros (獨立出來，不再被擠壓)
                        Text(stringResource(R.string.sandbox_daily_avg_macros), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).techGlassBorder().padding(8.dp).techGlow(SandboxTechColors.NeonP)) {
                                MacroStatItem(label = stringResource(R.string.protein), value = res.avgDailyP, color = SandboxTechColors.NeonP)
                            }
                            Box(modifier = Modifier.weight(1f).techGlassBorder().padding(8.dp).techGlow(SandboxTechColors.NeonC)) {
                                MacroStatItem(label = stringResource(R.string.carbs), value = res.avgDailyC, color = SandboxTechColors.NeonC)
                            }
                            Box(modifier = Modifier.weight(1f).techGlassBorder().padding(8.dp).techGlow(SandboxTechColors.NeonF)) {
                                MacroStatItem(label = stringResource(R.string.fats), value = res.avgDailyF, color = SandboxTechColors.NeonF)
                            }
                        }

                    }
                }
            }

            // --- Daily Breakdown List ---
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDailyDetails = !showDailyDetails }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.sandbox_daily_breakdown), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(if (showDailyDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
            }

            AnimatedVisibility(visible = showDailyDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.forEach { date ->
                        val dateStr = date.toString()

                        // [FIX] 這裡的邏輯修正了：
                        // 如果是 Draft 模式，我們要去 Draft 裡找「符合當天日期」的餐點
                        // 如果 Draft 裡完全沒有這個日期的資料（比如這天沒規劃），是否要 fallback？
                        // 這裡的邏輯是：
                        // 1. 先看 Draft 有沒有這天的特定規劃 (Specific)
                        // 2. 如果沒有，且 Draft 裡面的資料都是「非當前範圍日期」(Template)，則顯示 Template (這裡簡化處理，先只濾日期)

                        val plans = if (selectedStrategy == SimulationStrategy.REPEAT_DRAFT) {
                            // 過濾 Draft 中屬於這一天的数据
                            val dailySpecific = state.draftPlans.filter { it.date == dateStr }

                            // [Smart Logic]
                            // 如果 Draft 裡有資料，但完全沒有對應到日期的資料 (例如剛 Reset 的 Template 都是舊日期)，
                            // 且 draftPlans 的日期都不在 simulation 範圍內，則視為通用 Template，每一天都顯示它。
                            // 但如果你用了 Daily Mode 編輯，日期就會對上。

                            if (dailySpecific.isNotEmpty()) {
                                dailySpecific
                            } else {
                                // 檢查 Draft 是否包含 "範圍內的其他日期"
                                // 如果 Draft 包含範圍內的其他天，表示這是 Specific Plan，這一天就是空的 (Empty)
                                // 如果 Draft 全部都是範圍外的日期 (例如 Baseline copy 過來的)，則視為 Template
                                val hasRangeDates = state.draftPlans.any {
                                    val d = LocalDate.parse(it.date)
                                    !d.isBefore(state.simulationStart) && !d.isAfter(state.simulationEnd)
                                }

                                if (hasRangeDates) emptyList() else state.draftPlans
                            }
                        } else {
                            state.rangeData[dateStr]
                        }

                        SimulationDayRow(
                            date = date,
                            plans = plans,
                            mealTypeOptions = mealTypeOptions,
                            labelForMealType = labelForMealType
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationDayRow(
    date: LocalDate,
    plans: List<MealPlanEntry>?,
    mealTypeOptions: List<MealTypeOption>,
    labelForMealType: (String?) -> String
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dateStr = date.format(DateTimeFormatter.ofPattern("MM/dd"))

    val groupedPlans = remember(plans) {
        plans?.groupBy { it.mealId }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .techGlassBorder(RoundedCornerShape(12.dp))
            .background(SandboxTechColors.GlassCard, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {

        // Date Column
        Column(
            modifier = Modifier.width(50.dp).padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dateStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SandboxTechColors.AccentCyan)
            Text(dayName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }


        Spacer(Modifier.width(8.dp))
        VerticalDivider(height = 24.dp)
        Spacer(Modifier.width(8.dp))

        // Content Column (Read Only)
        Column(modifier = Modifier.weight(1f)) {
            if (plans == null) {
                Box(Modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(R.string.sandbox_calendar_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else if (plans.isEmpty()) {
                Box(Modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(R.string.empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                val mealMap = remember(mealTypeOptions) { mealTypeOptions.associateBy { it.key } }
                val knownKeys: List<String?> = mealTypeOptions.map { it.key }
                val orderedKeys: List<String?> =
                    (knownKeys +
                            (groupedPlans
                                ?.keys
                                ?.filterNot { it in knownKeys }
                                ?.sortedWith(compareBy<String?> { it ?: "" })
                                ?: emptyList<String?>()
                                    )
                            ).distinct()

                orderedKeys.forEach { key ->
                    val groupItems = groupedPlans?.get(key).orEmpty()
                    if (groupItems.isEmpty()) return@forEach

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        val color = mealMap[key]?.color ?: MaterialTheme.colorScheme.outline
                        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = labelForMealType(key),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    groupItems.forEach { plan ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stripBrackets(plan.name),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatKcal(plan.kcal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SandboxTechColors.AccentCyan
                            )

                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f))
                Text(
                    text = stringResource(R.string.total_kcal_fmt_simple, formatKcal(plans.sumOf { it.kcal })),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SandboxTechColors.AccentCyan,
                    modifier = Modifier.align(Alignment.End)
                )

            }
        }
    }
}
@Composable
fun DirectMacroInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Double) -> Unit
) {
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }

    val p = protein.toDoubleOrNull() ?: 0.0
    val c = carbs.toDoubleOrNull() ?: 0.0
    val f = fats.toDoubleOrNull() ?: 0.0
    val k = kcal.toDoubleOrNull() ?: 0.0

    // 校验规则: Kcal 必须 >= P*4 + C*4 + F*9
    val minKcal = (p * 4) + (c * 4) + (f * 9)
    val isValid = k > 0 && k >= minKcal

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SandboxTechColors.DeepBackground,
        titleContentColor = SandboxTechColors.AccentCyan,
        title = { Text(stringResource(R.string.sandbox_quick_add_macros), fontWeight = FontWeight.Bold) },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.protein_g)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.carbs_g)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it },
                        label = { Text(stringResource(R.string.fats_g)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text(stringResource(R.string.totalkcal)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        val delta = k - minKcal
                        if (delta < 0) {
                            Text(stringResource(R.string.sandbox_min_kcal_error, minKcal.roundToInt()), color = SandboxTechColors.AccentMagenta)
                        } else {
                            Text(stringResource(R.string.sandbox_min_kcal_hint, minKcal.roundToInt()), color = Color.Gray)
                        }

                    },
                    isError = k > 0 && k < minKcal
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(k, p, c, f) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SandboxTechColors.AccentCyan,
                    contentColor = Color.Black
                )
            ) {
                Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
            }

        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
@Composable
private fun VerticalDivider(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .height(height)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
// --- Helper Composables ---

@Composable
private fun DateStrip(
    selectedDate: String,
    startDate: LocalDate,
    endDate: LocalDate,
    onDateSelected: (String) -> Unit
) {
    val today = remember { LocalDate.now() }
    val currentSelection = remember(selectedDate) {
        try { LocalDate.parse(selectedDate) } catch (e: Exception) { today }
    }

    val days = remember(startDate, endDate) {
        val count = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        (0 until count).map { startDate.plusDays(it.toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { date ->
            DayTab(
                date = date,
                isSelected = date == currentSelection,
                isToday = date == today,
                onClick = { onDateSelected(date.toString()) }
            )
        }
    }
}

@Composable
private fun DayTab(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SandboxTechColors.AccentCyan.copy(alpha = 0.15f) else SandboxTechColors.GlassCard,
        contentColor = if (isSelected) SandboxTechColors.AccentCyan else Color.Gray,
        border = BorderStroke(1.dp, if (isSelected) SandboxTechColors.AccentCyan else SandboxTechColors.TechBorder),
        modifier = Modifier.width(52.dp).height(64.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfWeek.name.take(3),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .background(contentColor, CircleShape)
                )
            }
        }
    }
}
@Composable
private fun rememberMealTypeOptions(): List<MealTypeOption> {
    val context = LocalContext.current
    val mealDefsStore = remember { MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }
    return mealDefs.map { def ->
        MealTypeOption(def.id, def.name, mealColorForKind(def.kind))
    }
}

@Composable
private fun mealColorForKind(kind: MealKind?): Color = when (kind) {
    MealKind.BREAKFAST -> MealooTheme.colors.breakfast
    MealKind.LUNCH -> MealooTheme.colors.lunch
    MealKind.DINNER -> MealooTheme.colors.dinner
    MealKind.SNACK -> MealooTheme.colors.snack
    MealKind.CUSTOM, null -> MaterialTheme.colorScheme.primary
}

private fun orderedMealLabels(summary: KcalSummary, mealTypeOptions: List<MealTypeOption>): List<String> {
    val base = mealTypeOptions.map { it.label }.toMutableList()
    summary.perMealKcal.keys.forEach { label ->
        if (!base.contains(label)) base.add(label)
    }
    return base
}

private fun copyPlans(plans: List<MealPlanEntry>): List<MealPlanEntry> = plans.map { it.copy() }

private fun scalePlans(
    plans: List<MealPlanEntry>,
    multiplier: Double,
    mealType: String?
): List<MealPlanEntry> {
    return plans.map { plan ->
        if (mealType == null || plan.mealId == mealType) {
            plan.copy(
                servings = plan.servings * multiplier,
                kcal = plan.kcal * multiplier,
                grams = plan.grams * multiplier,
                protein = plan.protein * multiplier,
                carbs = plan.carbs * multiplier,
                fats = plan.fats * multiplier,
                cholesterol = plan.cholesterol * multiplier
            )
        } else {
            plan
        }
    }
}

private fun computeKcalSummary(
    plans: List<MealPlanEntry>,
    labelForMealType: (String?) -> String
): KcalSummary {
    val perMeal = plans.groupBy { labelForMealType(it.mealId) }
        .mapValues { entry -> entry.value.sumOf { it.kcal } }
    val total = perMeal.values.sum()
    return KcalSummary(totalKcal = total, perMealKcal = perMeal)
}

private fun buildPlanChanges(
    baseline: List<MealPlanEntry>,
    draft: List<MealPlanEntry>
): List<PlanChange> {
    val baselineById = baseline.associateBy { it.id }
    val draftById = draft.associateBy { it.id }
    val changes = mutableListOf<PlanChange>()

    draft.forEach { plan ->
        val base = baselineById[plan.id]
        if (base == null) {
            changes.add(
                PlanChange(
                    planId = plan.id,
                    mealType = plan.mealId,
                    recipeName = plan.name,
                    beforeServings = null,
                    afterServings = plan.servings,
                    beforeKcal = null,
                    afterKcal = plan.kcal
                )
            )
            return@forEach
        }
        if (!approxEqual(base.servings, plan.servings) || !approxEqual(base.kcal, plan.kcal)) {
            changes.add(
                PlanChange(
                    planId = plan.id,
                    mealType = plan.mealId,
                    recipeName = plan.name,
                    beforeServings = base.servings,
                    afterServings = plan.servings,
                    beforeKcal = base.kcal,
                    afterKcal = plan.kcal
                )
            )
        }
    }

    baseline.forEach { plan ->
        if (!draftById.containsKey(plan.id)) {
            changes.add(
                PlanChange(
                    planId = plan.id,
                    mealType = plan.mealId,
                    recipeName = plan.name,
                    beforeServings = plan.servings,
                    afterServings = null,
                    beforeKcal = plan.kcal,
                    afterKcal = null
                )
            )
        }
    }

    return changes.sortedWith(compareBy({ it.mealType ?: "" }, { it.recipeName }))
}

private fun approxEqual(a: Double, b: Double, epsilon: Double = 0.0001): Boolean {
    return abs(a - b) <= epsilon
}

private fun formatKcal(value: Double): String = value.roundToInt().toString()

private fun formatServings(value: Double): String {
    val rounded = value.roundToInt().toDouble()
    return if (approxEqual(value, rounded)) {
        rounded.toInt().toString()
    } else {
        "%.2f".format(value)
    }
}

private fun formatSigned(value: Double): String {
    val sign = if (value > 0) "+" else if (value < 0) "-" else ""
    return "$sign${formatKcal(abs(value))}"
}

@Composable
private fun MacroStatItem(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape).techGlow(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "${value.roundToInt()}g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

