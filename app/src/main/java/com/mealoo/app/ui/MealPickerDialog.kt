package com.mealoo.app.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mealoo.app.R
import com.mealoo.app.data.RecipeDataStore
import com.mealoo.app.db.InventoryRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import com.mealoo.app.db.FoodDbRepository
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.model.LocalizedNutrientEntry
import com.mealoo.app.model.RecipeEntry
import com.mealoo.app.model.SelectedFood
import com.mealoo.app.model.UnifiedFoodDisplayItem
import com.mealoo.app.model.UserFoodItem
import com.mealoo.app.model.UserFoodStorage
import com.mealoo.app.util.LocaleUtil
import com.mealoo.app.util.stripBrackets
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mealoo.app.model.displayCostLabel
import com.mealoo.app.ui.design.CompactTextField
import com.mealoo.app.ui.design.MealooTextField
import com.mealoo.app.ui.design.MealooTheme
import com.mealoo.app.util.ContextLevel
import com.mealoo.app.util.FoodContext
import java.util.Locale
import com.mealoo.app.util.toLocalizedNutrientName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.roundToInt

fun scaleRecipeNutrients(recipe: RecipeEntry, grams: Double): SelectedFood {
    val totalGram = recipe.totalWeightGram
        ?: recipe.ingredients.sumOf { it.amount }.takeIf { it > 0 }
        ?: 100.0

    val ratio = grams / totalGram
    println("Scale: ${recipe.name} | grams=$grams / total=$totalGram → kcal=${recipe.kcalPerServing * ratio}")

    return SelectedFood(
        fdcId = null,
        source = "RECIPE",
        name = recipe.name,
        grams = grams,
        standardNutrients = mapOf(
            "Energy" to recipe.kcalPerServing * ratio,
            "Protein" to recipe.protein * ratio,
            "Available carbohydrate" to recipe.carbs * ratio,
            "Total lipid (fat)" to recipe.fats * ratio
        )
    )
}
data class PickSummaryRow(
    val title: String,
    val grams: Double,
    val kcal: Double
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<SelectedFood>, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val foodRepo = remember { FoodDbRepository.getInstance(context) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val themeColors = MealooTheme.colors
    val tabColors = remember(themeColors) {
        listOf(
            themeColors.lunch,
            themeColors.carbs,
            themeColors.breakfast,
            themeColors.water
        )
    }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeDetail by remember { mutableStateOf<ActiveFoodDetail?>(null) }
    var isSeeded by remember { mutableStateOf(false) }
    var allFoods by remember { mutableStateOf<List<LocalizedNutrientEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFoods by remember { mutableStateOf<List<Pair<LocalizedNutrientEntry, Double>>>(emptyList()) }
    var userFoods by remember { mutableStateOf<List<UserFoodItem>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<RecipeEntry>>(emptyList()) }
    val inventoryRepo = remember { InventoryRepository.getInstance(context) }
    val storageItems by inventoryRepo.itemsFlow().collectAsState(initial = emptyList())
    val storageUsables by remember(storageItems) {
        mutableStateOf(storageItems.filter { it.standardNutrientsPer100g != null })
    }
    var pickedFromStorage by remember { mutableStateOf<Map<InventoryItem, Double>>(emptyMap()) }

    LaunchedEffect(Unit) {
        isLoading = true
        var loadedUserFoods: List<UserFoodItem> = emptyList()
        var loadedRecipes: List<RecipeEntry> = emptyList()
        withContext(Dispatchers.IO) {
            foodRepo.ensureSeeded(context)
            loadedUserFoods = UserFoodStorage.loadUserFoods(context)
            loadedRecipes = RecipeDataStore(context).getRecipes()
        }
        userFoods = loadedUserFoods
        recipes = loadedRecipes
        isSeeded = true
        isLoading = false
    }

    LaunchedEffect(searchQuery, isSeeded) {
        if (!isSeeded) return@LaunchedEffect
        val trimmed = searchQuery.trim()
        val results = withContext(Dispatchers.IO) {
            foodRepo.searchLocalizedFoods(trimmed, limit = if (trimmed.isEmpty()) 200 else 50)
        }
        Log.i("MealPicker", "query='$trimmed' results=${results.size}")
        allFoods = results

    }

    val unifiedFoods = remember(allFoods, userFoods, searchQuery) {
        val system = allFoods.map {
            UnifiedFoodDisplayItem(
                id = it.fdcId.toString(),
                name = LocaleUtil.getLocalizedName(it),
                source = "DATA",
                nutrients = it.nutrients,
                category = LocaleUtil.getLocalizedCategory(it),
            )
        }
        val filteredUserFoods = if (searchQuery.isBlank()) {
            userFoods
        } else {
            userFoods.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        val user = filteredUserFoods.map {
            UnifiedFoodDisplayItem(
                id = it.id,
                name = it.name,
                source = "USER",
                nutrients = standardizeNutrients(it.nutrientsPer100g),
                category = null,
            )
        }
        system + user
    }

    val (dbFoods, userFoodCandidates) = remember(unifiedFoods) {
        val (dbFoods, otherFoods) = unifiedFoods.partition { it.source == "DATA" }
        val userFoodsOnly = otherFoods.filter { it.source == "USER" }
        dbFoods to userFoodsOnly
    }
    var selectedRecipes by remember { mutableStateOf<List<RecipeEntry>>(emptyList()) }
    var selectedRecipesWithGrams by remember { mutableStateOf<Map<RecipeEntry, Double>>(emptyMap()) }
    var inputGrams by remember { mutableStateOf("") }
    var showBrackets by remember { mutableStateOf(false) }

    // Tabs
    val tabs = listOf(
        stringResource(R.string.tab_food) to Icons.Default.Restaurant,
        stringResource(R.string.recipe) to Icons.Default.MenuBook,
        stringResource(R.string.tab_my) to Icons.Default.Person,
        stringResource(R.string.tab_storage) to Icons.Default.Inventory2
    )

    val foodRows = remember(dbFoods, showBrackets, tabColors) {
        dbFoods.map { food ->
            val displayName = if (showBrackets) food.name else stripBrackets(food.name)
            MealPickerRowModel(
                key = stableFoodKey(food),
                name = displayName,
                subtitle = null,
                kcalLabel = kcalPer100gLabel(context, food.nutrients),
                icon = iconForFoodCategory(food.category) ?: Icons.Default.Restaurant,
                iconColor = tabColors[0],
                nutrients = food.nutrients,
                detailSource = MealPickerDetailSource.UnifiedFood(food)
            )
        }
    }

    val recipeRows = remember(recipes, showBrackets, tabColors) {
        recipes.map { recipe ->
            val nutrients = recipe100(recipe)
            val displayName = if (showBrackets) recipe.name else stripBrackets(recipe.name)
            MealPickerRowModel(
                key = stableRecipeKey(recipe),
                name = displayName,
                subtitle = null,
                kcalLabel = kcalPer100gLabel(context, nutrients),
                icon = Icons.Default.MenuBook,
                iconColor = tabColors[1],
                nutrients = nutrients,
                detailSource = MealPickerDetailSource.Recipe(recipe)
            )
        }
    }

    val myRows = remember(userFoodCandidates, showBrackets, tabColors) {
        userFoodCandidates
            .asSequence()
            .map { food ->
                val displayName = if (showBrackets) food.name else stripBrackets(food.name)
                MealPickerRowModel(
                    key = "MY:${stableFoodKey(food)}",
                    name = displayName,
                    subtitle = null,
                    kcalLabel = kcalPer100gLabel(context, food.nutrients),
                    icon = Icons.Default.Person,
                    iconColor = tabColors[2],
                    nutrients = food.nutrients,
                    detailSource = MealPickerDetailSource.UnifiedFood(food)
                )
            }
            .toList()
    }

    val storageRows = remember(storageUsables, tabColors) {
        storageUsables.map { inv ->
            MealPickerRowModel(
                key = "INV:${inv.id}",
                name = inv.name,
                subtitle = inv.displayCostLabel(),
                kcalLabel = inv.standardNutrientsPer100g?.let { kcalPer100gLabel(context, it) },
                icon = Icons.Default.Inventory2,
                iconColor = tabColors[3],
                nutrients = inv.standardNutrientsPer100g ?: emptyMap(),
                detailSource = MealPickerDetailSource.Storage(inv)
            )
        }
    }

    var showFabReviewSheet by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { selectedTab = it }
    }
    val totalSelectedCount = selectedFoods.size + selectedRecipes.size + pickedFromStorage.size

    fun kcalFromPer100(nutrients: Map<String, Double>, grams: Double): Double {
        val std = standardizeNutrients(nutrients)
        val per100 = std["Energy"] ?: 0.0
        return per100 * grams / 100.0
    }

    val summaryRows: List<PickSummaryRow> = remember(selectedFoods, selectedRecipesWithGrams, pickedFromStorage) {
        buildList {
            // Foods (DATA/USER) - selectedFoods: List<Pair<LocalizedNutrientEntry, Double>>
            selectedFoods.forEach { (entry, grams) ->
                add(
                    PickSummaryRow(
                        title = LocaleUtil.getLocalizedName(entry),
                        grams = grams,
                        kcal = kcalFromPer100(entry.nutrients, grams)
                    )
                )
            }

            // Recipes
            selectedRecipesWithGrams.forEach { (recipe, grams) ->
                val scaled = scaleRecipeNutrients(recipe, grams) // Energy 已经是该 grams 的 kcal
                val kcal = scaled.standardNutrients["Energy"] ?: 0.0
                add(PickSummaryRow(title = recipe.name, grams = grams, kcal = kcal))
            }

            // Storage
            pickedFromStorage.forEach { (inv, grams) ->
                val per100 = inv.standardNutrientsPer100g ?: emptyMap()
                add(PickSummaryRow(title = inv.name, grams = grams, kcal = kcalFromPer100(per100, grams)))
            }
        }
    }
    val totalKcal: Double = remember(summaryRows) { summaryRows.sumOf { it.kcal } }
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ){
        BackHandler { if (showFabReviewSheet) showFabReviewSheet = false else onDismiss() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                AnimatedVisibility(
                    visible = totalSelectedCount > 0 && !showFabReviewSheet,
                    enter = androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { showFabReviewSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 88.dp)
                    ) {
                        androidx.compose.material3.BadgedBox(
                            badge = {
                                if (totalSelectedCount > 0) {
                                    androidx.compose.material3.Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = totalSelectedCount.toString(),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        ) {
                            // 这里放图标，建议用 "Check" 或 "Done" 表示确认
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.confirm),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize()){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // 这里 Scaffold 会自动计算除去 bottomBar 后的剩余空间
                ){
                    // --- Header Area ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding() // 顶部避让状态栏
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                    ){
                        // Top Bar: Close / Title / Add
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Close Button
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }

                            Text(
                                text = stringResource(R.string.select_food),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Add Custom Food Button
                            TextButton(
                                onClick = { showAddFoodDialog = true },
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.add_food))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_food), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        // Tabs
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tabs.size) { index ->
                                val selected = selectedTab == index
                                val color = tabColors[index]

                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        // 條件 1: 距離上次點擊超過 300ms (防手抖/連點)
                                        // 條件 2: 目標頁面不是當前正在前往的頁面 (防重複觸發)
                                        if (now - lastClickTime > 300 && pagerState.targetPage != index) {
                                            lastClickTime = now
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                    },
                                    label = { Text(tabs[index].first) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = tabs[index].second,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = if (selected) null else FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color.copy(alpha = 0.15f),
                                        selectedLabelColor = color.copy(alpha = 1f),
                                        selectedLeadingIconColor = color.copy(alpha = 1f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // --- Content Area ---
                    Box(modifier = Modifier.weight(1f)) {
                        if (isLoading) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
                            ) {
                                items(count = 12, key = { it }) { FoodItemRowSkeleton() }
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                            ) { page ->
                                val listModifier = Modifier.fillMaxSize()
                                val contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)

                                when (page) {
                                    0 -> { // Food
                                        LazyColumn(modifier = listModifier, contentPadding = contentPadding) {
                                            items(items = foodRows, key = { it.key },contentType = { "food_row" }) { row ->
                                                FoodItemRow(
                                                    name = row.name,
                                                    subtitle = row.subtitle,
                                                    kcalLabel = row.kcalLabel,
                                                    icon = row.icon,
                                                    iconColor = row.iconColor,
                                                    onOpenDetail = { activeDetail = ActiveFoodDetail(row) }
                                                )
                                            }
                                        }
                                    }
                                    1 -> { // Recipe
                                        LazyColumn(modifier = listModifier, contentPadding = contentPadding) {
                                            items(items = recipeRows, key = { it.key },contentType = { "food_row" }) { row ->
                                                FoodItemRow(
                                                    name = row.name,
                                                    subtitle = row.subtitle,
                                                    kcalLabel = row.kcalLabel,
                                                    icon = row.icon,
                                                    iconColor = row.iconColor,
                                                    onOpenDetail = { activeDetail = ActiveFoodDetail(row) }
                                                )
                                            }
                                        }
                                    }
                                    2 -> { // My
                                        LazyColumn(modifier = listModifier, contentPadding = contentPadding) {
                                            items(items = myRows, key = { it.key },contentType = { "food_row" }) { row ->
                                                FoodItemRow(
                                                    name = row.name,
                                                    subtitle = row.subtitle,
                                                    kcalLabel = row.kcalLabel,
                                                    icon = row.icon,
                                                    iconColor = row.iconColor,
                                                    onOpenDetail = { activeDetail = ActiveFoodDetail(row) }
                                                )
                                            }
                                        }
                                    }
                                    3 -> { // Storage
                                        if (storageUsables.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.storage_empty),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        } else {
                                            LazyColumn(modifier = listModifier, contentPadding = contentPadding) {
                                                items(items = storageRows, key = { it.key },contentType = { "food_row" }) { row ->
                                                    FoodItemRow(
                                                        name = row.name,
                                                        subtitle = row.subtitle,
                                                        kcalLabel = row.kcalLabel,
                                                        icon = row.icon,
                                                        iconColor = row.iconColor,
                                                        onOpenDetail = { activeDetail = ActiveFoodDetail(row) }
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
                if (showFabReviewSheet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showFabReviewSheet = false }
                    )
                }

                AnimatedVisibility(
                    visible = showFabReviewSheet,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter) // 靠底對齊
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .clickable(enabled = false) {}, // 攔截點擊，防止穿透
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        ReviewSheetContent(
                            totalSelectedCount = totalSelectedCount,
                            totalKcal = totalKcal,
                            summaryRows = summaryRows,
                            onCancel = { showFabReviewSheet = false },
                            onConfirm = {
                                val result = buildList {
                                    // Foods
                                    selectedFoods.forEach { (entry, grams) ->
                                        add(
                                            SelectedFood(
                                                fdcId = entry.fdcId,
                                                source = if (entry.fdcId != null) "DATA" else "USER",
                                                name = LocaleUtil.getLocalizedName(entry),
                                                grams = grams,
                                                standardNutrients = standardizeNutrients(entry.nutrients),
                                                category = LocaleUtil.getLocalizedCategory(entry)
                                            )
                                        )
                                    }
                                    // Recipes
                                    selectedRecipesWithGrams.forEach { (recipe, grams) ->
                                        add(scaleRecipeNutrients(recipe, grams))
                                    }
                                    // Storage
                                    pickedFromStorage.forEach { (inv, grams) ->
                                        add(
                                            SelectedFood(
                                                fdcId = inv.fdcId,
                                                source = "STORAGE",
                                                name = inv.name,
                                                grams = grams,
                                                standardNutrients = inv.standardNutrientsPer100g!!,
                                                inventoryId = inv.id
                                            )
                                        )
                                    }
                                }
                                onConfirm(result, "MIXED") // 呼叫 Dialog 的 onConfirm
                                showFabReviewSheet = false // 關閉 Sheet
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Sub-Dialogs ---

    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onAdd = { newFood ->
                scope.launch {
                    val updated = userFoods + newFood
                    UserFoodStorage.saveUserFoods(context, updated)
                    userFoods = updated
                }
            }
        )
    }

    activeDetail?.let { detail ->
        // 判斷來源是否為庫存 (Storage)
        if (detail.source is MealPickerDetailSource.Storage) {
            InventoryQuantityDialog(
                inventoryItem = detail.source.inv,
                onDismiss = { activeDetail = null },
                onConfirm = { grams ->
                    // 庫存確認邏輯
                    val inv = detail.source.inv
                    pickedFromStorage = pickedFromStorage + (inv to grams)
                    activeDetail = null
                }
            )
        } else {
            FoodDetailCard(
                name = detail.name,
                nutrients = detail.nutrients,
                onDismiss = { activeDetail = null },
                onConfirm = { grams ->
                    when (val source = detail.source) {
                        is MealPickerDetailSource.UnifiedFood -> {
                            val food = source.food
                            val fdcId = food.id.toIntOrNull()
                            val base = if (food.source == "DATA" && fdcId != null) {
                                allFoods.firstOrNull { it.fdcId == fdcId }
                            } else null
                            val entry = base ?: LocalizedNutrientEntry(
                                fdcId = fdcId,
                                nameEn = food.name,
                                nameZh = food.name,
                                nameJa = food.name,
                                categoryEn = food.category ?: "",
                                categoryZh = food.category ?: "",
                                categoryJa = food.category ?: "",
                                nutrients = food.nutrients,
                            )
                            selectedFoods = selectedFoods + (entry to grams)
                        }
                        is MealPickerDetailSource.Recipe -> {
                            val recipe = source.recipe
                            selectedRecipesWithGrams = selectedRecipesWithGrams + (recipe to grams)
                            if (!selectedRecipes.contains(recipe)) selectedRecipes = selectedRecipes + recipe
                        }
                        // Storage 已經被上面的 if 攔截處理了，這裡理論上不會執行到
                        is MealPickerDetailSource.Storage -> {
                            /* no-op */
                        }
                    }
                    activeDetail = null
                }
            )
        }
    }
}
@Composable
private fun ReviewSheetContent(
    totalSelectedCount: Int,
    totalKcal: Double,
    summaryRows: List<PickSummaryRow>,
    onCancel: () -> Unit,
    onConfirm: () -> Unit // 這裡的 onConfirm 只是一個單純的訊號，不帶參數
) {
    val totalKcalRounded = totalKcal.roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：標題資訊 (保持不變)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.meal_picker_selected_count_desc, totalSelectedCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.meal_picker_total_kcal_desc, totalKcalRounded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右側：操作按鈕群組 (Cancel 返回 + Confirm 打勾)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp) // 按鈕之間的間距
            ) {
                // Cancel: 返回圖標
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, // 使用返回圖標
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Confirm: 實心打勾圖標 (強調操作)
                FilledIconButton(
                    onClick = onConfirm,
                    enabled = totalSelectedCount > 0,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.confirm)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        // --- 列表區 ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp), // 限制最大高度
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(summaryRows) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stripBrackets(row.title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.common_grams_desc, row.grams),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.common_kcal_desc, row.kcal.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {}
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
@Composable
fun FoodItemRow(
    name: String,
    subtitle: String? = null,
    kcalLabel: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Restaurant,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onOpenDetail: () -> Unit
) {
    // Row is intentionally stateless; detail dialog is owned by the parent.

    // 计算热量显示 (Kcal)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧图标 (彩色底托)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        // 2. 文本信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle or Kcal info
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (!kcalLabel.isNullOrBlank()) {
                    Text(
                        text = kcalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // 3. 添加按钮 (圆形淡色背景)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(onClick = onOpenDetail), // Default to detail for grams
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private sealed interface MealPickerDetailSource {
    data class UnifiedFood(val food: UnifiedFoodDisplayItem) : MealPickerDetailSource
    data class Recipe(val recipe: RecipeEntry) : MealPickerDetailSource
    data class Storage(val inv: InventoryItem) : MealPickerDetailSource
}

private data class MealPickerRowModel(
    val key: String,
    val name: String,
    val subtitle: String?,
    val kcalLabel: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val nutrients: Map<String, Double>,
    val detailSource: MealPickerDetailSource
)

private data class ActiveFoodDetail(
    val name: String,
    val nutrients: Map<String, Double>,
    val source: MealPickerDetailSource
) {
    constructor(row: MealPickerRowModel) : this(
        name = row.name,
        nutrients = row.nutrients,
        source = row.detailSource
    )
}

private fun stableFoodKey(food: UnifiedFoodDisplayItem): String = "FOOD:${food.source}:${food.id}"

private fun stableRecipeKey(recipe: RecipeEntry): String {
    val base = recipe.id.takeIf { it.isNotBlank() } ?: recipe.name.trim()
    return "RECIPE:$base:${recipe.ingredients.hashCode()}"
}

private fun kcalPer100gLabel(context: Context, nutrients: Map<String, Double>): String? {
    val energy = nutrients["Energy"] ?: nutrients["enercKcal"]
    val value = energy?.takeIf { it > 0 } ?: return null
    return context.getString(R.string.meal_picker_kcal_per_100g_desc, value)
}

@Composable
fun FoodItemRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {

            // 第一行：名称占位
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            Spacer(Modifier.height(6.dp))

            // 第二行：subtitle 占位
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            )
        }

        Spacer(Modifier.width(8.dp))

        // 加号按钮占位
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )
    }
}

private fun iconForFoodCategory(category: String?): androidx.compose.ui.graphics.vector.ImageVector? {
    val rawCategory = category?.trim().orEmpty()
    if (rawCategory.isEmpty()) return null

    val normalized = rawCategory.lowercase(Locale.getDefault())
    fun hasAny(vararg keywords: String): Boolean = keywords.any { normalized.contains(it) }

    return when {
        hasAny("beverage", "drink", "飲料", "し好", "stimulating") -> Icons.Default.LocalDrink
        hasAny("dairy", "milk", "乳", "乳製品") -> Icons.Default.Icecream
        hasAny("egg", "eggs", "卵", "蛋") -> Icons.Default.EggAlt
        hasAny("vegetable", "vegetables", "野菜", "蔬菜") -> Icons.Default.Grass
        hasAny("fruit", "fruits", "果物", "水果", "果実", "果") -> Icons.Default.LocalFlorist
        hasAny("fish", "finfish", "shellfish", "seafood", "魚", "貝") -> Icons.Default.SetMeal
        hasAny(
            "meat",
            "beef",
            "pork",
            "poultry",
            "lamb",
            "veal",
            "sausage",
            "luncheon",
            "ham",
            "牛",
            "豚",
            "鶏",
            "羊",
            "肉"
        ) -> Icons.Default.LunchDining
        hasAny("legume", "bean", "beans", "lentil", "pea", "豆") -> Icons.Default.Eco
        hasAny("nut", "nuts", "seed", "seeds", "ナッツ", "種") -> Icons.Default.Eco
        hasAny(
            "cereal",
            "grain",
            "grains",
            "pasta",
            "baked",
            "bakery",
            "confection",
            "sugar",
            "sweet",
            "sweets",
            "starch",
            "potato",
            "穀",
            "麦",
            "麺",
            "菓子",
            "砂糖",
            "糖",
            "いも"
        ) -> Icons.Default.BakeryDining
        hasAny(
            "soup",
            "soups",
            "sauce",
            "sauces",
            "gravy",
            "spice",
            "spices",
            "herb",
            "herbs",
            "seasoning",
            "調味",
            "香辛",
            "香料",
            "スープ",
            "ソース"
        ) -> Icons.Default.SoupKitchen
        hasAny("seaweed", "海藻", "藻") -> Icons.Default.WaterDrop
        hasAny("prepared", "processed", "restaurant", "外食") -> Icons.Default.Fastfood
        else -> null
    }
}

fun standardizeNutrients(nutrients: Map<String, Double>): Map<String, Double> {
    val mapping = mapOf(
        "enercKcal" to "Energy",
        "prot" to "Protein",
        "fat" to "Total lipid (fat)",
        "chocdf" to "Carbohydrate, by difference"
    )
    return nutrients.mapKeys { (key, _) -> mapping[key] ?: key }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailCard(
    name: String,
    nutrients: Map<String, Double>?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val n = nutrients ?: emptyMap()
    val themeColors = MealooTheme.colors

    val p = n["Protein"] ?: n["protein"] ?: 0.0
    val c = n["Carbohydrate, by difference"] ?: n["carbohydrate"] ?: 0.0
    val f = n["Total lipid (fat)"] ?: n["fat"] ?: 0.0
    val kcal = n["Energy"] ?: n["energy"] ?: 0.0
    val fiber = n["Fiber, total dietary"] ?: n["fiber"] ?: 0.0
    val sugar = n["Sugars, total including NLEA"] ?: n["sugar"] ?: 0.0
    val sodium = n["Sodium, Na"] ?: n["sodium"] ?: 0.0
    val potassium = n["Potassium, K"] ?: n["potassium"] ?: 0.0
    val satFat = n["Fatty acids, total saturated"] ?: n["saturated_fat"] ?: 0.0
    val cholesterol = n["Cholesterol"] ?: n["cholesterol"] ?: 0.0

    val total = (p + c + f).toFloat()
    val validTotal = if (total > 0f) total else 1f

    // 输入状态
    var amountText by remember { mutableStateOf("100") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp).aspectRatio(1f)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()

                            val diameter = size.minDimension - strokeWidth
                            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                            val topLeftOffset = androidx.compose.ui.geometry.Offset(
                                x = (size.width - diameter) / 2,
                                y = (size.height - diameter) / 2
                            )

                            drawCircle(
                                color = Color(0xFFF5F5F5),
                                style = Stroke(width = strokeWidth),
                                radius = diameter / 2
                            )

                            var startAngle = -90f
                            val pSweep = (p.toFloat() / validTotal) * 360f
                            val cSweep = (c.toFloat() / validTotal) * 360f
                            val fSweep = (f.toFloat() / validTotal) * 360f

                            // 3. 在 drawArc 中傳入 topLeft 與 size
                            if (p > 0) {
                                drawArc(
                                    color = themeColors.protein,
                                    startAngle = startAngle,
                                    sweepAngle = pSweep,
                                    useCenter = false,
                                    topLeft = topLeftOffset,
                                    size = arcSize,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += pSweep
                            }

                            if (c > 0) {
                                drawArc(
                                    color = themeColors.carbs,
                                    startAngle = startAngle,
                                    sweepAngle = cSweep,
                                    useCenter = false,
                                    topLeft = topLeftOffset,
                                    size = arcSize,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += cSweep
                            }

                            if (f > 0) {
                                drawArc(
                                    color = themeColors.fats,
                                    startAngle = startAngle,
                                    sweepAngle = fSweep,
                                    useCenter = false,
                                    topLeft = topLeftOffset,
                                    size = arcSize,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${kcal.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSurface)
                            Text("kcal", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // 右側列表使用 weight(1f)，確保它能獲得所有剩餘寬度
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroLegendRow(themeColors.protein, stringResource(R.string.protein), p)
                        MacroLegendRow(themeColors.carbs, stringResource(R.string.carbs), c)
                        MacroLegendRow(themeColors.fats, stringResource(R.string.fats), f)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                // ================= PART 2: 详细数据 (Micro Grid) =================
                // 使用 FlowRow 或者 Column+Row 模拟 2列 Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        MicroNutrientItem(
                            label = stringResource(R.string.fiber),
                            value = "%.1f g".format(fiber),
                            modifier = Modifier.weight(1f)
                        )
                        MicroNutrientItem(
                            label = stringResource(R.string.sugar),
                            value = "%.1f g".format(sugar),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        MicroNutrientItem(
                            label = stringResource(R.string.sodium),
                            value = "%.0f mg".format(sodium),
                            modifier = Modifier.weight(1f)
                        )
                        MicroNutrientItem(
                            label = stringResource(R.string.potassium),
                            value = "%.0f mg".format(potassium),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        MicroNutrientItem(
                            label = stringResource(R.string.saturated_fat),
                            value = "%.1f g".format(satFat),
                            modifier = Modifier.weight(1f)
                        )
                        MicroNutrientItem(
                            label = stringResource(R.string.detail_cholesterol),
                            value = "%.0f mg".format(cholesterol),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // ================= PART 3: 输入区 =================
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                    },
                    label = { Text(stringResource(R.string.food_consumption_amount)) },
                    suffix = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grams = amountText.toDoubleOrNull()
                    if (grams != null && grams > 0) onConfirm(grams) else isError = true
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.add), fontWeight = FontWeight.Bold,color = MaterialTheme.colorScheme.onTertiary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// --- 辅助组件 1: Macro Legend (保持之前的多彩设计) ---
@Composable
private fun MacroLegendRow(color: Color, label: String, value: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth() // 確保 Row 佔滿右側可用空間
    ) {
        // 1. 小圓點
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))

        // 2. 標籤：改用 weight 並限制單行
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f), // 關鍵：自動佔滿剩餘空間
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(4.dp)) // 標籤與數值間的最小間距

        // 3. 數值：保持靠右
        Text(
            text = "${"%.1f".format(value)}g",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

// --- 辅助组件 2: Micro Item (新的紧凑型详细数据) ---
@Composable
private fun MicroNutrientItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小圆点 (灰色，区分于 Macro)
        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun NutrientSection(
    title: String,
    items: List<Map.Entry<String, Double>>,
    grams: Double
) {
    if (items.isEmpty()) return
    val ctx = LocalContext.current

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))

        items.forEach { (key, value) ->
            val est = value * grams / 100.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key.toLocalizedNutrientName(ctx),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.common_number_one_decimal_desc, est),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onAdd: (UserFoodItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var foodName by remember { mutableStateOf("") }
    var baseGramsText by remember { mutableStateOf("") }
    var nutrients by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val allFields = remember { mutableStateMapOf<String, String>() }

    var expandedFat by remember { mutableStateOf(false) }
    var expandedCarb by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun updateField(key: String, value: String) {
        allFields[key] = value
        val parsed = value.toDoubleOrNull()
        nutrients = if (parsed != null) nutrients + (key to parsed) else nutrients - key
    }

    val baseGrams = baseGramsText.toDoubleOrNull()
    val confirmEnabled =
        foodName.isNotBlank() &&
                nutrients.isNotEmpty() &&
                baseGrams != null &&
                baseGrams > 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp) // 限制最大高度，避免超出螢幕
            ) {
                // --- Title Header ---
                Text(
                    text = stringResource(R.string.add_food), // "Create Custom Food"
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)
                )

                // --- Scrollable Content ---
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // 1. Basic Info Section
                    Text(
                        text = stringResource(R.string.meal_picker_basic_info_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    MealooTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = stringResource(R.string.name),
                        placeholder = stringResource(R.string.meal_picker_food_name_hint),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    MealooTextField(
                        value = baseGramsText,
                        onValueChange = { raw ->
                            var cleaned = raw.filter { it.isDigit() || it == '.' }
                            if (cleaned.count { it == '.' } > 1) {
                                cleaned = cleaned.dropLast(1)
                            }
                            baseGramsText = cleaned
                        },
                        label = stringResource(R.string.meal_picker_serving_size_label),
                        placeholder = stringResource(R.string.meal_picker_serving_size_hint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(24.dp))

                    // 2. Nutrition Section
                    Text(
                        text = stringResource(R.string.meal_picker_nutrition_facts_serving_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Macros
                    LabelAndInput(
                        label = stringResource(R.string.energy),
                        key = "enercKcal",
                        unit = stringResource(R.string.kcal),
                        allFields = allFields,
                        onValueChange = ::updateField,
                        isHighlight = true
                    )
                    Spacer(Modifier.height(8.dp))

                    LabelAndInput(
                        label = stringResource(R.string.protein),
                        key = "prot",
                        unit = stringResource(R.string.common_unit_g_label),
                        allFields = allFields,
                        onValueChange = ::updateField
                    )

                    // Expandable Fat
                    LabelAndInputWithArrow(
                        label = stringResource(R.string.fats),
                        key = "fat",
                        unit = stringResource(R.string.common_unit_g_label),
                        expanded = expandedFat,
                        onToggle = { expandedFat = !expandedFat },
                        allFields = allFields,
                        onValueChange = ::updateField
                    )
                    if (expandedFat) {
                        SubInput(
                            label = stringResource(R.string.saturated_fat),
                            key = "fasat",
                            unit = stringResource(R.string.common_unit_g_label),
                            allFields = allFields,
                            onValueChange = ::updateField
                        )
                        SubInput(
                            label = stringResource(R.string.trans_fat),
                            key = "fattrans",
                            unit = stringResource(R.string.common_unit_g_label),
                            allFields = allFields,
                            onValueChange = ::updateField
                        )
                    }

                    // Expandable Carbs
                    LabelAndInputWithArrow(
                        label = stringResource(R.string.carbs),
                        key = "chocdf",
                        unit = stringResource(R.string.common_unit_g_label),
                        expanded = expandedCarb,
                        onToggle = { expandedCarb = !expandedCarb },
                        allFields = allFields,
                        onValueChange = ::updateField
                    )
                    if (expandedCarb) {
                        SubInput(
                            label = stringResource(R.string.sugar),
                            key = "sugar",
                            unit = stringResource(R.string.common_unit_g_label),
                            allFields = allFields,
                            onValueChange = ::updateField
                        )
                        SubInput(
                            label = stringResource(R.string.dietary_fiber),
                            key = "fib",
                            unit = stringResource(R.string.common_unit_g_label),
                            allFields = allFields,
                            onValueChange = ::updateField
                        )
                    }

                    // Sodium
                    LabelAndInput(
                        label = stringResource(R.string.sodium),
                        key = "na",
                        unit = stringResource(R.string.common_unit_mg_label),
                        allFields = allFields,
                        onValueChange = ::updateField
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // --- Actions Footer ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val grams = baseGrams ?: return@Button
                            if (grams <= 0.0) return@Button

                            // Convert to per 100g
                            val nutrientsPer100 = nutrients.mapValues { (_, v) ->
                                v * 100.0 / grams
                            }

                            val newItem = UserFoodItem(
                                name = foodName,
                                nutrientsPer100g = nutrientsPer100
                            )
                            scope.launch {
                                val oldFoods = UserFoodStorage.loadUserFoods(context)
                                UserFoodStorage.saveUserFoods(context, oldFoods + newItem)
                                onAdd(newItem)
                            }
                            onDismiss()
                        },
                        enabled = confirmEnabled,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- 辅助 UI 组件 (针对 AddFoodDialog 优化) ---

@Composable
fun LabelAndInput(
    label: String,
    key: String,
    unit: String,
    allFields: Map<String, String>,
    onValueChange: (String, String) -> Unit,
    isHighlight: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // 右侧输入框变小，靠右对齐
        CompactTextField(
            value = allFields[key] ?: "",
            onValueChange = { onValueChange(key, it) },
            modifier = Modifier.width(100.dp), // 固定宽度，整齐
            singleLine = true,
            placeholder = stringResource(R.string.common_zero_hint),
            trailingIcon = {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            textAlign = TextAlign.End, // 数字靠右
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldHeight = 36.dp, // 稍微矮一点
            rounded = 8.dp
        )
    }
}

@Composable
fun LabelAndInputWithArrow(
    label: String,
    key: String,
    unit: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    allFields: Map<String, String>,
    onValueChange: (String, String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Label area is clickable to toggle
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 0f else -90f), // 向下或向右
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        CompactTextField(
            value = allFields[key] ?: "",
            onValueChange = { onValueChange(key, it) },
            modifier = Modifier.width(100.dp),
            singleLine = true,
            placeholder = stringResource(R.string.common_zero_hint),
            trailingIcon = {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            textAlign = TextAlign.End,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldHeight = 36.dp,
            rounded = 8.dp
        )
    }
}

@Composable
fun SubInput(
    label: String,
    key: String,
    unit: String,
    allFields: Map<String, String>,
    onValueChange: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(28.dp)) // 缩进
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // 灰色字
            modifier = Modifier.weight(1f)
        )
        CompactTextField(
            value = allFields[key] ?: "",
            onValueChange = { onValueChange(key, it) },
            modifier = Modifier.width(100.dp),
            singleLine = true,
            placeholder = stringResource(R.string.common_zero_hint),
            trailingIcon = {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            textAlign = TextAlign.End,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldHeight = 36.dp,
            rounded = 8.dp
        )
    }
}

val userKeyToStandardKey = mapOf(
    "enercKcal" to "Energy",
    "prot" to "Protein",
    "fat" to "Total lipid (fat)",
    "chocdf" to "Carbohydrate, by difference"
)
@Composable
fun LabelAndInput(
    label: String,
    key: String,
    unit: String,
    allFields: Map<String, String>,
    onValueChange: (String, String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) { // 恢復垂直居中
        Text(label, modifier = Modifier.weight(1f))
        CompactTextField(
            value = allFields[key] ?: "",
            onValueChange = { onValueChange(key, it) },
            modifier = Modifier.weight(2f),
            singleLine = true,
            label = null,
            textAlign = TextAlign.End,
            trailingIcon = {
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
    Spacer(Modifier.height(8.dp))
}
@Composable
fun ContextHighlightCard(context: FoodContext) {
    val (bgColor, contentColor) = when (context.level) {
        ContextLevel.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ContextLevel.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ContextLevel.WARNING -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = context.icon,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = context.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = context.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun ContextTag(context: FoodContext) {
    val (bgColor, contentColor) = when (context.level) {
        ContextLevel.WARNING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }

    androidx.compose.material3.SuggestionChip(
        onClick = {},
        label = { Text(context.title, style = MaterialTheme.typography.labelSmall) },
        icon = { Text(context.icon, style = MaterialTheme.typography.labelSmall) },
        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
            containerColor = bgColor,
            labelColor = contentColor
        ),
        border = null,
        modifier = Modifier.height(28.dp)
    )
}

@Composable
fun AttributeGauge(label: String, score: Int, highLabel: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Only show the label if it's significant, otherwise just the bar
            if (score >= 70) {
                Text(
                    text = highLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(score)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = getScoreColor(score),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
fun recipe100(recipe: RecipeEntry): Map<String, Double> {
    val totalGram = recipe.totalWeightGram
        ?: recipe.ingredients.sumOf { it.amount }.takeIf { it > 0 }
        ?: 100.0

    return mapOf(
        "Energy" to recipe.kcalPerServing * 100.0 / totalGram,                // kcal/100g
        "Protein" to recipe.protein * 100.0 / totalGram,                      // g/100g
        "Carbohydrate, by difference" to recipe.carbs * 100.0 / totalGram,    // g/100g
        "Total lipid (fat)" to recipe.fats * 100.0 / totalGram                // g/100g
    )
}
@Composable
fun InventoryQuantityDialog(
    inventoryItem: InventoryItem,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    // 1. 計算預設值邏輯
    val defaultGramsStr = remember(inventoryItem) {
        val amount = inventoryItem.quantity.toDouble() // BigDecimal -> Double
        val unit = inventoryItem.unit.lowercase()
        val gramsPerUnit = inventoryItem.gramsPerUnit

        when {
            unit == "g" || unit == "ml" -> amount.toString()

            // 如果單位是 pcs 且知道單顆重量 -> 自動算出總重
            (unit == "pcs" || unit == "piece" || unit == "個") && gramsPerUnit != null -> {
                (amount * gramsPerUnit).toString()
            }

            // 其他情況 (pcs 但不知道多重)，不預填，讓使用者自己打
            else -> ""
        }
    }

    var inputStr by remember { mutableStateOf(defaultGramsStr) }
    var inputError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = inventoryItem.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 顯示庫存來源標記
                Text(
                    text = stringResource(R.string.meal_picker_from_storage_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // --- 庫存資訊卡片 ---
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.meal_picker_stock_remaining_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.meal_picker_stock_amount_desc,
                                inventoryItem.quantity.toString(),
                                inventoryItem.unit
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // 如果有單顆重量資訊，顯示提示
                        if (inventoryItem.unit == "pcs" && inventoryItem.gramsPerUnit != null) {
                            Text(
                                text = stringResource(
                                    R.string.meal_picker_grams_per_piece_desc,
                                    inventoryItem.gramsPerUnit
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- 輸入框 ---
                OutlinedTextField(
                    value = inputStr,
                    onValueChange = {
                        inputStr = it
                        inputError = false
                    },
                    label = { Text(stringResource(R.string.detail_grams)) },
                    isError = inputError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text(stringResource(R.string.common_unit_g_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 警告提示
                if (inventoryItem.unit == "pcs" && inventoryItem.gramsPerUnit == null) {
                    Text(
                        text = stringResource(R.string.meal_picker_pcs_weight_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grams = inputStr.toDoubleOrNull()
                    if (grams != null && grams > 0) {
                        onConfirm(grams)
                    } else {
                        inputError = true
                    }
                }
            ) {
                Text(stringResource(R.string.confirm),color=MaterialTheme.colorScheme.onTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
