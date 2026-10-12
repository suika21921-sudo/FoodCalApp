@file:Suppress("ProduceStateDoesNotAssignValue")

package com.mealoo.app.ui
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.CoroutineScope
import androidx.navigation.NavHostController
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.ui.unit.dp
import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import android.widget.Toast
import com.mealoo.app.model.IntakeEntry
import com.mealoo.app.model.MealMacroSummary
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import com.mealoo.app.model.IntakeStats
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.StackedBarChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.mealoo.app.data.*
import com.mealoo.app.db.InventoryRepository
import com.mealoo.app.db.SavingsRepository
import com.mealoo.app.model.EatEntry
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mealoo.app.util.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import com.mealoo.app.model.calculateStats
import java.text.SimpleDateFormat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.mealoo.app.R
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.RecipeEntry
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.pow
import com.mealoo.app.data.MealDefsStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.google.api.services.drive.Drive
import com.mealoo.app.model.MealDef
import com.mealoo.app.model.MealKind
import com.mealoo.app.model.UNCLASSIFIED_MEAL_ID
import com.mealoo.app.utils.dailyMacroTargets
import com.mealoo.app.utils.mealMacroTargets
import kotlin.math.exp
import com.mealoo.app.DeepLinkViewModel
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.inset
import com.mealoo.app.ui.design.CompactTextField
import com.mealoo.app.ui.design.MealooButton
import com.mealoo.app.ui.design.MealooButtonStyle
import com.mealoo.app.ui.design.MealooCard
import com.mealoo.app.ui.design.MealooTextField
import com.mealoo.app.ui.design.MealooTheme


object MainTechColors {
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
    borderColor: Color = MainTechColors.TechBorder
) = this
    .clip(shape)
    .border(1.dp, borderColor, shape)
@Composable
private fun Modifier.techGlow(
    color: Color = MainTechColors.AccentCyan,
    alpha: Float = 0.15f
) = this.background(
    androidx.compose.ui.graphics.Brush.radialGradient(
        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
        radius = 800f
    )
)

data class ScreenItem(val route: String,val icon: ImageVector)


private val LocalDrive = staticCompositionLocalOf<Drive?> { null }

@Composable
fun DateSelectorRow(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormatter = remember { SimpleDateFormat("yyyy-MM-dd EEE", Locale.getDefault()) }

    // 用选中的日期来更新 calendar
    val calendar = remember(selectedDate) { Calendar.getInstance() }.apply {
        time = dateFormatter.parse(selectedDate) ?: Date()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 每次点击都新建一次 DatePickerDialog
                val cal = Calendar.getInstance().apply {
                    time = dateFormatter.parse(selectedDate) ?: Date()
                }
                DatePickerDialog(
                    ContextThemeWrapper(context, R.style.MealooDatePickerDark),
                    { _, year, month, dayOfMonth ->
                        val picked = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                        onDateSelected(picked)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayFormatter.format(calendar.time),
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "選擇日期"
        )
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainScreen(
    deepLinkViewModel: DeepLinkViewModel,
    navController: NavHostController,
    dataStore: IntakeDataStore,
    drive: Drive?
) {
    var currentTab by rememberSaveable { mutableStateOf("main") }
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val appCtx = context.applicationContext

    val savingsRepo = remember(appCtx) { SavingsRepository.getInstance(appCtx) }
    val inventoryRepo = remember(appCtx) { InventoryRepository.getInstance(appCtx) }
    val storageItems by inventoryRepo.itemsFlow().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        deepLinkViewModel.importEvent.collect { json ->
            Log.i("Mealoo_Debug_Nav", "MainScreen: Received import signal. Navigating to 'evaluate'...")
            navController.navigate("evaluate") {
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(LocalDrive provides drive) {
        Scaffold(
            containerColor = MainTechColors.DeepBackground,
            bottomBar = {
                if (isBottomBarVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 1. 先處理系統小白條，確保膠囊不會貼死螢幕底部
                            .navigationBarsPadding()
                            // 2. 設定膠囊外圍的懸浮邊距 (Margin)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            // 3. 鎖定膠囊本身的高度
                            .height(64.dp)
                            // 4. 繪製科技感半透明背景與邊框
                            .background(
                                color = MainTechColors.DeepBackground.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .techGlassBorder(RoundedCornerShape(24.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ){

                        val items = listOf(
                            ScreenItem("main", Icons.Filled.Home),
                            ScreenItem("recipe", Icons.Filled.BookmarkBorder),
                            ScreenItem("storage", Icons.Default.Inventory2),
                            ScreenItem("calendar", Icons.Filled.CalendarToday),
                            ScreenItem("evaluate", Icons.Outlined.StackedBarChart),
                        )

                        items.forEach { item ->
                            val selected = currentTab == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = item.route },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.route,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor =  Color.Transparent,
                                    selectedIconColor = MainTechColors.AccentCyan,
                                    unselectedIconColor = Color.Gray.copy(alpha = 0.5f)
                                ),

                                alwaysShowLabel = false
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(
                        top = innerPadding.calculateTopPadding()
                    )
                    .fillMaxSize()
            ) {
                when (currentTab) {
                    "main" -> {
                        MainPage(
                            navController = navController,
                            dataStore = dataStore,
                            onToggleBottomBar = { isVisible -> isBottomBarVisible = isVisible }
                        )
                    }
                    "recipe" -> {
                        val context = LocalContext.current
                        val recipeDataStore = remember { RecipeDataStore(context) }
                        RecipePage(dataStore = recipeDataStore)
                    }
                    "calendar" -> {
                        CalendarPage(navController = navController)
                    }
                    "storage" -> {
                        StoragePage(
                            savingsRepo = savingsRepo,
                            store = inventoryRepo,
                            allItems = storageItems
                        )
                    }
                    "evaluate" -> {
                        val prefStore = remember { UserPreferenceDataStore(appCtx) }
                        val userPref by produceState(
                            initialValue = UserPreferenceDataStore.createLocalizedDefaultPreference(appCtx),
                            appCtx
                        ) {
                            value = runCatching { prefStore.getPreference() }
                                .getOrElse { UserPreferenceDataStore.createLocalizedDefaultPreference(appCtx) }
                        }
                        val intakeStore = remember(appCtx) { IntakeDataStore(appCtx) }

                        EvaluatePage(
                            deepLinkViewModel = deepLinkViewModel,
                            storageItems = storageItems,
                            userPref = userPref,
                            intakeStore = intakeStore,
                            savingsRepo = savingsRepo,
                            drive = drive
                        )
                    }
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainPage(
    navController: NavHostController,
    dataStore: IntakeDataStore,
    onToggleBottomBar: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val intakeStore = dataStore

    // --- 1. 狀態與數據獲取 ---
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by rememberSaveable { mutableStateOf(dateFormatter.format(Date())) }

    // 處理與 CalendarPage 的導航交互
    LaunchedEffect(selectedDate) {
        navController.currentBackStackEntry?.savedStateHandle?.set("selectedDate", selectedDate)
    }
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("selectedDate")
            ?.let { selectedDate = it }
    }

    // 獲取 Intake Entries
    val intakeEntries by intakeStore
        .entriesFlow(selectedDate)
        .collectAsState(initial = emptyList())

    // 計算 Stats
    val stats = remember(intakeEntries) { calculateStats(intakeEntries) }
    val prefStore = remember { UserPreferenceDataStore(context) }
    var userPref by remember { mutableStateOf<UserPreference?>(null) }

    // 監聽 Settings 返回的更新
    val updatedJsonFlow = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("settings_updated_pref_json", null)
    val updatedJson by updatedJsonFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        userPref = prefStore.getPreference()
    }
    LaunchedEffect(updatedJson) {
        updatedJson?.let { json ->
            val latest = Json.decodeFromString(UserPreference.serializer(), json)
            userPref = latest
            navController.currentBackStackEntry?.savedStateHandle?.set("settings_updated_pref_json", null)
        }
    }

    // --- 4. 局部 UI 狀態 ---
    var showMealPicker by remember { mutableStateOf(false) }
    var showDrinkSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showMealPicker) {
        onToggleBottomBar(!showMealPicker)
    }

    var selectedMealId by remember { mutableStateOf("") }
    var selectedMealDetail by remember { mutableStateOf<Pair<String, List<EatEntry>>?>(null) }

    val eatEntries = remember(intakeEntries) { intakeEntries.filterIsInstance<EatEntry>() }
    fun onMealDetailClick(mealId: String) {
        val mealEntries = eatEntries.filter { (it.mealId ?: UNCLASSIFIED_MEAL_ID) == mealId }
        selectedMealDetail = mealId to mealEntries
    }

    val dailyStore = remember { DailyMealConfigStore(context) }
    val mealDefsStore = remember { MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    val mealDefsById = remember(mealDefs) { mealDefs.associateBy { it.id } }
    val dailyCfgFlow = remember(selectedDate) { dailyStore.flow(selectedDate) }
    val dailyCfg by dailyCfgFlow.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }

    val templateMealIds = remember(mealDefs) { mealDefs.map { it.id } }
    val mealIdsAll = dailyCfg?.mealIds?.takeIf { it.isNotEmpty() } ?: templateMealIds

    fun inferMealId(entry: EatEntry): String =
        entry.mealId?.takeIf { it.isNotBlank() } ?: UNCLASSIFIED_MEAL_ID

    val inferredTodayIds = remember(eatEntries) {
        eatEntries.map { inferMealId(it) }.distinct()
    }

    val mealIds = remember(mealIdsAll, templateMealIds, inferredTodayIds) {
        mealIdsAll.filter { it != UNCLASSIFIED_MEAL_ID && (it in templateMealIds || it in inferredTodayIds) }
    }

    // 這些變數設為 nullable 或空集合，稍後傳入 item
    var mealTargets: List<Pair<String, Double>> = emptyList()
    var mealCurrentKcal: Map<String, Double> = emptyMap()
    var unclassifiedEntries: List<EatEntry> = emptyList()

    unclassifiedEntries = remember(eatEntries) {
        eatEntries.filter { it.mealId.isNullOrBlank() || it.mealId == UNCLASSIFIED_MEAL_ID }
    }

    // 只有當 userPref 存在時才進行複雜計算
    if (userPref != null) {
        LaunchedEffect(inferredTodayIds, mealIdsAll, selectedDate) {
            val missing = inferredTodayIds.filter { it != UNCLASSIFIED_MEAL_ID && it !in mealIdsAll }
            if (missing.isNotEmpty()) {
                val newIds = mealIdsAll + missing
                val seed = dailyCfg?.ratioByMealId?.takeIf { it.isNotEmpty() } ?: userPref!!.mealRatioMap
                val migrated = recomputeMealRatios(newIds, seed.toList(), 0.1)
                dailyStore.save(
                    selectedDate,
                    DailyMealConfig(mealIds = newIds, ratioByMealId = migrated.toMap())
                )
            }
        }

        val finalRatio: Map<String, Double> = remember(dailyCfg, mealIds) {
            val m = dailyCfg?.ratioByMealId ?: emptyMap()
            if (m.keys.containsAll(mealIds)) m else {
                val seed = dailyCfg?.ratioByMealId?.takeIf { it.isNotEmpty() } ?: userPref!!.mealRatioMap
                recomputeMealRatios(mealIds, seed.toList(), 0.1).toMap()
            }
        }

        val totalKcalTarget = userPref!!.dailyKcalTarget

        mealTargets = remember(mealIds, finalRatio, totalKcalTarget) {
            mealIds.map { id -> id to (totalKcalTarget * (finalRatio[id] ?: 0.0) / 100.0) }
        }

        mealCurrentKcal = remember(eatEntries, mealIds) {
            mealIds.associateWith { mealId ->
                eatEntries.asSequence()
                    .filter { (it.mealId ?: UNCLASSIFIED_MEAL_ID) == mealId }
                    .sumOf { it.kcal }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp), // 全局 Padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // Item 之間的間距
        ) {
            item (key = "topbar"){
                TopBar(
                    navController = navController,
                    onSettingsClick = {
                        val snapshotJson = userPref?.let {
                            Json.encodeToString(UserPreference.serializer(), it)
                        }
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("pref_snapshot_json", snapshotJson)
                        navController.navigate("settings")
                    }
                )
            }

            item(key = "dashboard") {
                CombinedDashboardCard(
                    stats = stats,
                    userGoalKcal = userPref?.dailyKcalTarget ?: 2000.0,
                    userGoalWater = 2500.0,
                    onAddDrinkClick = { showDrinkSheet = true }
                )
            }

            item(key = "nutrients") {
                val dailyTargets = userPref?.let { pref ->
                    dailyMacroTargets(
                        weightKg = pref.weightKg.toDouble(),
                        dailyKcalTarget = pref.dailyKcalTarget,
                        proteinGPerKg = pref.proteinGPerKg,
                        fatGPerKg = pref.fatGPerKg
                    )
                }
                
                val maxVals = dailyTargets?.let {
                    listOf(it.proteinG.toInt(), it.carbsG.toInt(), it.fatG.toInt())
                } ?: listOf(200, 300, 70)

                CombinedNutrientCard(
                    items = listOf(
                        Triple(stringResource(R.string.protein), stats.totalProtein to "g", Icons.Default.FitnessCenter),
                        Triple(stringResource(R.string.carbs), stats.totalCarbs to "g", Icons.Default.Grass),
                        Triple(stringResource(R.string.fats), stats.totalFats to "g", Icons.Default.WaterDrop)
                    ),
                    maxValues = maxVals
                )
            }

            item(key = "date_selector") {
                DateSelectorRow(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }

            if (userPref != null) {
                item {
                    MealCategoryCard(
                        mealTargets = mealTargets,           // 傳入上方計算好的結果
                        mealCurrentKcal = mealCurrentKcal,
                        mealDefsById = mealDefsById,
                        intakeEntries = eatEntries,
                        unclassifiedEntries = unclassifiedEntries,
                        selectedDate = selectedDate,
                        dataStore = dataStore,
                        onMealAddClick = {
                            selectedMealId = it
                            showMealPicker = true
                        },
                        userPref = userPref,
                        prefStore = prefStore,
                        onMealDetailClick = { onMealDetailClick(it) },
                        dailyStore = dailyStore,
                        onAddNewMealCategoryClick = {
                            navController.navigate("settings")
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(100.dp))
            }
        }

        if (showDrinkSheet) {
            DrinkAddBottomSheet(
                visible = true,
                onDismiss = { showDrinkSheet = false },
                onAdd = { water, caffeine, sugar, abv, vol, name ->
                    scope.launch {
                        val gramsAlcohol = if (abv != null && vol != null) vol * (abv / 100.0) * 0.789 else 0.0
                        val kcalAlcohol = gramsAlcohol * 7.0
                        val kcalSugar = (sugar ?: 0.0) * 4.0
                        val totalKcal = kcalAlcohol + kcalSugar

                        dataStore.addEntry(
                            DrinkEntry(
                                name = name ?: "Drink",
                                kcal = totalKcal,
                                caffeine = caffeine ?: 0.0,
                                sugarGrams = sugar ?: 0.0,
                                waterML = water ?: 0.0,
                                alcoholVolumeML = vol,
                                alcoholPercent = abv?.toFloat(),
                                date = selectedDate,
                                category = null
                            )
                        )
                    }
                    showDrinkSheet = false
                }
            )
        }
        if (showMealPicker) {
            MealPickerDialog(
                onDismiss = { showMealPicker = false },
                onConfirm = { selectedFoods, selectedSource ->
                    showMealPicker = false
                    scope.launch {
                        selectedFoods.forEach { food ->
                            val needsScaling = food.source != "RECIPE"
                            val ratio = if (needsScaling) (food.grams / 100.0) else 1.0
                            val kcal = (food.standardNutrients["Energy"] ?: 0.0) * ratio
                            val protein = (food.standardNutrients["Protein"] ?: 0.0) * ratio
                            val carbs = (
                                    food.standardNutrients["Available carbohydrate"]
                                        ?: food.standardNutrients["Carbohydrate, by difference"]
                                        ?: 0.0
                                    ) * ratio
                            val fats = (food.standardNutrients["Total lipid (fat)"] ?: 0.0) * ratio
                            val cholesterol = (food.standardNutrients["Cholesterol"] ?: 0.0) * ratio
                            val gramsValue = when {
                                food.grams != null && food.grams > 0.0 -> food.grams
                                else -> 100.0
                            }
                            val entry = EatEntry(
                                name = food.name,
                                mealId = selectedMealId,
                                fdcId = food.fdcId,
                                source = food.source,
                                grams = gramsValue,
                                kcal = kcal,
                                protein = protein,
                                carbs = carbs,
                                fats = fats,
                                cholesterol = cholesterol,
                                date = selectedDate,
                                category = food.category
                            )
                            dataStore.addEntry(entry)
                        }
                    }
                }
            )
        }
        userPref?.let { pref ->
            selectedMealDetail?.let { (mealId, entries) ->
                val mealDef = mealDefsById[mealId]
                val mealLabel = when (mealDef?.kind) {
                    MealKind.BREAKFAST -> context.getString(R.string.breakfast)
                    MealKind.LUNCH -> context.getString(R.string.lunch)
                    MealKind.DINNER -> context.getString(R.string.dinner)
                    MealKind.SNACK -> context.getString(R.string.snack)
                    else -> when {
                        mealId == UNCLASSIFIED_MEAL_ID -> context.getString(R.string.unclassified)
                        mealDef?.name != null -> mealDef.name
                        else -> context.getString(R.string.meal)
                    }
                }
                val totalKcal = pref.dailyKcalTarget
                val ratioMap = dailyCfg?.ratioByMealId?.takeIf { it.isNotEmpty() }
                    ?: pref.mealRatioMap
                val fallbackRatio = if (ratioMap.isNotEmpty()) 100.0 / ratioMap.size else 0.0
                val mealRatio = ratioMap[mealId] ?: fallbackRatio

                val dailyTargets = dailyMacroTargets(
                    weightKg = pref.weightKg.toDouble(),
                    dailyKcalTarget = totalKcal,
                    proteinGPerKg = pref.proteinGPerKg,
                    fatGPerKg = pref.fatGPerKg
                )
                val mealTargets = mealMacroTargets(dailyTargets, mealRatio)

                MealDetailBottomSheet(
                    mealId = mealId,
                    mealLabel = mealLabel,
                    mealKind = mealDef?.kind,
                    entries = entries,
                    userPref = pref,
                    onDismiss = { selectedMealDetail = null },
                    onDeleteEntry = { entryId ->
                        scope.launch {
                            val newAll = intakeEntries.filterNot { it.id == entryId }
                            intakeStore.updateEntriesForDate(selectedDate, newAll)
                            val remaining = entries.filterNot { it.id == entryId }
                            selectedMealDetail =
                                if (remaining.isEmpty()) null else (mealId to remaining)
                        }
                    },
                    onEditEntry = { /* TODO */ },
                    targetProteinG = mealTargets.proteinG,
                    targetCarbG = mealTargets.carbsG,
                    targetFatG = mealTargets.fatG
                )
            }
        }
    }
}
@Composable
fun CombinedNutrientCard(
    items: List<Triple<String, Pair<Double, String>, Any>>,
    maxValues: List<Int>
) {
    val colors = listOf(MainTechColors.NeonP, MainTechColors.NeonC, MainTechColors.NeonF)

    // 1. 單一 GlassCard 容器
    Surface(
        color = MainTechColors.GlassCard,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .techGlassBorder()
    ) {
        // 2. 內部使用 SpaceEvenly 均分三個環
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, triple ->
                val (label, valueUnit, _) = triple
                val (value, unit) = valueUnit
                val max = maxValues.getOrElse(index) { 100 }

                // 動畫進度
                val animatedProgress by animateFloatAsState(
                    targetValue = (value / max.toFloat()).coerceIn(0.0, 1.0).toFloat(),
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "progress_$index"
                )

                val baseColor = colors[index]

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 3. 數值置中於圓環內
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 軌道底色
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = baseColor.copy(alpha = 0.15f),
                            strokeWidth = 6.dp,
                            trackColor = Color.Transparent
                        )
                        // 進度條
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = baseColor,
                            strokeWidth = 6.dp,
                            strokeCap = StrokeCap.Round
                        )

                        // 內部數值與單位
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.0f".format(value),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 標籤放到底部
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun CombinedDashboardCard(
    stats: IntakeStats,
    userGoalKcal: Double = 2000.0,
    userGoalWater: Double = 2500.0,
    onAddDrinkClick: () -> Unit
) {
    // 在 Canvas 外獲取顏色，避免 Scope 錯誤
    val waterColor = MealooTheme.colors.water
    // 假設 MainTechColors.AccentCyan 或自定義 Cyan 已定義
    val accentCyan = Color(0xFF00E5FF)

    val kcalProgress by animateFloatAsState(
        targetValue = (stats.totalKcal / userGoalKcal).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "KcalProgress"
    )

    val waterProgress by animateFloatAsState(
        targetValue = (stats.totalWater / userGoalWater).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "WaterProgress"
    )

    Surface(
        color = MainTechColors.GlassCard,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .techGlassBorder()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Dashboard Canvas ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // 矩形容器
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthOuter = 16.dp.toPx()
                    val strokeWidthInner = 12.dp.toPx()
                    val gap = 12.dp.toPx()
                    val startAngle = 140f
                    val sweepAngle = 260f

                    val trackColor = Color.White.copy(alpha = 0.05f)

                    // === 核心修正：強制圓形計算 ===

                    // 1. 計算可用高度作為基準 (200.dp in px)
                    val baseDimension = size.height

                    // 2. 定義繪圖正方形邊長 (扣除線寬，防止切邊)
                    val arcSideLength = baseDimension - strokeWidthOuter

                    // 3. 計算 topLeft 位移，使正方形在寬度上置中
                    val topLeftOffset = Offset(
                        x = (size.width - arcSideLength) / 2, // 水平置中
                        y = strokeWidthOuter / 2 // 垂直微調線寬
                    )

                    // 4. 定義正方形邊界 Size
                    val circularBoundsSize = Size(arcSideLength, arcSideLength)

                    // ==============================

                    // 1. 繪製外圈：熱量 (Energy) - 使用 circularBoundsSize 確保圓形
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeftOffset, // 強制位置
                        size = circularBoundsSize, // 強制正方形尺寸
                        style = Stroke(width = strokeWidthOuter, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentCyan,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * kcalProgress,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = circularBoundsSize,
                        style = Stroke(width = strokeWidthOuter, cap = StrokeCap.Round)
                    )

                    // 2. 繪製內圈：水份 (Hydration) - 同心縮放
                    val concentricInset = strokeWidthOuter / 2 + gap + strokeWidthInner / 2

                    // 計算內圈的 TopLeft 與 Size
                    val innerTopLeft = Offset(
                        x = topLeftOffset.x + concentricInset,
                        y = topLeftOffset.y + concentricInset
                    )
                    val innerSizeLength = arcSideLength - concentricInset * 2
                    val innerBoundsSize = Size(innerSizeLength, innerSizeLength)

                    // 繪製水份軌道
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerBoundsSize,
                        style = Stroke(width = strokeWidthInner, cap = StrokeCap.Round)
                    )
                    // 繪製水份進度
                    drawArc(
                        color = waterColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * waterProgress,
                        useCenter = false,
                        topLeft = innerTopLeft,
                        size = innerBoundsSize,
                        style = Stroke(width = strokeWidthInner, cap = StrokeCap.Round)
                    )
                }

                // 中心數值 (保持不變)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.energy),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "%.0f".format(stats.totalKcal),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ %.0f kcal".format(userGoalKcal),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            // --- 下半部：水份次要資訊與按鈕 (保持不變) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.water),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f / %.0f ml".format(stats.totalWater, userGoalWater),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = waterColor // 使用外面的變數
                    )
                }

                MealooButton(
                    text = stringResource(R.string.add_drink),
                    onClick = onAddDrinkClick,
                    style = MealooButtonStyle.Primary,
                    modifier = Modifier.height(40.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}
@Composable
fun UnclassifiedMealRow(
    entries: List<EatEntry>,
    mealIds: List<String>,
    mealDefsById: Map<String, MealDef>,
    onRelocateEntry: (EatEntry, String) -> Unit
) {
    if (entries.isEmpty()) return

    val unclassifiedLabel = stringResource(R.string.unclassified)
    val totalKcal = entries.sumOf { it.kcal }
    val themeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- 1. 标题行 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标背景 (仿 MealRow，但使用灰色)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = unclassifiedLabel,
                    tint = themeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))

            // 标题
            Text(
                text = unclassifiedLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(themeColor.copy(alpha = 0.10f))
        )

        Spacer(Modifier.height(8.dp))

        // --- 3. 数值行 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 当前热量 (使用灰色加粗)
            Text(
                text = String.format("%.0f kcal", totalKcal),
                style = MaterialTheme.typography.bodyMedium,
                color = themeColor,
                fontWeight = FontWeight.Bold
            )
            // 右侧留空，因为没有目标值
        }

        Spacer(Modifier.height(8.dp))

        // --- 4. 食物列表 ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { entry ->
                var menuExpanded by remember(entry.id) { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小圆点 (仿 MealRow)
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.width(10.dp))

                    // 食物名称与热量
                    val sub = String.format("%.0f kcal", entry.kcal)
                    Text(
                        text = "${stripBrackets(entry.name)} - $sub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // 移动图标 (只有当有其他餐次可选时显示)
                    if (mealIds.isNotEmpty()) {
                        Box {
                            // 使用 IconButton 增加点击区域
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(24.dp) // 按钮整体大小
                            ) {
                                Icon(
                                    // 使用“移动文件”图标，如果没有该图标可换回 Icons.Default.Edit 或 Icons.Default.ArrowForward
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Relocate",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp) // 图标本身大小
                                )
                            }

                            // 移动菜单
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                mealIds.forEach { mealId ->
                                    val label = mealDefsById[mealId]?.name ?: stringResource(R.string.meal)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            menuExpanded = false
                                            onRelocateEntry(entry, mealId)
                                        }
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
@Composable
fun EnergyDashboardContent(stats: IntakeStats, goalKcal: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // 使用新組件
            OptimizedNutritionPieChart(
                stats = stats,
                thickness = 16.dp, // 稍微調細一點更精緻
                modifier = Modifier.fillMaxSize()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "%.0f".format(stats.totalKcal),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/ %.0f".format(goalKcal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MacroItem(stringResource(R.string.protein), stats.totalProtein, MealooTheme.colors.protein)
            MacroItem(stringResource(R.string.carbs), stats.totalCarbs, MealooTheme.colors.carbs)
            MacroItem(stringResource(R.string.fats), stats.totalFats, MealooTheme.colors.fats)
        }
    }
}

// --- 组件：自定义切换 Tab ---
@Composable
fun DashboardTabRow(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(4.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {
        TabPill(
            text = stringResource(R.string.energy), // "Energy" or "Total Intake"
            index = 0,
            selectedIndex = selectedIndex,
            onSelect = onTabSelected
        )
        TabPill(
            text = stringResource(R.string.water), // "Water" or "Hydration"
            index = 1,
            selectedIndex = selectedIndex,
            onSelect = onTabSelected
        )
    }
}

@Composable
fun RowScope.TabPill(text: String, index: Int, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val isSelected = index == selectedIndex
    val bgColor = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isSelected) MainTechColors.AccentCyan else Color.Gray
    val glowColor = if (isSelected) MainTechColors.AccentCyan.copy(alpha = 0.1f) else Color.Transparent


    // 使用 Surface 而不是 Button 以获得更好的自定义控制
    Surface(
        onClick = { onSelect(index) },
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.then(if (isSelected) Modifier.techGlassBorder(RoundedCornerShape(8.dp), MainTechColors.AccentCyan.copy(alpha=0.3f)) else Modifier)
        ) {

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
@Composable
fun OptimizedNutritionPieChart(
    stats: IntakeStats,
    modifier: Modifier = Modifier,
    thickness: Dp = 16.dp,
    gapAngle: Float = 3f // 區塊間的縫隙角度
) {
    // 1. 計算目標角度 (將數據轉為 0~360 度的佔比)
    val total = (stats.totalProtein * 4 + stats.totalCarbs * 4 + stats.totalFats * 9).coerceAtLeast(1.0)
    val pRatio = (stats.totalProtein * 4 / total).toFloat()
    val cRatio = (stats.totalCarbs * 4 / total).toFloat()
    val fRatio = (stats.totalFats * 9 / total).toFloat()

    // 2. 動畫狀態管理
    // 使用 visible 狀態來觸發「從 0 到 目標值」的進場動畫
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // 使用單一總進度動畫 (0f -> 1f)，這比分別對三個數值做動畫更輕量且同步
    val animationProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "PieChartEnter"
    )

    // 3. 顏色定義 (從您原本的 DashColors 獲取，或直接定義)
    val colorProtein = MainTechColors.NeonP
    val colorCarbs = MainTechColors.NeonC
    val colorFats = MainTechColors.NeonF
    val trackColor = Color.White.copy(alpha = 0.05f)


    Canvas(modifier = modifier) {
        val stroke = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
        val diameter = size.minDimension
        val radius = diameter / 2

        // 畫底色軌道 (完整的圓環)
        drawCircle(
            color = trackColor,
            style = stroke,
            radius = radius - thickness.toPx() / 2 // 內縮一半寬度確保不被切裁
        )

        // 根據動畫進度計算當前的掃過角度
        val currentTotalSweep = 360f * animationProgress

        // 起始角度 (通常從 -90度 也就是 12點鐘方向開始)
        var startAngle = -90f

        // 輔助函式：畫一段弧
        fun drawSegment(ratio: Float, color: Color) {
            val sweep = ratio * 360f * animationProgress // 隨進度縮放
            // 只有當角度大於縫隙時才畫，避免視覺雜亂
            if (sweep > gapAngle) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep - gapAngle, // 扣除縫隙
                    useCenter = false,
                    style = stroke
                )
            }
            startAngle += sweep
        }

        // 依序繪製 P, C, F
        drawSegment(pRatio, colorProtein)
        drawSegment(cRatio, colorCarbs)
        drawSegment(fRatio, colorFats)
    }
}
// --- 内容：热量 (Energy) ---
@Composable
fun MacroItem(label: String, value: Double, color: Color, unit: String = "g") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 彩色小條
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "%.0f $unit".format(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HydrationDashboardContent(
    stats: IntakeStats,
    goalWater: Double,
    onAddClick: () -> Unit
) {
    var showChart by remember { mutableIntStateOf(0) }
    val netWater = stats.totalWater
    val totalProgress by animateFloatAsState(
        targetValue = (netWater / goalWater).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "WaterProgress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeWidth = 16.dp,
                )
                CircularProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MealooTheme.colors.water,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.drink_water_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(
                        text = "%.0f".format(netWater),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(stringResource(R.string.water_goal_ml_fmt).format(goalWater), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.width(16.dp))

            // 右側：三大飲水數據
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val sugarValue = runCatching { stats.totalSugar }.getOrNull() ?: 0.0

                MacroItem(stringResource(R.string.drink_sugar_label), sugarValue, Color(0xFFE91E63), "g")
                MacroItem(stringResource(R.string.drink_caffeine_label), stats.totalCaffeine, Color(0xFF795548), "mg")
                MacroItem(stringResource(R.string.alcohol), stats.totalAlcoholGram, Color(0xFFF44336), "g")
            }
        }

        Spacer(Modifier.height(20.dp))

        MealooButton(
            text = stringResource(R.string.add_drink),
            onClick = onAddClick,
            style = MealooButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )


        Spacer(Modifier.height(16.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(
                selected = showChart == 1,
                label = stringResource(R.string.caffeine_curve),
                icon = Icons.Default.Coffee,
                onClick = { showChart = if (showChart == 1) 0 else 1 }
            )
            ModeChip(
                selected = showChart == 2,
                label = stringResource(R.string.alcohol_curve),
                icon = Icons.Default.LocalBar,
                onClick = { showChart = if (showChart == 2) 0 else 2 }
            )
        }

        AnimatedVisibility(visible = showChart != 0) {
            Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                if (showChart == 1) {
                    Text(stringResource(R.string.metabolism_estimate_halflife), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    CaffeineMetabolismChart(
                        totalCaffeineMg = stats.totalCaffeine,
                        halfLifeHours = 5.0,
                        horizonHours = 16
                    )
                } else if (showChart == 2) {
                    Text(stringResource(R.string.metabolism_estimate_linear), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    AlcoholMetabolismChart(
                        totalAlcoholGram = stats.totalAlcoholGram,
                        horizonHours = 12
                    )
                }
            }
        }
    }
}
@Composable
fun ModeChip(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrinkAddBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (
        waterMl: Double?,
        caffeineMg: Double?,
        sugarGrams: Double?,
        abvPercent: Double?,
        alcoholVolMl: Double?,
        name: String?
    ) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 輸入狀態
    var name by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("250") }
    var caffeine by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var abv by remember { mutableStateOf("") }
    var vol by remember { mutableStateOf("") }

    // 輔助組件：統一的柔和 Chip 列表
    @Composable
    fun QuickChipsRow(items: List<Pair<String, () -> Unit>>) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (label, onClick) ->
                SuggestionChip(
                    onClick = onClick,
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    border = null,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(28.dp) // 降低 chip 高度，使其更精緻
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MainTechColors.DeepBackground,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) },
        modifier = Modifier.techGlassBorder(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {

        val maxH = (LocalConfiguration.current.screenHeightDp * 0.85f).dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxH)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.add),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // --- 1. 基本資訊 (Name & Water) ---
            CompactTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.drink_name_optional_label),
                placeholder = stringResource(R.string.drink_name_placeholder),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                CompactTextField(
                    value = water,
                    onValueChange = { t -> water = t.filter { it.isDigit() } },
                    label = stringResource(R.string.drink_water_label),
                    trailingIcon = { Text("ml", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                QuickChipsRow(listOf(
                    "150" to { water = "150" },
                    "250" to { water = "250" },
                    "330" to { water = "330" },
                    "500" to { water = "500" }
                ))
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))


            // --- 2. 營養素 (Sugar & Caffeine 並排，大幅節省空間) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sugar
                Column(modifier = Modifier.weight(1f)) {
                    CompactTextField(
                        value = sugar,
                        onValueChange = { t -> sugar = t.filter { it.isDigit() || it == '.' } },
                        label = stringResource(R.string.drink_sugar_label),
                        trailingIcon = { Text("g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    val sugarNone = stringResource(R.string.sugar_level_none)
                    val sugarLight = stringResource(R.string.sugar_level_light)
                    val sugarHalf = stringResource(R.string.sugar_level_half)
                    QuickChipsRow(listOf(
                        sugarNone to { sugar = "0" },
                        sugarLight to { sugar = "5" },
                        sugarHalf to { sugar = "15" }
                    ))
                }

                // Caffeine
                Column(modifier = Modifier.weight(1f)) {
                    CompactTextField(
                        value = caffeine,
                        onValueChange = { t -> caffeine = t.filter { it.isDigit() } },
                        label = stringResource(R.string.drink_caffeine_label),
                        trailingIcon = { Text("mg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    QuickChipsRow(listOf(
                        "50" to { caffeine = "50" },
                        "100" to { caffeine = "100" },
                        "150" to { caffeine = "150" }
                    ))
                }
            }

            // --- 3. 酒精 ---
            Column {
                Text(stringResource(R.string.drink_alcohol_optional_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactTextField(
                        value = abv,
                        onValueChange = { t -> abv = t.filter { it.isDigit() || it == '.' } },
                        label = null,
                        placeholder = stringResource(R.string.drink_abv_placeholder),
                        trailingIcon = { Text("%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    CompactTextField(
                        value = vol,
                        onValueChange = { t -> vol = t.filter { it.isDigit() } },
                        label = null,
                        placeholder = stringResource(R.string.drink_volume_placeholder),
                        trailingIcon = { Text("ml", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                val beer = stringResource(R.string.drink_quick_beer)
                val wine = stringResource(R.string.drink_quick_wine)
                val shot = stringResource(R.string.drink_quick_shot)
                QuickChipsRow(listOf(
                    beer to { abv = "5"; vol = "330" },
                    wine to { abv = "12"; vol = "150" },
                    shot to { abv = "40"; vol = "30" }
                ))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))

                val canSubmit = (water.toDoubleOrNull() ?: 0.0) > 0.0 ||
                        (caffeine.toDoubleOrNull() ?: 0.0) > 0.0 ||
                        (sugar.toDoubleOrNull() ?: 0.0) > 0.0 ||
                        (abv.toDoubleOrNull() != null && vol.toDoubleOrNull() != null) ||
                        name.isNotBlank()

                MealooButton(
                    text = stringResource(R.string.add),
                    onClick = {
                        onAdd(
                            water.toDoubleOrNull(),
                            caffeine.toDoubleOrNull(),
                            sugar.toDoubleOrNull(),
                            abv.toDoubleOrNull(),
                            vol.toDoubleOrNull(),
                            name.ifBlank { null }
                        )
                    },
                    enabled = canSubmit,
                    style = MealooButtonStyle.Primary
                )
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
// —— 简易折线图：咖啡因（指数衰减）——
@Composable
private fun CaffeineMetabolismChart(
    totalCaffeineMg: Double,
    halfLifeHours: Double = 5.0,
    horizonHours: Int = 24,
    absorptionMinutes: Double = 45.0
) {
    val points = remember(totalCaffeineMg, halfLifeHours, horizonHours, absorptionMinutes) {
        val n0 = totalCaffeineMg.toFloat()
        (0..horizonHours).map { h ->
            val tMin = h * 60.0
            val remain = when {
                n0 <= 0f -> 0f
                tMin < absorptionMinutes -> (n0 * (tMin / absorptionMinutes)).toFloat() // 线性吸收
                else -> {
                    val tHr = (tMin - absorptionMinutes) / 60.0
                    (n0 * 0.5f.pow((tHr / halfLifeHours).toFloat()))
                }
            }
            remain
        }
    }
    SmallLineChart(
        points = points,
        maxY = (points.maxOrNull() ?: 0f).coerceAtLeast(1f),
        xTick = 4,
        yLabel = "mg",
        xLabelBuilder = { h -> "${h}h" }
    )
}
@Composable
private fun CaffeineEffectChart(
    totalCaffeineMg: Double,
    halfLifeHours: Double = 5.0,
    absorptionMinutes: Double = 45.0,
    tauEffectHours: Double = 3.5,
    horizonHours: Int = 24
) {
    val points = remember(totalCaffeineMg, halfLifeHours, absorptionMinutes, tauEffectHours, horizonHours) {
        val n0 = totalCaffeineMg.toFloat()
        (0..horizonHours).map { h ->
            val tMin = h * 60.0
            val conc = when {
                n0 <= 0f -> 0f
                tMin < absorptionMinutes -> (n0 * (tMin / absorptionMinutes)).toFloat() // 吸收
                else -> {
                    val tHr = (tMin - absorptionMinutes) / 60.0
                    (n0 * 0.5f.pow((tHr / halfLifeHours).toFloat()))
                }
            }
            // 体感 = 归一化浓度 × 额外衰减（表现“清醒感”先强后弱）
            val tHrAll = tMin / 60.0
            val effect = if (n0 <= 0f) 0f
            else (conc / n0) * exp(-tHrAll / tauEffectHours).toFloat()

            effect.coerceIn(0f, 1f)
        }
    }
    SmallLineChart(
        points = points,
        maxY = 1f,
        xTick = 4,
        yLabel = "强度",
        xLabelBuilder = { h -> "${h}h" }
    )
}
// —— 简易折线图：酒精（线性消除）——
@Composable
private fun AlcoholMetabolismChart(
    totalAlcoholGram: Double,
    eliminationGramPerHour: Double = 7.0,
    horizonHours: Int = 12
) {
    val points = remember(totalAlcoholGram, eliminationGramPerHour, horizonHours) {
        val n0 = totalAlcoholGram.toFloat()
        (0..horizonHours).map { h ->
            val remain = (n0 - eliminationGramPerHour.toFloat() * h).coerceAtLeast(0f)
            remain
        }
    }
    SmallLineChart(
        points = points,
        maxY = (points.maxOrNull() ?: 0f).coerceAtLeast(1f),
        xTick = 2,
        yLabel = "g",
        xLabelBuilder = { h -> "${h}h" }
    )
}

// —— 通用小折线图（简洁、无外部库）——
@Composable
private fun SmallLineChart(
    points: List<Float>,
    maxY: Float,
    xTick: Int,
    yLabel: String,
    xLabelBuilder: (Int) -> String
) {
    val height = 120.dp
    val pad = 8.dp
    val stroke = 2.dp
    val colorSV = MaterialTheme.colorScheme.surfaceVariant
    val colorPRIM = MaterialTheme.colorScheme.primary

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(pad)
        ) {

            if (points.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val n = points.size - 1
            val maxX = n.toFloat()
            val maxYv = maxY

            // 网格（x 方向）
            val xStep = w / maxX
            for (i in 0..n step xTick) {
                val x = i * xStep
                drawLine(
                    color = colorSV,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }
            // 曲线
            for (i in 0 until n) {
                val x1 = (i / maxX) * w
                val y1 = h - (points[i] / maxYv) * h
                val x2 = ((i + 1) / maxX) * w
                val y2 = h - (points[i + 1] / maxYv) * h
                drawLine(
                    color = colorPRIM,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = stroke.toPx()
                )
            }
        }
        // 简易坐标说明
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(xLabelBuilder(points.lastIndex), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(yLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MealCategoryCard(
    mealTargets: List<Pair<String, Double>>,
    mealCurrentKcal: Map<String, Double>,
    mealDefsById: Map<String, MealDef>,
    intakeEntries: List<EatEntry>,
    unclassifiedEntries: List<EatEntry>,
    selectedDate: String,
    dataStore: IntakeDataStore,
    onMealAddClick: (String) -> Unit,
    userPref: UserPreference?,
    prefStore: UserPreferenceDataStore,
    onMealDetailClick: (String) -> Unit,
    dailyStore: DailyMealConfigStore,
    onAddNewMealCategoryClick: () -> Unit = {}
) {
    // 1. 改用 Column 作為外部容器，取代原本的單一 Card
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp) // 卡片之間的間距
    ) {
        // 2. 遍歷每一個餐次，渲染獨立卡片
        mealTargets.forEach { (mealId, targetKcal) ->
            val currentKcal = mealCurrentKcal[mealId] ?: 0.0
            val mealDef = mealDefsById[mealId]
            val mealName = mealDef?.name ?: mealId
            val customColor = mealDef?.trackColorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
            val customIcon = mealIconByName(mealDef?.iconName)

            IndividualMealCard(
                mealId = mealId,
                mealName = mealName,
                currentKcal = currentKcal,
                targetKcal = targetKcal,
                trackColor = customColor,
                icon = customIcon,
                onCardClick = { onMealDetailClick(mealId) },
                onAddClick = { onMealAddClick(mealId) }
            )
        }
    }
}

@Composable
private fun IndividualMealCard(
    mealId: String,
    mealName: String,
    currentKcal: Double,
    targetKcal: Double,
    trackColor: Color? = null,
    icon: ImageVector? = null,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    // 進度條動畫
    val progress by animateFloatAsState(
        targetValue = if (targetKcal > 0) (currentKcal / targetKcal).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "meal_progress_$mealId"
    )

    Surface(
        color = MainTechColors.GlassCard,
        modifier = Modifier
            .fillMaxWidth()
            .techGlassBorder()
            .clickable(onClick = onCardClick), // 整張卡片可點擊進入詳細頁
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：小型環狀進度條 + 名稱首字
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.05f),
                    strokeWidth = 4.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = trackColor ?: MainTechColors.AccentCyan,
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = mealName.take(1),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中間：名稱與數值
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${currentKcal.toInt()} / ${targetKcal.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右側：記帳/新增食物按鈕 (加強點擊區與視覺回饋)
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .background(MainTechColors.AccentCyan.copy(alpha = 0.15f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_record), // 請替換為適當的字串資源
                    tint = MainTechColors.AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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

private fun mealIconForKind(kind: MealKind?): ImageVector = when (kind) {
    MealKind.BREAKFAST -> Icons.Default.BreakfastDining
    MealKind.LUNCH -> Icons.Default.LunchDining
    MealKind.DINNER -> Icons.Default.DinnerDining
    MealKind.SNACK -> Icons.Default.Fastfood
    MealKind.CUSTOM, null -> Icons.Default.Fastfood
}

private fun mealIconByName(name: String?): ImageVector? = when (name) {
    "Breakfast" -> Icons.Default.BreakfastDining
    "Lunch" -> Icons.Default.LunchDining
    "Dinner" -> Icons.Default.DinnerDining
    "Snack" -> Icons.Default.Fastfood
    "Restaurant" -> Icons.Default.Restaurant
    "Coffee" -> Icons.Default.Coffee
    "LocalBar" -> Icons.Default.LocalBar
    "Icecream" -> Icons.Default.Icecream
    "Egg" -> Icons.Default.Egg
    "BakeryDining" -> Icons.Default.BakeryDining
    "Cake" -> Icons.Default.Cake
    "LocalPizza" -> Icons.Default.LocalPizza
    "LocalDrink" -> Icons.Default.LocalDrink
    "RamenDining" -> Icons.Default.RamenDining
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun MealRow(
    mealKind: MealKind?,
    label: String,
    currentKcal: Double,
    targetKcal: Double,
    mealItems: List<String> = emptyList(),
    editMode: Boolean = false,
    onAddClick: () -> Unit = {},
    onDeleteClick: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val mealColor = mealColorForKind(mealKind)
    val mealIcon = mealIconForKind(mealKind)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!editMode) onClick() }
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = mealColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mealIcon,
                    contentDescription = label,
                    tint = mealColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.0f".format(currentKcal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = mealColor
                    )
                    Text(
                        text = " / %.0f kcal".format(targetKcal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            if (editMode) {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentEnforcement provides false
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-8).dp)
                    ) {
                        IconButton(onClick = { onMoveUp?.invoke() }) {
                            Icon(Icons.Default.ArrowUpward, null)
                        }
                        IconButton(onClick = { onMoveDown?.invoke() }) {
                            Icon(Icons.Default.ArrowDownward, null)
                        }
                        IconButton(onClick = { onDeleteClick?.invoke() }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                // 加號按鈕：改用 FilledTonal 風格，更現代
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MainTechColors.AccentCyan.copy(alpha = 0.15f),
                        contentColor = MainTechColors.AccentCyan
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }

            }
        }

        Spacer(Modifier.height(12.dp))

        ModernMealProgressBar(
            currentKcal = currentKcal,
            targetKcal = targetKcal,
            color = mealColor,
            modifier = Modifier.fillMaxWidth(),
            height = 10.dp
        )

        if (mealItems.isNotEmpty() && !editMode) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = mealItems.joinToString(" · ") {
                    stripBrackets(it)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
@Composable
fun TopBar(
    navController: NavHostController,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appCtx = context.applicationContext
    val intakeStore = remember(appCtx) { IntakeDataStore(appCtx) }
    val drive = LocalDrive.current

    // ✅ 用 Google 帐号 + drive 判断登入状态
    val account = GoogleSignIn.getLastSignedInAccount(context)
    val photoUrl = account?.photoUrl
    val email = account?.email
    val isLoggedIn = drive != null && account != null

    val repository = remember(drive) { drive?.let { IntakeRepository(it) } }

    // ✅ 雲端同步：完全限縮在 TopBar，且 TopBar 自己從 savedStateHandle 讀 selectedDate
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { dateFormatter.format(Date()) }
    val backStackEntry = navController.currentBackStackEntry
    val selectedDateFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("selectedDate", today)
    }
    val selectedDate by selectedDateFlow?.collectAsState(initial = today)
        ?: remember { mutableStateOf(today) }

    LaunchedEffect(selectedDate, repository) {
        if (repository == null) return@LaunchedEffect
        val cloudEntries = repository.loadEntriesForDate(selectedDate)
        if (cloudEntries.isNotEmpty()) {
            intakeStore.updateEntriesForDate(selectedDate, cloudEntries)
        }
    }

    // 供登出用的 client（配置和 LoginScreen 一致）
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：头像 + 下拉菜单
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { accountMenuExpanded = true }
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(R.string.avatar),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.avatar),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.more_accounts),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = accountMenuExpanded,
                onDismissRequest = { accountMenuExpanded = false }
            ) {
                if (!isLoggedIn) {
                    // ───── 未登入：只提供登入 ─────
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.login_with_google)) },
                        onClick = {
                            accountMenuExpanded = false
                            navController.navigate("login")
                        }
                    )
                } else {
                    // 顶部显示邮箱
                    DropdownMenuItem(
                        text = { Text(email ?: stringResource(R.string.no_email)) },
                        onClick = { /* no-op */ }
                    )

                    // ───── 已登入：上传最近 7 天 ─────
                    DropdownMenuItem(
                        text = { Text("上传最近 7 天到云端") },
                        onClick = {
                            accountMenuExpanded = false
                            if (repository == null) {
                                Toast.makeText(
                                    context,
                                    "尚未启用云端备份（Drive 为 null）",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@DropdownMenuItem
                            }

                            scope.launch {
                                uploading = true
                                try {
                                    val ok = repository.uploadRecent7Days { date ->
                                        intakeStore.getEntriesForDate(date)
                                    }
                                    val msg = if (ok) {
                                        "已上传最近 7 天记录"
                                    } else {
                                        "最近 7 天没有可上传的记录"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "上传失败：${e.localizedMessage ?: ""}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    uploading = false
                                }
                            }
                        }
                    )

                    // ───── 登出（Google Sign-Out） ─────
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.logout)) },
                        onClick = {
                            accountMenuExpanded = false
                            // 清掉 Google 帐号（下次启动 AppRoot 那里就拿不到 lastAccount 了）
                            googleSignInClient.signOut()
                            // UI 回到 login
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }
        }
        // 中间：App 名
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(MainTechColors.AccentCyan, MainTechColors.AccentMagenta)
                )
            )
        )

        // 右侧：设定按钮 + 上传中指示
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uploading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp),
                    strokeWidth = 2.dp
                )
            }
            IconButton(onClick = { onSettingsClick() }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.settings),
                    tint = MainTechColors.AccentCyan
                )
            }

        }
    }
}

@Composable
private fun ModernMealProgressBar(
    currentKcal: Double,
    targetKcal: Double,
    color: Color, // 接收餐次的主题色
    modifier: Modifier = Modifier,
    height: Dp = 8.dp // 稍微变细一点，更精致
) {
    if (targetKcal <= 0.0) return
    // 计算进度 (0.0 ~ 1.0+)
    val progress = (currentKcal / targetKcal).toFloat()
    // 是否超出
    val isExceeded = progress > 1.0f
    val exceedRatio = (progress - 1.0f).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50)) // 完全圆角 (胶囊状)
    ) {
        val w = size.width
        val h = size.height
        // 1. 绘制轨道 (Track) - 使用主色的 15% 透明度，保持通透感
        drawRect(
            color = color.copy(alpha = 0.15f),
            size = Size(w, h)
        )
        // 2. 绘制进度 (Progress)
        val progressWidth = w * progress.coerceAtMost(1f)
        drawRect(
            color = color,
            size = Size(progressWidth, h)
        )
        // 3. 绘制超出部分 (Excess) - 如果超出，在末端叠加半透明红色
        if (isExceeded) {
            val excessWidth = w * exceedRatio
            drawRect(
                color = Color(0xFFEF5350).copy(alpha = 0.6f), // 柔和的红
                size = Size(excessWidth, h)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailBottomSheet(
    mealId: String,
    mealLabel: String,
    mealKind: MealKind?,
    entries: List<EatEntry>,
    userPref: UserPreference?, // <--- ✅ ADD THIS parameter
    onDismiss: () -> Unit,
    onDeleteEntry: (entryId: String) -> Unit = {},
    onEditEntry: (entry: EatEntry) -> Unit = {},
    targetProteinG: Double,
    targetCarbG: Double,
    targetFatG: Double
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Semantic configuration
    val mealColor = mealColorForKind(mealKind)

    val mealIcon = mealIconForKind(mealKind)

    // 2. Data Aggregation
    data class AggItem(
        val title: String,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val fats: Double,
        val grams: Double?,
        val items: List<EatEntry>
    )

    val aggregated = remember(entries, mealId) {
        entries.groupBy { it.name }
            .map { (title, list) ->
                AggItem(
                    title = title,
                    kcal = list.sumOf { it.kcal },
                    protein = list.sumOf { it.protein },
                    carbs = list.sumOf { it.carbs },
                    fats = list.sumOf { it.fats },
                    grams = list.mapNotNull { it.grams }.takeIf { it.isNotEmpty() }?.sum(),
                    items = list
                )
            }
            .sortedByDescending { it.kcal }
    }

    // Calculate Totals for Scoring
    val totalKcal = remember(aggregated) { aggregated.sumOf { it.kcal } }
    val totalProtein = remember(aggregated) { aggregated.sumOf { it.protein } }
    val totalCarbs = remember(aggregated) { aggregated.sumOf { it.carbs } }
    val totalFats = remember(aggregated) { aggregated.sumOf { it.fats } }

    // 3. ✅ Calculate Score
    val scoreDetail by produceState<IntakeMacroScoreDetail?>(
        initialValue = null,
          mealId,
        totalKcal,
        totalProtein,
        totalCarbs,
        totalFats,
        userPref
    ) {
        value = null
        val pref = userPref ?: return@produceState
        if (totalKcal <= 0.0) return@produceState

        // 等 Sheet 進入畫面（至少完成首幀），避免打開時因評分計算阻塞動畫/首幀
        withFrameNanos { }

        value = withContext(Dispatchers.Default) {
            NutritionScorer.scoreMealIntake(
                  mealId = mealId,
                  mealLabel = mealLabel,
                mealKcal = totalKcal,
                mealProtein = totalProtein,
                mealCarbs = totalCarbs,
                mealFats = totalFats,
                userPref = pref
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MainTechColors.DeepBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.techGlassBorder(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // --- Header ---
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(mealColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mealIcon,
                        contentDescription = null,
                        tint = mealColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = mealLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.meal_total_kcal, totalKcal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.White.copy(alpha=0.05f), CircleShape)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp), tint = Color.Gray)
                }

            }

            Spacer(Modifier.height(16.dp))

            // --- ✅ NEW: Score Overview Section (Replaces the simple target text) ---
            if (scoreDetail != null) {
                MealScoreOverview(
                    scoreDetail = scoreDetail!!,
                    actualValues = MealMacroSummary(totalKcal, totalProtein, totalCarbs, totalFats),
                    targetValues = Triple(targetProteinG, targetCarbG, targetFatG) // Optional: for display reference
                )
            } else {
                // Fallback if no score available (e.g. 0 kcal or no prefs)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.target),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.meal_target_macro, targetProteinG, targetCarbG, targetFatG),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- List Items ---
            if (aggregated.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.meal_no_items), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(aggregated, key = { it.title }) { agg ->
                        ExpandableAggRowVsTarget(
                            title = agg.title,
                            kcal = agg.kcal,
                            protein = agg.protein,
                            carbs = agg.carbs,
                            fats = agg.fats,
                            grams = agg.grams,
                            targetProteinG = targetProteinG,
                            targetCarbG = targetCarbG,
                            targetFatG = targetFatG,
                            entries = agg.items,
                            onEditEntry = onEditEntry,
                            onDeleteEntry = onDeleteEntry,
                            mealColor = mealColor
                        )
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealScoreOverview(
    scoreDetail: IntakeMacroScoreDetail,
    actualValues: MealMacroSummary,
    targetValues: Triple<Double, Double, Double>
) {
    val grade = scoreDetail.grade
    val gradeColor = getGradeColor(grade)

    Surface(
        color = MainTechColors.GlassCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .techGlassBorder()
    ) {

        Column(Modifier.padding(16.dp)) {
            // Row 1: Grade and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Nutrition Score",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Based on your macro targets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Grade Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(gradeColor, CircleShape)
                ) {
                    Text(
                        text = grade,
                        color = MaterialTheme.colorScheme.surface,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Row 2: Score Bars
            CompactScoreBar("Energy", scoreDetail.kcalScore, actualValues.kcal, "kcal")
            CompactScoreBar("Protein", scoreDetail.proteinScore, actualValues.protein, "g")
            CompactScoreBar("Carbs", scoreDetail.carbScore, actualValues.carbs, "g")
            CompactScoreBar("Fats", scoreDetail.fatScore, actualValues.fats, "g")

            // Row 3: Suggestions (Chips)
            if (scoreDetail.notes.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scoreDetail.notes.take(3).forEach { note -> // Limit to 3 to save space
                        val cleanNote = note.replace(Regex("^(Breakfast|Lunch|Dinner|Snack).*? "), "").trim()
                        SuggestionChip(
                            onClick = {},
                            label = { Text(cleanNote, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactScoreBar(label: String, score: Int, value: Double, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label area
        Row(modifier = Modifier.width(80.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .fillMaxHeight()
                    .background(getScoreColor(score).copy(alpha = 0.8f))

            )
        }

        // Value area
        Row(
            modifier = Modifier.width(70.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${value.toInt()}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score)
            )
        }
    }
}
@Composable
private fun ExpandableAggRowVsTarget(
    title: String,
    kcal: Double,
    protein: Double,
    carbs: Double,
    fats: Double,
    grams: Double?,
    targetProteinG: Double,
    targetCarbG: Double,
    targetFatG: Double,
    entries: List<EatEntry>,
    onEditEntry: (EatEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    mealColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    // 使用 Card 代替 Surface，风格统一
    Surface(
        color = MainTechColors.GlassCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .techGlassBorder()
    ) {

        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 食物名称
                Text(
                    text = stripBrackets(title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, // 加粗
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                // 热量 (使用餐次主题色)
                Text(
                    text = "${"%.0f".format(kcal)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = mealColor // 呼应餐次颜色
                )

                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 6.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                data class MacroPct(val label: String, val gram: Double, val pct: Double, val color: Color)

                val list = buildList {
                    if (targetProteinG > 0.0 && protein > 0.0)
                        add(MacroPct(stringResource(R.string.protein), protein, (protein / targetProteinG).coerceAtLeast(0.0), MealooTheme.colors.protein))
                    if (targetCarbG > 0.0 && carbs > 0.0)
                        add(MacroPct(stringResource(R.string.carbs), carbs, (carbs / targetCarbG).coerceAtLeast(0.0), MealooTheme.colors.carbs))
                    if (targetFatG > 0.0 && fats > 0.0)
                        add(MacroPct(stringResource(R.string.fats), fats, (fats / targetFatG).coerceAtLeast(0.0), MealooTheme.colors.fats))
                }.sortedByDescending { it.pct }

                if (list.isEmpty()) {
                    Text(
                        text = stringResource(R.string.meal_item_ratio_small),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    list.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Label (P/C/F)
                            Text(
                                text = s.label,
                                modifier = Modifier.width(42.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(s.color.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(s.pct.toFloat().coerceAtMost(1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(s.color)
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // Value
                            Text(
                                text = "${"%.0f".format(s.gram)}g · ${"%.0f".format(s.pct * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 原始记录（操作区）
                Spacer(Modifier.height(8.dp))
                entries.forEach { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 小圆点
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = if (e.grams != null && e.grams > 0) "${"%.0f".format(e.grams)}g" else stringResource(R.string.unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.weight(1f))

                        // 操作按钮（更现代的风格）
                        IconButton(
                            onClick = { onEditEntry(e) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onDeleteEntry(e.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun Any.toPainter(): Painter {
    return when (this) {
        is Painter -> this
        is ImageVector -> rememberVectorPainter(this)
        is Int -> painterResource(id = this)
        else -> painterResource(id = R.drawable.ic_unknown) // fallback
    }
}
