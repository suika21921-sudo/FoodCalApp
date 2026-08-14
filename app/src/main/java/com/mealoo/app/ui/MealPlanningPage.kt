package com.mealoo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mealoo.app.R
import com.mealoo.app.data.MealDefsStore
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.data.UserPreferenceDataStore
import com.mealoo.app.model.MealKind
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.SelectedFood
import com.mealoo.app.ui.design.MealooTheme
import com.mealoo.app.util.stripBrackets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
private fun mealColorForKind(kind: MealKind?): Color = when (kind) {
    MealKind.BREAKFAST -> MealooTheme.colors.breakfast
    MealKind.LUNCH -> MealooTheme.colors.lunch
    MealKind.DINNER -> MealooTheme.colors.dinner
    MealKind.SNACK -> MealooTheme.colors.snack
    MealKind.CUSTOM, null -> MaterialTheme.colorScheme.primary
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuPlanningPage(
    navController: NavHostController,
    dateString: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val planStore = remember { MealPlanRepository.getInstance(context) }
    val mealDefsStore = remember { MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }

    val prefStore = remember { UserPreferenceDataStore(context) }
    var userPref by remember {
        mutableStateOf(UserPreferenceDataStore.createLocalizedDefaultPreference(context))
    }
    LaunchedEffect(Unit) {
        userPref = prefStore.getPreference()
    }

    val dates = remember(dateString) {
        dateString.split(",").mapNotNull {
            try { LocalDate.parse(it.trim()) } catch (e: Exception) { null }
        }.sorted()
    }

    var currentFocusDate by remember { mutableStateOf(dates.firstOrNull() ?: LocalDate.now()) }
    var applyToAllDates by remember { mutableStateOf(dates.size > 1) }

    // MealPicker 状态
    var showMealPicker by remember { mutableStateOf(false) }
    var currentTargetMealId by remember { mutableStateOf("") }

    val currentPlans by remember(currentFocusDate) {
        planStore.entriesFlow(currentFocusDate.toString())
    }.collectAsState(initial = emptyList())

    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.plan_menu_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, spotColor = Color.Black.copy(alpha = 0.1f)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, context.getString(R.string.menu_plan_saved_toast), android.widget.Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(stringResource(id = R.string.complete_planning_button), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ... (日期切换条代码保持不变) ...
            if (dates.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.selected_days_count, dates.size), style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = applyToAllDates,
                        onClick = { applyToAllDates = !applyToAllDates },
                        label = { Text(stringResource(id = R.string.apply_to_all_dates)) },
                        leadingIcon = { if (applyToAllDates) Icon(Icons.Default.DoneAll, null, Modifier.size(16.dp)) }
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dates) { date ->
                        DateCapsule(
                            date = date,
                            isSelected = date == currentFocusDate,
                            onClick = { currentFocusDate = date }
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // --- 3. 餐次列表 ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mealDefs) { mealDef ->
                    // ✅ 修改 3：过滤 MealPlanEntry
                    val items = currentPlans.filter {
                        it.mealId == mealDef.id
                    }

                    // 计算目标热量
                    val ratio = userPref.mealRatioMap[mealDef.id]
                        ?: (100.0 / maxOf(1, mealDefs.size))
                    val targetKcal = userPref.dailyKcalTarget * (ratio / 100.0)
                    val currentKcal = items.sumOf { it.kcal }

                    MenuMealCard(
                        mealLabel = mealDef.name,
                        color = mealColorForKind(mealDef.kind),
                        items = items,
                        targetKcal = targetKcal,
                        currentKcal = currentKcal,
                        onAddClick = {
                            currentTargetMealId = mealDef.id
                            showMealPicker = true
                        },
                        onRemoveClick = { planItem ->
                            scope.launch {
                                val targetDates = if (applyToAllDates) dates else listOf(currentFocusDate)
                                targetDates.forEach { date ->
                                    // ✅ 修改 4：调用 removeEntry
                                    planStore.removeEntry(date.toString(), planItem.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showMealPicker) {
        MealPickerDialog(
            onDismiss = { showMealPicker = false },
            onConfirm = { selectedFoods, _ ->
                showMealPicker = false
                scope.launch {
                    // 1. 準備目標日期清單 (List<String>)
                    val targetDateStrings = if (applyToAllDates) {
                        dates.map { it.toString() }
                    } else {
                        listOf(currentFocusDate.toString())
                    }

                    try {
                        // 2. 建立模板 Entries
                        // 日期先給空字串或任意值即可，因為 addEntriesToDates 會自動用目標日期覆寫它
                        // ID 也會在那邊重新生成
                        val templateEntries = selectedFoods.map { food ->
                            food.toMealPlanEntry(date = "", mealId = currentTargetMealId)
                        }

                        // ✅ 3. 呼叫新的批量寫入方法 (一次 IO 完成所有日期寫入)
                        planStore.addEntriesToDates(targetDateStrings, templateEntries)

                        val msg = if (applyToAllDates) context.getString(R.string.added_to_multiple_days, dates.size) else context.getString(R.string.added_successfully)
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        // 由於移除了庫存驗證 Exception，這裡主要是抓取潛在的 IO 錯誤
                        val message = e.localizedMessage ?: context.getString(R.string.add_failed)
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}
// --- 现代化的日期胶囊 (仿 DayCell) ---
@Composable
fun DateCapsule(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val formatter = DateTimeFormatter.ofPattern("dd")

    Column(
        modifier = Modifier
            .width(52.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.name.take(3),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f)
        )
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun MenuMealCard(
    mealLabel: String,
    color: Color,
    items: List<MealPlanEntry>, // ✅ 这里的类型改了
    targetKcal: Double,
    currentKcal: Double,
    onAddClick: () -> Unit,
    onRemoveClick: (MealPlanEntry) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = mealLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalIconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                ) {
                    Icon(Icons.Default.Add, stringResource(id = R.string.add), Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 进度条区域
            if (targetKcal > 0) {
                val progress = (currentKcal / targetKcal).toFloat().coerceIn(0f, 1f)
                val percentage = (currentKcal / targetKcal * 100).toInt()
                val isExceeded = currentKcal > targetKcal

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(if (isExceeded) Color(0xFFEF5350) else color)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.kcal_amount_format, currentKcal.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExceeded) Color(0xFFEF5350) else color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.kcal_target_percentage_format, targetKcal.toInt(), percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Items List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.click_top_right_to_add),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stripBrackets(item.name), // ✅ 使用 item.name
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(id = R.string.kcal_amount_format, item.kcal.toInt()) + " · x${item.servings}", // ✅ 使用 item.kcal
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onRemoveClick(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, stringResource(id = R.string.remove_button_description), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ 扩展函数：SelectedFood -> MealPlanEntry (带营养素计算)
fun SelectedFood.toMealPlanEntry(date: String, mealId: String): MealPlanEntry {
    // 1. 获取基础每100g (或每份) 的营养数据
    val energy = this.standardNutrients["Energy"] ?: 0.0
    val proteinVal = this.standardNutrients["Protein"] ?: 0.0
    val carbsVal = this.standardNutrients["Carbohydrate, by difference"] ?: 0.0
    val fatsVal = this.standardNutrients["Total lipid (fat)"] ?: 0.0
    val cholVal = this.standardNutrients["Cholesterol"] ?: 0.0

    // 2. 计算倍率
    // 如果是 RECIPE (份菜)，standardNutrients 通常已经是每份的值，factor = 1.0 (或者根据选中的 serving 计算)
    // 如果是 USDA/SHOP (基础食材)，standardNutrients 通常是 per 100g，factor = grams / 100
    val factor = if (this.source == "RECIPE") {
        1.0
    } else {
        this.grams / 100.0
    }

    return MealPlanEntry(
        id = UUID.randomUUID().toString(),
        date = date,
        mealId = mealId,
        name = this.name,
        source = this.source,

        // 数量
        grams = this.grams,
        servings = 1.0,
        amount = this.grams,
        amountUnit = "g",

        // 营养 (总值)
        kcal = energy * factor,
        protein = proteinVal * factor,
        carbs = carbsVal * factor,
        fats = fatsVal * factor,
        cholesterol = cholVal * factor,

        // 追溯
        fdcId = this.fdcId,
        inventoryId = this.inventoryId
    )
}
