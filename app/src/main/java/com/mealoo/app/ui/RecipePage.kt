@file:Suppress("ProduceStateDoesNotAssignValue")

package com.mealoo.app.ui
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import kotlinx.coroutines.flow.map
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mealoo.app.data.RecipeDataStore
import com.mealoo.app.model.RecipeEntry
import com.mealoo.app.db.FoodDbRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource

import java.io.File
import com.mealoo.app.model.*
import com.mealoo.app.R
import java.util.Locale
import java.util.UUID
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mealoo.app.db.MealPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import com.mealoo.app.ui.design.MealooTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import coil.request.ImageRequest
import com.mealoo.app.data.migrateFlatTagsToV2
import com.mealoo.app.util.TagCatalog
import com.mealoo.app.util.localizeTag
import com.mealoo.app.util.localizeTagCategory
import com.mealoo.app.data.RecipeRepository
import com.mealoo.app.data.UserPreferenceDataStore
import com.mealoo.app.db.toEntity
import com.mealoo.app.db.toModel
import com.mealoo.app.db.InventoryRepository
import com.mealoo.app.util.TagMultiSelectDropdown
import com.mealoo.app.util.queryUnifiedFoods
import com.mealoo.app.util.UnifiedFoodSourceScope
import com.mealoo.app.util.stripBrackets
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import com.mealoo.app.model.AppCurrency

// 頁面內部導航狀態
private enum class PlanViewState {
    DASHBOARD, // 卡片列表
    EDITOR     // 編輯特定方案
}
data class TagInfo(
    val id: String,
    val zh: String,
    val ja: String,
    val en: String
)
private val userKeyToStandard = mapOf(
    "enercKcal" to "Energy",
    "prot" to "Protein",
    "fat" to "Total lipid (fat)",
    "chocdf" to "Carbohydrate, by difference"
)
fun localizePredefinedTag(id: String, localeLang: String = Locale.getDefault().language): String {
    val tag = predefinedTags.find { it.id == id } ?: return id
    return when (localeLang) {
        "zh" -> tag.zh
        "ja" -> tag.ja
        else -> tag.en
    }
}
val predefinedTags = listOf(
    TagInfo("breakfast", "早餐", "朝ごはん", "Breakfast"),
    TagInfo("lunch", "午餐", "昼ごはん", "Lunch"),
    TagInfo("dinner", "晚餐", "夕食", "Dinner"),
    TagInfo("high_protein", "高蛋白", "高たんぱく", "High Protein"),
    TagInfo("low_carb", "低碳水", "低炭水化物", "Low Carb"),
    TagInfo("vegan", "纯素", "ヴィーガン", "Vegan"),
    TagInfo("low_fat", "低脂", "低脂肪", "Low Fat"),
    TagInfo("low_kcal", "低卡", "低カロリー", "Low Calorie"),
    TagInfo("chinese_food", "中華料理", "中華料理", "Chinese Food")
)
@Composable
fun RecipeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_cd), tint = Color.Gray)
                }
            }
        } else null,
        shape = RoundedCornerShape(24.dp), // 圓角風格
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent, // 無邊框，靠陰影或背景區隔
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true
    )
}

private enum class RecipePageMode {
    LIBRARY, // 食譜庫
    PLAN     // 週菜單
}
const val UNCLASSIFIED_MEAL_ID = "unclassified"
private fun generateMockWeeklyPlan(context: Context, planCount: Int): MenuPlanSummary {
    val newId = UUID.randomUUID().toString()
    val standardMealIds = listOf("breakfast", "lunch", "dinner")
    val entries = mutableListOf<MealPlanEntry>()

    for (dayIndex in 1..7) {
        for (mealId in standardMealIds) {
            entries.add(
                MealPlanEntry(
                    id = UUID.randomUUID().toString(),
                    date = dayIndex.toString(),
                    mealId = mealId,
                    name = context.getString(R.string.test_meal_fmt, dayIndex.toString(), mealId),
                    source = "MOCK",
                    sourcePlanId = newId,
                    grams = 200.0,
                    servings = 1.0,
                    kcal = (400..600).random().toDouble(),
                    protein = 20.0,
                    carbs = 50.0,
                    fats = 15.0,
                    cholesterol = 0.0,
                    recipeId = null,
                    cost = java.math.BigDecimal((50..150).random()),
                    currency = "TWD",
                    isCompleted = false
                )
            )
        }
    }

    return MenuPlanSummary(
        id = newId,
        name = context.getString(R.string.test_plan_name_fmt, planCount + 1),
        description = context.getString(R.string.test_plan_desc),
        dayCount = 7,
        entries = entries
    )
}
@Composable
fun RecipePage(
    dataStore: RecipeDataStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Data Sources ---
    val recipeRepository = remember { RecipeRepository(context.applicationContext) }
    val inventoryRepo = remember { InventoryRepository.getInstance(context) }
    val mealPlanRepository = remember { MealPlanRepository.getInstance(context) }

    // --- UI State ---
    var currentMode by remember { mutableStateOf(RecipePageMode.LIBRARY) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 載入食譜
    val recipes by remember(recipeRepository) {
        recipeRepository.allRecipes.map { entities -> entities.map { it.toModel() } }
    }.collectAsState(initial = null)

    // 載入庫存 (for AddRecipe)
    val storageItems by inventoryRepo.itemsFlow().collectAsState(initial = emptyList())

    // 初始化預設食譜
    LaunchedEffect(Unit) {
        val currentRecipes = dataStore.getRecipes()
        if (currentRecipes.isEmpty()) {
            val defaultRecipes = dataStore.loadRecipesFromAsset()
            if (defaultRecipes.isNotEmpty()) {
                dataStore.saveRecipes(defaultRecipes)
            }
        }
    }

    Scaffold(
        containerColor = MainTechColors.DeepBackground,
        contentWindowInsets = WindowInsets(0, 90, 0, 0),
        floatingActionButton = {
            if (currentMode == RecipePageMode.LIBRARY) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_recipe_cd))
                }

            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
        ) {
            // --- Top Bar with Segmented Control ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MainTechColors.DeepBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 標題與模式切換
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.recipe),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 簡單的切換器 (Switch/Segmented Button)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            TabButton(
                                text = stringResource(R.string.library_tab), // "食譜庫"
                                isSelected = currentMode == RecipePageMode.LIBRARY,
                                onClick = { currentMode = RecipePageMode.LIBRARY }
                            )
                            TabButton(
                                text = stringResource(R.string.weekly_tab), // "週菜單" (需新增資源)
                                isSelected = currentMode == RecipePageMode.PLAN,
                                onClick = { currentMode = RecipePageMode.PLAN }
                            )
                        }
                    }
                }
            }

            // --- Content Body ---
            Box(modifier = Modifier.weight(1f)) {
                when (currentMode) {
                    RecipePageMode.LIBRARY -> {
                        RecipeLibraryView(
                            recipes = recipes,
                            recipeRepository = recipeRepository,
                            onShowAddDialog = { showAddDialog = true }
                        )
                    }
                    RecipePageMode.PLAN -> {
                        WeeklyPlanView(
                            mealPlanRepository = mealPlanRepository,
                            recipeRepository = recipeRepository
                        )
                    }
                }
            }
        }

        // Add Recipe Sheet
        if (showAddDialog) {
            AddRecipeSheet(
                onDismiss = { showAddDialog = false },
                onSave = { newRecipe ->
                    scope.launch {
                        recipeRepository.addRecipe(newRecipe.toEntity())
                    }
                },
                storageItems = storageItems,
                recipeEntries = recipes ?: emptyList()
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ==========================================
// 1. Recipe Library View (原有的列表邏輯)
// ==========================================
@Composable
fun RecipeLibraryView(
    recipes: List<RecipeEntry>?,
    recipeRepository: RecipeRepository,
    onShowAddDialog: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRecipe by remember { mutableStateOf<RecipeEntry?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredRecipes = remember(recipes, searchQuery, activeFilters) {
        val safeRecipes = recipes ?: emptyList()
        var result = if (searchQuery.isBlank()) safeRecipes else safeRecipes.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
        // 簡單過濾邏輯
        if (activeFilters.isNotEmpty()) {
            result = result.filter { recipe ->
                activeFilters.all { (cat, selectedIds) ->
                    val recipeTags = recipe.tagsV2[cat] ?: emptyList()
                    selectedIds.any { it in recipeTags }
                }
            }
        }
        result
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 搜尋與篩選列
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                RecipeSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            }
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(
                    onClick = { filterMenuExpanded = true },
                    modifier = Modifier.background(Color.White, CircleShape).border(1.dp, Color(0xFFEEEEEE), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilters.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                FilterDropdownMenu(
                    expanded = filterMenuExpanded,
                    active = activeFilters,
                    onToggle = { cat, id, selected ->
                        activeFilters = buildMap {
                            putAll(activeFilters)
                            val cur = get(cat)?.toMutableSet() ?: mutableSetOf()
                            if (selected) cur.add(id) else cur.remove(id)
                            if (cur.isEmpty()) remove(cat) else put(cat, cur)
                        }
                    },
                    onClear = { activeFilters = emptyMap() },
                    onDismiss = { filterMenuExpanded = false }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (recipes == null) {
            // 資料載入中，什麼都不畫，或是放個 Box 佔位
            Box(Modifier.weight(1f))
        } else if (filteredRecipes.isEmpty()) {
            EmptyStateView(searchQuery)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { selectedRecipe = recipe },
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }

    if (selectedRecipe != null) {
        RecipeDetailDialog(
            recipe = selectedRecipe!!,
            onDismiss = { selectedRecipe = null },
            onDelete = { toDelete ->
                scope.launch {
                    recipeRepository.removeRecipe(toDelete.toEntity())
                    selectedRecipe = null
                }
            }
        )
    }
}

@Composable
fun EmptyStateView(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val emptyIcon = if (query.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.MenuBook
        Icon(
            imageVector = emptyIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (query.isNotEmpty()) {
            Text(
                text = stringResource(R.string.no_search_result_title, query),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.no_recipe),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.click_add_to_create),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WeeklyPlanView(
    mealPlanRepository: MealPlanRepository,
    recipeRepository: RecipeRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State ---
    var viewState by remember { mutableStateOf(PlanViewState.DASHBOARD) }
    var activePlan by remember { mutableStateOf<MenuPlanSummary?>(null) }
    val allRecipes by remember(recipeRepository) {
        recipeRepository.allRecipes.map { list -> list.map { it.toModel() } }
    }.collectAsState(initial = emptyList())

    val savedPlans by mealPlanRepository.getAllPlansFlow().collectAsState(initial = null)

    fun createNewPlan(isTest: Boolean = false) {
        val currentPlanCount = savedPlans?.size ?: 0

        val newPlan = if (isTest) {
            generateMockWeeklyPlan(context, currentPlanCount)
        } else {
            MenuPlanSummary(
                id = UUID.randomUUID().toString(),
                name = context.getString(R.string.test_plan_name_fmt, currentPlanCount + 1),
                description = "",
                dayCount = 7,
                entries = emptyList()
            )
        }

        activePlan = newPlan
        viewState = PlanViewState.EDITOR

        scope.launch(Dispatchers.IO) {
            mealPlanRepository.savePlan(newPlan)
        }
    }
    fun updateActivePlan(updatedPlan: MenuPlanSummary) {
        activePlan = updatedPlan // 樂觀更新 UI，保證編輯器畫面不卡頓
        scope.launch(Dispatchers.IO) {
            mealPlanRepository.savePlan(updatedPlan)
        }
    }
    AnimatedContent(targetState = viewState, modifier = Modifier.fillMaxSize(), label = "PlanViewSwitch") { state ->
        when (state) {
            PlanViewState.DASHBOARD -> {
                PlanDashboard(
                    plans = savedPlans,
                    onTestClick = { createNewPlan(isTest = true) },
                    onCreateClick = { createNewPlan() },
                    onPlanClick = { plan ->
                        activePlan = plan
                        viewState = PlanViewState.EDITOR
                    },
                    onDeleteClick = { planId ->
                        scope.launch(Dispatchers.IO) {
                            mealPlanRepository.deletePlan(planId)
                        }
                    }
                )
            }
            PlanViewState.EDITOR -> {
                activePlan?.let { plan ->
                    PlanEditor(
                        plan = plan,
                        allRecipes = allRecipes,
                        onBack = { viewState = PlanViewState.DASHBOARD },
                        onUpdate = { updated -> updateActivePlan(updated) },
                        onDeletePlan = {
                            scope.launch(Dispatchers.IO) {
                                mealPlanRepository.deletePlan(plan.id)
                            }
                            viewState = PlanViewState.DASHBOARD
                        }
                    )
                }
            }
        }
    }
}

// --- Sub-Component: Dashboard (卡片列表) ---
@Composable
fun PlanDashboard(
    plans: List<MenuPlanSummary>?,
    onCreateClick: () -> Unit,
    onTestClick: () -> Unit,
    onPlanClick: (MenuPlanSummary) -> Unit,
    onDeleteClick: (String) -> Unit
){
    Box(modifier = Modifier.fillMaxSize()) {
        if (plans == null) {
        } else if (plans.isEmpty()) {
            EmptyStateView(stringResource(R.string.no_menu_plans))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans) { plan ->
                    PlanSummaryCard(
                        plan = plan,
                        onClick = { onPlanClick(plan) },
                        onDelete = { onDeleteClick(plan.id) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            SmallFloatingActionButton(
//                onClick = onTestClick,
//                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
//                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
//            ) {
//                Icon(Icons.Default.BugReport, contentDescription = stringResource(R.string.generate_test_data_cd))
//            }
            // 主要按鈕：空白新增
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_plan_cd))
            }
        }
    }
}

// --- Sub-Component: Plan Editor (編輯器) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditor(
    plan: MenuPlanSummary,
    allRecipes: List<RecipeEntry>,
    onBack: () -> Unit,
    onUpdate: (MenuPlanSummary) -> Unit,
    onDeletePlan: () -> Unit
) {
    var showRecipePicker by remember { mutableStateOf(false) }
    var activeDayOffset by remember { mutableStateOf(0) }
    var activeMealId by remember { mutableStateOf(UNCLASSIFIED_MEAL_ID) } // 🔥 1. 新增：紀錄目前要加到哪一餐
    var planName by remember { mutableStateOf(plan.name) }
    var dayCount by remember(plan.id) {
        mutableIntStateOf(
            maxOf(plan.dayCount, plan.entries.maxOfOrNull { it.date.toIntOrNull() ?: 1 } ?: 1)
        )
    }
    val context = LocalContext.current
    val mealDefsStore = remember { com.mealoo.app.data.MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) { mealDefsStore.getAll() }

    Column(modifier = Modifier.fillMaxSize()) {

        // 1. 原本的 TopBar 直接作為 Column 的第一個元件
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBackIos, contentDescription = stringResource(R.string.back_cd), modifier = Modifier.size(20.dp))
            }

            BasicTextField(
                value = planName,
                onValueChange = {
                    planName = it
                    onUpdate(plan.copy(name = it))
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.avg_kcal_per_day_fmt, plan.avgKcal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dayCount) { dayIndex ->
                val dayStr = (dayIndex + 1).toString()
                val dailyEntries = plan.entries.filter { it.date == dayStr }

                DayEditorCard(
                    dayIndex = dayIndex,
                    entries = dailyEntries,
                    mealDefs = mealDefs,
                    onAddClick = { mealId ->
                        activeDayOffset = dayIndex
                        activeMealId = mealId
                        showRecipePicker = true
                    },
                    onRemoveEntry = { entryId ->
                        val newEntries = plan.entries.filterNot { it.id == entryId }
                        onUpdate(plan.copy(entries = newEntries))
                    },
                    onDeleteDay = {
                        // 🔥 3. 判斷：大於一天則遞補，剩下一天則刪除方案
                        if (dayCount > 1) {
                            val targetDayInt = dayIndex + 1
                            val newDayCount = dayCount - 1

                            val updatedEntries = plan.entries
                                .filterNot { (it.date.toIntOrNull() ?: 1) == targetDayInt }
                                .map { entry ->
                                    val currentDayInt = entry.date.toIntOrNull() ?: 1
                                    if (currentDayInt > targetDayInt) {
                                        entry.copy(date = (currentDayInt - 1).toString())
                                    } else {
                                        entry
                                    }
                                }

                            dayCount = newDayCount
                            onUpdate(plan.copy(dayCount = newDayCount, entries = updatedEntries))
                        } else {
                            // 剩最後一天，直接觸發整包刪除
                            onDeletePlan()
                        }
                    }
                )
            }

            // 🔥 底部的新增按鈕
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            dayCount += 1
                            onUpdate(plan.copy(dayCount = dayCount))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_day_n, dayCount + 1))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_day_n, dayCount + 1))
                    }
                }
            }
        }
    }

    if (showRecipePicker) {
        RecipePickerDialog(
            recipes = allRecipes,
            onDismiss = { showRecipePicker = false },
            onRecipeSelected = { recipe ->
                val dayStr = (activeDayOffset + 1).toString()
                val newEntry = MealPlanEntry(
                    id = UUID.randomUUID().toString(),
                    date = dayStr,
                    mealId = activeMealId,
                    name = recipe.name,
                    source = "RECIPE",
                    sourcePlanId = plan.id,
                    grams = recipe.totalWeightGram,
                    servings = 1.0,
                    kcal = recipe.kcalPerServing,
                    protein = recipe.protein,
                    carbs = recipe.carbs,
                    fats = recipe.fats,
                    cholesterol = 0.0,
                    recipeId = recipe.id,
                    inventoryId = null,
                    cost = recipe.cost ?: BigDecimal.ZERO,
                    currency = recipe.currency,
                    amount = null,
                    amountUnit = null,
                    isCompleted = false
                )

                // 更新 Plan
                onUpdate(plan.copy(entries = plan.entries + newEntry))
                showRecipePicker = false
            }
        )
    }
}

// --- UI: Plan Summary Card (Dashboard 用) ---
@Composable
fun PlanSummaryCard(
    plan: MenuPlanSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_cd),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 數據摘要行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Avg Kcal
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFE65100), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.avg_kcal_per_day_fmt, plan.avgKcal),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Total Cost
                if (plan.totalCost > BigDecimal.ZERO) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.total_cost_label) + ": " + formatCurrency(plan.totalCost, AppCurrency.TWD), // 幣別需動態處理
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.contains_meals_fmt, plan.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// --- UI: Day Editor Card (Editor 用) ---
@Composable
private fun CompactMacroItem(icon: ImageVector, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = "%.0f".format(value),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorCard(
    dayIndex: Int,
    entries: List<MealPlanEntry>,
    mealDefs: List<com.mealoo.app.model.MealDef>, // 確保引入正確的 Model
    onAddClick: (String) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onDeleteDay: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) } // 🔥 卡片展開狀態

    val totalKcal = entries.sumOf { it.kcal }
    val totalProtein = entries.sumOf { it.protein }
    val totalCarbs = entries.sumOf { it.carbs }
    val totalFats = entries.sumOf { it.fats }

    OutlinedCard(
        onClick = { expanded = !expanded }, // 🔥 點擊整張卡片展開/收合
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(12.dp)) {
            // --- Header (永遠顯示) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day ${dayIndex + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 🔥 加上箭頭 Icon 明示可展開
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp).size(20.dp)
                )

                Spacer(Modifier.weight(1f))

                // 右側四宮格營養素
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMacroItem(Icons.Default.LocalFireDepartment, totalKcal, Color(0xFFFF5722))
                    CompactMacroItem(Icons.Default.FitnessCenter, totalProtein, Color(0xFFD32F2F))
                    CompactMacroItem(Icons.Default.Grass, totalCarbs, Color(0xFF1976D2))
                    CompactMacroItem(Icons.Default.WaterDrop, totalFats, Color(0xFFFBC02D))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteDay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Day",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- Body (展開時才顯示) ---
            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    color = Color(0xFFEEEEEE)
                )

                // 1. 餐點列表：依據餐次分組顯示
                if (entries.isNotEmpty()) {
                    val grouped = entries.groupBy { it.mealId }
                    grouped.forEach { (mealId, mealEntries) ->
                        val mealName = mealDefs.find { it.id == mealId }?.name ?: "未分類"

                        // 餐次小標題
                        Text(
                            text = mealName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )

                        // 該餐次底下的所有餐點
                        mealEntries.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stripBrackets(entry.name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${"%.0f".format(entry.kcal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp).clickable { onRemoveEntry(entry.id) },
                                    tint = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_meals),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.add_meal_to), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mealDefs) { def ->
                        AssistChip(
                            onClick = { onAddClick(def.id) },
                            label = { Text(def.name) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}
// --- UI: Recipe Picker Dialog (選擇器) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePickerDialog(
    recipes: List<RecipeEntry>,
    onDismiss: () -> Unit,
    onRecipeSelected: (RecipeEntry) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(recipes, searchQuery) {
        if (searchQuery.isBlank()) recipes else recipes.filter { it.name.contains(searchQuery, true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 600.dp),
        title = { Text(stringResource(R.string.select_recipe_to_add_title)) },
        text = {
            Column {
                RecipeSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { recipe ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRecipeSelected(recipe) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 縮圖 (可選)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.size(48.dp)
                            ) {
                                // 這裡可以放 AsyncImage，如果有的話
                                Icon(Icons.Outlined.Restaurant, null, tint = Color.Gray, modifier = Modifier.padding(12.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(recipe.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(R.string.kcal_srv_cost_fmt, recipe.kcalPerServing, formatCurrency(recipe.cost, AppCurrency.fromCode(recipe.currency?: "TWD"))),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Divider(color = Color(0xFFEEEEEE))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    active: Map<String, Set<String>>,
    onToggle: (cat: String, id: String, selected: Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)
    ) {
        // 顶部操作区
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(id = R.string.filter), style = MaterialTheme.typography.titleSmall)
            Row {
                TextButton(onClick = onClear) { Text(stringResource(id = R.string.reset)) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
            }
        }

        val catalog: List<Pair<String, Set<String>>> = listOf(
            "meal" to TagCatalog.meal,
            "diet" to TagCatalog.diet,
            "cuisine" to TagCatalog.cuisine,
            "course" to TagCatalog.course,
            "taste" to TagCatalog.taste,
            "method" to TagCatalog.method,
            "device" to TagCatalog.device,
            "occasion" to TagCatalog.occasion,
            "allergen" to TagCatalog.allergen,
            "difficulty" to TagCatalog.difficulty
        )

        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)                // 给滚动区一个明确上界
                .verticalScroll(rememberScrollState()) // 用普通 Column + verticalScroll
                .padding(vertical = 4.dp)
        ) {
            catalog.forEachIndexed { index, (cat, ids) ->

                Text(
                    text = localizeTagCategory(cat),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(start = 16.dp, top = 8.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary // 用主题 primary 强调
                )

                ids.forEach { id ->
                    val checked = active[cat]?.contains(id) == true
                    DropdownMenuItem(
                        text = {
                            Text(
                                localizeTag(cat, id),
                                color = MaterialTheme.colorScheme.onSurface // 普通黑灰色
                            )
                        },
                        onClick = { onToggle(cat, id, !checked) },
                        trailingIcon = { Checkbox(checked = checked, onCheckedChange = null) }
                    )
                }

                if (index != catalog.lastIndex) {
                    Divider(Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    onDismiss: () -> Unit,
    onSave: (RecipeEntry) -> Unit,
    storageItems: List<InventoryItem>,
    recipeEntries: List<RecipeEntry>
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val foodRepo = remember { FoodDbRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State Variables ---
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var stripBracketsEnabled by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    val userPreferenceDataStore = remember { UserPreferenceDataStore(context) }
    val userPreference by produceState(initialValue = UserPreferenceDataStore.createLocalizedDefaultPreference(context), key1 = userPreferenceDataStore) {
        value = userPreferenceDataStore.getPreference()
    }
    var currency by remember { mutableStateOf(userPreference.homeCurrency) }
    var ingredients by remember { mutableStateOf<List<RecipeIngredient>>(emptyList()) }
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientGrams by remember { mutableStateOf("") }
    var allFoodOptions by remember { mutableStateOf<List<UnifiedFoodDisplayItem>>(emptyList()) }
    var visibleCount by remember { mutableStateOf(30) }
    var isSearching by remember { mutableStateOf(false) }
    var foodSourceScope by remember { mutableStateOf(UnifiedFoodSourceScope.ALL) }
    var selectedFdcId by remember { mutableStateOf<Int?>(null) }
    var selectedTagIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var newIngredientCost by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timeRequiredInput by remember { mutableStateOf("") }
    var gramsDialogFood by remember { mutableStateOf<UnifiedFoodDisplayItem?>(null) }

    // Steps & Notes State
    var newStep by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var newIngredientText by remember { mutableStateOf("") }
    var ingredientsText by remember { mutableStateOf<List<String>>(emptyList()) }

    val error_invalid_grams = stringResource(R.string.error_invalid_grams)
    var foodOptions = allFoodOptions.take(visibleCount.coerceAtMost(allFoodOptions.size))

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            val rawPath = copyUriToAppStorage(context, uri)
            val compressedFile = rawPath?.let { compressImageFile(it, context) }
            imagePath = compressedFile?.absolutePath ?: rawPath
        }
    }

    val isFormValid by derivedStateOf {
        name.isNotBlank() && ingredients.isNotEmpty()
    }

    // --- Search Logic ---
    LaunchedEffect(newIngredientName, foodSourceScope, storageItems, recipeEntries) {
        val keyword = newIngredientName.trim()
        if (keyword.isEmpty()) {
            allFoodOptions = emptyList()
            visibleCount = 0
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        val result = withContext(Dispatchers.Default) {
            queryUnifiedFoods(
                context = context,
                keyword = keyword,
                scope = foodSourceScope,
                storageItems = storageItems,
                recipeEntries = recipeEntries
            )
        }
        allFoodOptions = result
        visibleCount = result.size.coerceAtMost(30)
        isSearching = false
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        properties = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.Inherit, isFocusable = true, shouldDismissOnBackPress = false)
    ) {
        val isImeVisible = WindowInsets.ime.getBottom(density) > 0
        val onBack = remember(isImeVisible) {
            {
                if (isImeVisible) focusManager.clearFocus() else onDismiss()
            }
        }
        BackHandler(enabled = true) { onBack() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp) // 统一左右边距
        ) {
            // 1. Header Title
            Text(
                text = stringResource(R.string.new_recipe),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp, top = 0.dp)
            )

            // 2. Image Cover
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (imagePath != null) {
                        AsyncImage(
                            model = File(imagePath!!),
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally){
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.upload_image),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Basic Info Section
            MealooTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.name),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Time Input
                MealooTextField(
                    value = timeRequiredInput,
                    onValueChange = { timeRequiredInput = it },
                    label = stringResource(R.string.time) + " (min)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.weight(1f)) {
                    MealooTextField(
                        value = currency.code,
                        onValueChange = {},
                        label = stringResource(R.string.currency),
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    var expanded by remember { mutableStateOf(false) }
                    Box(Modifier.matchParentSize().clickable { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AppCurrency.values().filter { it != AppCurrency.OTHER }.forEach { appCurrency ->
                            DropdownMenuItem(text = { Text(appCurrency.code) }, onClick = { currency = appCurrency; expanded = false })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            TagMultiSelectDropdown(
                selectedIds = selectedTagIds,
                onSelectedChange = { selectedTagIds = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // 4. Ingredients Section
            SectionHeader(title = stringResource(R.string.ingredient))

            // Search Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newIngredientName,
                    onValueChange = {
                        newIngredientName = it
                        selectedFdcId = null
                    },
                    placeholder = { Text(stringResource(R.string.ingredient)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        IconButton(onClick = { stripBracketsEnabled = !stripBracketsEnabled }) {
                            Icon(
                                imageVector = if (stripBracketsEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (stripBracketsEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )
            }

            // Source Filter Chips
            val locale = Locale.getDefault().language
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(UnifiedFoodSourceScope.values()) { scope ->
                    val label = when (scope) {
                        UnifiedFoodSourceScope.ALL -> stringResource(R.string.all)
                        else -> scope.name
                    }
                    val isSelected = foodSourceScope == scope
                    FilterChip(
                        selected = isSelected,
                        onClick = { foodSourceScope = scope },
                        label = { Text(label) },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if(isSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant, enabled = true, selected = false)
                    )
                }
            }
            // Search Results
            AnimatedVisibility(visible = newIngredientName.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (isSearching) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (foodOptions.isEmpty()) {
                        Text(
                            stringResource(R.string.cannot_find_with) + " " + stringResource(R.string.matching_food),
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn {
                            items(foodOptions) { item ->
                                val displayName = if (stripBracketsEnabled) stripBrackets(item.name) else item.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            gramsDialogFood = item
                                            newIngredientGrams = ""

                                            if (item.source == "STORAGE") {
                                                val inv = storageItems.find { it.id == item.id }
                                                if (inv != null && inv.cost != null && inv.quantity > BigDecimal.ZERO) {

                                                    val invCurrency = inv.currency
                                                    if (ingredients.isEmpty() && currency.code == AppCurrency.TWD.code && invCurrency != AppCurrency.TWD.code) {
                                                        currency = AppCurrency.fromCode(invCurrency)
                                                    }
                                                    else if (currency.code != invCurrency) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.currency_mismatch_warning, invCurrency, currency.code))
                                                        }
                                                    }
                                                    val totalCost = inv.cost
                                                    val qty = inv.quantity

                                                    val costPer100g: BigDecimal? = when (inv.unit.lowercase()) {
                                                        "g", "ml" -> {
                                                            totalCost.divide(qty, 10, RoundingMode.HALF_UP)
                                                                .multiply(BigDecimal(100))
                                                        }
                                                        "pcs" -> {
                                                            val gpu = inv.gramsPerUnit
                                                            if (gpu != null && gpu > 0) {
                                                                val totalGrams = qty.multiply(BigDecimal.valueOf(gpu))
                                                                totalCost.divide(totalGrams, 10, RoundingMode.HALF_UP)
                                                                    .multiply(BigDecimal(100))
                                                            } else {
                                                                null
                                                            }
                                                        }
                                                        else -> null
                                                    }

                                                    newIngredientCost = costPer100g?.stripTrailingZeros()?.toPlainString() ?: ""
                                                } else {
                                                    newIngredientCost = ""
                                                }
                                            } else {
                                                newIngredientCost = ""
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                            }
                            if (foodOptions.size < allFoodOptions.size) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        TextButton(onClick = { visibleCount += 30 }) { Text(stringResource(R.string.loading)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (ingredients.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ingredients.forEachIndexed { idx, ing ->
                        IngredientItemCard(
                            ingredient = ing,
                            currency = currency,
                            onRemove = { ingredients = ingredients.filterIndexed { i, _ -> i != idx } },
                            onCostChange = { newCostStr ->
                                val newCost = newCostStr.toBigDecimalOrNull()
                                ingredients = ingredients.toMutableList().also { list ->
                                    list[idx] = list[idx].copy(cost = newCost)
                                }
                            }
                        )
                    }
                    val totalCost: BigDecimal = ingredients.fold(BigDecimal.ZERO) { acc, ing ->
                        val per100 = ing.cost ?: BigDecimal.ZERO
                        val ratio = BigDecimal.valueOf(ing.amount)
                            .divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
                        acc + per100.multiply(ratio)
                    }

                    Text(
                        text = stringResource(R.string.total_cost) + " " + formatCurrency(totalCost, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // 5. Steps Section
            SectionHeader(title = stringResource(R.string.steps))
            steps.forEachIndexed { i, s ->
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        "${i + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
            // Add Step Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                MealooTextField(
                    value = newStep,
                    onValueChange = { newStep = it },
                    placeholder = stringResource(R.string.placeholder_step_example),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newStep.isNotBlank()) {
                            steps = steps + newStep.trim()
                            newStep = ""
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.ArrowCircleUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // 6. Notes (Optional)
            SectionHeader(title = stringResource(R.string.ingredient_note))
            ingredientsText.forEach { s ->
                Row(Modifier.padding(bottom = 4.dp)) {
                    Text(stringResource(R.string.common_bullet), modifier = Modifier.padding(end = 8.dp), color = Color.Gray)
                    Text(s, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MealooTextField(
                    value = newIngredientText,
                    onValueChange = { newIngredientText = it },
                    placeholder = stringResource(R.string.placeholder_ingredient_note),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newIngredientText.isNotBlank()) {
                            ingredientsText = ingredientsText + newIngredientText.trim()
                            newIngredientText = ""
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.ArrowCircleUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Description
            MealooTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.description),
                singleLine = false,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 7. Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (name.isBlank()) {
                                snackbarHostState.showSnackbar(context.getString(R.string.error_name_required))
                                return@launch
                            }
                            if (ingredients.isEmpty()) {
                                snackbarHostState.showSnackbar(context.getString(R.string.error_need_ingredient))
                                return@launch
                            }
                            val allFoods = withContext(Dispatchers.IO) {
                                foodRepo.ensureSeeded(context)
                                val ids = ingredients.mapNotNull { it.matchedFdcId }.distinct()
                                ids.mapNotNull { foodRepo.getFoodById(it) }
                            }
                            val finalTagsV2 = migrateFlatTagsToV2(selectedTagIds)
                            val newRecipe = buildRecipeEntry(
                                name = name,
                                tagsV2 = finalTagsV2,
                                totalGram = ingredients.sumOf { it.amount },
                                timeRequired = timeRequiredInput.toIntOrNull(),
                                ingredients = ingredients,
                                allFoods = allFoods,
                                localImagePath = imagePath,
                                steps = steps,
                                ingredientsText = ingredientsText,
                                description = description,
                                currency = currency
                            )
                            onSave(newRecipe)
                            onDismiss()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save),color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }

    if (gramsDialogFood != null) {
        AlertDialog(
            onDismissRequest = {
                gramsDialogFood = null
                newIngredientGrams = ""
                newIngredientCost = ""
            },
            title = { Text(stringResource(R.string.grams), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = stripBrackets(gramsDialogFood!!.name),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    MealooTextField(
                        value = newIngredientGrams,
                        onValueChange = { newIngredientGrams = it },
                        label = stringResource(R.string.grams_g_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    MealooTextField(
                        value = newIngredientCost,
                        onValueChange = { newIngredientCost = it },
                        label = stringResource(R.string.cost_per_100g_fmt, currency.code),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val grams = newIngredientGrams.toDoubleOrNull()
                        if (grams == null || grams <= 0.0) {
                            scope.launch { snackbarHostState.showSnackbar(error_invalid_grams) }
                            return@Button
                        }
                        val food = gramsDialogFood!!
                        val costPer100g = newIngredientCost
                            .toBigDecimalOrNull()

                        ingredients = ingredients + RecipeIngredient(
                            name = food.name,
                            amount = grams,
                            source = food.source,
                            matchedFdcId = food.id.toIntOrNull(),
                            nutrientsPer100g = food.nutrients,
                            cost = costPer100g
                        )
                        gramsDialogFood = null
                        newIngredientGrams = ""
                        newIngredientCost = ""
                        newIngredientName = ""
                        selectedFdcId = null
                        foodOptions = emptyList()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.confirm),color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { gramsDialogFood = null }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        )
    }
}

// --- Helper Components for Aesthetic Look ---

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}



@Composable
private fun IngredientItemCard(
    ingredient: RecipeIngredient,
    currency: AppCurrency,
    onRemove: () -> Unit,
    onCostChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stripBrackets(ingredient.name.ifBlank { stringResource(R.string.unknown) }),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "${"%.0f".format(ingredient.amount)} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${currency.code}/100g",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                BasicTextField(
                    value = ingredient.cost?.toString() ?: "",
                    onValueChange = onCostChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(60.dp)
                        .padding(top = 2.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    singleLine = true
                )
            }

            Spacer(Modifier.width(12.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFBDBDBD))
            }
        }
    }
}
fun buildRecipeEntry(
    name: String,
    totalGram: Double,
    timeRequired: Int? = null,
    ingredients: List<RecipeIngredient>,
    allFoods: List<FoodItem>,
    localImagePath: String? = "",
    steps: List<String> = emptyList(),
    tagsV2: Map<String, List<String>>,
    ingredientsText: List<String> = emptyList(),
    description: String? = null,
    currency: AppCurrency? = null
): RecipeEntry {
    val foodMap = allFoods.associateBy { it.fdcId }
    var kcal = 0.0
    var protein = 0.0
    var carbs = 0.0
    var fats = 0.0
    var totalCost = BigDecimal.ZERO

    for (i in ingredients) {
        val g = i.amount

        i.cost?.let { per100Cost ->
            val ratio = BigDecimal.valueOf(g)
                .divide(BigDecimal(100), 6, RoundingMode.HALF_UP)

            totalCost = totalCost.add(per100Cost.multiply(ratio))
        }

        val n: Map<String, Double>? = when {
            i.source == "DATA" && i.matchedFdcId != null -> {
                foodMap[i.matchedFdcId]?.nutrients?.mapValues { it.value.amount as Double }
            }
            i.nutrientsPer100g != null -> {
                i.nutrientsPer100g.map { (key, value) ->
                    val stdKey = userKeyToStandard[key] ?: key
                    stdKey to value
                }.toMap()
            }
            else -> null
        }

        if (n != null) {
            kcal += (n["Energy"] ?: 0.0) * g / 100.0
            protein += (n["Protein"] ?: 0.0) * g / 100.0
            carbs += (n["Carbohydrate, by difference"] ?: 0.0) * g / 100.0
            fats += (n["Total lipid (fat)"] ?: 0.0) * g / 100.0
        }
    }

    return RecipeEntry(
        id = UUID.randomUUID().toString(),
        name = name,
        kcalPerServing = kcal,
        totalWeightGram = totalGram,
        protein = protein,
        carbs = carbs,
        fats = fats,
        tagsV2 = tagsV2,
        timeRequired = timeRequired,
        ingredients = ingredients,
        localImagePath = localImagePath,
        steps = steps,
        description = description,
        ingredientsText = ingredientsText,
        cost = totalCost.takeIf { it > BigDecimal.ZERO },
        currency = currency?.code
    )
}
@Composable
fun RecipeCard(
    recipe: RecipeEntry,
    onClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val mealPlanRepository = remember { MealPlanRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    fun RecipeEntry.toMealPlanEntry(
        mealId: String,
        date: String,
        servings: Double = 1.0
    ): MealPlanEntry {
        return MealPlanEntry(
            id = UUID.randomUUID().toString(),
            date = date,
            mealId = mealId,
            name = this.name,
            source = "RECIPE",
            grams = (this.totalWeightGram * servings).takeIf { it.isFinite() } ?: 0.0,
            servings = servings,
            kcal = (this.kcalPerServing * servings).takeIf { it.isFinite() } ?: 0.0,
            protein = (this.protein * servings).takeIf { it.isFinite() } ?: 0.0,
            carbs = (this.carbs * servings).takeIf { it.isFinite() } ?: 0.0,
            fats = (this.fats * servings).takeIf { it.isFinite() } ?: 0.0,
            cholesterol = 0.0, // Set to 0.0 as RecipeEntry does not have it, and MealPlanEntry's cholesterol is non-nullable.
            fdcId = null,
            recipeId = this.id,
            inventoryId = null,
            cost = this.cost?.multiply(BigDecimal.valueOf(servings))?.takeIf { it >= BigDecimal.ZERO } ?: BigDecimal.ZERO,
            currency = this.currency,
            amount = null,
            amountUnit = null
        )
    }
    // 图片加载逻辑
    val imageModel: Any? = remember(recipe.localImagePath, recipe.imageUrl) {
        when {
            !recipe.localImagePath.isNullOrBlank() -> File(recipe.localImagePath!!)
            !recipe.imageUrl.isNullOrBlank()       -> recipe.imageUrl
            else                                   -> null
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 2.dp), // 外边距，产生悬浮感
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)) // 极淡的边框增加精致感
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // 卡片内部内边距
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 图片区域：使用圆角矩形，比圆形更能展示食物细节
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier.size(72.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                    error = painterResource(R.drawable.ic_recipe_placeholder),
                    fallback = painterResource(R.drawable.ic_recipe_placeholder),
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. 中间信息区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 菜谱名称
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color(0xFF1A1A1A),
                    maxLines = 1
                )

                // 信息行：时间 & 热量
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 时间 (灰色/中性)
                    if (recipe.timeRequired != null) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${recipe.timeRequired}${stringResource(R.string.min_suffix)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    // 热量 (橙色 - 增加活力)
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9800), // Orange
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "%.0f".format(recipe.kcalPerServing),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE65100) // Darker Orange text
                    )
                }
            }

            // 3. 右侧操作与价格区域
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(72.dp) // 与图片等高，确保对齐
            ) {
                // 菜单按钮
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp) // 稍微调小一点，显得精致
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz, // 用横向三点比竖向更适合卡片布局
                            contentDescription = stringResource(R.string.more_actions),
                            tint = Color.LightGray
                        )
                    }

                    // 下拉菜单
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        // 选项1：加入菜单
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_menu)) },
                            leadingIcon = { Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                menuExpanded = false
                                val tomorrow = java.time.LocalDate.now().plusDays(1)
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val selected = java.time.LocalDate.of(y, m + 1, d)
                                        val dateStr = selected.toString()
                                        scope.launch {
                                            mealPlanRepository.addEntry(dateStr, recipe.toMealPlanEntry(
                                                mealId = UNCLASSIFIED_MEAL_ID,
                                                date = dateStr,
                                                servings = 1.0
                                            ))
                                            snackbarHostState.showSnackbar(context.getString(R.string.added_to_menu, dateStr))
                                        }
                                    },
                                    tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth
                                ).show()
                            }
                        )
                        // 选项2：发送到库存
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sent_to_storage)) },
                            leadingIcon = { Icon(Icons.Default.Inventory2, null, tint = Color(0xFF795548)) }, // Brownish for storage
                            onClick = {
                                menuExpanded = false
                                RecipeToStorageBridge.incoming.tryEmit(recipe)
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.sent_to_storage)) }
                            }
                        )
                    }
                }

                // 价格显示 (绿色 - 放在右下角)
                if (recipe.cost != null && recipe.cost > BigDecimal.ZERO) {
                    Surface(
                        color = Color(0xFFE8F5E9), // Light Green bg
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formatCurrency(recipe.cost, AppCurrency.fromCode(recipe.currency ?: AppCurrency.USD.code)),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32), // Dark Green text
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun RecipeDetailDialog(
    recipe: RecipeEntry,
    onDismiss: () -> Unit,
    onDelete: (RecipeEntry) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val foodRepo = remember { FoodDbRepository.getInstance(context) }
    val localeLang = Locale.getDefault().language
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Colors
    val bgColor = if (isDark) Color(0xFF090D12) else Color(0xFFF8F8F8)
    val cardColor = if (isDark) Color(0xFF0D1117).copy(alpha = 0.92f) else Color.White
    val borderColor = if (isDark) Color(0xFF30363D) else Color(0xFFEEEEEE)
    val accentCyan = Color(0xFF00D2FF)
    val textPrimary = if (isDark) Color.White else Color(0xFF1A1A1A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF757575)

    // 预加载食物数据用于显示原始名称
    val foodMap by produceState<Map<Int, FoodItem>>(initialValue = emptyMap(), recipe) {
        val ids = recipe.ingredients.mapNotNull { it.matchedFdcId }.distinct()
        val list = withContext(Dispatchers.IO) {
            foodRepo.ensureSeeded(context)
            ids.mapNotNull { foodRepo.getFoodById(it) }
        }
        value = list.associateBy { it.fdcId }
    }

    val imageModel: Any? = remember(recipe.localImagePath, recipe.imageUrl) {
        when {
            !recipe.localImagePath.isNullOrBlank() -> File(recipe.localImagePath!!)
            !recipe.imageUrl.isNullOrBlank()       -> recipe.imageUrl
            else                                   -> null
        }
    }

    BackHandler { onDismiss() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .padding(bottom = 88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // --- 1. Hero Image Section ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    // Image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                        error = painterResource(R.drawable.ic_recipe_placeholder),
                        fallback = painterResource(R.drawable.ic_recipe_placeholder),
                        contentDescription = recipe.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay (stronger in dark mode)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = if (isDark) 0.3f else 0.1f),
                                        Color.Black.copy(alpha = if (isDark) 0.85f else 0.6f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(color = Color.Black, blurRadius = 8f)
                            ),
                            color = Color.White,
                            maxLines = 2
                        )
                        if (recipe.timeRequired != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${recipe.timeRequired}${context.getString(R.string.min_suffix)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentCyan
                                )
                            }
                        }
                    }
                }

                // --- 2. Scrollable Content ---
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {

                    // Info chips row (weight & cost)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (recipe.totalWeightGram > 0.0) {
                            InfoChip(
                                icon = Icons.Default.Scale,
                                label = stringResource(R.string.total_weight),
                                value = stringResource(R.string.common_grams_value_desc, "%.0f".format(recipe.totalWeightGram)),
                                color = accentCyan,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        recipe.cost?.takeIf { it > java.math.BigDecimal.ZERO }?.let { c ->
                            InfoChip(
                                icon = Icons.Default.AttachMoney,
                                label = stringResource(R.string.cost),
                                value = formatCurrency(c, AppCurrency.fromCode(recipe.currency ?: AppCurrency.USD.code)),
                                color = Color(0xFF43A047),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Nutrient Cards ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutrientCard(
                            label = stringResource(R.string.energy),
                            value = "%.0f".format(recipe.kcalPerServing),
                            unit = stringResource(R.string.kcal_unit),
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = Color(0xFFFF5722),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientCard(
                            label = stringResource(R.string.protein),
                            value = "%.1f".format(recipe.protein),
                            unit = stringResource(R.string.common_unit_g_label),
                            icon = Icons.Default.FitnessCenter,
                            accentColor = Color(0xFFEF5350),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientCard(
                            label = stringResource(R.string.carbs),
                            value = "%.1f".format(recipe.carbs),
                            unit = stringResource(R.string.common_unit_g_label),
                            icon = Icons.Default.Grass,
                            accentColor = Color(0xFF42A5F5),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        NutrientCard(
                            label = stringResource(R.string.fats),
                            value = "%.1f".format(recipe.fats),
                            unit = stringResource(R.string.common_unit_g_label),
                            icon = Icons.Default.WaterDrop,
                            accentColor = Color(0xFFFDD835),
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // --- Tags ---
                    if (recipe.tagsV2.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        FlowRow(mainAxisSpacing = 6.dp, crossAxisSpacing = 6.dp) {
                            recipe.tagsV2.values.flatten().forEach { id ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentCyan.copy(alpha = if (isDark) 0.15f else 0.1f))
                                        .border(1.dp, accentCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#${localizeTag("unknown", id)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accentCyan,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // --- Description ---
                    recipe.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(16.dp))
                        DetailSectionTitle(stringResource(R.string.description), isDark = isDark)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // --- Ingredients ---
                    if (recipe.ingredients.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        DetailSectionTitle(stringResource(R.string.ingredient), isDark = isDark)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        ) {
                            recipe.ingredients.forEachIndexed { index, ing ->
                                val rawName = when (ing.source) {
                                    "DATA" -> {
                                        val food = ing.matchedFdcId?.let { foodMap[it] }
                                        when (localeLang) {
                                            "zh" -> food?.nameZh
                                            "ja" -> food?.nameJa
                                            else -> food?.nameEn
                                        } ?: ing.name
                                    }
                                    else -> ing.name
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(accentCyan, CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = stripBrackets(rawName),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textPrimary
                                        )
                                    }
                                    Text(
                                        text = "%.0f g".format(ing.amount),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = accentCyan
                                    )
                                }

                                if (index < recipe.ingredients.size - 1) {
                                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                                }
                            }
                        }

                        if (recipe.ingredientsText.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            recipe.ingredientsText.forEach { tip ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Info, null, tint = textSecondary, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tip, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                }
                            }
                        }
                    }

                    // --- Steps ---
                    if (recipe.steps.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        DetailSectionTitle(stringResource(R.string.steps), isDark = isDark)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recipe.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(accentCyan.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, accentCyan.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = accentCyan
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // --- Bottom Action Bar ---
                HorizontalDivider(color = borderColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delete button
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                    }

                    // Close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(stringResource(R.string.close), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.confirm)) },
            text = { Text(stringResource(R.string.action_irreversible)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(recipe)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// --- 辅助组件 (Aesthetic Components) ---

@Composable
private fun DetailSectionTitle(title: String, modifier: Modifier = Modifier, isDark: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = if (isDark) Color(0xFF00D2FF) else MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.Black,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val bgColor = if (isDark) color.copy(alpha = 0.12f) else color.copy(alpha = 0.1f)
    val borderColor = if (isDark) color.copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
            }
        }
    }
}

@Composable
private fun NutrientCard(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val bgColor = if (isDark) accentColor.copy(alpha = 0.1f) else accentColor.copy(alpha = 0.08f)
    val borderCol = if (isDark) accentColor.copy(alpha = 0.25f) else accentColor.copy(alpha = 0.15f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = accentColor)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.7f), fontSize = 10.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 1)
    }
}
fun copyUriToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imageDir = File(context.filesDir, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        val file = File(imageDir, "recipe_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output -> inputStream.copyTo(output) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun compressImageFile(originalPath: String, context: Context): File? {
    return try {
        val bitmap = android.graphics.BitmapFactory.decodeFile(originalPath)
        val compressedFile = File(context.filesDir, "compressed_${System.currentTimeMillis()}.jpg")
        val out = compressedFile.outputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
        out.flush()
        out.close()
        compressedFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun formatCurrency(amount: BigDecimal?, currencyCode: AppCurrency? = AppCurrency.TWD): String {
    val value = amount ?: BigDecimal.ZERO
    return try {
        val fmt = NumberFormat.getCurrencyInstance()
        fmt.currency = Currency.getInstance(currencyCode?.code)
        fmt.format(value)
    } catch (_: Exception) {
        (currencyCode?.symbol ?: "$") + value.stripTrailingZeros().toPlainString()
    }
}
