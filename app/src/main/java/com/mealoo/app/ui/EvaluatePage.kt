@file:OptIn(ExperimentalMaterial3Api::class)

package com.mealoo.app.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mealoo.app.data.UserPreference
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.mealoo.app.ui.design.MealooTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.api.services.drive.Drive
import com.mealoo.app.R
import com.mealoo.app.data.IntakeDataStore
import com.mealoo.app.data.DiscountEventDataStore
import com.mealoo.app.data.PromptKind
import com.mealoo.app.data.PromptStore
import com.mealoo.app.data.PromptVariable
import com.mealoo.app.db.SavingsRepository
import com.mealoo.app.data.SavedPrompt
import com.mealoo.app.db.ShopRepository
import com.mealoo.app.db.toEntity
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.model.MealPlanEntry
import com.mealoo.app.model.MenuPlanSummary
import com.mealoo.app.ui.design.CompactTextField
import com.mealoo.app.DeepLinkViewModel
import com.mealoo.app.ui.design.MealooCard
import com.mealoo.app.ui.design.NoteTextField
import com.mealoo.app.util.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.URLEncoder
import java.util.UUID
import androidx.compose.animation.core.animateFloat

private enum class EvaluateScreenMode {
    DASHBOARD,
    PROMPT_BUILDER,
    SAVINGS_DASHBOARD,
    SANDBOX,
    LOCAL_LAB
}

@Composable
fun EvaluatePage(
    deepLinkViewModel: DeepLinkViewModel,
    drive: Drive? = null,
    userPref: UserPreference,
    intakeStore: IntakeDataStore,
    savingsRepo: SavingsRepository,
    modifier: Modifier = Modifier,
    storageItems: List<InventoryItem> = emptyList()
) {
    var currentMode by rememberSaveable { mutableStateOf(EvaluateScreenMode.DASHBOARD) }

    LaunchedEffect(Unit) {
        deepLinkViewModel.importEvent.collect { json ->
            Log.i("Mealoo_Debug_Nav", "EvaluatePage: Received import signal. Switching to PROMPT_BUILDER mode.")
            currentMode = EvaluateScreenMode.PROMPT_BUILDER
        }
    }

    BackHandler(enabled = currentMode != EvaluateScreenMode.DASHBOARD) {
        currentMode = EvaluateScreenMode.DASHBOARD
    }

    AnimatedContent(
        targetState = currentMode,
        transitionSpec = {
            if (targetState == EvaluateScreenMode.DASHBOARD) {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(
                    slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()).togetherWith(
                    slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "EvaluatePageNavigation"
    ) {
        mode ->
        when (mode) {
            EvaluateScreenMode.DASHBOARD -> {
                EvaluateDashboard(
                    onNavigate = { currentMode = it }
                )
            }
            EvaluateScreenMode.PROMPT_BUILDER -> {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                val mealPlanRepo = remember { com.mealoo.app.db.MealPlanRepository.getInstance(context.applicationContext) }
                val recipeRepo = remember { com.mealoo.app.data.RecipeRepository(context.applicationContext) }
                val subscriptionRepo = remember(context.applicationContext) { com.mealoo.app.data.repository.SubscriptionRepository() }

                PromptBuildSection(
                    subscriptionRepo = subscriptionRepo,
                    deepLinkViewModel = deepLinkViewModel,
                    storageItems = storageItems,
                    userPref = userPref,
                    onBackToMenu = { currentMode = EvaluateScreenMode.DASHBOARD },
                    onDataParsed = { type, data ->
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                when (type) {
                                    "MealPlanSummary" -> {
                                        val dto = data as com.mealoo.app.data.MealPlanSummaryDto
                                        val newId = java.util.UUID.randomUUID().toString()

                                        // 將 AI 產生的 DTO 轉換為你 UI 在用的 Domain Model (MenuPlanSummary)
                                        val entries = dto.entries.map { e ->
                                            MealPlanEntry(
                                                id = UUID.randomUUID().toString(),
                                                date = "1", // AI 生成的預設先放第一天，使用者可後續在 WeeklyPlanView 調整
                                                mealId = e.mealId,
                                                name = e.name,
                                                source = "AI_MAGIC_INJECT",
                                                sourcePlanId = newId,
                                                grams = 0.0,
                                                servings = 1.0,
                                                kcal = e.kcal,
                                                protein = e.protein,
                                                carbs = e.carbs,
                                                fats = e.fats,
                                                cholesterol = 0.0,
                                                recipeId = null,
                                                inventoryId = null,
                                                cost = BigDecimal.ZERO,
                                                currency = "TWD",
                                                amount = null,
                                                amountUnit = null,
                                                isCompleted = false
                                            )
                                        }
                                        val plan = MenuPlanSummary(
                                            id = newId,
                                            name = dto.name,
                                            description = dto.description,
                                            dayCount = dto.dayCount,
                                            entries = entries
                                        )
                                        mealPlanRepo.savePlan(plan)

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "計畫已儲存至週菜單！", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "RecipeEntry" -> {
                                        val recipe = data as com.mealoo.app.model.RecipeEntry
                                        recipeRepo.addRecipe(recipe.toEntity())

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "食譜已儲存至食譜庫！", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "EatEntry", "DrinkEntry" -> {
                                        val intake = data as com.mealoo.app.model.IntakeEntry
                                        intakeStore.addEntry(intake)

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            Toast.makeText(context, "飲食紀錄已新增！", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "寫入資料庫失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
            EvaluateScreenMode.SAVINGS_DASHBOARD -> {
                val context = LocalContext.current
                val appCtx = context.applicationContext
                val shopRepo = remember(appCtx) { ShopRepository.getInstance(appCtx) }
                val eventStore = remember(appCtx) { DiscountEventDataStore(appCtx) }
                SavingsDashboard(
                    savingsRepo = savingsRepo,
                    shopRepo = shopRepo,
                    eventStore = eventStore,
                    subscriptionRepo = remember(appCtx) { com.mealoo.app.data.repository.SubscriptionRepository() },
                    onBackToMenu = { currentMode = EvaluateScreenMode.DASHBOARD }
                )
            }
            EvaluateScreenMode.SANDBOX -> {
                SandBoxFeature(
                    userPref = userPref,
                    onBack = { currentMode = EvaluateScreenMode.DASHBOARD }
                )
            }
            EvaluateScreenMode.LOCAL_LAB -> {
                LocalLabFeature(
                    onBack = { currentMode = EvaluateScreenMode.DASHBOARD }
                )
            }
        }
    }
}

@Composable
private fun EvaluateDashboard(
    onNavigate: (EvaluateScreenMode) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column {
                Text(
                    text = stringResource(R.string.toolbox),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.toolbox_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            SectionHeader(title = stringResource(R.string.ai_lab), icon = Icons.Filled.AutoAwesome)
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            TechLargeToolCard(
                title = stringResource(R.string.prompt_builder_title),
                description = stringResource(R.string.prompt_builder_desc),
                icon = Icons.Outlined.Inventory2,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigate(EvaluateScreenMode.PROMPT_BUILDER) }
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = stringResource(R.string.analysis_calc), icon = Icons.Filled.Analytics)
        }

        item {
            TechToolCard(
                title = stringResource(R.string.tool_savings_title),
                description = stringResource(R.string.tool_savings_desc),
                icon = Icons.Filled.Savings,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = { onNavigate(EvaluateScreenMode.SAVINGS_DASHBOARD) }
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            TechToolCard(
                title = stringResource(R.string.diet_sandbox),
                description = stringResource(R.string.diet_sandbox_desc),
                icon = Icons.Outlined.Science,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = { onNavigate(EvaluateScreenMode.SANDBOX) }
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            SectionHeader(title = "本地 AI 模型", icon = Icons.Outlined.AutoAwesome)
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            TechLargeToolCard(
                title = "Mealoo 本地 AI 實驗室",
                description = "使用本地端的 AI 模型，離線分析食材以及自訂風味生成食譜。",
                icon = Icons.Filled.AutoAwesome,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigate(EvaluateScreenMode.LOCAL_LAB) }
            )
            Spacer(Modifier.height(88.dp))
        }
        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(Modifier.height(88.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLabFeature(
    onBack: () -> Unit,
    evaluateViewModel: EvaluateViewModel = viewModel()
) {
    val aiLoading by evaluateViewModel.aiLoading.collectAsState()
    val aiResult by evaluateViewModel.aiResult.collectAsState()
    val testInput by evaluateViewModel.testInput.collectAsState()
    val isEngineReady by evaluateViewModel.isEngineReady.collectAsState()
    val isEngineLoading by evaluateViewModel.isEngineLoading.collectAsState()
    val context = LocalContext.current

    val recipes by evaluateViewModel.recipes.collectAsState()
    
    LaunchedEffect(Unit) {
        evaluateViewModel.loadRecipes(context)
    }

    var showMealPicker by remember { mutableStateOf(false) }
    var selectedFoodName by remember { mutableStateOf("") }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var sweet by remember { mutableFloatStateOf(0f) }
    var sour by remember { mutableFloatStateOf(0f) }
    var bitter by remember { mutableFloatStateOf(0f) }
    var spicy by remember { mutableFloatStateOf(0f) }
    var salty by remember { mutableFloatStateOf(0f) }
    var umami by remember { mutableFloatStateOf(0f) }
    var fat by remember { mutableFloatStateOf(0f) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var expandedRecipe by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<com.mealoo.app.model.RecipeEntry?>(null) }
    var missingInput by remember { mutableStateOf("") }
    var useAiAssistant by remember { mutableStateOf(false) }

    if (showMealPicker) {
        MealPickerDialog(
            onDismiss = { showMealPicker = false },
            onConfirm = { foods, _ ->
                if (foods.isNotEmpty()) {
                    val ids = foods.joinToString(",") { it.fdcId?.toString() ?: it.inventoryId ?: it.name }
                    evaluateViewModel.updateTestInput(ids)
                    selectedFoodName = foods.joinToString("、") { stripBrackets(it.name) }
                    evaluateViewModel.updateSelectedRawFoods(foods)
                }
                showMealPicker = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Mealoo 本地 AI 實驗室", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("選擇食材或調配風味，與本地模型共創食譜", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // 引擎狀態
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("步驟 1：初始化引擎", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            isEngineReady -> "✅ 引擎就緒"
                            isEngineLoading -> "⏳ 正在載入中..."
                            else -> "❌ 尚未載入"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEngineReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isEngineLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    Button(
                        onClick = { evaluateViewModel.loadEngine(context) },
                        enabled = !isEngineReady,
                        modifier = Modifier.alpha(if (isEngineReady) 1f else pulseAlpha)
                    ) {
                        Text(if (isEngineReady) "已就緒" else "載入模型")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("從食材生成") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("從風味生成") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("食材替代") })
        }
        
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item {
                if (selectedTab == 0) {
                    // 食材選擇
                    Text("步驟 2：選擇食材", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showMealPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEngineReady
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (!isEngineReady) "請先載入引擎" else if (selectedFoodName.isNotEmpty()) "已選: $selectedFoodName" else "開啟食材選擇器",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { evaluateViewModel.generateFoodRecipe(context, testInput) },
                        enabled = isEngineReady && !aiLoading && testInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("生成中...")
                        } else {
                            Text("生成食譜")
                        }
                    }
                } else if (selectedTab == 1) {
                    // 風味選擇
                    Text("步驟 2：調配風味 (0-5分)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    val flavors = listOf(
                        "甜 (Sweet)" to sweet,
                        "酸 (Sour)" to sour,
                        "苦 (Bitter)" to bitter,
                        "辣 (Spicy)" to spicy,
                        "鹹 (Salty)" to salty,
                        "鮮 (Umami)" to umami,
                        "脂 (Fat)" to fat
                    )
                    val setters = listOf(
                        { v: Float -> sweet = v },
                        { v: Float -> sour = v },
                        { v: Float -> bitter = v },
                        { v: Float -> spicy = v },
                        { v: Float -> salty = v },
                        { v: Float -> umami = v },
                        { v: Float -> fat = v }
                    )

                    flavors.withIndex().forEach { (idx, flavor) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(flavor.first, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = flavor.second,
                                onValueChange = setters[idx],
                                valueRange = 0f..5f,
                                steps = 4,
                                modifier = Modifier.weight(1f)
                            )
                            Text(flavor.second.toInt().toString(), modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val profile = intArrayOf(
                                sweet.toInt(), sour.toInt(), bitter.toInt(),
                                spicy.toInt(), salty.toInt(), umami.toInt(), fat.toInt()
                            )
                            evaluateViewModel.generateFoodRecipeByFlavor(profile)
                        },
                        enabled = isEngineReady && !aiLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("生成中...")
                        } else {
                            Text("按風味生成食譜")
                        }
                    }
                } else {
                    // 食材替代方案
                    Text("步驟 2：選擇食譜與缺少的部分食材", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { expandedRecipe = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEngineReady && recipes.isNotEmpty()
                        ) {
                            Text(selectedRecipe?.name ?: if (recipes.isEmpty()) "目前沒有已儲存的食譜" else "選擇想要製作的食譜", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = expandedRecipe, onDismissRequest = { expandedRecipe = false }) {
                            recipes.forEach { recipe ->
                                DropdownMenuItem(
                                    text = { Text(recipe.name) },
                                    onClick = { selectedRecipe = recipe; expandedRecipe = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = missingInput,
                        onValueChange = { missingInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("請輸入缺少的食材 (例如: 醬油、青蔥)") },
                        enabled = isEngineReady,
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val ingredientsStr = selectedRecipe?.ingredientsText?.joinToString("、") ?: ""
                            evaluateViewModel.generateRecipeSubstitutes(
                                recipeName = selectedRecipe?.name ?: "",
                                recipeIngredients = ingredientsStr,
                                missing = missingInput
                            )
                        },
                        enabled = isEngineReady && !aiLoading && selectedRecipe != null && missingInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("生成中...")
                        } else {
                            Text("向 AI 尋求替代方案")
                        }
                    }
                }
            }

            item {
                if (aiLoading) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("AI 正在生成食譜，請稍候...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (aiResult.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))

                    // 🚀 核心優化：大括號掃描器，從字串中萃取所有 JSON 物件並轉換為 RecipeEntry 結構
                    val parsedRecipes = remember(aiResult) {
                        val list = mutableListOf<com.mealoo.app.model.RecipeEntry>()
                        var bracketCount = 0
                        var startIndex = -1
                        val format = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
                        for (i in aiResult.indices) {
                            if (aiResult[i] == '{') {
                                if (bracketCount == 0) startIndex = i
                                bracketCount++
                            } else if (aiResult[i] == '}') {
                                bracketCount--
                                if (bracketCount == 0 && startIndex != -1) {
                                    try {
                                        val jsonString = aiResult.substring(startIndex, i + 1)
                                        var parsedRecipe = format.decodeFromString<com.mealoo.app.model.RecipeEntry>(jsonString)

                                        // 🚀 步驟 1：廚藝數學分配引擎 (重新計算克數以供預覽)
                                        val TARGET_TOTAL_GRAMS = 400.0
                                        val ROLE_WEIGHTS = mapOf(
                                            "MAIN_CARB" to 40.0, "MAIN_PROTEIN" to 30.0, "SIDE_VEG" to 20.0,
                                            "LIQUID" to 6.0, "OIL" to 3.0, "DRY_SPICE" to 1.0
                                        )

                                        val parsedRoles = parsedRecipe.ingredients.map { ing ->
                                            var role = "UNKNOWN"
                                            var cleanName = ing.name
                                            if (cleanName.contains(":")) {
                                                role = cleanName.substringBefore(":").trim().uppercase()
                                                cleanName = cleanName.substringAfter(":").trim()
                                            } else if (cleanName.contains("：")) {
                                                role = cleanName.substringBefore("：").trim().uppercase()
                                                cleanName = cleanName.substringAfter("：").trim()
                                            }
                                            Triple(role, cleanName, ing)
                                        }

                                        val activeRoles = parsedRoles.map { it.first }.toSet()
                                        val activeTotalWeight = activeRoles.sumOf { ROLE_WEIGHTS[it] ?: 0.0 }.takeIf { it > 0.0 } ?: 1.0
                                        val roleCounts = parsedRoles.groupingBy { it.first }.eachCount()

                                        val updatedIngredients = parsedRoles.map { (role, _, ing) ->
                                            val roleWeight = ROLE_WEIGHTS[role]
                                            val newGrams = if (roleWeight != null) {
                                                val allocatedForRole = (roleWeight / activeTotalWeight) * TARGET_TOTAL_GRAMS
                                                val finalGrams = allocatedForRole / (roleCounts[role] ?: 1)
                                                if (role == "DRY_SPICE" || role == "OIL") {
                                                    Math.round(finalGrams * 10.0) / 10.0
                                                } else {
                                                    Math.round(finalGrams).toDouble()
                                                }
                                            } else {
                                                ing.amount.coerceIn(5.0, 150.0)
                                            }
                                            ing.copy(amount = newGrams)
                                        }

                                        parsedRecipe = parsedRecipe.copy(ingredients = updatedIngredients)
                                        list.add(parsedRecipe)
                                    } catch (e: Exception) { }
                                    startIndex = -1
                                }
                            }
                        }
                        list
                    }

                    if (parsedRecipes.isNotEmpty()) {
                        // 針對抓出來的每一個食譜產生一張 Card
                        parsedRecipes.forEachIndexed { index, recipe ->
                            val rName = recipe.name.ifBlank { "未命名食譜 ${index + 1}" }
                            val rDesc = recipe.description ?: ""
                            val rTime = recipe.timeRequired ?: 0
                            val rIngs = recipe.ingredients
                            val rSteps = recipe.steps

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // --- 標題區 ---
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = rName.ifBlank { "未命名食譜 ${index + 1}" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (rDesc.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(text = rDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (rTime > 0) {
                                        Spacer(Modifier.height(8.dp))
                                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text(text = "⏱️ $rTime 分鐘", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }

                                    if (rIngs.isNotEmpty() || rSteps.isNotEmpty()) {
                                        Spacer(Modifier.height(16.dp))
                                        HorizontalDivider()
                                        Spacer(Modifier.height(16.dp))
                                    }

                                    // --- 食材清單區 ---
                                    if (rIngs.isNotEmpty()) {
                                        Text(text = "食材清單", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(8.dp))

                                        rIngs.forEach { ing ->
                                            val cleanName = ing.name.substringAfter(":").substringAfter("：").trim()
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = cleanName.ifBlank { "未知食材" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                // 過濾掉未設定的 0 克
                                                if (ing.amount > 0.0) {
                                                    val amountStr = if (ing.amount % 1.0 == 0.0) ing.amount.toInt().toString() else ing.amount.toString()
                                                    Text(text = "$amountStr 克", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }

                                    // --- 步驟清單區 ---
                                    if (rSteps.isNotEmpty()) {
                                        Text(text = "烹飪步驟", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(8.dp))

                                        rSteps.forEachIndexed { i, step ->
                                            Row(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(text = "${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Text(text = step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), lineHeight = 20.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- 底部操作區 (所有食譜共用寫入按鈕) ---
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("模型輔助補全", style = MaterialTheme.typography.labelMedium)
                                            Spacer(Modifier.width(8.dp))
                                            Switch(
                                                checked = useAiAssistant,
                                                onCheckedChange = { useAiAssistant = it },
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                        Text("使用 AI 修正俗名食材數據", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            evaluateViewModel.saveGeneratedRecipe(context, selectedTab, selectedRecipe, useAiAssistant)
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("寫入 Recipe")
                                    }
                                }
                            }
                        }

                    } else {
                        // 完全找不到 JSON 時，直接顯示純文字
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiResult,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(56.dp))
            }
            item{
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechLargeToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            // 使用帶有透明度的底色營造層次
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        // 科技風核心：細邊框與強調色
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 局部漸層背景裝飾（可選，增加科技感）
        Box(
            modifier = Modifier.background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 12.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
        }
    }
}

fun getGradeColor(grade: String): Color = when (grade) {
    "A" -> Color(0xFF4CAF50)
    "B" -> Color(0xFF8BC34A)
    "C" -> Color(0xFFFFC107)
    "D" -> Color(0xFFFF9800)
    "E" -> Color(0xFFF44336)
    else -> Color.Gray
}

fun getScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

enum class PromptBuildTool {
    MENU,
    PROMPT_LIBRARY,
    AI_PROMPT,
    MAGIC_INJECT
}
@Composable
fun PromptBuildSection(
    subscriptionRepo: com.mealoo.app.data.repository.SubscriptionRepository,
    deepLinkViewModel: DeepLinkViewModel,
    storageItems: List<InventoryItem>,
    userPref: UserPreference?,
    modifier: Modifier = Modifier,
    onBackToMenu: (() -> Unit)? = null,
    onDataParsed: (String, Any) -> Unit
) {
    var currentSubTool by remember { mutableStateOf(PromptBuildTool.MENU) }

    val context = LocalContext.current
    val promptStore = remember { PromptStore(context) }
    LaunchedEffect(Unit) {
        deepLinkViewModel.importEvent.collect { json ->
            Log.i("Mealoo_Debug_Nav", "PromptBuildSection: Switching to MAGIC_INJECT sub-tool.")
            currentSubTool = PromptBuildTool.MAGIC_INJECT
        }
    }
    Box(modifier = modifier) {
        val canHandleBack = currentSubTool != PromptBuildTool.MENU || onBackToMenu != null
        BackHandler(enabled = canHandleBack) {
            when (currentSubTool) {
                PromptBuildTool.MENU -> onBackToMenu?.invoke()
                else -> currentSubTool = PromptBuildTool.MENU
            }
        }

        when (currentSubTool) {

            PromptBuildTool.MENU -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBackToMenu != null) {
                            IconButton(onClick = onBackToMenu) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(start = if (onBackToMenu != null) 4.dp else 0.dp)) {
                            Text(
                                text = stringResource(R.string.prompt_builder_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.prompt_builder_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            TechToolCard(
                                title = stringResource(R.string.ai_Prompt),
                                description = stringResource(R.string.ai_Prompt_desc),
                                icon = Icons.Filled.Restaurant,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = { currentSubTool = PromptBuildTool.AI_PROMPT }
                            )
                        }
                        item {
                            TechToolCard(
                                title = stringResource(R.string.magic_import),
                                description = stringResource(R.string.ai_paste_sync_to_database),
                                icon = Icons.Filled.AutoAwesome,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                onClick = { currentSubTool = PromptBuildTool.MAGIC_INJECT }
                            )
                        }
                        item {
                            TechToolCard(
                                title = stringResource(R.string.prompt_library),
                                description = stringResource(R.string.prompt_library_desc),
                                icon = Icons.Filled.Edit,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = { currentSubTool = PromptBuildTool.PROMPT_LIBRARY }
                            )
                        }
                    }
                }
            }

            // ================= Prompt 模板庫 =================
            PromptBuildTool.PROMPT_LIBRARY -> {
                PromptLibraryFeature(
                    promptStore = promptStore,
                    subscriptionRepo = subscriptionRepo,
                    onBack = { currentSubTool = PromptBuildTool.MENU }
                )
            }
            PromptBuildTool.MAGIC_INJECT -> {
                MagicInjectFeature(
                    deepLinkViewModel = deepLinkViewModel,
                    onBack = { currentSubTool = PromptBuildTool.MENU },
                    onDataParsed = onDataParsed
                )
            }

            PromptBuildTool.AI_PROMPT -> {
                AiInjectFeature(
                    storageItems = storageItems,
                    userPref = userPref,
                    promptStore = promptStore,
                    onBack = { currentSubTool = PromptBuildTool.MENU },
                    onRecipeParsed = { _ ->
                    }
                )
            }
        }
    }
}
@Composable
fun MagicInjectFeature(
    deepLinkViewModel: DeepLinkViewModel,
    onBack: () -> Unit,
    onDataParsed: (String, Any) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var jsonInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var failedRawJson by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        deepLinkViewModel.importEvent.collect { json ->
            jsonInput = json
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_magic_inject), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            InjectorSection(
                jsonInput = jsonInput,
                onJsonChange = {
                    jsonInput = it
                    parseError = null
                    failedRawJson = null
                },
                parseError = parseError,
                onPaste = {
                    val clipboardText = clipboardManager.getText()?.text
                    if (!clipboardText.isNullOrBlank()) {
                        jsonInput = clipboardText
                        parseError = null
                        failedRawJson = null
                    }
                },
                onInject = {
                    val result = tryParseAndInject(context, jsonInput)
                    if (result.isSuccess) {
                        val parsedData = result.parsedData
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        jsonInput = ""
                        failedRawJson = null

                        if (parsedData != null && result.type != null) {
                            onDataParsed(result.type, parsedData)
                        }
                    }else {
                        parseError = result.message
                        failedRawJson = result.payload
                    }
                }
            )

            // 內嵌式智慧修正區塊 (取代容易 Crash 的 Dialog)
            AnimatedVisibility(visible = failedRawJson != null && parseError != null) {
                InlineJsonCorrectionSection(
                    initialJson = failedRawJson ?: "",
                    errorMessage = parseError ?: "",
                    onCancel = {
                        failedRawJson = null
                        parseError = null
                    },
                    onRetry = { correctedJson ->
                        // 將修正後的 JSON 填回輸入框並自動清除錯誤狀態，讓使用者再次點擊「注入」或我們直接幫他再測一次
                        jsonInput = correctedJson
                        failedRawJson = null
                        parseError = null
                        Toast.makeText(context, "已套用修正，請再次點擊匯入", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun InlineJsonCorrectionSection(
    initialJson: String,
    errorMessage: String,
    onCancel: () -> Unit,
    onRetry: (String) -> Unit
) {
    // 1. 智慧攔截：從 SerializationException 中抓取缺失的欄位名稱
    val missingFields = remember(errorMessage) {
        val regex = Regex("Field '(.*?)' is required")
        regex.findAll(errorMessage).map { it.groupValues[1] }.toSet().toList()
    }

    // 儲存動態生成的 TextField 內容
    var dynamicInputs by remember(missingFields) {
        mutableStateOf(missingFields.associateWith { "" })
    }

    // 兜底用的完整 JSON 編輯器
    var editableJson by remember(initialJson) { mutableStateOf(initialJson) }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "解析失敗，請補齊資訊",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

            // 策略 A：如果有明確抓到缺失欄位，提供各別的 TextField
            if (missingFields.isNotEmpty()) {
                Text("快速補齊缺失欄位：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)

                missingFields.forEach { field ->
                    CompactTextField(
                        value = dynamicInputs[field] ?: "",
                        onValueChange = { newVal ->
                            dynamicInputs = dynamicInputs.toMutableMap().apply { put(field, newVal) }
                        },
                        label = "補齊 '$field'",
                        placeholder = "輸入數值或文字",
                        containerColor = MaterialTheme.colorScheme.surface,
                        showBorder = true
                    )
                }

                Button(
                    onClick = {
                        try {
                            // 嘗試動態將這些欄位塞進原 JSON 的最外層 Object 中
                            val element = kotlinx.serialization.json.Json.parseToJsonElement(initialJson)
                            if (element is kotlinx.serialization.json.JsonObject) {
                                val map = element.toMutableMap()
                                dynamicInputs.forEach { (key, value) ->
                                    val num = value.toDoubleOrNull()
                                    if (num != null) {
                                        map[key] = kotlinx.serialization.json.JsonPrimitive(num)
                                    } else {
                                        map[key] = kotlinx.serialization.json.JsonPrimitive(value)
                                    }
                                }
                                onRetry(kotlinx.serialization.json.JsonObject(map).toString())
                                return@Button
                            }
                        } catch (e: Exception) {
                            // 萬一 JSON 連解析成 JsonElement 都失敗，直接忽略，讓使用者用下方的完整編輯器
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("自動補齊並重試")
                }

                Spacer(Modifier.height(8.dp))
                Text("或手動編輯原始 JSON：", style = MaterialTheme.typography.labelMedium)
            }

            // 策略 B：兜底用的完整 NoteTextField (無論如何都顯示，以防巢狀結構錯誤無法用簡單 KV 補齊)
            NoteTextField(
                value = editableJson,
                onValueChange = { editableJson = it },
                label = null,
                placeholder = "{ ... }",
                minHeight = 150.dp,
                maxHeight = 250.dp,
                showCounter = false,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onRetry(editableJson) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (missingFields.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("儲存 JSON 並重試")
                }
            }
        }
    }
}
@Composable
fun PromptLibraryFeature(
    promptStore: PromptStore,
    subscriptionRepo: com.mealoo.app.data.repository.SubscriptionRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPrompts by promptStore.promptsFlow().collectAsState(initial = emptyList())
    val isPro by subscriptionRepo.isPro.collectAsState()

    // 10 prompts limit for free users
    val PROMPT_LIMIT = 10
    val limitReached = !isPro && savedPrompts.size >= PROMPT_LIMIT

    // UI 狀態
    var showEditDialog by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf<SavedPrompt?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.prompt_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.prompt_help_step1))
                    Text(stringResource(R.string.prompt_help_step2))
                    Text(stringResource(R.string.prompt_help_step3))
                    Text(stringResource(R.string.prompt_help_step4))
                    Text(stringResource(R.string.prompt_help_step5))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(R.string.prompt_help_confirm)) }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (limitReached) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "免費版上限: $PROMPT_LIMIT/ $PROMPT_LIMIT (請升級Pro)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = {
                        if (limitReached) {
                            Toast.makeText(context, "已達免費用戶 $PROMPT_LIMIT 則上限，請解鎖 Pro 或刪除舊提示詞。", Toast.LENGTH_LONG).show()
                        } else {
                            editingPrompt = null
                            showEditDialog = true
                        }
                    },
                    containerColor = if (limitReached) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (limitReached) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 88.dp),
                    shape = CircleShape
                ) { 
                    Icon(Icons.Filled.Edit, stringResource(R.string.add_prompt_description))
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // 只留一點點呼吸空間即可

            // --- Header 區域 ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.prompt_inspiration_library),
                    style = MaterialTheme.typography.titleLarge, // 🔥 微調：標題改稍微小一點點的 titleLarge 讓比例更好，或維持 headlineMedium
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    scope.launch {
                        DefaultPrompts.getAllDefaults(context = context).forEach { promptStore.upsert(it) }
                        Toast.makeText(context, context.getString(R.string.toast_defaults_restored), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Outlined.CloudDone, stringResource(R.string.restore_defaults_description), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help_description), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 🔥 修改 3: 這裡原本是 24.dp，改為 12.dp，讓列表離標題近一點
            Spacer(modifier = Modifier.height(12.dp))

            // --- 瀑布流內容區 ---
            if (savedPrompts.isEmpty()) {
                EmptyLibraryState(onLoadDefaults = {
                    scope.launch { DefaultPrompts.getAllDefaults(context = context).forEach { promptStore.upsert(it) } }
                })
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 68.dp)
                ) {
                    items(savedPrompts, key = { it.id }) {
                        prompt ->
                        TechPromptCard(
                            prompt = prompt,
                            onEdit = {
                                editingPrompt = prompt
                                showEditDialog = true
                            },
                            onDelete = {
                                scope.launch { promptStore.delete(prompt.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    // ... Dialog 保持不變 ...
    if (showEditDialog) {
        PromptEditDialog(
            initialPrompt = editingPrompt,
            onDismiss = { showEditDialog = false },
            onConfirm = { newPrompt ->
                scope.launch { promptStore.upsert(newPrompt) }
                showEditDialog = false
            }
        )
    }
}

// 🎨 色彩主題定義 helper
data class PromptThemeColor(val container: Color, val onContainer: Color)

@Composable
fun getPromptTheme(kind: PromptKind): PromptThemeColor {
    // 使用 Material Theme 的變體顏色，或自定義柔和色系
    return when (kind) {
        PromptKind.INVENTORY_RECIPE -> PromptThemeColor(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        ) // 暖色/食物
        PromptKind.INVENTORY_WEEKLY_PLAN -> PromptThemeColor(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        ) // 冷色/規劃
        PromptKind.INVENTORY_SHOPPING_LIST -> PromptThemeColor(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        ) // 主色/清單
        PromptKind.GENERIC -> PromptThemeColor(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        ) // 灰色/通用
    }
}

@Composable
fun TechPromptCard(
    prompt: SavedPrompt,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val theme = getPromptTheme(prompt.kind)
    val accentColor = theme.onContainer

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 標籤 Chip：保留微型化，改為外框線條或低透明度底
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = getKindLabel(prompt.kind),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // 統一用標準文字色，確保易讀
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = prompt.body,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun EmptyLibraryState(onLoadDefaults: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CloudQueue,
                null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.empty_library_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onLoadDefaults,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.load_default_templates))
            }
        }
    }
}

// Helper: 翻譯 Kind
@Composable
fun getKindLabel(kind: PromptKind): String {
    return when(kind) {
        PromptKind.GENERIC -> stringResource(R.string.prompt_kind_generic)
        PromptKind.INVENTORY_RECIPE -> stringResource(R.string.prompt_kind_recipe)
        PromptKind.INVENTORY_WEEKLY_PLAN -> stringResource(R.string.prompt_kind_weekly)
        PromptKind.INVENTORY_SHOPPING_LIST -> stringResource(R.string.prompt_kind_shopping)
    }
}
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PromptEditDialog(
    initialPrompt: SavedPrompt?,
    onDismiss: () -> Unit,
    onConfirm: (SavedPrompt) -> Unit
) {
    val context = LocalContext.current
    // 1. 狀態管理：使用 TextFieldValue 控制游標
    var title by remember { mutableStateOf(initialPrompt?.title ?: "") }
    var bodyState by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialPrompt?.body ?: "",
                selection = androidx.compose.ui.text.TextRange(initialPrompt?.body?.length ?: 0)
            )
        )
    }

    // 類型選擇與變數
    var selectedKind by remember { mutableStateOf(initialPrompt?.kind ?: PromptKind.GENERIC) }
    var expandKindMenu by remember { mutableStateOf(false) }
    val allVariables = remember { PromptVariable.values().toList() }

    val mockDataMap = mapOf(
        PromptVariable.INVENTORY_BLOCK to context.getString(R.string.mock_data_inventory),
        PromptVariable.SERVINGS to context.getString(R.string.mock_data_servings),
        PromptVariable.EXTRA_NOTE to context.getString(R.string.mock_data_extra_note),
        PromptVariable.GOAL to context.getString(R.string.mock_data_goal),
        PromptVariable.DIET_TYPE to context.getString(R.string.mock_data_diet_type),
        PromptVariable.MACRO_PREF_BLOCK to context.getString(R.string.mock_data_macro_pref),
        PromptVariable.LIFESTYLE_BLOCK to context.getString(R.string.mock_data_lifestyle),
        PromptVariable.TODAY_DATE to context.getString(R.string.mock_data_today),
        PromptVariable.WEIGHT_KG to context.getString(R.string.mock_data_weight),
        PromptVariable.HEIGHT_CM to context.getString(R.string.mock_data_height),
        PromptVariable.AGE to context.getString(R.string.mock_data_age),
        PromptVariable.DAILY_KCAL_TARGET to context.getString(R.string.mock_data_kcal_target),
        PromptVariable.ACTIVITY_LEVEL to context.getString(R.string.mock_data_activity_level),
        PromptVariable.MEAL_RATIO_MAP to context.getString(R.string.mock_data_meal_ratio),
    )

    // 3. 語法高亮轉換器 (Syntax Highlighting)
    val variableSyntaxHighlighter = remember {
        VisualTransformation { text ->
            val builder = AnnotatedString.Builder()
            val rawText = text.text
            val regex = Regex("\\{\\{[A-Z_]+\\}\\}") // 匹配 {{VAR_NAME}}
            var lastIndex = 0

            regex.findAll(rawText).forEach { matchResult ->
                // 一般文字
                builder.append(rawText.substring(lastIndex, matchResult.range.first))

                // 變數高亮 (藍色 + 粗體)
                builder.withStyle(
                    SpanStyle(
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    builder.append(matchResult.value)
                }

                lastIndex = matchResult.range.last + 1
            }
            // 剩餘文字
            if (lastIndex < rawText.length) {
                builder.append(rawText.substring(lastIndex))
            }

            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }
    }

    // 4. 即時預覽產生器 (Live Preview Generator)
    val previewText = remember(bodyState.text) {
        var processed = bodyState.text
        allVariables.forEach { variable ->
            val token = "{{${variable.name}}}"
            // 這裡模擬您想要的格式： {VAR: MockData}
            val mockValue = mockDataMap[variable] ?: "..."
            // 將 {{VAR}} 替換為 {VAR: value} 顯示在預覽中
            processed = processed.replace(token, "{${variable.name}: $mockValue}")
        }
        processed
    }

    // 插入變數 helper
    fun insertVariable(variable: PromptVariable) {
        val currentText = bodyState.text
        val selection = bodyState.selection
        val token = "{{${variable.name}}}"
        val newText = currentText.replaceRange(selection.start, selection.end, token)
        bodyState = androidx.compose.ui.text.input.TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(selection.start + token.length)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                if (initialPrompt == null) stringResource(R.string.add_template)
                else stringResource(R.string.edit_template),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp) // 稍微加高以容納預覽
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Metadata 區塊 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MealooTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = stringResource(R.string.title_label),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // --- 變數工具列 ---
                Column {
                    Text(
                        text = stringResource(R.string.insert_variable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allVariables) { variable ->
                            SuggestionChip(
                                onClick = { insertVariable(variable) },
                                label = { Text(variable.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                                ),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "附加輸出格式約束 (JSON Schema)", // 建議抽到 strings.xml
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(OutputFormatConstraint.entries) { format ->
                            AssistChip(
                                onClick = {
                                    val currentText = bodyState.text
                                    val newText = if (currentText.endsWith("\n")) {
                                        currentText + "\n" + format.schemaTemplate
                                    } else {
                                        currentText + "\n\n" + format.schemaTemplate
                                    }
                                    bodyState = androidx.compose.ui.text.input.TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(newText.length)
                                    )
                                },
                                label = { Text(format.label) },
                                leadingIcon = {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                                )
                            )
                        }
                    }
                }

                MealooTextField(
                    value = bodyState,
                    onValueChange = { bodyState = it },
                    label = stringResource(R.string.prompt_edit_mode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    singleLine = false,
                    visualTransformation = variableSyntaxHighlighter
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.prompt_preview_title_sim),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        Text(
                            text = if (previewText.isBlank()) stringResource(R.string.prompt_preview_placeholder) else previewText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val usedVars = PromptVariable.values()
                        .filter { bodyState.text.contains("{{${it.name}}}") }
                        .toSet()

                    val newId = initialPrompt?.id ?: java.util.UUID.randomUUID().toString()
                    onConfirm(
                        SavedPrompt(
                            id = newId,
                            title = title.ifBlank { context.getString(R.string.untitled_prompt) },
                            body = bodyState.text,
                            kind = selectedKind,
                            variables = usedVars
                        )
                    )
                },
                enabled = title.isNotBlank() && bodyState.text.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInjectFeature(
    storageItems: List<InventoryItem>,
    userPref: UserPreference?,
    promptStore: PromptStore,
    onBack: () -> Unit,
    onRecipeParsed: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // --- State Management ---
    var selectedInventoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastTemplateId by remember { mutableStateOf<String?>(null) }

    // Load Prompts
    val savedPrompts by promptStore.promptsFlow().collectAsState(initial = DefaultPrompts.getAllDefaults(context = context))
    var selectedTemplate by remember { mutableStateOf<SavedPrompt?>(null) }

    // Input States
    var extraNote by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(1) }

    // Json Inject State
    var jsonInput by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }

    // --- Logic & Derived State ---
    val requirements = remember(selectedTemplate) {
        selectedTemplate?.let { resolvePromptRequirements(it) } ?: emptySet()
    }
    val needsInventory = requirements.contains(PromptInputRequirement.InventoryBlock)

    // Select first template by default
    LaunchedEffect(savedPrompts) {
        if (selectedTemplate == null && savedPrompts.isNotEmpty()) {
            selectedTemplate = savedPrompts.first()
        }
    }

    // Auto-select inventory logic
    LaunchedEffect(selectedTemplate?.id, storageItems) {
        val tmpl = selectedTemplate
        val allIds = storageItems.map { it.id }.toSet()

        if (!needsInventory) {
            selectedInventoryIds = emptySet()
            lastTemplateId = tmpl?.id
            return@LaunchedEffect
        }
        if (tmpl?.id != null && tmpl.id != lastTemplateId) {
            selectedInventoryIds = allIds
            lastTemplateId = tmpl.id
            return@LaunchedEffect
        }
        val shrunk = selectedInventoryIds.intersect(allIds)
        selectedInventoryIds = if (shrunk.isEmpty() && allIds.isNotEmpty()) allIds else shrunk
        lastTemplateId = tmpl?.id
    }

    // Generate Prompt String
    val generatedPrompt by remember(
        selectedTemplate, userPref, extraNote, servings,
        storageItems, selectedInventoryIds, needsInventory
    ) {
        derivedStateOf {
            val tmpl = selectedTemplate ?: return@derivedStateOf ""
            val pref = userPref ?: return@derivedStateOf context.getString(R.string.pref_not_loaded)

            val invForPrompt = if (needsInventory) {
                storageItems.filter { selectedInventoryIds.contains(it.id) }
            } else {
                storageItems
            }

            val basePrompt = PromptFillerEngine.fill(
                template = tmpl,
                inventory = invForPrompt,
                userPref = pref,
                inputs = PromptRuntimeInputs(servings = servings, extraNote = extraNote)
            )

            basePrompt
        }
    }

    // Validation
    val servingsError = remember(requirements, servings) {
        if (requirements.contains(PromptInputRequirement.Servings) && servings <= 0) context.getString(R.string.error_servings_positive) else null
    }
    val inventoryError = remember(needsInventory, storageItems, selectedInventoryIds) {
        if (needsInventory && storageItems.isNotEmpty() && selectedInventoryIds.isEmpty()) context.getString(R.string.error_select_inventory) else null
    }
    val canCopy = selectedTemplate != null && userPref != null && servingsError == null && inventoryError == null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_magic_inject), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) {
        innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GeneratorSection(
                savedPrompts = savedPrompts,
                selectedTemplate = selectedTemplate,
                onTemplateSelect = { selectedTemplate = it },
                requirements = requirements,
                userPref = userPref,
                storageItems = storageItems,
                selectedInventoryIds = selectedInventoryIds,
                onSelectedInventoryIdsChange = { selectedInventoryIds = it },
                servings = servings,
                onServingsChange = { servings = it },
                extraNote = extraNote,
                onExtraNoteChange = { extraNote = it },
                servingsError = servingsError,
                inventoryError = inventoryError,
                generatedPrompt = generatedPrompt,
                canCopy = canCopy,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(generatedPrompt))
                    Toast.makeText(context, context.getString(R.string.toast_prompt_copied), Toast.LENGTH_SHORT).show()

                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://gemini.google.com/app")
                        )
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

                        context.startActivity(intent)

                    } catch (e: android.content.ActivityNotFoundException) {
                        e.printStackTrace() // 會印在 System.err
                        Toast.makeText(context, context.getString(R.string.error_no_browser), Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        // 其他錯誤
                        e.printStackTrace()
                        // 用 System.out.println 繞過 Logcat 過濾器
                        println("MEALOO_ERROR_TAG: 無法開啟網頁: $e")
                        Toast.makeText(context, "開啟失敗: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}
// --- Sub-Components for Clean Layout ---

@Composable
private fun GeneratorSection(
    savedPrompts: List<SavedPrompt>,
    selectedTemplate: SavedPrompt?,
    onTemplateSelect: (SavedPrompt) -> Unit,
    requirements: Set<PromptInputRequirement>,
    userPref: UserPreference?,
    storageItems: List<InventoryItem>,
    selectedInventoryIds: Set<String>,
    onSelectedInventoryIdsChange: (Set<String>) -> Unit,
    servings: Int,
    onServingsChange: (Int) -> Unit,
    extraNote: String,
    onExtraNoteChange: (String) -> Unit,
    servingsError: String?,
    inventoryError: String?,
    generatedPrompt: String,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ){
        Column(modifier = Modifier.padding(16.dp)) {
            PromptTypeSelector(
                prompts = savedPrompts,
                selected = selectedTemplate,
                onSelect = onTemplateSelect
            )

            Spacer(Modifier.height(8.dp))

            DynamicInputBlock(
                requirements = requirements,
                userPref = userPref,
                template = selectedTemplate,
                storageItems = storageItems,
                selectedInventoryIds = selectedInventoryIds,
                onSelectedInventoryIdsChange = onSelectedInventoryIdsChange,
                servings = servings,
                onServingsChange = onServingsChange,
                extraNote = extraNote,
                onExtraNoteChange = onExtraNoteChange,
                servingsError = servingsError,
                inventoryError = inventoryError
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(generatedPrompt))

                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, generatedPrompt) // 傳入 Prompt
                            type = "text/plain"
                            setPackage("com.google.android.apps.bard") // 強制指定給 Gemini App
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        try {
                            context.startActivity(sendIntent)
                            Toast.makeText(context, context.getString(R.string.toast_sent_to_gemini), Toast.LENGTH_SHORT).show()
                        } catch (e: android.content.ActivityNotFoundException) {
                            Log.e("MealooDebug", "未安裝或找不到 Gemini App: $e")
                            Toast.makeText(context, context.getString(R.string.toast_gemini_not_installed), Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = true, // 如果要嚴謹點，可以改為 enabled = generatedPrompt.isNotBlank()
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)),
                ) {
                    Text(stringResource(R.string.open_gemini_app), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = {
                        val tag = "Mealoo_Outbound"
                        Log.d(tag, "====== 準備送往 Gemini ======")

                        // 0. 兜底：先寫入剪貼簿，確保無論如何使用者都能手動貼上
                        clipboardManager.setText(AnnotatedString(generatedPrompt))

                        // 1. 首選策略：透過 ACTION_SEND 喚醒 Gemini App
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, generatedPrompt)
                            type = "text/plain"
                            setPackage("com.google.android.apps.bard") // 指定 Gemini App
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        try {
                            Log.d(tag, "嘗試使用 ACTION_SEND 開啟 Gemini App...")
                            context.startActivity(sendIntent)
                            Toast.makeText(context, "已傳送至 Gemini App", Toast.LENGTH_SHORT).show()
                        } catch (e: android.content.ActivityNotFoundException) {
                            Log.w(tag, "未安裝 Gemini App，啟動 Web Fallback")

                            // 2. 備用策略：沒裝 App，退回瀏覽器網頁版
                            val encodedPrompt = try {
                                java.net.URLEncoder.encode(generatedPrompt, "UTF-8")
                            } catch (ex: Exception) { "" }

                            // 檢查 URL 安全長度 (瀏覽器 URL 限制約 2000 字元)
                            val targetUrl = if (encodedPrompt.length < 1800) {
                                "https://gemini.google.com/app?q=$encodedPrompt"
                            } else {
                                Log.w(tag, "Prompt 太長，放棄 URL 帶入，純開網頁")
                                "https://gemini.google.com/app"
                            }

                            val webIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(targetUrl)
                            )
                            webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

                            try {
                                context.startActivity(webIntent)
                                if (encodedPrompt.length >= 1800) {
                                    Toast.makeText(context, "內容過長已複製，請手動貼上", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "已開啟 Gemini 網頁版", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e2: Exception) {
                                Log.e(tag, "Web Fallback 失敗", e2)
                                Toast.makeText(context, "無法開啟瀏覽器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("送往 Gemini", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun InjectorSection(
    jsonInput: String,
    onJsonChange: (String) -> Unit,
    parseError: String?,
    onPaste: () -> Unit,
    onInject: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = jsonInput,
                onValueChange = onJsonChange,
                label = { Text(stringResource(R.string.paste_json_label)) },
                placeholder = { Text(stringResource(R.string.json_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                ),
                trailingIcon = {
                    if (jsonInput.isEmpty()) {
                        IconButton(onClick = onPaste) {
                            Icon(Icons.Default.ContentPaste, "Paste")
                        }
                    } else {
                        IconButton(onClick = { onJsonChange("") }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }
                }
            )

            if (parseError != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(parseError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onInject,
                modifier = Modifier.fillMaxWidth(),
                enabled = jsonInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.magic_import))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.supported_types),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
data class InjectionResult(
    val isSuccess: Boolean,
    val message: String,
    val type: String? = null,
    val payload: String? = null,
    val parsedData: Any? = null
)

private fun tryParseAndInject(context: android.content.Context, jsonRaw: String): InjectionResult {
    return when (val result = JsonSanitizer.parseAiResponse(jsonRaw)) {
        is AiParseResult.Success -> {
            val typeStr = result.typeStr
            val cleanJson = result.rawJson

            val message = when (typeStr) {
                "EatEntry" -> context.getString(R.string.success_eat_entry)
                "DrinkEntry" -> context.getString(R.string.success_drink_entry)
                "RecipeEntry" -> context.getString(R.string.success_recipe_entry)
                "MealPlanSummary" -> context.getString(R.string.success_meal_plan_summary)
                else -> context.getString(R.string.success_unknown_entry_fmt, typeStr)
            }

            InjectionResult(
                isSuccess = true,
                message = message,
                type = typeStr,
                payload = cleanJson,
                parsedData = result.data
            )
        }
        is AiParseResult.Failure -> {
            InjectionResult(
                isSuccess = false,
                message = context.getString(R.string.error_json_parse_failed, result.errorMessage),
                payload = result.rawJson
            )
        }
    }
}
fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}