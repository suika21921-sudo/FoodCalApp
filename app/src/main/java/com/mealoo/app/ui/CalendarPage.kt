package com.mealoo.app.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.VerticalDivider
import android.app.DatePickerDialog
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mealoo.app.R
import com.mealoo.app.model.calculateStats
import com.mealoo.app.data.DiaryNoteStorage
import com.mealoo.app.data.IntakeDataStore
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.data.UserPreferenceDataStore
import com.mealoo.app.model.DiaryNoteEntry
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.EatEntry
import com.mealoo.app.model.IntakeEntry
import com.mealoo.app.model.IntakeStats
import com.mealoo.app.data.MealDefsStore
import com.mealoo.app.data.repository.SubscriptionRepository
import com.mealoo.app.model.MealDef
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.DEFAULT_BREAKFAST_ID
import com.mealoo.app.model.DEFAULT_LUNCH_ID
import com.mealoo.app.model.DEFAULT_DINNER_ID
import com.mealoo.app.model.UNCLASSIFIED_MEAL_ID
import com.mealoo.app.ui.design.MealooCard
import com.mealoo.app.ui.design.ProFeatureDialog
import com.mealoo.app.util.stripBrackets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun MonthAbbreviation(month: java.time.Month): String {
    return when (month) {
        java.time.Month.JANUARY -> stringResource(R.string.month_jan)
        java.time.Month.FEBRUARY -> stringResource(R.string.month_feb)
        java.time.Month.MARCH -> stringResource(R.string.month_mar)
        java.time.Month.APRIL -> stringResource(R.string.month_apr)
        java.time.Month.MAY -> stringResource(R.string.month_may)
        java.time.Month.JUNE -> stringResource(R.string.month_jun)
        java.time.Month.JULY -> stringResource(R.string.month_jul)
        java.time.Month.AUGUST -> stringResource(R.string.month_aug)
        java.time.Month.SEPTEMBER -> stringResource(R.string.month_sep)
        java.time.Month.OCTOBER -> stringResource(R.string.month_oct)
        java.time.Month.NOVEMBER -> stringResource(R.string.month_nov)
        java.time.Month.DECEMBER -> stringResource(R.string.month_dec)
    }
}

@Composable
fun WeekdayAbbreviation(dayOfWeek: java.time.DayOfWeek): String {
    return when (dayOfWeek) {
        java.time.DayOfWeek.SUNDAY -> stringResource(id = R.string.weekday_sun)
        java.time.DayOfWeek.MONDAY -> stringResource(id = R.string.weekday_mon)
        java.time.DayOfWeek.TUESDAY -> stringResource(id = R.string.weekday_tue)
        java.time.DayOfWeek.WEDNESDAY -> stringResource(id = R.string.weekday_wed)
        java.time.DayOfWeek.THURSDAY -> stringResource(id = R.string.weekday_thu)
        java.time.DayOfWeek.FRIDAY -> stringResource(id = R.string.weekday_fri)
        java.time.DayOfWeek.SATURDAY -> stringResource(id = R.string.weekday_sat)
    }
}

private data class MacroTargets(
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val kcal: Double
)
private fun drinkLabel(e: DrinkEntry): String {
    val parts = mutableListOf<String>()
    if (e.waterML > 0) parts += "${e.waterML.toInt()}ml"
    if (e.caffeine > 0) parts += "☕${e.caffeine.toInt()}mg"
    val abv = e.alcoholPercent
    val vol = e.alcoholVolumeML
    if (abv != null && vol != null && abv > 0f && vol > 0.0) {
        parts += "🍺${abv}%/${vol.toInt()}ml"
    }
    return if (parts.isEmpty()) "Drink" else parts.joinToString(" · ")
}

data class DailyCachedData(
    val date: LocalDate,
    val intakeEntries: List<IntakeEntry> = emptyList(),
    val notes: List<DiaryNoteEntry> = emptyList(),
    val plans: List<MealPlanEntry> = emptyList(),
    val totalKcal: Double = 0.0
)

@Composable
fun CalendarPage(navController: NavHostController) {
    val subscriptionRepository = remember { SubscriptionRepository() }
    val isPro by subscriptionRepository.isPro.collectAsState()
    var showProDialog by remember { mutableStateOf(false) }
    val view = LocalView.current
    var isDiaryView by remember { mutableStateOf(false) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val selectedDate = remember(displayedMonth) {
        mutableStateOf(
            if (today.month == displayedMonth.month && today.year == displayedMonth.year) today else null
        )
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mealDefsStore = remember { MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow()
        .collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }
    val intakeStore = remember { IntakeDataStore(context) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var multiSelectedDates by remember { mutableStateOf(setOf<LocalDate>()) }
    val haptic = LocalHapticFeedback.current
    val noteStore = remember { DiaryNoteStorage(context) }

    // ✅ 修改: 使用 MealPlanRepository
    val planStore = remember { MealPlanRepository.getInstance(context) }
    val prefStore = remember { UserPreferenceDataStore(context) }

    val monthCache = remember { mutableStateOf<Map<LocalDate, DailyCachedData>>(emptyMap()) }
    val isCacheLoading = remember { mutableStateOf(true) }
    val targetKcal = remember { mutableDoubleStateOf(2000.0) }

    val firstDayOfMonth = displayedMonth.atDay(1)
    val dayOfWeekOfFirst = firstDayOfMonth.dayOfWeek.value % 7
    val startDate = firstDayOfMonth.minusDays(dayOfWeekOfFirst.toLong())
    val dates = List(42) { startDate.plusDays(it.toLong()) }

    LaunchedEffect(displayedMonth) {
        isCacheLoading.value = true
        targetKcal.doubleValue = prefStore.getPreference().dailyKcalTarget

        val queryDates = dates.map { it.toString() }

        val intakeMap = intakeStore.getEntriesForDateRange(queryDates)
        val noteMap = noteStore.getNotesForDateRange(queryDates)
        val planMap = planStore.getEntriesForDateRange(queryDates)

        val newCache = mutableMapOf<LocalDate, DailyCachedData>()
        dates.forEach { date ->
            val dateStr = date.toString()
            val entries = intakeMap[dateStr] ?: emptyList()
            val notes = noteMap[dateStr] ?: emptyList()
            val plans = planMap[dateStr] ?: emptyList()

            newCache[date] = DailyCachedData(
                date = date,
                intakeEntries = entries,
                notes = notes,
                plans = plans,
                totalKcal = entries.sumOf { it.kcal }
            )
        }

        monthCache.value = newCache
        isCacheLoading.value = false
    }

    val onRefreshDate = { date: LocalDate ->
        scope.launch {
            val dateStr = date.toString()
            val entries = intakeStore.getEntriesForDate(dateStr)
            val notes = noteStore.getNotesForDate(dateStr)
            val plans = planStore.getEntries(dateStr)

            val updatedData = DailyCachedData(
                date = date,
                intakeEntries = entries,
                notes = notes,
                plans = plans,
                totalKcal = entries.sumOf { it.kcal }
            )
            monthCache.value = monthCache.value.toMutableMap().apply { put(date, updatedData) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        isSelectionMode = false
                        multiSelectedDates = emptySet()
                    }) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = stringResource(id = R.string.selected_days_count, multiSelectedDates.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            if (multiSelectedDates.isNotEmpty()) {
                                val sortedDates = multiSelectedDates.sorted().map { it.toString() }
                                val dateString = sortedDates.joinToString(",")

                                navController.navigate("menu_planning/$dateString")

                                isSelectionMode = false
                                multiSelectedDates = emptySet()
                            }
                        },
                        enabled = multiSelectedDates.isNotEmpty()
                    ) {
                        Text(stringResource(id = R.string.go_to_planning))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    cal.set(displayedMonth.year, displayedMonth.monthValue - 1, 1)

                                    DatePickerDialog(
                                        ContextThemeWrapper(context, R.style.MealooDatePickerDark),
                                        { _, year, month, dayOfMonth ->
                                            val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                                            displayedMonth = YearMonth.from(newDate)
                                            selectedDate.value = newDate
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column {
                                Text(
                                    text = displayedMonth.month.name.lowercase()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 28.sp
                                )
                                Text(
                                    text = "${displayedMonth.year}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Row(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), CircleShape)
                                .clip(CircleShape)
                        ) {
                            IconButton(
                                onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = stringResource(id = R.string.previous_month),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(20.dp).width(1.dp).align(Alignment.CenterVertically),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)
                            )
                            IconButton(
                                onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = stringResource(id = R.string.next_month),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(
                        onClick = { isDiaryView = !isDiaryView },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isDiaryView) Icons.Default.CalendarToday else Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isDiaryView) stringResource(R.string.calendar) else stringResource(R.string.diary),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        if (isDiaryView) {
            DiaryViewCached(
                displayedMonth = displayedMonth,
                monthCache = monthCache.value,
                mealDefs = mealDefs,
                onRefreshDate = { onRefreshDate(it) }
            )
        } else {
            val isLandscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            @Composable
            fun WeekHeaderRow() {
                val weekdayLabels = listOf(
                    stringResource(id = R.string.weekday_sun),
                    stringResource(id = R.string.weekday_mon),
                    stringResource(id = R.string.weekday_tue),
                    stringResource(id = R.string.weekday_wed),
                    stringResource(id = R.string.weekday_thu),
                    stringResource(id = R.string.weekday_fri),
                    stringResource(id = R.string.weekday_sat)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weekdayLabels.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        WeekHeaderRow()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(dates.size) { index ->
                                val date = dates[index]
                                val cachedDay = monthCache.value[date]
                                DayCell(
                                    date = date,
                                    displayedMonth = displayedMonth,
                                    today = today,
                                    isSelected = if (isSelectionMode) date in multiSelectedDates else (!isSelectionMode && selectedDate.value == date),
                                    isSelectionMode = isSelectionMode,
                                    totalKcal = cachedDay?.totalKcal ?: 0.0,
                                    targetKcal = targetKcal.doubleValue,
                                    hasEntries = (cachedDay?.totalKcal ?: 0.0) > 0,onDateClick = {
                                        if (isSelectionMode) {
                                            if (date in multiSelectedDates) {
                                                multiSelectedDates = multiSelectedDates - date
                                            } else {
                                                multiSelectedDates = multiSelectedDates + date
                                            }
                                        } else {
                                            selectedDate.value = date
                                        }
                                    },
                                    onDateLongClick = {
                                        if (isPro) {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                multiSelectedDates = setOf(date)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        } else {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                            } else {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            }
                                            showProDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp)
                    ) {
                        selectedDate.value?.let { date ->
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                item {
                                    DiaryEntryCardUnified(
                                        date = date,
                                        scope = rememberCoroutineScope()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                WeekHeaderRow()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(dates.size) { index ->
                        val date = dates[index]
                        val cachedDay = monthCache.value[date]
                        DayCell(
                            date = date,
                            displayedMonth = displayedMonth,
                            today = today,
                            isSelected = if (isSelectionMode) date in multiSelectedDates else (!isSelectionMode && selectedDate.value == date),
                            isSelectionMode = isSelectionMode,
                            totalKcal = cachedDay?.totalKcal ?: 0.0,
                            targetKcal = targetKcal.doubleValue,
                            hasEntries = (cachedDay?.totalKcal ?: 0.0) > 0,onDateClick = {
                                if (isSelectionMode) {
                                    if (date in multiSelectedDates) {
                                        multiSelectedDates = multiSelectedDates - date
                                    } else {
                                        multiSelectedDates = multiSelectedDates + date
                                    }
                                } else {
                                    selectedDate.value = date
                                }
                            },
                            onDateLongClick = {
                                if (isPro) {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        multiSelectedDates = setOf(date)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                    showProDialog = true
                                }
                            }
                        )
                    }
                }

                selectedDate.value?.let { date ->
                    val data = monthCache.value[date] ?: DailyCachedData(date)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        item {
                            DiaryEntrySummaryCard(
                                data = data,
                                targetKcal = targetKcal.doubleValue
                            )
                        }
                    }
                }
            }
        }
    }
    if (showProDialog) {
        ProFeatureDialog(
            onDismiss = { showProDialog = false },
            onUpgrade = {
                showProDialog = false
                navController.navigate("settings?start=account")
            }
        )
    }
}
@Composable
fun DiaryViewCached(
    displayedMonth: YearMonth,
    mealDefs: List<MealDef>,
    monthCache: Map<LocalDate, DailyCachedData>,
    onRefreshDate: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState()
    val today = LocalDate.now()
    val context = LocalContext.current
    val prefStore = remember { UserPreferenceDataStore(context) }

    val targetKcal by produceState(initialValue = 2000.0) {
        value = try { prefStore.getPreference().dailyKcalTarget } catch (e: Exception) { 2000.0 }
    }

    LaunchedEffect(displayedMonth) {
        if (today.month == displayedMonth.month && today.year == displayedMonth.year) {
            listState.scrollToItem(today.dayOfMonth - 1)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        items(displayedMonth.lengthOfMonth()) { index ->
            val date = displayedMonth.atDay(index + 1)
            val dayData = monthCache[date] ?: DailyCachedData(date)

            DiaryEntryCardStateless(
                data = dayData,
                targetKcal = targetKcal,
                mealDefs = mealDefs,
                onRefreshRequest = { onRefreshDate(date) }
            )
        }
    }
}

@Composable
fun DiaryEntryCardStateless(
    data: DailyCachedData,
    targetKcal: Double,
    mealDefs: List<MealDef>,
    onRefreshRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val noteStore = remember { DiaryNoteStorage(context) }

    val date = data.date
    val entries = data.intakeEntries
    var diaryNotes = data.notes
    val plans = data.plans // ✅ 这里的 plans 类型已经是 List<MealPlanEntry>

    val isToday = date == LocalDate.now()

    var showAddForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    var showMacroSheet by remember { mutableStateOf(false) }

    val stats = remember(entries) { calculateStats(entries) }

    val unclassifiedLabel = stringResource(R.string.unclassified)
    val drinkLabelStr = stringResource(R.string.drink)
    val mealDefsById = remember(mealDefs) { mealDefs.associateBy { it.id } }
    val orderedMealIds = remember(mealDefs) { mealDefs.map { it.id } }

    val unifiedItems: List<Pair<String, List<String>>> = remember(entries, diaryNotes, mealDefs) {
        val list = mutableListOf<Pair<String, List<String>>>()
        val drinkGroupId = "drink"
        val grouped = entries.groupBy { entry ->
            when (entry) {
                is EatEntry -> entry.mealId?.takeIf { it.isNotBlank() } ?: UNCLASSIFIED_MEAL_ID
                is DrinkEntry -> drinkGroupId
                else -> UNCLASSIFIED_MEAL_ID
            }
        }
        val mapped = grouped.mapValues { (_, group) ->
            group.map { e ->
                val strip = stripBrackets(e.name)
                when (e) {
                    is EatEntry -> {
                        val clean = strip.substringAfter(" - ", strip)
                        "$clean|${"%.0f".format(e.kcal)}"
                    }
                    is DrinkEntry -> {
                        val label = drinkLabel(e)
                        "$label|${"%.0f".format(e.kcal)}"
                    }
                    else -> "${strip}|${"%.0f".format(e.kcal)}"
                }
            }
        }

        val defaultOrder = listOf(DEFAULT_BREAKFAST_ID, DEFAULT_LUNCH_ID, DEFAULT_DINNER_ID)
        val knownOrder = if (orderedMealIds.isNotEmpty()) orderedMealIds else defaultOrder
        val keys = mapped.keys.toList()
        val (priority, others) = keys.partition { it in knownOrder }
        val sortedKeys = priority.sortedBy { knownOrder.indexOf(it) } +
                others.filter { it != drinkGroupId && it != UNCLASSIFIED_MEAL_ID } +
                listOfNotNull(if (mapped.containsKey(UNCLASSIFIED_MEAL_ID)) UNCLASSIFIED_MEAL_ID else null) +
                listOfNotNull(if (mapped.containsKey(drinkGroupId)) drinkGroupId else null)

        fun labelForId(id: String): String = when (id) {
            UNCLASSIFIED_MEAL_ID -> unclassifiedLabel
            drinkGroupId -> drinkLabelStr
            else -> mealDefsById[id]?.name ?: context.getString(R.string.meal)
        }

        list.addAll(sortedKeys.map { labelForId(it) to (mapped[it] ?: emptyList()) })
        diaryNotes.forEach { note ->
            if (note.title.isNotBlank()) list += note.title to listOf("${note.content}|note")
        }
        list
    }

    MealooCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        backgroundColor = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow,
    ){
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MonthAbbreviation(date.month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = WeekdayAbbreviation(date.dayOfWeek),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                val totalKcal = stats.totalKcal
                val ratio = if (targetKcal > 0.0) (totalKcal / targetKcal).coerceIn(0.0, 1.0) else 0.0
                val percent = (if (targetKcal > 0) totalKcal / targetKcal * 100 else 0.0).roundToInt()
                val progressColor = colorForDailyKcal(totalKcal, targetKcal)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape) // <--- 新增这行，裁剪点击区域为圆形
                        .clickable { showMacroSheet = true }
                ){
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(52.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 4.dp,
                    )
                    CircularProgressIndicator(
                        progress = { ratio.toFloat() },
                        modifier = Modifier.size(52.dp),
                        color = progressColor,
                        strokeWidth = 4.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.kcal_unit),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            if (unifiedItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
                )
            } else {
                unifiedItems.forEach { (title, lines) ->
                    if (title == drinkLabelStr) {
                        val drinks = entries.filterIsInstance<DrinkEntry>()
                        if (drinks.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.drink_timeline_title, title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            DrinkTimeline(
                                drinks = drinks,
                                isToday = isToday,
                                modifier = Modifier.heightIn(max = 240.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    } else {
                        val matchingNote = diaryNotes.find { it.title == title }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (matchingNote != null) {
                                Spacer(Modifier.width(4.dp))
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = stringResource(id = R.string.menu_content_description),
                                        modifier = Modifier.size(16.dp).clickable { expanded = true },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                scope.launch {
                                                    val newNotes = diaryNotes.filterNot { it.id == matchingNote.id }
                                                    noteStore.saveNotesForDate(date.toString(), newNotes)
                                                    onRefreshRequest()
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            lines.forEach { rawLine ->
                                val parts = rawLine.split("|")
                                val content = parts.getOrNull(0) ?: rawLine
                                val meta = parts.getOrNull(1) ?: ""

                                if (meta == "note") {
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = content.replace("｜", ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = stringResource(id = R.string.kcal_amount_format, meta.toIntOrNull() ?: 0),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ✅ 修改: 显示 MealPlanEntry
            if (plans.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.plan_emoji), fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.menu),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        plans.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 修正字段引用: p.name, p.servings
                                Text("• ${p.name}", style = MaterialTheme.typography.bodySmall)
                                Text("x${"%.1f".format(p.servings)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(visible = showAddForm) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(R.string.title), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text(stringResource(R.string.content), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAddForm = false }) { Text(stringResource(R.string.cancel)) }
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() || newContent.isNotBlank()) {
                                    val newEntry = DiaryNoteEntry(
                                        date = date.toString(),
                                        title = newTitle,
                                        content = newContent
                                    )
                                    scope.launch {
                                        val newNotes = diaryNotes + newEntry
                                        noteStore.saveNotesForDate(date.toString(), newNotes)

                                        newTitle = ""
                                        newContent = ""
                                        showAddForm = false
                                        onRefreshRequest()
                                    }
                                }
                            }
                        ) { Text(stringResource(R.string.add)) }
                    }
                }
            }

            if (!showAddForm) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary.copy(alpha=0.7f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_record))
                    }
                }
            }
        }
    }

    if (showMacroSheet) {
        MacroBreakdownSheet(
            stats = stats,
            targetKcal = targetKcal,
            onDismiss = { showMacroSheet = false }
        )
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    displayedMonth: YearMonth,
    today: LocalDate,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    totalKcal: Double,
    targetKcal: Double,
    hasEntries: Boolean,
    onDateClick: () -> Unit,
    onDateLongClick: () -> Unit
) {
    val isThisMonth = date.month == displayedMonth.month
    val isToday = date == today

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary // Light blue
        !isThisMonth -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val borderColor = if (isSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onDateClick,
                onLongClick = onDateLongClick
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onSurface
                } else if (!isThisMonth) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        if (hasEntries && isThisMonth) {
            val color = colorForDailyKcal(totalKcal, targetKcal)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (targetKcal > 0) (totalKcal / targetKcal).coerceIn(0.0, 1.0).toFloat() else 0f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

fun colorForDailyKcal(kcal: Double, target: Double): Color {
    val ratio = (kcal / target).coerceIn(0.0, 2.0)
    return when {
        ratio >= 1.2 -> Color.Red
        ratio >= 0.6 -> Color(0xFF4CAF50)
        ratio > 0.0 -> Color(0xFFB0BEC5)
        else -> Color.Transparent
    }
}
@Composable
fun DiaryEntryCardUnified(
    date: LocalDate,
    scope: CoroutineScope
) {
    val today = LocalDate.now()
    val isToday = date == today
    val context = LocalContext.current

    val intakeStore = remember { IntakeDataStore(context) }
    val noteStore = remember { DiaryNoteStorage(context) }
    val planStore = remember { MealPlanRepository.getInstance(context) }
    val prefStore = remember { UserPreferenceDataStore(context) }
    val mealDefsStore = remember { MealDefsStore(context) }
    var entries by remember { mutableStateOf<List<IntakeEntry>>(emptyList()) }
    var diaryNotes by remember { mutableStateOf<List<DiaryNoteEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var showAddForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    LaunchedEffect(date) {
        loading = true
        entries = intakeStore.getEntriesForDate(date.toString())
        diaryNotes = noteStore.getNotesForDate(date.toString())
        loading = false
    }

    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }

    val stats by remember(entries) { mutableStateOf(calculateStats(entries)) }
    val dailyTarget by produceState(initialValue = 2000.0, key1 = date) {
        value = try { prefStore.getPreference().dailyKcalTarget } catch (e: Exception) { 2000.0 }
    }

    val plans by planStore.entriesFlow(date.toString()).collectAsState(initial = emptyList())

    val unclassifiedLabel = stringResource(R.string.unclassified)
    val drinkLabelStr = stringResource(R.string.drink)

    val mealDefsById = remember(mealDefs) { mealDefs.associateBy { it.id } }
    val orderedMealIds = remember(mealDefs) { mealDefs.map { it.id } }

    val unifiedItems: List<Pair<String, List<String>>> = remember(entries, diaryNotes, mealDefs) {
        val list = mutableListOf<Pair<String, List<String>>>()
        val drinkGroupId = "drink"
        val grouped = entries.groupBy { entry ->
            when (entry) {
                is EatEntry -> entry.mealId?.takeIf { it.isNotBlank() } ?: UNCLASSIFIED_MEAL_ID
                is DrinkEntry -> drinkGroupId
                else -> UNCLASSIFIED_MEAL_ID
            }
        }
        val mapped = grouped.mapValues { (_, group) ->
            group.map { e ->
                val strip = stripBrackets(e.name)
                when (e) {
                    is EatEntry -> {
                        val clean = strip.substringAfter(" - ", strip)
                        "$clean|${"%.0f".format(e.kcal)}"
                    }
                    is DrinkEntry -> {
                        val label = drinkLabel(e)
                        "$label|${"%.0f".format(e.kcal)}"
                    }
                    else -> "${strip}|${"%.0f".format(e.kcal)}"
                }
            }
        }

        val defaultOrder = listOf(DEFAULT_BREAKFAST_ID, DEFAULT_LUNCH_ID, DEFAULT_DINNER_ID)
        val knownOrder = if (orderedMealIds.isNotEmpty()) orderedMealIds else defaultOrder
        val keys = mapped.keys.toList()
        val (priority, others) = keys.partition { it in knownOrder }
        val sortedKeys = priority.sortedBy { knownOrder.indexOf(it) } +
                others.filter { it != drinkGroupId && it != UNCLASSIFIED_MEAL_ID } +
                listOfNotNull(if (mapped.containsKey(UNCLASSIFIED_MEAL_ID)) UNCLASSIFIED_MEAL_ID else null) +
                listOfNotNull(if (mapped.containsKey(drinkGroupId)) drinkGroupId else null)

        fun labelForId(id: String): String = when (id) {
            UNCLASSIFIED_MEAL_ID -> unclassifiedLabel
            drinkGroupId -> drinkLabelStr
            else -> mealDefsById[id]?.name ?: context.getString(R.string.meal)
        }

        list.addAll(sortedKeys.map { labelForId(it) to (mapped[it] ?: emptyList()) })
        diaryNotes.forEach { note ->
            if (note.title.isNotBlank()) list += note.title to listOf("${note.content}|note")
        }
        list
    }

    var showMacroSheet by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isToday) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MonthAbbreviation(date.month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = WeekdayAbbreviation(date.dayOfWeek),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                val totalKcal = entries.sumOf { it.kcal }
                val ratio = if (dailyTarget > 0.0) (totalKcal / dailyTarget).coerceIn(0.0, 1.0) else 0.0
                val percent = (if (dailyTarget > 0) totalKcal / dailyTarget * 100 else 0.0).roundToInt()
                val progressColor = colorForDailyKcal(totalKcal, dailyTarget)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showMacroSheet = true }
                ){
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(52.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 4.dp,
                    )
                    CircularProgressIndicator(
                        progress = { ratio.toFloat() },
                        modifier = Modifier.size(52.dp),
                        color = progressColor,
                        strokeWidth = 4.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            } else if (unifiedItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
                )
            } else {
                unifiedItems.forEach { (title, lines) ->
                    if (title == drinkLabelStr) {
                        val drinks = entries.filterIsInstance<DrinkEntry>()
                        if (drinks.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "🥤 $title",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            DrinkTimeline(
                                drinks = drinks,
                                isToday = isToday,
                                modifier = Modifier.heightIn(max = 240.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    } else {
                        val matchingNote = diaryNotes.find { it.title == title }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (matchingNote != null) {
                                Spacer(Modifier.width(4.dp))
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        modifier = Modifier.size(16.dp).clickable { expanded = true },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                diaryNotes = diaryNotes.filterNot { it.id == matchingNote.id }
                                                scope.launch { noteStore.saveNotesForDate(date.toString(), diaryNotes) }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            lines.forEach { rawLine ->
                                val parts = rawLine.split("|")
                                val content = parts.getOrNull(0) ?: rawLine
                                val meta = parts.getOrNull(1) ?: ""

                                if (meta == "note") {
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = content.replace("｜", ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$meta kcal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ✅ 修改: 显示 MealPlanEntry
            if (plans.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.menu),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        plans.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // p.name, p.servings
                                Text("• ${p.name}", style = MaterialTheme.typography.bodySmall)
                                Text("x${"%.1f".format(p.servings)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(visible = showAddForm) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(stringResource(R.string.title), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text(stringResource(R.string.content), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showAddForm = false }) { Text(stringResource(R.string.cancel)) }
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() || newContent.isNotBlank()) {
                                    val newEntry = DiaryNoteEntry(
                                        date = date.toString(),
                                        title = newTitle,
                                        content = newContent
                                    )
                                    diaryNotes = diaryNotes + newEntry
                                    scope.launch { noteStore.saveNotesForDate(date.toString(), diaryNotes) }
                                    newTitle = ""
                                    newContent = ""
                                    showAddForm = false
                                }
                            }
                        ) { Text(stringResource(R.string.add)) }
                    }
                }
            }

            if (!showAddForm) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary.copy(alpha=0.7f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_record))
                    }
                }
            }
        }
    }

    if (showMacroSheet) {
        MacroBreakdownSheet(
            stats = stats,
            targetKcal = dailyTarget,
            onDismiss = { showMacroSheet = false }
        )
    }
}
fun getDaySuffix(day: Int): String {
    return if (day in 11..13) "th"
    else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
@Composable
fun DiaryEntrySummaryCard(
    data: DailyCachedData,
    targetKcal: Double
) {
    val context = LocalContext.current
    val date = data.date
    val isToday = date == LocalDate.now()
    val entries = data.intakeEntries

    val stats = remember(entries) { calculateStats(entries) }

    var showMacroSheet by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isToday) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MonthAbbreviation(date.month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = WeekdayAbbreviation(date.dayOfWeek),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                val totalKcal = stats.totalKcal
                val ratio = if (targetKcal > 0.0) (totalKcal / targetKcal).coerceIn(0.0, 1.0) else 0.0
                val percent = (if (targetKcal > 0) totalKcal / targetKcal * 100 else 0.0).roundToInt()
                val progressColor = colorForDailyKcal(totalKcal, targetKcal)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showMacroSheet = true }
                ){
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(52.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 4.dp,
                    )
                    CircularProgressIndicator(
                        progress = { ratio.toFloat() },
                        modifier = Modifier.size(52.dp),
                        color = progressColor,
                        strokeWidth = 4.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showMacroSheet) {
        MacroBreakdownSheet(
            stats = stats,
            targetKcal = targetKcal,
            onDismiss = { showMacroSheet = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroBreakdownSheet(
    stats: IntakeStats,
    targetKcal: Double,
    onDismiss: () -> Unit
) {
    val pKcal = stats.totalProtein * 4.0
    val cKcal = stats.totalCarbs * 4.0
    val fKcal = stats.totalFats * 9.0
    val aKcal = stats.totalAlcoholGram * 7.0
    val denom = (pKcal + cKcal + fKcal + aKcal).takeIf { it > 0 } ?: 1.0
    val ps = pKcal / denom
    val cs = cKcal / denom
    val fs = fKcal / denom
    val ashare = aKcal / denom

    val context = LocalContext.current
    val tips = remember(stats, targetKcal) { buildMacroTips(context, stats, targetKcal) }

    val prefStore = remember(context) { UserPreferenceDataStore(context) }
    val dailyTargets by produceState<MacroTargets>(
        initialValue = MacroTargets(0.0, 0.0, 0.0, 0.0)
    ) {
        val p = prefStore.getPreference()
        value = MacroTargets(
            protein = p.dailyProteinTarget,
            carbs   = p.dailyCarbsTarget,
            fats    = p.dailyFatsTarget,
            kcal    = p.dailyKcalTarget
        )
    }
    val targetP = dailyTargets.protein
    val targetC = dailyTargets.carbs
    val targetF = dailyTargets.fats
    val targetEFromPref = dailyTargets.kcal


    var tab by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.macro_daily_share)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.macro_target_progress)) })
            }
            if (tab == 0) {
                Text(stringResource(R.string.macro_daily_share), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                MacroRowDot(stringResource(R.string.protein), stats.totalProtein, "g", ps, Color(0xFF4CAF50))
                MacroRowDot(stringResource(R.string.carbs),   stats.totalCarbs,   "g", cs, Color(0xFF2196F3))
                MacroRowDot(stringResource(R.string.fats),   stats.totalFats,    "g", fs, Color(0xFFFF9800))
                if (aKcal > 0.0) {
                    MacroRowDot(stringResource(R.string.macro_alcohol_equivalent), stats.totalAlcoholGram, "g", ashare, Color(0xFFF44336))
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.totalkcal_fmt,stats.totalKcal.roundToInt()))
                    if ((targetKcal.takeIf { it > 0 } ?: targetEFromPref) > 0) {
                        val tk = if (targetKcal > 0) targetKcal else targetEFromPref
                        val delta = (stats.totalKcal - tk).roundToInt()
                        Text(
                            if (delta >= 0) stringResource(R.string.exceed_kcal,delta) else stringResource(R.string.macro_remaining_to_target, -delta),
                            color = if (delta > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (tips.isNotEmpty()) {
                    Text(stringResource(R.string.macro_suggestions_tips), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tips.forEach { t -> Text("• $t", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            } else {
                Text(stringResource(R.string.macro_daily_target_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                val tk = if (targetKcal > 0) targetKcal else targetEFromPref
                ProgressRow(title = stringResource(R.string.totalkcal), value = stats.totalKcal, target = tk, unit = "kcal")

                if (targetP > 0) ProgressRow(stringResource(R.string.protein), stats.totalProtein, targetP, "g")
                if (targetC > 0) ProgressRow(stringResource(R.string.carbs),   stats.totalCarbs,   targetC, "g")
                if (targetF > 0) ProgressRow(stringResource(R.string.fats),   stats.totalFats,    targetF, "g")
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) { Text(stringResource(R.string.close)) }
        }
    }
}

@Composable
private fun ProgressRow(
    title: String,
    value: Double,
    target: Double,
    unit: String
) {
    if (target <= 0) return
    val ratio = (value / target).coerceIn(0.0, 2.0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            val right = "${"%.0f".format(value)} / ${"%.0f".format(target)} $unit"
            Text(right, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = (ratio.coerceIn(0.0, 1.0)).toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
        if (ratio >= 1.0) {
            val exceed = (value - target).roundToInt()
            Text(stringResource(R.string.macro_exceeded_by, exceed, unit), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else {
            val remain = (target - value).roundToInt()
            Text(stringResource(R.string.macro_remaining_amount, remain, unit), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
private fun MacroRowDot(
    title: String,
    amount: Double,
    unit: String,
    share: Double,
    dotColor: Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(8.dp))
            Text(
                "$title  ${"%.0f".format(amount)} $unit  ·  ${"%.0f".format(share * 100)}%",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share.toFloat().coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dotColor.copy(alpha = 0.9f))
            )
        }
    }
}
private fun buildMacroTips(context: Context, stats: IntakeStats, targetKcal: Double): List<String> {
    val tips = mutableListOf<String>()
    val tot = stats.totalKcal
    if (tot > 0) {
        val p = stats.totalProtein * 4.0 / tot
        val c = stats.totalCarbs   * 4.0 / tot
        val f = stats.totalFats    * 9.0 / tot

        if (p < 0.20) {
            val needG = (((0.20 * tot) - stats.totalProtein * 4.0) / 4.0).coerceAtLeast(0.0)
            tips += context.getString(R.string.tip_protein_low, needG.roundToInt())
        }
        if (c > 0.55) tips += context.getString(R.string.tip_carbs_high)
        if (f > 0.40) tips += context.getString(R.string.tip_fat_high)
    }

    if (stats.totalCholesterol > 300) tips += context.getString(R.string.tip_cholesterol_high, stats.totalCholesterol.roundToInt())
    if (stats.totalAlcoholGram > 0) tips += context.getString(R.string.tip_alcohol_consumed, stats.totalAlcoholGram.roundToInt())

    if (targetKcal > 0 && tot > 0) {
        val delta = (tot - targetKcal).roundToInt()
        if (delta > 0) tips += context.getString(R.string.tip_kcal_exceeded, delta)
        if (delta < 0) tips += context.getString(R.string.tip_kcal_remaining, -delta)
    }
    return tips
}
