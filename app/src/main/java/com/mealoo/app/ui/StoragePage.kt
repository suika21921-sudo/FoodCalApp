package com.mealoo.app.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mealoo.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.mealoo.app.db.FoodDbRepository
import androidx.compose.ui.unit.dp
import com.mealoo.app.db.InventoryRepository
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.db.SavingsRepository
import com.mealoo.app.db.ShopRepository
import com.mealoo.app.ui.design.MealooTechColors
import com.mealoo.app.ui.design.mealooTechGlassBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import com.mealoo.app.data.*
import com.mealoo.app.pricing.EventRule
import com.mealoo.app.pricing.Step
import com.mealoo.app.model.*
import com.mealoo.app.util.AlertType
import com.mealoo.app.util.InventoryAlert
import com.mealoo.app.util.InventoryAlertEngine
import com.mealoo.app.util.NOTE_TAG_THAW
import com.mealoo.app.util.hasThawTag
import com.mealoo.app.util.normalizeToEnglish
import com.mealoo.app.util.stripBrackets
import com.mealoo.app.util.CPMetricType
import com.mealoo.app.util.PriceCalcUtils
import com.mealoo.app.util.toCostInfo
import com.mealoo.app.util.NutritionScorer
import com.mealoo.app.util.buildFoodContexts
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import com.mealoo.app.util.DateField
import com.mealoo.app.util.DiscountRuleDropdown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import androidx.compose.runtime.saveable.rememberSaveable
import com.mealoo.app.util.PricingEngine
import com.mealoo.app.util.PricingInput
import com.mealoo.app.util.Result
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.mealoo.app.ui.design.MealooTextField
import com.mealoo.app.util.ContextLevel
import com.mealoo.app.util.FoodContext
import com.mealoo.app.util.OsmLocationPicker
import com.mealoo.app.util.calculateDiscountedTotal
import com.mealoo.app.util.navigateToShop
import kotlinx.coroutines.yield
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mealoo.app.ui.design.CompactDropdownFieldMenu
import com.mealoo.app.ui.design.CompactTextField
import com.mealoo.app.ui.design.MealooButton
import com.mealoo.app.ui.design.MealooButtonStyle
import com.mealoo.app.ui.design.MealooTheme
import com.mealoo.app.ui.design.NoteTextField
import java.net.URLEncoder
import kotlin.math.*

private const val TAG_SAVINGS = "SavingsFlow"

private data class HubTab(
    val index: Int,
    val titleRes: Int,
    val icon: ImageVector,
)
// --- 🎨 Storage Colors & Styles ---
private object StorageColors {
    val Bg: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val CardBg: Color @Composable get() = MaterialTheme.colorScheme.surface
    val Outline: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    // Locations
    val Pantry: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB74D).copy(alpha = 0.8f) else Color(0xFFFFCC80)
    val Fridge: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF64B5F6).copy(alpha = 0.8f) else Color(0xFF90CAF9)
    val Freezer: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4DD0E1).copy(alpha = 0.8f) else Color(0xFF80DEEA)
    val Other: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    // States
    val Fresh: Color @Composable get() = MealooTheme.colors.protein
    val Expiring: Color @Composable get() = MealooTheme.colors.fats
    val Expired: Color @Composable get() = MaterialTheme.colorScheme.error

    // ✅ Nutrients (统一颜色)
    val Energy: Color @Composable get() = MealooTheme.colors.fats
    val Protein: Color @Composable get() = MealooTheme.colors.protein
    val Carb: Color @Composable get() = MealooTheme.colors.carbs
    val Fat: Color @Composable get() = Color(0xFFEF5350)
}
// --- 🎨 Shop & Shopping Colors ---
private object ShopColors {
    val RetailAccent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4DD0E1) else Color(0xFF00ACC1)
    val RetailBg: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF006064).copy(alpha = 0.2f) else Color(0xFFE0F7FA)

    val RestaurantAccent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFF8A65) else Color(0xFFFF7043)
    val RestaurantBg: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFBF360C).copy(alpha = 0.2f) else Color(0xFFFBE9E7)

    val HybridAccent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF9575CD) else Color(0xFF7E57C2)
    val HybridBg: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4527A0).copy(alpha = 0.2f) else Color(0xFFEDE7F6)

    val DiscountRed: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFEF5350) else Color(0xFFE53935)
    val DiscountBg: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)

    // Helper to get color pair based on shop type
    @Composable
    fun getTheme(type: ShopCategory): Pair<Color, Color> = when(type) {
        ShopCategory.RETAIL -> RetailAccent to RetailBg
        ShopCategory.RESTAURANT -> RestaurantAccent to RestaurantBg
        ShopCategory.HYBRID -> HybridAccent to HybridBg
    }
}

// 跨页桥：RecipePage -> StoragePage
object RecipeToStorageBridge {
    val incoming = MutableSharedFlow<RecipeEntry?>(replay = 1)
    fun clear() { incoming.tryEmit(null) }
}

@Composable
private fun isCompactPhone(): Boolean {
    val cfg = LocalConfiguration.current
    return cfg.screenWidthDp < 600
}
// 轻量描述可套用的规则，以及如何把它“应用”到某个 DiscountItem 上
private data class RuleBadge(
    val id: String,
    val title: String,
    val applyTo: (DiscountItem) -> DiscountItem
)

private val FieldShape = RoundedCornerShape(12.dp) // 与 TextField 一致的圆角
private data class ShoppingNeed(val name: String, val deficitGrams: Double)
private const val SOURCE_TYPE_SHOP_PRODUCT = "SHOP_PRODUCT"

@Composable
private fun InventoryRowActions(
    compact: Boolean,
    onQuickConsumeToIntake: () -> Unit,
    onThaw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    quickConsumeLabel: String = "" // 你现在列表里的“-50”
) {
    if (!compact) {
        // 平板：横向按钮（移除了快速消耗）
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuickConsumeToIntake) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.storage_consume_add_meal_action)
                )
            }
            IconButton(onClick = onThaw) {
                Icon(
                    Icons.Outlined.AcUnit,
                    contentDescription = stringResource(R.string.storage_thaw_partial_action)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    } else {
        // 手机：折叠为三点菜单
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.common_more_actions_action)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // ✅ 新增菜单项：消耗并加入餐次…
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.meal_record)) },
                    onClick = { expanded = false; onQuickConsumeToIntake() }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.AcUnit, null) },
                    text = { Text(stringResource(R.string.storage_thaw_remove_action)) },
                    onClick = {
                        expanded = false
                        onThaw()
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    text = { Text(stringResource(R.string.edit)) },
                    onClick = {
                        expanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun IntegratedSearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilterIndex: Int,
    onFilterChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 定義篩選選項
    val filters = listOf(stringResource(R.string.all), stringResource(R.string.loc_fridge), stringResource(R.string.loc_freezer), stringResource(R.string.loc_pantry), stringResource(R.string.gender_other))
    val currentFilterLabel = filters.getOrElse(selectedFilterIndex) { stringResource(R.string.all) }

    var expanded by remember { mutableStateOf(false) }

    // 外層容器：白底 + 陰影 + 圓角
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // 標準觸控高度
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 左側：搜尋輸入框 ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // 清除按鈕
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable { expanded = true }
                    .padding(end = 16.dp, start = 8.dp, top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentFilterLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .width(120.dp)
                ) {
                    filters.forEachIndexed { index, label ->
                        val isSelected = index == selectedFilterIndex

                        DropdownMenuItem(
                            modifier = Modifier.background(
                                if (isSelected) Color.LightGray.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            text = {
                                Text(
                                    text = label,
                                    // 選中時加粗
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onFilterChange(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun ExpirationFilterBar(
    currentFilter: FilterType,
    onSelect: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // 增加上下 Padding，让阴影不被切掉，同时与上方元素拉开一点呼吸感
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterType.values().forEach { type ->
            val isSelected = currentFilter == type

            // --- 1. 定义文字 ---
            val label = when (type) {
                FilterType.ALL -> stringResource(R.string.all)
                FilterType.SOON -> stringResource(R.string.expired3d)
                FilterType.EXPIRED -> stringResource(R.string.expired)
            }

            // --- 2. 定义颜色 (让过期/即期更有警示感) ---
            // 选中时：
            // - ALL: 黑色 (Primary)
            // - SOON: 橙色 (Warning)
            // - EXPIRED: 红色 (Error)
            val activeColor = when(type) {
                FilterType.ALL -> MaterialTheme.colorScheme.primary
                FilterType.SOON -> Color(0xFFFF9800) // 橙色
                FilterType.EXPIRED -> MaterialTheme.colorScheme.error // 红色
            }

            val containerColor = if (isSelected) activeColor else MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant

            // 选中时阴影加深，强调状态
            val elevation = if (isSelected) 4.dp else 1.dp

            // --- 3. 渲染胶囊 ---
            Surface(
                onClick = { onSelect(type) },
                shape = CircleShape,
                color = containerColor,
                shadowElevation = elevation,
                modifier = Modifier.height(32.dp) // 稍微比顶部的 Location Filter (36dp) 矮一点点，体现层级
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 如果选中了 即期/过期，可以加个小图标增强提示 (可选)
                    if (isSelected && type == FilterType.EXPIRED) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                }
            }
        }
    }
}
// ---------- UI ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoragePage(
    savingsRepo: SavingsRepository,
    store: InventoryRepository,
    allItems: List<InventoryItem>
) {
    val context = LocalContext.current
    val mealPlanStore = remember { MealPlanRepository.getInstance(context) }
    val shopRepo = remember { ShopRepository.getInstance(context) }
    val intakeStore = remember { IntakeDataStore(context) }
    val scope = rememberCoroutineScope()
    val prefStore = remember { UserPreferenceDataStore(context) }
    val mealDefsStore = remember { MealDefsStore(context) }
    val userPrefState = produceState<UserPreference?>(initialValue = null, key1 = prefStore) {
        value = prefStore.getPreference()
    }
    val eventStore = remember { DiscountEventDataStore(context) }
    val cartStore = remember { ShoppingCartStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }

    // States
    var showAddCartProductDialog by remember { mutableStateOf(false) }
    var showAlertSheet by remember { mutableStateOf(false) }
    var showShoppingHub by remember { mutableStateOf(false) }
    var pendingAddToIntake by remember { mutableStateOf<Pair<InventoryItem, Double>?>(null) }
    var pendingThaw by remember { mutableStateOf<InventoryItem?>(null) }
    var requireExpiry by remember { mutableStateOf(false) }
    var addDialogOpen by remember { mutableStateOf(false) }
    var viewing: InventoryItem? by remember { mutableStateOf(null) }
    var editing: InventoryItem? by remember { mutableStateOf(null) }
    var leftoverInit by remember { mutableStateOf<InventoryItem?>(null) }
    var requirements by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var showThawCard by rememberSaveable { mutableStateOf(true) }
    var tomorrowIngredients by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(allItems) {
        val today = LocalDate.now()
        val dates = (0..7).map { today.plusDays(it.toLong()).toString() }

        val plansMap = mealPlanStore.getEntriesForDateRange(dates)
        val allEntries = plansMap.values.flatten()

        val tomorrow = today.plusDays(1).toString()
        tomorrowIngredients = plansMap[tomorrow]?.map { it.name.trim().lowercase() }?.toSet() ?: emptySet()

        // 建立索引加速匹配
        val itemById = allItems.associateBy { it.id }
        val byFdc = allItems.filter { it.fdcId != null }.groupBy { it.fdcId!! }
        val byName = allItems.associateBy { it.name.trim().lowercase() }

        val reqMap = mutableMapOf<String, Double>()

        allEntries.forEach { entry ->
            var match: InventoryItem? = null

            // 優先級 1: 直接對應 Inventory ID (最準)
            if (entry.inventoryId != null) {
                match = itemById[entry.inventoryId]
            }
            // 優先級 2: FDC ID
            if (match == null && entry.fdcId != null) {
                match = byFdc[entry.fdcId]?.firstOrNull()
            }
            // 優先級 3: 名稱匹配
            if (match == null) {
                match = byName[entry.name.trim().lowercase()]
            }

            if (match != null) {
                val amountInInvUnit = when (match.unit.lowercase()) {
                    "g", "ml" -> entry.grams
                    "pcs" -> {
                        val gpu = match.gramsPerUnit ?: 0.0
                        if (gpu > 0.0) entry.grams / gpu else 0.0
                    }
                    else -> 0.0
                }

                if (amountInInvUnit > 0.0) {
                    reqMap[match.id] = (reqMap[match.id] ?: 0.0) + amountInInvUnit
                }
            }
        }
        requirements = reqMap
    }

    // Filter & Search
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FilterType.ALL) }
    val today = remember { startOfDay(Date()) }
    val nameById = remember(allItems) { allItems.associate { it.id to it.name } }
    val alerts = remember(allItems, requirements) {
        InventoryAlertEngine.computeAlerts(allItems, requirements)
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var locationFilter by remember { mutableStateOf("All") }
    val thawCandidates = remember(allItems, tomorrowIngredients) {
        allItems.filter { item ->
            item.location.equals("Freezer", ignoreCase = true) && (
                tomorrowIngredients.contains(item.name.trim().lowercase()) ||
                tomorrowIngredients.any { planName ->
                    item.name.contains(planName, ignoreCase = true) ||
                    planName.contains(item.name, ignoreCase = true)
                }
            )
        }
    }
    val filteredSorted = remember(allItems, query, filter, locationFilter) {
        allItems
            .filter { it.name.contains(query, ignoreCase = true) }
            // 1. 先過濾位置
            .filter { item ->
                when (locationFilter) {
                    "All" -> true
                    "Other" -> item.location != "Fridge" && item.location != "Freezer" && item.location != "Pantry"
                    else -> item.location.equals(locationFilter, ignoreCase = true)
                }
            }
            // 2. 再過濾過期狀態
            .filter { item ->
                when (filter) {
                    FilterType.ALL -> true
                    FilterType.SOON -> daysToExpiry(item.expiryDate, today)?.let { it in 0..3 } ?: false
                    FilterType.EXPIRED -> daysToExpiry(item.expiryDate, today)?.let { it < 0 } ?: false
                }
            }
            .sortedWith(compareBy(
                { item -> daysToExpiry(item.expiryDate, today) ?: Int.MAX_VALUE },
                { it.name.lowercase(Locale.getDefault()) }
            ))
    }

    // Recipe Import Logic
    LaunchedEffect(Unit) {
        RecipeToStorageBridge.incoming.filterNotNull().collect { recipe ->
            val total = (recipe.totalWeightGram.takeIf { it > 0.0 } ?: recipe.ingredients.sumOf { it.amount }.takeIf { it > 0.0 })
            val per100 = recipePer100g(recipe) ?: emptyMap()
            val currentHomeCurrency = userPrefState.value?.homeCurrency?.code ?: AppCurrency.USD.code
            leftoverInit = InventoryItem(
                name = recipe.name,
                quantity = BigDecimal.ONE,
                unit = "pcs",
                gramsPerUnit = total,
                standardNutrientsPer100g = per100,
                location = "Fridge",
                expiryDate = fmtDate(addDays(startOfDay(Date()), 3)),
                cost = null,
                currency = currentHomeCurrency
            )
            requireExpiry = true
            addDialogOpen = true
            RecipeToStorageBridge.clear()
        }
    }

    Scaffold(
        containerColor = MainTechColors.DeepBackground,
        contentWindowInsets = WindowInsets(0, 90, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 88.dp),
                onClick = { requireExpiry = false; addDialogOpen = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- Header Area ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.storagePage_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 🔥 新增：警告圖示 (Alert Badge)
                if (alerts.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showAlertSheet = true }) {
                        // 使用 BadgedBox 顯示紅點計數
                        androidx.compose.material3.BadgedBox(
                            badge = {
                                androidx.compose.material3.Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(alerts.size.toString())
                                }
                            }
                        ) {
                            // 根據警告類型顯示不同圖示 (Thaw 優先顯示雪花，否則顯示驚嘆號)
                            val hasThaw = alerts.any { it.type == AlertType.THAW_ALERT }
                            Icon(
                                imageVector = if (hasThaw) Icons.Outlined.AcUnit else Icons.Default.WarningAmber,
                                contentDescription = stringResource(R.string.common_alerts_desc),
                                tint = if (hasThaw) StorageColors.Freezer else StorageColors.Pantry,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))

                Surface(
                    onClick = { showShoppingHub = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                ) {
                    Box(Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IntegratedSearchFilterBar(
                    query = query,
                    onQueryChange = { query = it },
                    selectedFilterIndex = selectedTabIndex,
                    onFilterChange = { index ->
                        selectedTabIndex = index
                        locationFilter = when (index) {
                            1 -> "Fridge"
                            2 -> "Freezer"
                            3 -> "Pantry"
                            4 -> "Other"
                            else -> "All"
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            ExpirationFilterBar(
                currentFilter = filter,
                onSelect = { newType -> filter = newType }
            )

            Spacer(Modifier.height(16.dp))

            // 🔥 新增：解凍建議卡片
            if (showThawCard && thawCandidates.isNotEmpty()) {
                DefrostSuggestionCard(
                    items = thawCandidates,
                    onMoveToFridge = { item ->
                        scope.launch {
                            // 更新位置到冷藏，並加上備註
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val newNote = (item.note ?: "") + "\nThawed on $dateStr"
                            val updated = item.copy(
                                location = "Fridge",
                                note = newNote.trim()
                            )
                            store.upsert(updated)
                        }
                    },
                    onIgnore = { showThawCard = false }
                )
                Spacer(Modifier.height(16.dp))
            }

            // --- List Content ---
            when {
                filteredSorted.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyHint(filter, query, { query = "" }, { addDialogOpen = true })
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                    ) {
                        items(filteredSorted, key = { it.id }) { item ->
                            InventoryItemCard(
                                item = item,
                                today = today,
                                onEdit = { editing = it },
                                onDelete = { scope.launch { store.delete(it.id) } },
                                onQuickConsumeToIntake = { amount -> pendingAddToIntake = item to amount },
                                onThaw = { pendingThaw = it },
                                onView = { viewing = it },
                                nameById = nameById
                            )
                        }
                    }
                }
            }
        }
    }
    viewing?.let { itItem ->
        InventoryItemDetailDialog(
            item = itItem,
            onDismiss = { viewing = null },
            onQuickConsume = { amt -> scope.launch { store.consume(itItem.id, amt) } },
            onThaw = { pendingThaw = itItem },
            onEdit = {
                viewing = null
                editing = itItem
            },
            onDelete = {
                scope.launch { store.delete(itItem.id) }
                viewing = null
            }
        )
    }
    if (showAddCartProductDialog) {
        val manualItemNote = stringResource(R.string.shopping_manual_item_note_desc)
        val otherItemsLabel = stringResource(R.string.shopping_other_items_label)
        val dummyShop = ShopInfo(
            id = "manual_cart_shop",
            name = otherItemsLabel,
            address = null,
            type = ShopCategory.RETAIL,
            catalog = emptyList()
        )

        AddShopProductDialog(
            shop = dummyShop,
            onDismiss = { showAddCartProductDialog = false },
            onConfirmProduct = { product ->
                scope.launch {
                    cartStore.upsert(
                        ShoppingListItem(
                            name = product.name,
                            quantity = BigDecimal.ONE,
                            unit = "pcs",
                            basePrice = product.price,
                            currency = product.currency,
                            note = manualItemNote,
                            shopId = dummyShop.id,
                            shopProductId = product.id
                        )
                    )
                    showAddCartProductDialog = false
                }
            },
            defaultCurrencyCode = userPrefState.value?.homeCurrency?.code
        )
    }
    if (showShoppingHub) {
        GeminiShoppingHubSheet(
            onDismiss = { showShoppingHub = false },
            cartStore = cartStore,
            savingsRepo = savingsRepo,
            eventStore = eventStore,
            shopRepo = shopRepo,
            intakeStore = intakeStore,
            mealDefs = mealDefs,
            userPrefState= userPrefState,
            onAddCartItem = { showAddCartProductDialog = true },
        )
    }

    if (addDialogOpen) {
        AddOrEditItemDialog(
            title = stringResource(R.string.add),
            init = leftoverInit,
            onDismiss = { addDialogOpen = false; leftoverInit = null },
            onConfirm = { newItem -> scope.launch { store.upsert(newItem) }; addDialogOpen = false; leftoverInit = null },
            userPrefState = userPrefState,
            expiryRequired = requireExpiry
        )
    }
    if (editing != null) {
        AddOrEditItemDialog(
            title = stringResource(R.string.edit),
            init = editing,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                scope.launch { store.upsert(updated) }
                editing = null
            },
            userPrefState = userPrefState
        )
    }
    pendingThaw?.let { thawItem ->
        ThawSplitDialog(
            item = thawItem,
            onDismiss = { pendingThaw = null },
            onConfirm = { amount, location, extra ->
                scope.launch {
                    val result = store.thawInventoryItem(
                        parentId = thawItem.id,
                        needQty = amount.toDouble(),
                        targetLocation = location,
                        extraNote = extra
                    )
                    if (result is Result.Err) {
                        Toast.makeText(context, result.error.message, Toast.LENGTH_SHORT).show()
                    }
                }
                pendingThaw = null
            }
        )
    }
    // ✅ 弹出餐次选择，并真正执行“加入摄取 + 扣库存”
    val dateStr = remember { DATE_FMT.format(Date()) }
    pendingAddToIntake?.let { (invItem, amount) ->
        MealSelectDialog(
            mealDefs = mealDefs,
            onDismiss = { pendingAddToIntake = null },
            onPick = { mealId ->
                scope.launch {
                    // 1) 计算克数
                    val grams = when (invItem.unit) {
                        "g"   -> amount
                        "pcs" -> (invItem.gramsPerUnit ?: 0.0) * amount
                        else  -> amount
                    }
                    // 若没有每100g营养，无法入账
                    val per100Raw = invItem.standardNutrientsPer100g
                    if (per100Raw == null || grams <= 0.0) {
                        // 可加个Snackbar/Toast提示，这里省略
                        pendingAddToIntake = null
                        return@launch
                    }
                    val per100 = normalizeToEnglish(per100Raw!!)

                    // 2) 计算营养（与 MealPicker 口径一致：ratio=grams/100）
                    val r = grams / 100.0
                    val kcal = (per100["Energy"] ?: 0.0) * r
                    val protein = (per100["Protein"] ?: 0.0) * r
                    val carbs = (per100["Available carbohydrate"]
                        ?: per100["Carbohydrate, by difference"] ?: 0.0) * r
                    val fats = (per100["Total lipid (fat)"] ?: 0.0) * r
                    val cholesterol = (per100["Cholesterol"] ?: 0.0) * r

                    // 3) 写入 Intake（当前模型无 meal 字段，先把餐次贴在 name 前缀）
                    val entry = EatEntry(
                        name = invItem.name,
                        mealId = mealId,
                        kcal = kcal,
                        grams = grams,
                        protein = protein,
                        carbs = carbs,
                        fats = fats,
                        cholesterol = cholesterol,
                        fdcId = invItem.fdcId,
                        source = if (invItem.fdcId != null) "DATA" else "USER",
                        date = dateStr,
                        category = null
                    )
                    intakeStore.addEntry(entry)

                    // 4) 同步扣库存
                    store.consume(invItem.id, amount)

                    pendingAddToIntake = null
                }
            }
        )
    }
    if (showAlertSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAlertSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp) // 底部留白
            ) {
                Text(
                    text = stringResource(R.string.storage_alerts_title, alerts.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts) { alert ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StorageColors.Bg),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                showAlertSheet = false
                            }
                        ) {
                            Box(Modifier.padding(12.dp)) {
                                InventoryAlertRow(alert = alert)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ModernSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = { Text(stringResource(R.string.search), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = StorageColors.Outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if(selected) Color.Transparent else StorageColors.Outline,
            enabled = true, selected = selected
        )
    )
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    today: Date,
    onEdit: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit,
    onQuickConsumeToIntake: (Double) -> Unit,
    onThaw: (InventoryItem) -> Unit,
    onView: (InventoryItem) -> Unit,
    nameById: Map<String, String> = emptyMap()
) {
    val dte = daysToExpiry(item.expiryDate, today)
    val (badgeText, badgeColor, txtColor) = expiryBadge(dte)
    val alerts = remember(item.note, item.quantity, item.unit, item.location, nameById) {
        InventoryAlertEngine.alertsForItem(item, nameById)
    }

    // Determine Location Color & Icon
    val (locColor, locIcon) = when(item.location) {
        "Pantry" -> StorageColors.Pantry to Icons.Outlined.AllInbox
        "Fridge" -> StorageColors.Fridge to Icons.Outlined.Kitchen
        "Freezer" -> StorageColors.Freezer to Icons.Outlined.AcUnit
        else -> StorageColors.Other to Icons.Outlined.Inventory2
    }

    Card(
        onClick = { onView(item) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min) // Match height for color strip
                .fillMaxWidth()
        ) {
            // Left Color Strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(locColor)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                // Top Row: Location Icon + Name + Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(locIcon, null, tint = locColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        alerts.forEach { alert ->
                            InventoryAlertBadge(alert = alert)
                        }
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = badgeText,
                                color = txtColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Bottom Row: Quantity + Action
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.common_quantity_unit_desc,
                                item.quantity.toString(),
                                item.unit
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.cost != null) {
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_desc,
                                    AppCurrency.fromCode(item.currency).symbol,
                                    fmt(item.cost.toDouble())
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        if (alerts.isNotEmpty()) {
                            Text(
                                text = alerts.joinToString("\n") { it.subtitle },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Actions
                    val canThaw = item.quantity > BigDecimal.ZERO
                    if (isCompactPhone()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.meal_record)) },
                                    leadingIcon = { Icon(Icons.Default.Restaurant, null) },
                                    onClick = { expanded = false; onQuickConsumeToIntake(stepFor(item.unit)) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.storage_thaw_remove_action)) },
                                    leadingIcon = { Icon(Icons.Outlined.AcUnit, null) },
                                    enabled = canThaw,
                                    onClick = { expanded = false; onThaw(item) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { expanded = false; onEdit(item) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { expanded = false; onDelete(item) }
                                )
                            }
                        }
                    } else {
                        // Tablet: Row Buttons
                        Row {
                            IconButton(onClick = { onQuickConsumeToIntake(stepFor(item.unit)) }) {
                                Icon(Icons.Default.Restaurant, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onThaw(item) }, enabled = canThaw) {
                                Icon(Icons.Outlined.AcUnit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onEdit(item) }) { Icon(Icons.Outlined.Edit, null) }
                        }
                    }
                }
            }
        }
    }
}

// --- Styled Dialogs ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOrEditItemDialog(
    title: String,
    init: InventoryItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit,
    userPrefState: State<UserPreference?>,
    expiryRequired: Boolean = false
) {
    val context = LocalContext.current

    // --- state (Same logic as original, refreshed UI) ---
    var name by remember { mutableStateOf(init?.name ?: "") }
    var qtyText by remember { mutableStateOf(init?.quantity.toStringSafe()) }
    var unit by remember { mutableStateOf(init?.unit ?: "pcs") }
    var location by remember { mutableStateOf(init?.location ?: "Pantry") }
    var expiry by remember { mutableStateOf(init?.expiryDate) }
    var note by remember { mutableStateOf(init?.note ?: "") }
    val thawLocked = init?.note?.hasThawTag() == true
    var noteAppend by remember { mutableStateOf("") }
    var nutrients100 by remember { mutableStateOf(init?.standardNutrientsPer100g ?: emptyMap()) }
    var gramsPerUnitText by remember { mutableStateOf(init?.gramsPerUnit?.let { fmt(it) } ?: "") }
    var fdcId by remember { mutableStateOf(init?.fdcId) }
    var costText by remember {
        mutableStateOf(
            init?.cost
                ?.setScale(2, RoundingMode.HALF_UP)
                ?.toPlainString()
                ?: ""
        )
    }
    val defaultCurrencyCode = init?.currency ?: userPrefState.value?.homeCurrency?.code ?: AppCurrency.TWD.code
    var currency by remember(init?.currency, userPrefState.value?.homeCurrency?.code) {
        mutableStateOf(defaultCurrencyCode)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf(init?.category) }
    // External Dialog Triggers
    var showFoodPicker by remember { mutableStateOf(false) }
    var showAddFoodDialog by remember { mutableStateOf(false) }

    // Validation
    val qty = qtyText.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val gramsPerUnitVal = gramsPerUnitText.toDoubleOrNull()
    val isNameBlank = name.isBlank()
    val isQtyInvalid = qty <= BigDecimal.ZERO
    val isGramsInvalid = unit == "pcs" && (gramsPerUnitVal == null || gramsPerUnitVal <= 0.0)
    val isValid = !isNameBlank && !isQtyInvalid && !isGramsInvalid && (!expiryRequired || !expiry.isNullOrBlank())

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            securePolicy = SecureFlagPolicy.Inherit,
            isFocusable = true,
            shouldDismissOnBackPress = false
        )
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // 1. Basic Info
            Text(
                stringResource(R.string.storage_basic_info_title),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))

            CompactTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.name), fieldHeight = 44.dp)
            Spacer(Modifier.height(12.dp))

            val unitOptions = listOf(
                stringResource(R.string.common_unit_pcs_label),
                stringResource(R.string.common_unit_g_label),
                stringResource(R.string.common_unit_ml_label)
            )
            val unitLabelMap = mapOf(
                stringResource(R.string.common_unit_pcs_label) to "pcs",
                stringResource(R.string.common_unit_g_label) to "g",
                stringResource(R.string.common_unit_ml_label) to "ml"
            )
            val unitDisplay = when (unit) {
                "pcs" -> stringResource(R.string.common_unit_pcs_label)
                "g" -> stringResource(R.string.common_unit_g_label)
                "ml" -> stringResource(R.string.common_unit_ml_label)
                else -> unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactTextField(
                    value = qtyText, onValueChange = { qtyText = it },
                    label = stringResource(R.string.amount), modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    fieldHeight = 44.dp
                )
                CompactDropdownFieldMenu(
                    value = unitDisplay,
                    onValueChange = { unit = unitLabelMap[it] ?: it },
                    label = stringResource(R.string.unit),
                    options = unitOptions,
                    modifier = Modifier.weight(1f), fieldHeight = 44.dp, rounded = 12.dp
                )
            }
            if (unit == "pcs") {
                Spacer(Modifier.height(12.dp))
                CompactTextField(
                    value = gramsPerUnitText, onValueChange = { gramsPerUnitText = it },
                    label = stringResource(R.string.storage_single_weight_grams_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    fieldHeight = 44.dp
                )
            }

            Spacer(Modifier.height(12.dp))
            PriceWithCurrencyRow(
                price = costText,
                currency = currency,
                onChange = { newCurrency, newPrice ->
                    currency = newCurrency
                    costText = newPrice
                }
            )

            Spacer(Modifier.height(24.dp))

            // 2. Storage Detail
            Text(
                stringResource(R.string.storage_storage_expiry_title),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))

            val locOptions = listOf("Pantry", "Fridge", "Freezer")
            // Custom Chip Row for Location
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                locOptions.forEach { loc ->
                    val selected = location == loc
                    val color = when(loc) {
                        "Fridge" -> StorageColors.Fridge
                        "Freezer" -> StorageColors.Freezer
                        else -> StorageColors.Pantry
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { location = loc },
                        label = { Text(locLabel(loc)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if(selected) Color.Transparent else StorageColors.Outline, enabled = false, selected = true)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            DateField(
                expiry = expiry,
                onDateSelected = { expiry = it },
                label = stringResource(R.string.storage_expiration_date_label),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.note), style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            // 3. Nutrition (Card style)
            Text(
                stringResource(R.string.storage_nutrition_data_title),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(Icons.Default.Search, stringResource(R.string.Use_system_data), { showFoodPicker = true }, Modifier.weight(1f))
                ActionCard(Icons.Outlined.EditNote, stringResource(R.string.custom_data), { showAddFoodDialog = true }, Modifier.weight(1f))
            }
            if (nutrients100.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MacroPreviewBox(nutrients100, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
            if (thawLocked) {
                Text(
                    text = note.ifBlank { NOTE_TAG_THAW },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                CompactTextField(
                    value = noteAppend,
                    onValueChange = { noteAppend = it },
                    label = stringResource(R.string.storage_additional_note_label),
                    placeholder = stringResource(R.string.common_optional_hint),
                    modifier = Modifier.fillMaxWidth(),
                    fieldHeight = 44.dp
                )
            } else {
                NoteTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = stringResource(R.string.note),
                    placeholder = stringResource(R.string.common_optional_hint),
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 80.dp
                )
            }

            Spacer(Modifier.height(32.dp))
            MealooButton(
                text = stringResource(R.string.save),
                onClick = {
                    val mergedNote = if (thawLocked) {
                        val base = init?.note?.trim().orEmpty()
                        val extra = noteAppend.trim()
                        if (extra.isNotBlank()) {
                            context.getString(R.string.storage_note_merge_desc, base, extra)
                        } else {
                            base
                        }
                    } else {
                        note.trim()
                    }
                    onConfirm(InventoryItem(
                        id = init?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        quantity = qty,
                        unit = unit,
                        location = location,
                        expiryDate = expiry?.ifBlank { null },
                        note = mergedNote.ifBlank { null },
                        fdcId = fdcId,
                        standardNutrientsPer100g = nutrients100.takeIf { it.isNotEmpty() },
                        gramsPerUnit = gramsPerUnitVal,
                        cost = costText.toBigDecimalOrNull(),
                        currency = currency,
                        category = category
                    ))
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
                style = MealooButtonStyle.Primary
            )
        }
    }

    // Sub-dialogs
    if (showFoodPicker) {
        MealPickerDialog(
            onDismiss = { showFoodPicker = false },
            onConfirm = { selectedFoods, _ ->
                if (selectedFoods.isNotEmpty()) {
                    val food = selectedFoods.first()
                    fdcId = food.fdcId
                    nutrients100 = food.standardNutrients
                    if (name.isBlank()) name = stripBrackets( food.name)
                }
                showFoodPicker = false
            }
        )
    }
    if (showAddFoodDialog) {
        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            confirmButton = {},
            text = { AddFoodDialog({ showAddFoodDialog=false }) { f ->
                // Mapping logic similar to original
                val std = f.nutrientsPer100g.mapKeys { userKeyToStandardKey[it.key] ?: it.key }
                nutrients100 = std; if(name.isBlank()) name = f.name; showAddFoodDialog=false
            }}
        )
    }
}

@Composable
private fun ThawSplitDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, String, String?) -> Unit
) {
    val maxQty = item.quantity
    val baseAmount = when (item.unit) {
        "g", "ml" -> BigDecimal("100")
        else -> BigDecimal.ONE
    }
    val defaultAmount = if (baseAmount > maxQty) maxQty else baseAmount
    var amountText by remember(item.id) { mutableStateOf(defaultAmount.toStringSafe()) }
    var extraNote by remember { mutableStateOf("") }
    var targetLocation by remember { mutableStateOf("Fridge") }
    val locationOptions = listOf("Fridge", "Pantry", "Freezer")
    val context = LocalContext.current
    val locationLabels = remember(locationOptions, context) {
        locationOptions.associateWith { locLabel(context, it) }
    }
    val locationDisplay = locationLabels[targetLocation] ?: targetLocation
    val amount = amountText.toBigDecimalOrNull()
    val valid = amount != null && amount > BigDecimal.ZERO && amount <= maxQty

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (amount != null) {
                        onConfirm(amount, targetLocation, extraNote.ifBlank { null })
                    }
                    onDismiss()
                },
                enabled = valid
            ) {
                Text(stringResource(R.string.storage_thaw_remove_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.storage_thaw_partial_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.storage_thaw_removable_desc, fmt(maxQty), item.unit), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.amount)) },
                    placeholder = { Text(item.unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CompactDropdownFieldMenu(
                    value = locationDisplay,
                    onValueChange = { selectedLabel ->
                        locationLabels.entries.firstOrNull { it.value == selectedLabel }?.let { entry ->
                            targetLocation = entry.key
                        }
                    },
                    label = stringResource(R.string.storage_target_location_label),
                    options = locationLabels.values.toList(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = extraNote,
                    onValueChange = { extraNote = it },
                    label = { Text(stringResource(R.string.storage_additional_note_label)) },
                    placeholder = { Text(stringResource(R.string.common_optional_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// --- Reused UI Helpers (Locally defined to ensure standalone compilation) ---

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StorageColors.Outline),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MacroPreviewBox(nutrients: Map<String, Double>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = StorageColors.Bg),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val p = nutrients["Protein"] ?: 0.0
            val c = nutrients["Available carbohydrate"] ?: nutrients["Carbohydrate, by difference"] ?: 0.0
            val f = nutrients["Total lipid (fat)"] ?: 0.0
            val e = nutrients["Energy"] ?: 0.0

            NutrientCol(stringResource(R.string.energy), fmt(e), StorageColors.Energy)
            NutrientCol(
                stringResource(R.string.protein),
                stringResource(R.string.common_grams_value_desc, fmt(p)),
                StorageColors.Protein
            )
            NutrientCol(
                stringResource(R.string.carbs),
                stringResource(R.string.common_grams_value_desc, fmt(c)),
                StorageColors.Carb
            )
            NutrientCol(
                stringResource(R.string.fats),
                stringResource(R.string.common_grams_value_desc, fmt(f)),
                StorageColors.Fat
            )
        }
    }
}
@Composable
fun NutrientCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
private enum class FilterType { ALL, SOON, EXPIRED }

// ---------- Cards & Dialog ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiShoppingHubSheet(
    onDismiss: () -> Unit,
    cartStore: ShoppingCartStore,
    savingsRepo: SavingsRepository,
    eventStore: DiscountEventDataStore,
    shopRepo: ShopRepository,
    intakeStore: IntakeDataStore,
    mealDefs: List<MealDef>,
    onAddCartItem: () -> Unit,
    userPrefState: State<UserPreference?>,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var tabIndex by remember { mutableIntStateOf(1) } // 0: cart, 1: shops
    var selectedShop by remember { mutableStateOf<ShopInfo?>(null) }
    var initialAddModeIsDiscount by remember { mutableStateOf(false) }

    var showAddShopDialog by remember { mutableStateOf(false) }
    var shopForNewProduct by remember { mutableStateOf<ShopInfo?>(null) }
    var pendingEatProduct by remember { mutableStateOf<ShopProduct?>(null) }

    val shops by shopRepo.shopsFlow().collectAsState(initial = emptyList())
    val allEvents by eventStore.eventsFlow().collectAsState(initial = emptyList())
    val cartItems by cartStore.itemsFlow().collectAsState(initial = emptyList())

    var showDeleteShopConfirm by remember { mutableStateOf(false) }
    var showDeleteProductConfirm by remember { mutableStateOf<ShopProduct?>(null) }

    LaunchedEffect(shops) {
        val current = selectedShop ?: return@LaunchedEffect
        shops.find { it.id == current.id }?.let { latest ->
            if (latest != current) selectedShop = latest
        }
    }

    val tabs = remember {
        listOf(
            HubTab(0, R.string.cart_title, Icons.Outlined.ShoppingCart),
            HubTab(1, R.string.shop_title, Icons.Outlined.Storefront),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.9f)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedShop != null) {
                    IconButton(onClick = { selectedShop = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedShop!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showDeleteShopConfirm = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    androidx.compose.material3.TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[tabIndex])
                                    .height(3.dp)
                                    .padding(horizontal = 24.dp)
                                    .background(MealooTechColors.AccentCyan, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        tabs.forEach { tab ->
                            androidx.compose.material3.Tab(
                                selected = tabIndex == tab.index,
                                onClick = { tabIndex = tab.index },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 6.dp).size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(tab.titleRes),
                                            fontWeight = if (tabIndex == tab.index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = Color.Gray
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Box(Modifier.weight(1f)) {
                when {
                    selectedShop != null -> {
                        val shop = selectedShop!!
                        val relatedEvents = allEvents.filter { it.shopId == shop.id }
                        ShopDetailContent(
                            shop = shop,
                            events = relatedEvents,
                            onEat = { product -> pendingEatProduct = product },
                            onAddToCart = { product, appliedEvent ->
                                scope.launch {
                                    val existingItem = cartItems.find {
                                        it.shopId == shop.id && it.shopProductId == product.id
                                    }
                                    if (existingItem != null) {
                                        cartStore.upsert(existingItem.copy(quantity = existingItem.quantity + BigDecimal.ONE))
                                    } else {
                                        cartStore.upsert(
                                            ShoppingListItem(
                                                name = product.name,
                                                basePrice = product.price,
                                                currency = product.currency,
                                                discountEventId = appliedEvent?.id,
                                                shopId = shop.id,
                                                shopProductId = product.id,
                                                quantity = BigDecimal.ONE,
                                                note = context.getString(R.string.shopping_catalog_note_desc, shop.name)
                                            )
                                        )
                                    }
                                }
                            },
                            onAddEventItemToCart = { discountItem, eventId ->
                                scope.launch {
                                    cartStore.upsert(
                                        ShoppingListItem(
                                            name = discountItem.name,
                                            basePrice = discountItem.unitPrice,
                                            currency = userPrefState.value?.homeCurrency?.code ?: AppCurrency.USD.code,
                                            discountEventId = eventId,
                                            quantity = BigDecimal.ONE
                                        )
                                    )
                                }
                            },
                            onAddClick = { isDiscount ->
                                initialAddModeIsDiscount = isDiscount
                                shopForNewProduct = shop
                            },
                            onEditEventItems = { event, selectedProducts ->
                                scope.launch {
                                    val newItems = selectedProducts.map { p ->
                                        DiscountItem(
                                            name = p.name,
                                            unitPrice = p.price ?: BigDecimal.ZERO
                                        )
                                    }
                                    eventStore.updateEvent(event.copy(items = newItems))
                                }
                            },
                            onRemoveProduct = { product -> showDeleteProductConfirm = product },
                            onDeleteEvent = { eventToDelete ->
                                scope.launch {
                                    eventStore.deleteEvent(eventToDelete.id)
                                }
                            }
                        )
                    }

                    tabIndex == 0 -> ShoppingListContent(
                        cartStore = cartStore,
                        savingsRepo = savingsRepo,
                        eventStore = eventStore,
                        shopRepo = shopRepo,
                        onAddCartItem = onAddCartItem,
                        userPrefState = userPrefState
                    )

                    tabIndex == 1 && selectedShop == null -> ShopListContent(
                        shops = shops,
                        onShopClick = { selectedShop = it },
                        onAddShop = { showAddShopDialog = true },
                        onDeleteShop = { _ -> /* keep existing behavior */ }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddShopDialog) {
        AddShopDialog(
            onDismiss = { showAddShopDialog = false },
            onConfirm = { newShop ->
                scope.launch {
                    shopRepo.upsertShop(newShop)
                    showAddShopDialog = false
                }
            }
        )
    }

    if (pendingEatProduct != null) {
        val p = pendingEatProduct!!
        MealSelectDialog(
            mealDefs = mealDefs,
            onDismiss = { pendingEatProduct = null },
            onPick = { mealId ->
                scope.launch {
                    val nuts = p.nutrients ?: emptyMap()
                    intakeStore.addEntry(
                        EatEntry(
                            name = p.name,
                            mealId = mealId,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            kcal = nuts["Energy"] ?: 0.0,
                            protein = nuts["Protein"] ?: 0.0,
                            carbs = nuts["Available carbohydrate"] ?: 0.0,
                            fats = nuts["Total lipid (fat)"] ?: 0.0,
                            cholesterol = 0.0,
                            source = "SHOP_CATALOG",
                            cost = p.price,
                            currency = p.currency,
                            category = p.category?.firstOrNull()
                        )
                    )
                    pendingEatProduct = null
                }
            }
        )
    }

    if (shopForNewProduct != null) {
        val shop = shopForNewProduct!!
        AddShopProductDialog(
            shop = shop,
            initialIsDiscount = initialAddModeIsDiscount,
            onDismiss = { shopForNewProduct = null },
            onConfirmProduct = { product ->
                scope.launch {
                    shopRepo.addProductToShop(shop.id, product)
                    shopForNewProduct = null
                }
            },
            onConfirmDiscount = { event ->
                scope.launch {
                    eventStore.addEvent(event)
                    shopForNewProduct = null
                }
            },
            defaultCurrencyCode = userPrefState.value?.homeCurrency?.code
        )
    }

    if (showDeleteShopConfirm && selectedShop != null) {
        val shop = selectedShop!!
        AlertDialog(
            onDismissRequest = { showDeleteShopConfirm = false },
            title = { Text(stringResource(R.string.delete_shop_confirm_title)) },
            text = { Text(stringResource(R.string.delete_shop_confirm_message_fmt, shop.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteShopConfirm = false
                        scope.launch {
                            shopRepo.deleteShop(shop.id)
                            selectedShop = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteShopConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteProductConfirm != null && selectedShop != null) {
        val productToDelete = showDeleteProductConfirm!!
        val shop = selectedShop!!
        AlertDialog(
            onDismissRequest = { showDeleteProductConfirm = null },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.shopping_delete_product_confirm_desc, productToDelete.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val newCatalog = shop.catalog.filter { it.name != productToDelete.name }
                            val updatedShop = shop.copy(catalog = newCatalog)
                            shopRepo.upsertShop(updatedShop)
                            selectedShop = updatedShop
                        }
                        showDeleteProductConfirm = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProductConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
@Composable
fun ShopDetailContent(
    shop: ShopInfo,
    events: List<DiscountEvent>,
    onEat: (ShopProduct) -> Unit,
    onAddToCart: (ShopProduct, DiscountEvent?) -> Unit,
    onAddEventItemToCart: (DiscountItem, String) -> Unit,
    onAddClick: (isDiscount: Boolean) -> Unit,
    onEditEventItems: (DiscountEvent, List<ShopProduct>) -> Unit,
    onRemoveProduct: (ShopProduct) -> Unit,
    onDeleteEvent: (DiscountEvent) -> Unit = {}
) {
    val shopEvents = remember(events, shop.id) { events.filter { it.shopId == shop.id } }
    val catalogNames = shop.catalog.map { it.name }.toSet()
    var pickingEvent by remember { mutableStateOf<DiscountEvent?>(null) }

    var detailTab by remember { mutableIntStateOf(0) }
    val (themeColor, _) = ShopColors.getTheme(shop.type)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
        ) {
            androidx.compose.material3.TabRow(
                selectedTabIndex = detailTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (detailTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[detailTab])
                                .height(3.dp)
                                .padding(horizontal = 24.dp)
                                .background(MealooTechColors.AccentCyan, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = MealooTechColors.TechBorder.copy(alpha = 0.5f))
                }
            ) {
                androidx.compose.material3.Tab(
                    selected = detailTab == 0,
                    onClick = { detailTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.shop_mode_product),
                            fontWeight = if (detailTab == 0) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    selectedContentColor = MealooTechColors.AccentCyan,
                    unselectedContentColor = Color.Gray.copy(alpha = 0.6f)
                )

                androidx.compose.material3.Tab(
                    selected = detailTab == 1,
                    onClick = { detailTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.shop_mode_discount),
                                fontWeight = if (detailTab == 1) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (shopEvents.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(MealooTechColors.AccentMagenta, CircleShape)
                                )
                            }
                        }
                    },
                    selectedContentColor = MealooTechColors.AccentCyan,
                    unselectedContentColor = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
        // --- Content ---
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (detailTab == 1) {
                    if (shopEvents.isNotEmpty()) {
                        items(shopEvents, key = { it.id }) { event ->
                            DiscountEventCard(
                                event = event,
                                existingProductNames = catalogNames,
                                onPickProducts = { pickingEvent = event },
                                onAddToCart = { item -> onAddEventItemToCart(item, event.id) },
                                onDeleteEvent = { onDeleteEvent(event) }
                            )
                        }
                    } else {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.LocalOffer,
                                text = stringResource(R.string.shop_no_events)
                            )
                        }
                    }
                }
                if (detailTab == 0) {
                    if (shop.catalog.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.Storefront,
                                text = stringResource(R.string.shop_catalog_empty_hint)
                            )
                        }
                    } else {
                        itemsIndexed(shop.catalog, key = { _, it -> it.id }) { index, product ->
                            val appliedEvent = shopEvents.find { event -> event.items.any { it.name == product.name } }

                            Column {
                                ShopProductRow(
                                    product = product,
                                    shopType = shop.type,
                                    discountEvent = appliedEvent,
                                    onEat = { onEat(product) },
                                    onShop = { onAddToCart(product, appliedEvent) },
                                    onDelete = { onRemoveProduct(product) }
                                )
                                if (index < shop.catalog.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MealooTechColors.TechBorder.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(start = 72.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val fabColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (detailTab == 0) MealooTechColors.AccentCyan else MealooTechColors.AccentMagenta,
                label = "fabColor"
            )

            androidx.compose.material3.FloatingActionButton(
                onClick = { onAddClick(detailTab == 1) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .mealooTechGlassBorder(CircleShape, fabColor.copy(alpha = 0.5f)),
                containerColor = fabColor.copy(alpha = 0.2f),
                contentColor = fabColor,
                shape = CircleShape
            ) {
                androidx.compose.animation.Crossfade(targetState = detailTab, label = "fabIcon") { tab ->
                    Icon(
                        imageVector = if (tab == 0) Icons.Default.Add else Icons.Default.Sell,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (pickingEvent != null) {
        EventProductPickerSheet(
            shop = shop,
            event = pickingEvent!!,
            preselectedNames = pickingEvent!!.items.map { it.name }.toSet(),
            onDismiss = { pickingEvent = null },
            onConfirm = { selected ->
                onEditEventItems(pickingEvent!!, selected)
                pickingEvent = null
            },
            onDeleteEvent = {
                onDeleteEvent(pickingEvent!!)
                pickingEvent = null
            }
        )
    }
}
@Composable
private fun DiscountEventCard(
    event: DiscountEvent,
    existingProductNames: Set<String>,
    onPickProducts: () -> Unit,
    onAddToCart: (DiscountItem) -> Unit,
    onDeleteEvent: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .mealooTechGlassBorder(
                RoundedCornerShape(16.dp),
                MealooTechColors.AccentMagenta.copy(alpha = 0.5f)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Header: 科技感漸層發光區塊 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MealooTechColors.AccentMagenta.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MealooTechColors.DeepBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, MealooTechColors.AccentMagenta.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.LocalOffer,
                            contentDescription = null,
                            tint = MealooTechColors.AccentMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = MealooTechColors.AccentMagenta.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MealooTechColors.AccentMagenta.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = ruleLabel(event.rule),
                                style = MaterialTheme.typography.labelSmall,
                                color = MealooTechColors.AccentMagenta,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onPickProducts, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Edit, null, tint = MealooTechColors.AccentCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDeleteEvent, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MealooTechColors.TechBorder)

            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val visibleItems = event.items.filter { it.name in existingProductNames }

                if (visibleItems.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.shop_event_no_items),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onPickProducts) {
                            Text(stringResource(R.string.shop_event_pick_products), color = MealooTechColors.AccentCyan)
                        }
                    }
                } else {
                    visibleItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            OutlinedButton(
                                onClick = { onAddToCart(item) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MealooTechColors.AccentCyan),
                                border = BorderStroke(1.dp, MealooTechColors.AccentCyan.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(item.unitPrice.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (index < visibleItems.lastIndex) {
                            HorizontalDivider(
                                color = MealooTechColors.TechBorder.copy(alpha = 0.3f),
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                val dateText = if (event.endDate != null) {
                    stringResource(R.string.shop_event_period_fmt, event.startDate, event.endDate)
                } else {
                    stringResource(R.string.shop_event_period_open_fmt, event.startDate)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.2f))
        Spacer(Modifier.height(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventProductPickerSheet(
    shop: ShopInfo,
    event: DiscountEvent,
    preselectedNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<ShopProduct>) -> Unit,
    onDeleteEvent: () -> Unit // 新增：删除整个活动
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 使用 derivedStateOf 或直接 remember 并在 key 中加入 preselectedNames 确保同步
    var selectedNames by remember(preselectedNames) {
        mutableStateOf(preselectedNames)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MealooTechColors.DeepBackground,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.discount_pick_products_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // 关闭按钮
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }

            Text(
                text = stringResource(R.string.discount_event_title_prefix, event.title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // List
            if (shop.catalog.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.discount_no_products_simple),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(shop.catalog, key = { it.name }) { product ->
                        val checked = selectedNames.contains(product.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedNames = if (checked) {
                                        selectedNames - product.name
                                    } else {
                                        selectedNames + product.name
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedNames = if (isChecked) {
                                        selectedNames + product.name
                                    } else {
                                        selectedNames - product.name
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                product.price?.let { p ->
                                    Text(
                                        stringResource(R.string.common_currency_amount_desc, product.currency, p),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：彻底删除活动
                TextButton(
                    onClick = onDeleteEvent,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.shopping_delete_event_action))
                }

                // 右侧：保存选择
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selectedProducts = shop.catalog.filter { it.name in selectedNames }
                            onConfirm(selectedProducts)
                        },
                        // 如果没有变化，其实也可以禁用，但为了方便清空，保持启用
                        enabled = true
                    ) {
                        Text(stringResource(R.string.discount_apply_to_products))
                    }
                }
            }
            Spacer(Modifier.height(16.dp)) // Bottom padding
        }
    }
}
private enum class AddShopDialogMode {
    PRODUCT,
    DISCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopProductDialog(
    shop: ShopInfo,
    onDismiss: () -> Unit,
    onConfirmProduct: (ShopProduct) -> Unit,
    onConfirmDiscount: (DiscountEvent) -> Unit = {},
    defaultCurrencyCode: String? = null,
    initialIsDiscount: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    var mode by remember {
        mutableStateOf(if (initialIsDiscount) AddShopDialogMode.DISCOUNT else AddShopDialogMode.PRODUCT)
    }

    // 🎨 現代化科技風格色彩計畫：Product = Cyan, Discount = Magenta
    val primaryColor by animateColorAsState(
        if (mode == AddShopDialogMode.PRODUCT) MealooTechColors.AccentCyan else MealooTechColors.AccentMagenta,
        label = "primary"
    )
    val containerColor by animateColorAsState(
        if (mode == AddShopDialogMode.PRODUCT) MealooTechColors.AccentCyan.copy(alpha = 0.15f) else MealooTechColors.AccentMagenta.copy(alpha = 0.15f),
        label = "container"
    )
    val onContainerColor by animateColorAsState(
        if (mode == AddShopDialogMode.PRODUCT) MealooTechColors.AccentCyan else MealooTechColors.AccentMagenta,
        label = "onContainer"
    )

    // --- Product States ---
    var name by remember { mutableStateOf("") }
    var categoriesInput by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val initialCurrencyCode = defaultCurrencyCode ?: AppCurrency.TWD.code
    var currencyCode by remember(defaultCurrencyCode) { mutableStateOf(initialCurrencyCode) }
    var isFood by remember { mutableStateOf(true) }
    var nutrients by remember { mutableStateOf<Map<String, Double>?>(null) }

    // --- Discount States ---
    var eventTitle by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf<EventRule>(EventRule.None) }
    var ruleParamsValid by remember { mutableStateOf(true) }
    var pendingConfirmDiscount by remember { mutableStateOf(false) }

    // --- Sub-Dialog Triggers ---
    var showMealPicker by remember { mutableStateOf(false) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }

    val confirmEnabled = when (mode) {
        AddShopDialogMode.PRODUCT -> name.isNotBlank()
        AddShopDialogMode.DISCOUNT -> eventTitle.isNotBlank() && startDate.isNotBlank() && ruleParamsValid
    }

    // Discount Confirmation Logic
    LaunchedEffect(pendingConfirmDiscount) {
        if (!pendingConfirmDiscount) return@LaunchedEffect
        yield()
        pendingConfirmDiscount = false

        if (eventTitle.isNotBlank() && startDate.isNotBlank() && ruleParamsValid) {
            val tags = tagsInput.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
            val event = DiscountEvent(
                shopId = shop.id,
                title = eventTitle.trim(),
                startDate = startDate,
                endDate = endDate?.takeIf { it.isNotBlank() },
                note = note.ifBlank { null },
                tags = tags,
                url = null,
                rule = rule,
                items = emptyList()
            )
            onConfirmDiscount(event)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MealooTechColors.DeepBackground,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.8f),
        title = {
            // --- 乾淨現代的 Header ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 輕量化藥丸切換器 (Pill Switcher)
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp).height(44.dp).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        TabButton(
                            text = stringResource(R.string.shopping_product_tab_title),
                            selected = mode == AddShopDialogMode.PRODUCT,
                            activeColor = MealooTechColors.AccentCyan,
                            onClick = { mode = AddShopDialogMode.PRODUCT }
                        )
                        TabButton(
                            text = stringResource(R.string.shopping_discount_tab_title),
                            selected = mode == AddShopDialogMode.DISCOUNT,
                            activeColor = MealooTechColors.AccentMagenta,
                            onClick = { mode = AddShopDialogMode.DISCOUNT }
                        )
                    }
                }

                // 大標題與商店名稱
                Text(
                    text = if (mode == AddShopDialogMode.PRODUCT) stringResource(R.string.add_product_to_shop) else stringResource(R.string.add_discount_event),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                when (mode) {
                    // ============ PRODUCT FORM ============
                    AddShopDialogMode.PRODUCT -> {
                        MealooTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.shop_product_name_label),
                            placeholder = stringResource(R.string.shop_product_name_placeholder),
                            leadingIcon = { Icon(Icons.Default.Label, null, tint = primaryColor) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        MealooTextField(
                            value = categoriesInput,
                            onValueChange = { categoriesInput = it },
                            label = stringResource(R.string.categories),
                            placeholder = stringResource(R.string.shopping_category_placeholder),
                            leadingIcon = { Icon(Icons.Default.Category, null, tint = primaryColor) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 🟢 使用動態主題色的 Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SelectableColorChip(
                                selected = isFood,
                                label = stringResource(R.string.shopping_product_food_label),
                                icon = Icons.Default.Restaurant,
                                selectedColor = containerColor,
                                selectedContentColor = onContainerColor,
                                onClick = { isFood = true }
                            )
                            SelectableColorChip(
                                selected = !isFood,
                                label = stringResource(R.string.shopping_product_other_label),
                                icon = Icons.Default.Inventory2,
                                selectedColor = containerColor,
                                selectedContentColor = onContainerColor,
                                onClick = { isFood = false; nutrients = null }
                            )
                        }

                        PriceWithCurrencyRow(
                            price = price,
                            currency = currencyCode,
                            onChange = { newCur, newPri -> currencyCode = newCur; price = newPri }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MealooTechColors.TechBorder
                        )

                        if (isFood) {
                            Text(
                                stringResource(R.string.shopping_nutrition_optional_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f)
                            )

                            if (nutrients != null) {
                                Box {
                                    MacroPreviewBox(nutrients!!, Modifier.fillMaxWidth())
                                    IconButton(
                                        onClick = { nutrients = null },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 🟢 營養搜尋也改用動態主題色
                                    ColorfulActionCard(
                                        title = stringResource(R.string.shopping_nutrition_search_action),
                                        icon = Icons.Default.Search,
                                        color = containerColor,
                                        contentColor = onContainerColor,
                                        onClick = { showMealPicker = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ColorfulActionCard(
                                        title = stringResource(R.string.shopping_nutrition_custom_action),
                                        icon = Icons.Default.EditNote,
                                        color = containerColor,
                                        contentColor = onContainerColor,
                                        onClick = { showCustomFoodDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // ============ DISCOUNT FORM ============
                    AddShopDialogMode.DISCOUNT -> {
                        MealooTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = stringResource(R.string.discount_event_title_label),
                            placeholder = stringResource(R.string.discount_event_title_placeholder),
                            leadingIcon = { Icon(Icons.Default.Sell, null, tint = primaryColor) }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateField(
                                expiry = startDate,
                                onDateSelected = { startDate = it },
                                label = stringResource(R.string.discount_start_date_label),
                                modifier = Modifier.weight(1f),
                                containerColor = MealooTechColors.GlassCard,
                                contentColor = Color.White
                            )
                            DateField(
                                expiry = endDate,
                                onDateSelected = { endDate = it },
                                label = stringResource(R.string.shopping_discount_end_date_optional_label),
                                modifier = Modifier.weight(1f),
                                containerColor = MealooTechColors.GlassCard,
                                contentColor = Color.White
                            )
                        }

                        DiscountRuleDropdown(
                            value = rule,
                            onValueChange = { rule = it },
                            label = stringResource(R.string.shopping_discount_rule_label),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MealooTechColors.GlassCard,
                            contentColor = Color.White
                        )

                        Surface(
                            color = MealooTechColors.GlassCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MealooTechColors.TechBorder),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                RuleParamsEditorV2(
                                    rule = rule,
                                    onChange = { rule = it },
                                    onValidityChange = { ruleParamsValid = it }
                                )
                            }
                        }

                        MealooTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = stringResource(R.string.discount_tags_label),
                            placeholder = stringResource(R.string.discount_tags_placeholder),
                            leadingIcon = { Icon(Icons.Default.LocalOffer, null, tint = primaryColor) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    when (mode) {
                        AddShopDialogMode.PRODUCT -> {
                            focusManager.clearFocus(force = true)

                            val catList = categoriesInput.split(",", "，")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .ifEmpty { null }

                            val parsedPrice = price.trim()
                                .replace(",", "")
                                .takeIf { it.isNotBlank() }
                                ?.toBigDecimalOrNull()

                            onConfirmProduct(
                                ShopProduct(
                                    name = name.trim(),
                                    price = parsedPrice,
                                    currency = currencyCode,
                                    category = catList,
                                    shopId = shop.id,
                                    nutrients = nutrients
                                )
                            )
                        }
                        AddShopDialogMode.DISCOUNT -> {
                            focusManager.clearFocus(force = true)
                            pendingConfirmDiscount = true
                        }
                    }
                },
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.2f), contentColor = primaryColor),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp).mealooTechGlassBorder(CircleShape, primaryColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (mode == AddShopDialogMode.PRODUCT) stringResource(R.string.add_product_to_shop) else stringResource(R.string.discount_create_button),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showMealPicker) {
        MealPickerDialog(
            onDismiss = { showMealPicker = false },
            onConfirm = { selectedFoods, _ ->
                showMealPicker = false
                selectedFoods.firstOrNull()?.let { food ->
                    nutrients = food.standardNutrients
                    if (name.isBlank()) name = food.name
                }
            }
        )
    }
    if (showCustomFoodDialog) {
        AddFoodDialog(
            onDismiss = { showCustomFoodDialog = false },
            onAdd = { userFoodItem ->
                showCustomFoodDialog = false

                val mapping = mapOf(
                    "enercKcal" to "Energy",
                    "prot"      to "Protein",
                    "fat"       to "Total lipid (fat)",
                    "chocdf"    to "Carbohydrate, by difference",
                    "na"        to "Sodium, Na",
                    "fib"       to "Fiber, total dietary",
                    "sugar"     to "Sugars, total including NLEA",
                    "fasat"     to "Fatty acids, total saturated",
                    "fattrans"  to "Fatty acids, total trans"
                )
                val standardizedNutrients = userFoodItem.nutrientsPer100g.mapKeys { (key, _) ->
                    mapping[key] ?: key
                }

                nutrients = standardizedNutrients

                // 自動填入名稱
                if (name.isBlank()) name = userFoodItem.name
            }
        )
    }
}

// --- ✨ New UI Helpers for this Dialog ---

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(if (selected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
    val contentColor by animateColorAsState(if (selected) activeColor else Color.White.copy(alpha = 0.5f))

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .then(if (selected) Modifier.mealooTechGlassBorder(CircleShape, activeColor.copy(alpha = 0.5f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableColorChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    selectedColor: Color,
    selectedContentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) selectedColor else MealooTechColors.GlassCard)
            .border(1.dp, if (selected) selectedContentColor else MealooTechColors.TechBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) selectedContentColor else Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) selectedContentColor else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ColorfulActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(72.dp)
            .mealooTechGlassBorder(RoundedCornerShape(12.dp), color.copy(alpha = 0.5f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun ShopProductRow(
    product: ShopProduct,
    shopType: ShopCategory,
    discountEvent: DiscountEvent? = null,
    onEat: () -> Unit,
    onShop: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val priceResult = remember(product.price, discountEvent) {
        if (product.price != null) calculateDiscountedTotal(product.price, BigDecimal.ONE, discountEvent?.rule) else null
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShop() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (priceResult != null) {
                        if (priceResult.originalTotal != priceResult.finalTotal) {
                            Text(
                                stringResource(R.string.common_currency_amount_desc, product.currency, priceResult.finalTotal),
                                color = MealooTechColors.AccentMagenta,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                priceResult.originalTotal.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                stringResource(R.string.common_currency_amount_desc, product.currency, priceResult.originalTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    val kcal = product.nutrients?.get("Energy")
                    if (kcal != null && kcal > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Color.LightGray)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.common_kcal_value_desc, fmt(kcal)),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (priceResult?.label != null) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MealooTechColors.AccentMagenta.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, MealooTechColors.AccentMagenta.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = priceResult.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MealooTechColors.AccentMagenta,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MealooTechColors.TechBorder.copy(alpha = 0.5f))
                    .border(1.dp, MealooTechColors.TechBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.2f),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                ) {
                    IconButton(
                        onClick = onShop,
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(2.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.LightGray)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add)) },
                        leadingIcon = { Icon(Icons.Default.AddShoppingCart, null) },
                        onClick = { showMenu = false; onShop() }
                    )

                    if (shopType == ShopCategory.RESTAURANT || shopType == ShopCategory.HYBRID || !product.nutrients.isNullOrEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.meal_record)) },
                            leadingIcon = { Icon(Icons.Default.Restaurant, null) },
                            onClick = { showMenu = false; onEat() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}
@Composable
fun ShoppingListContent(
    cartStore: ShoppingCartStore,
    savingsRepo: SavingsRepository,
    eventStore: DiscountEventDataStore,
    shopRepo: ShopRepository,
    onAddCartItem: () -> Unit,
    userPrefState: State<UserPreference?>
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val items by cartStore.itemsFlow().collectAsState(initial = emptyList())
    val events by eventStore.eventsFlow().collectAsState(initial = emptyList())
    val shops by shopRepo.shopsFlow().collectAsState(initial = emptyList())
    val inventoryRepo = remember { InventoryRepository.getInstance(context) }
    val eventMap = remember(events) { events.associateBy { it.id } }
    val fallbackCurrency = userPrefState.value?.homeCurrency?.code ?: AppCurrency.TWD.code
    val targetCurrency = remember(items, fallbackCurrency) {
        items.firstOrNull { !it.currency.isNullOrBlank() }?.currency ?: fallbackCurrency
    }

    // UI States
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showSavingsDialog by remember { mutableStateOf(false) }
    var savingsNote by rememberSaveable { mutableStateOf("") }

    val shopById = remember(shops) { shops.associateBy { it.id } }
    val eventById = remember(events) { events.associateBy { it.id } }

    val snackbarHostState = remember { SnackbarHostState() }

    // --- 1. 即时计算总价与节省金额 (用于底部栏显示) ---
    val (displayTotal, displaySavings, hasSavings) = remember(items, events, targetCurrency) {
        val engineItems = items
            .filter { it.quantity > BigDecimal.ZERO }
            .map { item ->
                val rule = item.discountEventId?.let { eventById[it]?.rule }

                CartItem(
                    id = item.id,
                    name = item.name,
                    unitPrice = item.basePrice ?: BigDecimal.ZERO,
                    currency = item.currency?.ifBlank { targetCurrency },
                    Qty = item.quantity,
                    discount = rule
                )
            }

        val input = PricingInput(items = engineItems, currency = targetCurrency)
        val result = PricingEngine.calculate(input)

        if (result is Result.Ok) {
            val pricing = result.value
            val totalFinal = pricing.cartSubtotalAfterItems.toDouble()
            val totalOriginal = pricing.rawSubtotal.toDouble()
            val savings = (totalOriginal - totalFinal).coerceAtLeast(0.0)

            Triple(totalFinal, savings, savings > 0.01)
        } else {
            Triple(0.0, 0.0, false)
        }
    }


    // --- 2. 逻辑推导 (用于记帐归属) ---
    val hasAnyPrice = remember(items) { items.any { (it.basePrice ?: BigDecimal.ZERO) > BigDecimal.ZERO } }
    val checkoutEnabled = items.isNotEmpty() && hasAnyPrice

    val selectedEventId by cartStore.selectedEventIdFlow().collectAsState(initial = null)
    val inferredEventId = remember(items, selectedEventId) {
        selectedEventId ?: items.mapNotNull { it.discountEventId }.distinct().singleOrNull()
    }
    val inferredEvent = remember(inferredEventId, eventById) { inferredEventId?.let { eventById[it] } }
    val inferredShopId = remember(inferredEvent, items) {
        inferredEvent?.shopId ?: items.mapNotNull { it.shopId }.distinct().singleOrNull()
    }

    // --- 3. 核心功能函数 ---

    // A. 处理入库 (最后一步)
    val processCheckout = { configs: List<CheckoutConfig> ->
        scope.launch {
            configs.forEach { config ->
                val item = items.find { it.id == config.itemId } ?: return@forEach
                // 计算该商品的实际入库成本 (折扣后单价 * 数量)
                val rule = item.discountEventId?.let { eventById[it]?.rule }
                val unitPrice = calculateDiscountedTotal(item.basePrice ?: BigDecimal.ZERO, BigDecimal.ONE,rule).finalTotal
                val totalCost = unitPrice * item.quantity
                val currency = item.currency?.ifBlank { fallbackCurrency }
                var nutrientsToCopy: Map<String, Double>? = null
                var finalGramsPerUnit: Double? = null
                var categoryToCopy: String? = null

                if (item.shopId != null && item.shopProductId != null) {
                    val sourceShop = shops.find { it.id == item.shopId }
                    val sourceProduct = sourceShop?.catalog?.find { it.id == item.shopProductId }
                    if (sourceProduct != null) {
                        if (!sourceProduct.nutrients.isNullOrEmpty()) {
                            nutrientsToCopy = sourceProduct.nutrients
                        }
                        categoryToCopy = sourceProduct.category?.firstOrNull()
                    }
                }

                if (config.gramsPerPc.isNotBlank()) {
                    finalGramsPerUnit = config.gramsPerPc.toDoubleOrNull()
                }

                val inv = InventoryItem(
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    location = config.location,
                    cost = totalCost, // 保存折扣后的实际成本
                    currency = item.currency?.ifBlank { targetCurrency } ?: targetCurrency,
                    sourceType = SOURCE_TYPE_SHOP_PRODUCT,
                    sourceId = item.shopProductId,
                    standardNutrientsPer100g = nutrientsToCopy,
                    gramsPerUnit = finalGramsPerUnit,
                    category = categoryToCopy
                )

                inventoryRepo.upsert(inv)
                cartStore.delete(item.id)
            }
            cartStore.setSelectedEventId(null)
            snackbarHostState.showSnackbar(context.getString(R.string.shopping_checkin_cart_cleared_desc))
        }
    }

    // B. 处理记帐 (第一步，若有节省)
    suspend fun processSavingsCheckout(): Boolean {
        android.util.Log.d(TAG_SAVINGS,
            "calcSavings IN: items=${items.size} " +
                    "firstItem={name=${items.firstOrNull()?.name}, base=${items.firstOrNull()?.basePrice}, qty=${items.firstOrNull()?.quantity}, cur=${items.firstOrNull()?.currency}}"
        )

        // 使用 PricingEngine 进行严谨计算 (复用原逻辑)
        val zone = ZoneId.of("Asia/Taipei")
        val today = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cartItems = items
            .filter { it.quantity > BigDecimal.ZERO }
            .map { item ->
                val rule = inferredEventId?.let { eventById[it]?.rule }
                    ?: item.discountEventId?.let { eventById[it]?.rule }

                CartItem(
                    id = item.id,
                    image = item.imageUri,
                    name = item.name,
                    unitPrice = item.basePrice ?: BigDecimal.ZERO,
                    currency = item.currency,
                    Qty = item.quantity,
                    discount = rule,
                    plannedDate = item.plannedDate,
                    plannedSlot = item.plannedSlot
                )
            }

        android.util.Log.d(TAG_SAVINGS,
            "processSavingsCheckout BEFORE_ENGINE targetCurrency=$targetCurrency cartCurrencies=" +
                    cartItems.map { it.currency }.distinct()
        )

        val res = withContext(Dispatchers.Default) {
            PricingEngine.calculate(PricingInput(items = cartItems, currency = targetCurrency))
        }
        val pricing = when (res) {
            is Result.Err -> {
                scope.launch { snackbarHostState.showSnackbar(res.error.message) }
                return false
            }
            is Result.Ok -> res.value
        }

        val original = pricing.rawSubtotal
        val afterDiscount = pricing.cartSubtotalAfterItems
        val savings = original - afterDiscount

        if (original <= BigDecimal.ZERO || savings < BigDecimal.ZERO) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.savings_checkout_error_invalid)) }
            return false
        }

        val record = SavingsRecord(
            date = today,
            shopId = inferredShopId,
            eventId = inferredEventId,
            currency = targetCurrency,
            original = original,
            afterDiscount = afterDiscount,
            savings = savings,
            savingsRate = if (original >BigDecimal.ZERO) savings / original else BigDecimal.ZERO,
            itemCount = pricing.items.size,
            note = savingsNote.trim().ifBlank { null }
        )

        withContext(Dispatchers.IO) {
            savingsRepo.addRecord(record)
        }

        val savingsMsg = if (targetCurrency == AppCurrency.TWD.code || targetCurrency == AppCurrency.TWD.code) {
            savings.toString()
        } else {
            context.getString(
                R.string.common_currency_amount_desc,
                targetCurrency,
                String.format(Locale.US, "%.2f", savings)
            )
        }

        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.savings_recorded_fmt, savingsMsg))
        }
        return true
    }
    // 导航逻辑
    val groupedItems = remember(items, shopById, eventById) {
        items.groupBy { item ->
            val shopId = item.shopId ?: eventById[item.discountEventId]?.shopId
            shopId?.let { shopById[it] }
        }.entries.sortedBy { it.key?.name ?: "ZZZ" }
    }

    val navigableShops = remember(groupedItems) {
        groupedItems
            .mapNotNull { it.key }
            .filter { !it.address.isNullOrBlank() || !it.googlePlaceId.isNullOrBlank() }
            .distinctBy { it.id }
            .sortedBy { it.name }
    }

    val navigateToShop: (ShopInfo) -> Unit = remember(context) {
        { shop -> navigateToShop(context, shop.name, shop.address, shop.googlePlaceId) }
    }
    var isOptimizing by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val startOptimizedNavigation = {
        isOptimizing = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                scope.launch {
                    optimizeAndNavigate(context, loc?.latitude, loc?.longitude, navigableShops)
                    isOptimizing = false
                }
            }
            .addOnFailureListener {
                scope.launch {
                    optimizeAndNavigate(context, null, null, navigableShops)
                    isOptimizing = false
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startOptimizedNavigation()
        } else {
            scope.launch { optimizeAndNavigate(context, null, null, navigableShops) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                item { Spacer(Modifier.height(16.dp)) }

                if (groupedItems.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    groupedItems.forEach { (shop, shopItems) ->
                        item(key = shop?.id ?: "shop_other_${shopItems.hashCode()}") {
                            ShopGroupCard(
                                shopName = shop?.name ?: stringResource(R.string.shopping_other_items_label),
                                items = shopItems,
                                onUpdateQty = { id, delta ->
                                    scope.launch {
                                        val targetItem = items.find { it.id == id }
                                        if (targetItem != null) {
                                            val newQty = (targetItem.quantity + delta).let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it }
                                            if (newQty == BigDecimal.ZERO) {
                                                cartStore.delete(id)
                                            } else {
                                                cartStore.upsert(targetItem.copy(quantity = newQty))
                                            }
                                        }
                                    }
                                },
                                onMarkBought = { item, event ->
                                    scope.launch {
                                        val currency = item.currency?.ifBlank { fallbackCurrency }
                                        val discounted = calculateDiscountedTotal(item.basePrice ?: BigDecimal.ZERO, item.quantity, event?.rule)
                                        val totalCost = discounted.finalTotal

                                        val inv = InventoryItem(
                                            name = item.name,
                                            quantity = item.quantity,
                                            unit = item.unit,
                                            location = "Pantry",
                                            cost = totalCost,
                                            currency = item.currency?.ifBlank { targetCurrency } ?: targetCurrency,
                                            sourceType = if (item.shopProductId != null) SOURCE_TYPE_SHOP_PRODUCT else null,
                                            sourceId = item.shopProductId
                                        )
                                        inventoryRepo.upsert(inv)
                                        cartStore.delete(item.id)
                                    }
                                },
                                eventMap = eventMap
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    item { Spacer(Modifier.height(16.dp).navigationBarsPadding()) }
                }
            }
        }

        if (items.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    onClick = {
                        if (checkoutEnabled) {
                            if (hasSavings) showSavingsDialog = true else showCheckoutDialog = true
                        }
                    },
                    shape = CircleShape,
                    color = MealooTechColors.DeepBackground.copy(alpha = 0.9f),
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .mealooTechGlassBorder(CircleShape, MealooTechColors.TechBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        if (navigableShops.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        startOptimizedNavigation()
                                    } else {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                },
                                enabled = !isOptimizing,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MealooTechColors.AccentCyan.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, MealooTechColors.AccentCyan.copy(alpha = 0.3f), CircleShape)
                            ) {
                                if (isOptimizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MealooTechColors.AccentCyan, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.NearMe, contentDescription = "Navigation", tint = MealooTechColors.AccentCyan)
                                }
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "Total ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "$targetCurrency ${fmt(displayTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            if (hasSavings) {
                                Text(
                                    text = stringResource(R.string.savings_checkout_estimated_savings_fmt, displaySavings),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00FF88), // Neon Green
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(if (checkoutEnabled) MealooTechColors.AccentCyan.copy(alpha = 0.2f) else MealooTechColors.TechBorder.copy(alpha = 0.3f))
                                .border(1.dp, if (checkoutEnabled) MealooTechColors.AccentCyan.copy(alpha = 0.5f) else MealooTechColors.TechBorder.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.savings_checkout_button),
                                fontWeight = FontWeight.Bold,
                                color = if (checkoutEnabled) MealooTechColors.AccentCyan else Color.Gray.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (hasSavings) Icons.Default.Savings else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (checkoutEnabled) MealooTechColors.AccentCyan else Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        if (false && items.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val ntdLabel = stringResource(R.string.common_currency_ntd_label)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stringResource(R.string.savings_checkout_total_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_desc,
                                    ntdLabel,
                                    fmt(displayTotal)
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (hasSavings) {
                            Text(
                                text = stringResource(
                                    R.string.savings_checkout_estimated_savings_fmt,
                                    stringResource(
                                        R.string.common_currency_amount_desc,
                                        ntdLabel,
                                        fmt(displaySavings)
                                    )
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (hasSavings) {
                                android.util.Log.d(TAG_SAVINGS,
                                    "openSavingsDialog hasSavings=$hasSavings items=${items.size} " +
                                            "currencies=" + items.groupBy { it.currency }.mapValues { it.value.size }
                                )

                                showSavingsDialog = true
                                android.util.Log.d(TAG_SAVINGS,
                                    "openSavingsDialog hasSavings=$hasSavings items=${items.size} " +
                                            "currencies=" + items.groupBy { it.currency }.mapValues { it.value.size }
                                )

                            } else {
                                showCheckoutDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(stringResource(R.string.savings_checkout_button))
                        if (hasSavings) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Savings, null, modifier = Modifier.size(16.dp))
                        } else {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Input, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
    if (showCheckoutDialog) {
        BatchCheckoutDialog(
            selectedItems = items,
            shops = shops,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { configs ->
                processCheckout(configs)
                showCheckoutDialog = false
            }
        )
    }
    if (showSavingsDialog) {
        AlertDialog(
            onDismissRequest = { showSavingsDialog = false },
            icon = { Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.savings_checkout_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val displaySavingsText = displaySavings.toString()
                    Text(
                        text = stringResource(R.string.savings_checkout_dialog_body_fmt, displaySavingsText),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = savingsNote,
                        onValueChange = { savingsNote = it },
                        label = { Text(stringResource(R.string.savings_note_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.savings_note_placeholder)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSavingsDialog = false
                        scope.launch {
                            val currencies = items.map { it.currency }.distinct()
                            android.util.Log.d(TAG_SAVINGS,
                                "confirmSavings currencies=$currencies total=$displayTotal savings=$displaySavings " +
                                        "eventId=${selectedEventId}"
                            )
                            Log.d(TAG_SAVINGS, "confirmSavings BEFORE_PROCESS")
                            val success = processSavingsCheckout()
                            Log.d(TAG_SAVINGS, "confirmSavings AFTER_PROCESS success=$success")
                            // (如果失败了通常是金额问题，但也允许用户继续入库)
                            if (success || items.isNotEmpty()) {
                                showCheckoutDialog = true
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.savings_checkout_dialog_confirm))
                }
            },
            dismissButton = {
                // 跳过记帐，直接去入库
                TextButton(
                    onClick = {
                        showSavingsDialog = false
                        showCheckoutDialog = true
                    }
                ) {
                    Text(stringResource(R.string.savings_checkout_dialog_skip))
                }
            }
        )
    }
}
@Composable
fun ShopGroupCard(
    shopName: String,
    items: List<ShoppingListItem>,
    onUpdateQty: (String, BigDecimal) -> Unit,
    onMarkBought: (ShoppingListItem, DiscountEvent?) -> Unit,
    eventMap: Map<String, DiscountEvent> = emptyMap()
) {
    var expanded by remember { mutableStateOf(true) }

    // 放棄 Card 容器，改用 Column
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 8.dp) // 增加上下呼吸感
    ) {
        // --- 極簡大氣的 Header ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // 移除點擊水波紋，更像純文字標題
                ) { expanded = !expanded }
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = shopName,
                style = MaterialTheme.typography.titleLarge, // 加大字體
                fontWeight = FontWeight.ExtraBold,           // 極粗體
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = Color.Gray
            )
        }

        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    val event = item.discountEventId?.let { eventMap[it] }
                    ShopProductRowItem(
                        item = item,
                        onUpdateQty = onUpdateQty,
                        event = event,
                        onMarkBought = { onMarkBought(item, event) }
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                thickness = 0.5.dp,
                color = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
fun ShopProductRowItem(
    item: ShoppingListItem,
    onUpdateQty: (String, BigDecimal) -> Unit,
    event: DiscountEvent? = null,
    onMarkBought: () -> Unit
){
    val priceResult = remember(item.basePrice, event?.rule) {
        item.basePrice?.let { base -> calculateDiscountedTotal(base,BigDecimal.ONE, event?.rule) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 不需要 clickable 了
        verticalAlignment = Alignment.CenterVertically
    ){
        // Left: Name + Price + Discount info
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional: Mini Thumbnail
            if (item.imageUri != null) {
                CartItemThumbnail(
                    imageUri = item.imageUri,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 价格显示 (保持原样)
                if (priceResult != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (priceResult.originalTotal != priceResult.finalTotal) {
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_desc,
                                    item.currency?:"",
                                    priceResult.finalTotal
                                ),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_desc,
                                    item.currency?:"",
                                    priceResult.originalTotal
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_desc,
                                    item.currency?:"",
                                    priceResult.originalTotal
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else if (item.basePrice != null && item.basePrice > BigDecimal.ZERO) {
                    Text(
                        text = stringResource(
                            R.string.common_currency_amount_desc,
                            item.currency?:"",
                            item.basePrice
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // 折扣活动标签 (保持原样)
                if (event != null && event.rule !is EventRule.None) {
                    Spacer(Modifier.height(2.dp))
                    val discountLabel = ruleLabel(event.rule)
                    val context = LocalContext.current
                    val (_, stateText) = remember(event.startDate, event.endDate, context) {
                        deriveEventStateAndText(context, event.startDate, event.endDate)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = discountLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        if (stateText.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onMarkBought),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalQtyStepper(
            qty = item.quantity,
            onMinus = { onUpdateQty(item.id, BigDecimal.ONE.negate()) },
            onPlus = { onUpdateQty(item.id, BigDecimal.ONE) }
        )
    }
}
// 4. New Component: Horizontal Stepper (- 1 +)
@Composable
fun HorizontalQtyStepper(
    qty: BigDecimal,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50) // Pill shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            )
    ) {
        // Minus Button
        IconButton(
            onClick = onMinus,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.common_decrease_action),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Amount Text
        Text(
            text = fmt(qty), // Using existing fmt() helper
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = TextAlign.Center
        )

        // Plus Button
        IconButton(
            onClick = onPlus,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.common_increase_action),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopDialog(
    onDismiss: () -> Unit,
    onConfirm: (ShopInfo) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(ShopCategory.RETAIL) }
    if (showMapPicker) {
        // 顯示全螢幕地圖選點器
        Dialog(
            onDismissRequest = { showMapPicker = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                OsmLocationPicker(
                    onLocationPicked = { pickedAddress ->
                        address = pickedAddress
                        showMapPicker = false
                    },
                    onDismiss = { showMapPicker = false }
                )
            }
        }
    }else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MealooTechColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = { Text(stringResource(R.string.add_new_shopInfo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MealooTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.name),
                        placeholder = stringResource(R.string.shop_name_placeholder),
                        leadingIcon = { Icon(Icons.Default.Store, null, tint = MealooTechColors.AccentCyan) }
                    )

                    // 分類選擇
                    Column {
                        Text(stringResource(R.string.type), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShopCategory.values().forEach { cat ->
                                val (label, icon) = when (cat) {
                                    ShopCategory.RETAIL -> stringResource(R.string.retail) to Icons.Default.ShoppingCart
                                    ShopCategory.RESTAURANT -> stringResource(R.string.restaurant) to Icons.Default.Restaurant
                                    ShopCategory.HYBRID -> stringResource(R.string.hybrid) to Icons.Default.Storefront
                                }

                                val isSelected = type == cat

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(IntrinsicSize.Min)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MealooTechColors.AccentCyan.copy(alpha = 0.2f) else MealooTechColors.GlassCard)
                                            .border(1.dp, if (isSelected) MealooTechColors.AccentCyan else MealooTechColors.TechBorder, RoundedCornerShape(12.dp))
                                            .clickable { type = cat },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isSelected) MealooTechColors.AccentCyan else Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MealooTechColors.AccentCyan else Color.White.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    MealooTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = stringResource(R.string.shop_address_label_optional),
                        placeholder = stringResource(R.string.shop_address_placeholder),
                        leadingIcon = { Icon(Icons.Default.Place, null, tint = MealooTechColors.AccentCyan) },
                        trailingIcon = {
                            IconButton(onClick = { showMapPicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Pick from Map",
                                    tint = MealooTechColors.AccentCyan
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(ShopInfo(name = name, address = address.ifBlank { null }, type = type))
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.common_create))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// 2. 重構：原 EventMiniCard 改為 StoreCard，顯示 ShopInfo
@Composable
private fun StoreCard(
    shop: ShopInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (accentColor, bgColor) = ShopColors.getTheme(shop.type)
    val itemCount = shop.catalog.size

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MealooTechColors.GlassCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().mealooTechGlassBorder(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon Box with Themed Background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(shop.type) {
                        ShopCategory.RETAIL -> Icons.Default.ShoppingCart
                        ShopCategory.RESTAURANT -> Icons.Default.Restaurant
                        ShopCategory.HYBRID -> Icons.Default.Storefront
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // 2. Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                // Address or Type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = shop.address ?: stringResource(R.string.loc_pantry),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Item Count Badge (Pill)
            if (itemCount > 0) {
                Surface(
                    color = MealooTechColors.AccentCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(horizontal = 8.dp).border(0.5.dp, MealooTechColors.AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = stringResource(R.string.shopping_item_count_desc, itemCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MealooTechColors.AccentCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun ShopListContent(
    shops: List<ShopInfo>,
    onShopClick: (ShopInfo) -> Unit,
    onAddShop: () -> Unit,
    onDeleteShop: (ShopInfo) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (shops.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.shopping_shop_empty_desc), color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                androidx.compose.material3.Button(
                    onClick = onAddShop,
                    colors = ButtonDefaults.buttonColors(containerColor = MealooTechColors.AccentCyan.copy(alpha = 0.2f), contentColor = MealooTechColors.AccentCyan),
                    modifier = Modifier.mealooTechGlassBorder(CircleShape, MealooTechColors.AccentCyan.copy(alpha = 0.5f)),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row with Add Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.my_shop_fmt,shops.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onAddShop) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                }

                items(shops, key = { it.id }) { shop ->
                    StoreCard(
                        shop = shop,
                        onClick = { onShopClick(shop) },
                        onDelete = { onDeleteShop(shop) }
                    )
                }
            }
        }
    }
}
@Composable
private fun RuleParamsEditorV2(
    rule: EventRule,
    onChange: (EventRule) -> Unit,
    modifier: Modifier = Modifier,
    onValidityChange: (Boolean) -> Unit = {}
) {
    var currency by remember { mutableStateOf(AppCurrency.TWD.code) }
    val currencies = AppCurrency.values().filter { it != AppCurrency.OTHER }.map { it.symbol }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.discount_rule_params_title),
                style = MaterialTheme.typography.labelMedium,
                color = MealooTechColors.AccentCyan,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.width(100.dp)) {
                CompactDropdownFieldMenu(
                    value = currency,
                    onValueChange = { currency = it },
                    options = currencies,
                    label = null,
                    placeholder = "Currency",
                    fieldHeight = 36.dp,
                    rounded = 8.dp
                )
            }
        }

        when (rule) {
            is EventRule.None -> {
                SideEffect { onValidityChange(true) }
                Text(
                    text = stringResource(R.string.discount_no_params),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            is EventRule.PercentOffEach -> {
                PercentOffEachEditor(
                    rule = rule,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.AmountOffEach -> {
                AmountOffEachEditor(
                    rule = rule,
                    currency = currency,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.SecondPercent -> {
                SecondPercentEditor(
                    rule = rule,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.SecondMinus -> {
                SecondMinusEditor(
                    rule = rule,
                    currency = currency,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.BuyXGetY -> {
                BuyXGetYEditor(
                    rule = rule,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.BundleFixedPrice -> {
                BundleFixedPriceEditor(
                    rule = rule,
                    currency = currency,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.TierByQty -> {
                TierByQtyEditor(
                    rule = rule,
                    currency = currency,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.TierBySpend -> {
                TierBySpendEditor(
                    rule = rule,
                    currency = currency,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.Conditional -> {
                ConditionalEditor(
                    rule = rule,
                    onChange = onChange,
                    onValidityChange = onValidityChange
                )
            }
            is EventRule.FreeShipping -> {
                SideEffect { onValidityChange(true) }
                Text(
                    text = stringResource(R.string.discount_free_shipping_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseRuleDouble(raw: String): Double? {
    val s = raw.trim()
        .replace("%", "")
        .replace(",", "")
        .replace("，", "")
    return s.toDoubleOrNull()
}

private fun parseRuleInt(raw: String): Int? {
    val s = raw.trim()
        .replace(",", "")
        .replace("，", "")
    return s.toIntOrNull()
        ?: s.toDoubleOrNull()?.let { d ->
            if (d.isFinite() && d % 1.0 == 0.0) d.toInt() else null
        }
}

@Composable
private fun RuleNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    helperText: String? = null,
    integer: Boolean = false,
    required: Boolean = true,
    min: Double? = null,
    max: Double? = null,
    stepper: Boolean = false,
    step: Double = 1.0,
    modifier: Modifier = Modifier,
    onCommit: (Double) -> Unit,
    onValidityChange: (Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var showError by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    val errorRequired = stringResource(R.string.discount_rule_params_error_required)
    val errorNumber = stringResource(R.string.discount_rule_params_error_number)
    val errorInteger = stringResource(R.string.discount_rule_params_error_integer)
    val errorPercentRange = stringResource(R.string.discount_rule_params_error_percent_range)
    val errorNonNegative = stringResource(R.string.discount_rule_params_error_non_negative)
    val errorPositive = stringResource(R.string.discount_rule_params_error_positive)
    val errorMin1 = stringResource(R.string.discount_rule_params_error_min_1)
    val errorMin2 = stringResource(R.string.discount_rule_params_error_min_2)

    data class Validation(val ok: Boolean, val parsed: Double?, val error: String?)
    fun validate(text: String): Validation {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return if (required) {
                Validation(false, null, errorRequired)
            } else {
                Validation(true, null, null)
            }
        }

        val parsed = if (integer) {
            parseRuleInt(trimmed)?.toDouble()
        } else {
            parseRuleDouble(trimmed)
        }

        if (parsed == null) {
            return Validation(
                ok = false,
                parsed = null,
                error = if (integer) errorInteger else errorNumber
            )
        }

        if (min != null && parsed < min) {
            val msg = when (min) {
                0.0 -> errorNonNegative
                1.0 -> errorMin1
                2.0 -> errorMin2
                else -> errorPositive
            }
            return Validation(false, parsed, msg)
        }

        if (max != null && parsed > max) {
            val msg = when (max) {
                100.0 -> errorPercentRange
                else -> errorNumber
            }
            return Validation(false, parsed, msg)
        }

        return Validation(true, parsed, null)
    }

    fun commit() {
        val v = validate(value)
        if (v.ok && v.parsed != null) {
            onCommit(v.parsed)
        } else if (!v.ok) {
            showError = true
        }
    }

    val validity = validate(value)
    SideEffect { onValidityChange(validity.ok) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 1. 标签移到上方，更清晰
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 2. 左侧减号按钮 (如果有 Stepper)
            if (stepper) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        val base = validity.parsed ?: (min ?: 0.0)
                        val next = (base - step).let { v ->
                            when {
                                min != null && v < min -> min
                                max != null && v > max -> max
                                else -> v
                            }
                        }
                        val text = if (integer) next.toInt().toString() else next.toStringSafe()
                        onValueChange(text)
                        showError = false
                        val vNew = validate(text)
                        if (vNew.ok && vNew.parsed != null) onCommit(vNew.parsed)
                    },
                    modifier = Modifier.size(40.dp).mealooTechGlassBorder(RoundedCornerShape(8.dp), MealooTechColors.AccentCyan.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.common_decrease_action), tint = MealooTechColors.AccentCyan)
                }
                Spacer(Modifier.width(8.dp))
            }


            // 3. 中间输入框
            OutlinedTextField(
                value = value,
                onValueChange = {
                    showError = false
                    onValueChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                trailingIcon = suffix?.let { s ->
                    { Text(s, style = MaterialTheme.typography.bodyMedium, color = MealooTechColors.AccentCyan, modifier = Modifier.padding(end = 8.dp)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commit()
                        focusManager.clearFocus(force = true)
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MealooTechColors.TechBorder,
                    focusedBorderColor = MealooTechColors.AccentCyan,
                    focusedContainerColor = MealooTechColors.GlassCard,
                    unfocusedContainerColor = MealooTechColors.GlassCard,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = MealooTechColors.AccentCyan
                ),
                isError = showError && validity.error != null
            )

            // 4. 右侧加号按钮 (如果有 Stepper)
            if (stepper) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.IconButton(
                    onClick = {
                        val base = validity.parsed ?: (min ?: 0.0)
                        val next = (base + step).let { v ->
                            when {
                                min != null && v < min -> min
                                max != null && v > max -> max
                                else -> v
                            }
                        }
                        val text = if (integer) next.toInt().toString() else next.toStringSafe()
                        onValueChange(text)
                        showError = false
                        val vNew = validate(text)
                        if (vNew.ok && vNew.parsed != null) onCommit(vNew.parsed)
                    },
                    modifier = Modifier.size(40.dp).mealooTechGlassBorder(RoundedCornerShape(8.dp), MealooTechColors.AccentCyan.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = MealooTechColors.AccentCyan)
                }
            }
        }

        // 5. 辅助文字与错误提示
        if (showError && validity.error != null) {
            Text(
                text = validity.error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
@Composable
private fun PercentOffEachEditor(
    rule: EventRule.PercentOffEach,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var text by remember(rule) { mutableStateOf(rule.percent.toStringSafe()) }
    RuleNumberField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.discount_percent_each_label),
        suffix = "%",
        helperText = stringResource(R.string.shopping_discount_percent_hint),
        integer = false,
        min = 0.0,
        max = 100.0,
        stepper = true,
        step = 5.0, // [优化] 步进改为 5，更实用
        onCommit = { onChange(EventRule.PercentOffEach(BigDecimal.valueOf(it))) },
        onValidityChange = onValidityChange
    )
}

@Composable
private fun AmountOffEachEditor(
    rule: EventRule.AmountOffEach,
    currency: String,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var text by remember(rule) { mutableStateOf(rule.amount.toStringSafe()) }
    RuleNumberField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.discount_amount_each_label),
        suffix = currency,
        integer = false,
        min = 0.0,
        stepper = true, // [优化] 开启步进
        step = 5.0,
        onCommit = { onChange(EventRule.AmountOffEach(BigDecimal.valueOf(it))) },
        onValidityChange = onValidityChange
    )
}

private enum class TierValueMode { PERCENT, AMOUNT }

private data class TierStepDraft(
    val id: String = UUID.randomUUID().toString(),
    val thresholdText: String,
    val valueText: String
)
@Composable
private fun SecondPercentEditor(
    rule: EventRule.SecondPercent,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var text by remember(rule) { mutableStateOf(rule.percent.toStringSafe()) }
    RuleNumberField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.discount_second_percent_label),
        suffix = "%",
        helperText = stringResource(R.string.discount_rule_params_example_second_percent),
        integer = false,
        min = 0.0,
        max = 100.0,
        stepper = true,
        step = 1.0,
        onCommit = { onChange(EventRule.SecondPercent(BigDecimal.valueOf(it))) },
        onValidityChange = onValidityChange
    )
}

@Composable
private fun SecondMinusEditor(
    rule: EventRule.SecondMinus,
    currency: String,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var text by remember(rule) { mutableStateOf(rule.amount.toStringSafe()) }
    RuleNumberField(
        value = text,
        onValueChange = { text = it },
        label = stringResource(R.string.discount_second_minus_label),
        suffix = currency,
        helperText = stringResource(R.string.discount_rule_params_example_second_minus),
        integer = false,
        min = 0.0,
        stepper = false,
        onCommit = { onChange(EventRule.SecondMinus(BigDecimal.valueOf(it))) },
        onValidityChange = onValidityChange
    )
}

@Composable
private fun BuyXGetYEditor(
    rule: EventRule.BuyXGetY,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var buyText by remember(rule) { mutableStateOf(rule.buyQty.toInt().toString()) }
    var getText by remember(rule) { mutableStateOf(rule.getQty.toInt().toString()) }
    var freeGift by remember(rule) { mutableStateOf(rule.priceOfGetPercent <= BigDecimal.ZERO) }
    var giftPercentText by remember(rule) { mutableStateOf(rule.priceOfGetPercent.toStringSafe()) }

    val buy = parseRuleInt(buyText)
    val get = parseRuleInt(getText)
    val gift = if (freeGift) 0.0 else parseRuleDouble(giftPercentText)

    val valid = (buy != null && buy >= 1) &&
        (get != null && get >= 1) &&
        (freeGift || (gift != null && gift in 0.0..100.0))
    SideEffect { onValidityChange(valid) }

    fun commitAll() {
        val b = parseRuleInt(buyText)?.takeIf { it >= 1 } ?: return
        val g = parseRuleInt(getText)?.takeIf { it >= 1 } ?: return
        val gp = if (freeGift) 0.0 else (parseRuleDouble(giftPercentText)?.takeIf { it in 0.0..100.0 } ?: return)
        onChange(EventRule.BuyXGetY(
            buyQty = BigDecimal.valueOf(b.toDouble()), // 若 buyQty 定義為 BigDecimal
            getQty = BigDecimal.valueOf(g.toDouble()),
            priceOfGetPercent = BigDecimal.valueOf(gp)
        ))
    }

    Text(
        text = stringResource(R.string.discount_rule_params_example_buyxgety),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 第一行：买与送并排
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                RuleNumberField(
                    value = buyText,
                    onValueChange = { buyText = it },
                    label = stringResource(R.string.shopping_buy_piece_label),
                    integer = true,
                    min = 1.0,
                    stepper = true,
                    modifier = Modifier.weight(1f),
                    onCommit = { commitAll() }
                )
                RuleNumberField(
                    value = getText,
                    onValueChange = { getText = it },
                    label = stringResource(R.string.shopping_get_piece_label),
                    integer = true,
                    min = 1.0,
                    stepper = true,
                    modifier = Modifier.weight(1f),
                    onCommit = { commitAll() }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 第二行：赠品价格设定
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.shopping_gift_free_label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = freeGift,
                    onCheckedChange = {
                        freeGift = it
                        if (it) giftPercentText = "0"
                        commitAll()
                    }
                )
            }

            if (!freeGift) {
                RuleNumberField(
                    value = giftPercentText,
                    onValueChange = { giftPercentText = it },
                    label = stringResource(R.string.shopping_gift_price_percent_label),
                    suffix = "%",
                    helperText = stringResource(R.string.discount_rule_params_hint_gift_price_percent),
                    integer = false,
                    min = 0.0,
                    max = 100.0,
                    stepper = true,
                    step = 10.0,
                    onCommit = { commitAll() }
                )
            }
        }
    }
}

@Composable
private fun BundleFixedPriceEditor(
    rule: EventRule.BundleFixedPrice,
    currency: String,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var qtyText by remember(rule) { mutableStateOf(rule.bundleQty.toInt().toString()) }
    var priceText by remember(rule) { mutableStateOf(rule.bundlePrice.toStringSafe()) }

    val qty = parseRuleInt(qtyText)
    val price = priceText.toBigDecimalOrNull()
    val valid = (qty != null && qty >= 2) && (price != null && price > BigDecimal.ZERO)
    SideEffect { onValidityChange(valid) }

    fun commitAll() {
        val q = parseRuleInt(qtyText)?.takeIf { it >= 2 } ?: return
        val p = priceText.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return
        onChange(EventRule.BundleFixedPrice(bundleQty = q.toBigDecimal(), bundlePrice = p))
    }

    Text(
        text = stringResource(R.string.discount_rule_params_example_bundle_fixed_price),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        RuleNumberField(
            value = qtyText,
            onValueChange = { qtyText = it },
            label = stringResource(R.string.discount_bundle_qty_label),
            suffix = stringResource(R.string.discount_rule_params_unit_piece),
            integer = true,
            min = 2.0,
            stepper = true,
            step = 1.0,
            modifier = Modifier.weight(1f),
            onCommit = { commitAll() },
            onValidityChange = {}
        )
        RuleNumberField(
            value = priceText,
            onValueChange = { priceText = it },
            label = stringResource(R.string.discount_bundle_price_label),
            suffix = currency,
            integer = false,
            min = 0.01,
            stepper = false,
            modifier = Modifier.weight(1f),
            onCommit = { commitAll() },
            onValidityChange = {}
        )
    }
}

@Composable
private fun TierByQtyEditor(
    rule: EventRule.TierByQty,
    currency: String,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    val initialMode = remember(rule) {
        if (rule.steps.any { it.percent != null }) TierValueMode.PERCENT else TierValueMode.AMOUNT
    }
    var mode by remember(rule) { mutableStateOf(initialMode) }
    var steps by remember(rule) {
        val base = rule.steps.ifEmpty { listOf(Step(threshold = BigDecimal("2"), percent =BigDecimal("10"))) }
        mutableStateOf(
            base.map {
                TierStepDraft(
                    thresholdText = it.threshold.toInt().toString(),
                    valueText = when (initialMode) {
                        TierValueMode.PERCENT -> (it.percent ?: BigDecimal.ZERO).toStringSafe()
                        TierValueMode.AMOUNT -> (it.amount ?: BigDecimal.ZERO).toStringSafe()
                    }
                )
            }
        )
    }
    var listError by remember { mutableStateOf<String?>(null) }
    val tierInvalidMsg = stringResource(R.string.discount_rule_params_error_tier_invalid)

    fun validateAndBuild(): List<Step>? {
        val parsed = steps.mapNotNull { d ->
            val th = parseRuleInt(d.thresholdText)?.takeIf { it >= 2 } ?: return@mapNotNull null
            when (mode) {
                TierValueMode.PERCENT -> {
                    val vb = d.valueText.toBigDecimalOrNull() ?: return@mapNotNull null
                    if (vb < BigDecimal.ZERO || vb > BigDecimal("100")) return@mapNotNull null
                    Step(threshold = th.toBigDecimal(), percent = vb)
                }
                TierValueMode.AMOUNT -> {
                    val vb = d.valueText.toBigDecimalOrNull() ?: return@mapNotNull null
                    if (vb < BigDecimal.ZERO) return@mapNotNull null
                    Step(threshold = th.toBigDecimal(), amount = vb)
                }
            }
        }
        return if (parsed.size == steps.size && parsed.map { it.threshold }.distinct().size == parsed.size) parsed.sortedBy { it.threshold } else null
    }
    fun commitAll() {
        val b = validateAndBuild()
        if (b == null) { listError = tierInvalidMsg; return }
        listError = null
        onChange(EventRule.TierByQty(steps = b))
        steps = b.map { TierStepDraft(thresholdText = it.threshold.toInt().toString(), valueText = if (mode == TierValueMode.PERCENT) (it.percent ?: BigDecimal.ZERO).toStringSafe() else (it.amount ?: BigDecimal.ZERO).toStringSafe()) }
    }
    val built = validateAndBuild(); SideEffect { onValidityChange(built != null && built.isNotEmpty()) }

    Text(
        text = stringResource(R.string.discount_rule_params_example_tier_qty),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        FilterChip(
            selected = mode == TierValueMode.PERCENT,
            onClick = { mode = TierValueMode.PERCENT; steps = steps.map { it.copy(valueText = "") }; listError = null },
            label = { Text(stringResource(R.string.shopping_discount_percent_label)) }
        )
        FilterChip(
            selected = mode == TierValueMode.AMOUNT,
            onClick = { mode = TierValueMode.AMOUNT; steps = steps.map { it.copy(valueText = "") }; listError = null },
            label = { Text(stringResource(R.string.shopping_discount_amount_label, currency)) }
        )
    }

    // 阶梯列表 - [优化] 使用卡片式列表，不再挤在一起
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { idx, stepDraft ->
            TierStepRowCard(
                idx = idx,
                total = steps.size,
                threshold = stepDraft.thresholdText,
                value = stepDraft.valueText,
                valueSuffix = if (mode == TierValueMode.PERCENT) "%" else currency,
                thresholdLabel = stringResource(R.string.discount_qty_threshold_label),
                valueLabel = if (mode == TierValueMode.PERCENT) {
                    stringResource(R.string.discount_percent_label)
                } else {
                    stringResource(R.string.discount_amount_label)
                },
                onThresholdChange = { s -> steps = steps.toMutableList().also { it[idx] = it[idx].copy(thresholdText = s) } },
                onValueChange = { s -> steps = steps.toMutableList().also { it[idx] = it[idx].copy(valueText = s) } },
                onDelete = {
                    if (steps.size > 1) { steps = steps.toMutableList().also { it.removeAt(idx) }; commitAll() }
                },
                onCommit = { commitAll() }
            )
        }
    }

    if (listError != null) {
        Text(text = listError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }

    // 添加按钮
    OutlinedButton(
        onClick = {
            val nextThreshold = steps.mapNotNull { parseRuleInt(it.thresholdText) }.maxOrNull()?.plus(1)?.coerceAtLeast(2)?.toString() ?: "2"
            steps = steps + TierStepDraft(thresholdText = nextThreshold, valueText = "")
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.discount_rule_params_add_step))
    }
}

// [新增] 阶梯规则单行卡片，解决拥挤问题
@Composable
private fun TierStepRowCard(
    idx: Int,
    total: Int,
    threshold: String,
    value: String,
    valueSuffix: String,
    thresholdLabel: String,
    valueLabel: String,
    onThresholdChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    onCommit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface, // 白色或浅色背景
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：序号
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text((idx + 1).toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            // 中间：输入区 (两个输入框并排)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 门槛
                RuleNumberField(
                    value = threshold,
                    onValueChange = onThresholdChange,
                    label = thresholdLabel,
                    integer = true,
                    stepper = false, // 卡片内空间有限，关掉 Stepper
                    modifier = Modifier.weight(1f),
                    onCommit = { onCommit() },
                    required = true
                )

                // 数值
                RuleNumberField(
                    value = value,
                    onValueChange = onValueChange,
                    label = valueLabel,
                    suffix = valueSuffix,
                    stepper = false,
                    modifier = Modifier.weight(1f),
                    onCommit = { onCommit() },
                    required = true
                )
            }

            // 右侧：删除
            if (total > 1) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}
@Composable
private fun TierBySpendEditor(
    rule: EventRule.TierBySpend,
    currency: String,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var steps by remember(rule) {
        val base = rule.steps.ifEmpty {
            listOf(
                Step(
                    threshold = BigDecimal("100"),
                    amount = BigDecimal("10")
                )
            )
        }

        mutableStateOf(
            base.map {
                TierStepDraft(
                    thresholdText = it.threshold.toStringSafe(),
                    valueText = (it.amount ?: BigDecimal.ZERO).toStringSafe()
                )
            }
        )
    }
    var listError by remember { mutableStateOf<String?>(null) }
    val tierInvalidMsg = stringResource(R.string.discount_rule_params_error_tier_invalid)

    fun validateAndBuild(): List<Step>? {
        val parsed = steps.mapNotNull { d ->
            val th = d.thresholdText.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO } ?: return@mapNotNull null
            val a = d.valueText.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return@mapNotNull null
            Step(threshold = th, amount = a)
        }
        if (parsed.size != steps.size) return null
        val sorted = parsed.sortedBy { it.threshold }
        val unique = sorted.map { it.threshold }.distinct().size == sorted.size
        if (!unique) return null
        return sorted
    }

    val built = validateAndBuild()
    SideEffect { onValidityChange(built != null && built.isNotEmpty()) }

    fun commitAll() {
        val b = validateAndBuild()
        if (b == null) {
            listError = tierInvalidMsg
            return
        }
        listError = null
        onChange(EventRule.TierBySpend(steps = b))
        steps = b.map { TierStepDraft(thresholdText = it.threshold.toStringSafe(), valueText = (it.amount ?: BigDecimal.ZERO).toStringSafe()) }
    }

    Text(
        text = stringResource(R.string.discount_rule_params_example_tier_spend),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = {
                steps = listOf(TierStepDraft(thresholdText = "100", valueText = "10"))
                commitAll()
            },
            label = { Text(stringResource(R.string.discount_rule_params_tier_spend_template_1)) }
        )
        AssistChip(
            onClick = {
                steps = listOf(TierStepDraft(thresholdText = "200", valueText = "20"))
                commitAll()
            },
            label = { Text(stringResource(R.string.discount_rule_params_tier_spend_template_2)) }
        )
        AssistChip(
            onClick = {
                steps = listOf(
                    TierStepDraft(thresholdText = "100", valueText = "10"),
                    TierStepDraft(thresholdText = "200", valueText = "30")
                )
                commitAll()
            },
            label = { Text(stringResource(R.string.discount_rule_params_tier_spend_template_3)) }
        )
    }

    steps.forEachIndexed { idx, stepDraft ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RuleNumberField(
                value = stepDraft.thresholdText,
                onValueChange = { s ->
                    steps = steps.toMutableList().also { it[idx] = it[idx].copy(thresholdText = s) }
                },
                label = stringResource(R.string.discount_spend_threshold_label),
                suffix = currency,
                integer = false,
                min = 0.01,
                stepper = false,
                modifier = Modifier.weight(1f),
                onCommit = { commitAll() },
                onValidityChange = {}
            )
            RuleNumberField(
                value = stepDraft.valueText,
                onValueChange = { s ->
                    steps = steps.toMutableList().also { it[idx] = it[idx].copy(valueText = s) }
                },
                label = stringResource(R.string.discount_amount_label),
                suffix = currency,
                integer = false,
                min = 0.0,
                stepper = false,
                modifier = Modifier.weight(1f),
                onCommit = { commitAll() },
                onValidityChange = {}
            )
            IconButton(
                onClick = {
                    if (steps.size <= 1) return@IconButton
                    steps = steps.toMutableList().also { it.removeAt(idx) }
                    commitAll()
                },
                enabled = steps.size > 1
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        }
    }

    if (listError != null) {
        Text(
            text = listError!!,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    OutlinedButton(
        onClick = {
            val nextThreshold = steps
                .mapNotNull { parseRuleDouble(it.thresholdText) }
                .maxOrNull()
                ?.plus(1.0)
                ?.takeIf { it > 0.0 }
                ?.toStringSafe()
                ?: "100"
            steps = steps + TierStepDraft(thresholdText = nextThreshold, valueText = "")
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.discount_rule_params_add_step))
    }
}

@Composable
private fun ConditionalEditor(
    rule: EventRule.Conditional,
    onChange: (EventRule) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    SideEffect { onValidityChange(true) }

    var pm by remember(rule) { mutableStateOf(rule.eligiblePaymentMethods?.joinToString(",") ?: "") }
    var st by remember(rule) { mutableStateOf(rule.eligibleStores?.joinToString(",") ?: "") }
    var stack by remember(rule) { mutableStateOf(rule.stackable) }

    fun commitPm() {
        onChange(
            rule.copy(
                eligiblePaymentMethods = pm.split(',', '，')
                    .map { s -> s.trim() }
                    .filter { s -> s.isNotEmpty() }
                    .toSet()
                    .ifEmpty { null }
            )
        )
    }
    fun commitStores() {
        onChange(
            rule.copy(
                eligibleStores = st.split(',', '，')
                    .map { s -> s.trim() }
                    .filter { s -> s.isNotEmpty() }
                    .toSet()
                    .ifEmpty { null }
            )
        )
    }

    Text(
        text = stringResource(R.string.discount_rule_params_example_conditional),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    CompactTextField(
        value = pm,
        onValueChange = { pm = it },
        label = stringResource(R.string.discount_limit_payment_methods_hint),
        onFocusChange = { focused -> if (!focused) commitPm() }
    )
    CompactTextField(
        value = st,
        onValueChange = { st = it },
        label = stringResource(R.string.discount_limit_stores_hint),
        onFocusChange = { focused -> if (!focused) commitStores() }
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = stack,
            onCheckedChange = {
                stack = it
                onChange(rule.copy(stackable = it))
            }
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.discount_stackable_label))
    }
}
@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    today: Date,
    onEdit: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit,
    onQuickConsume: (Double) -> Unit,
    onQuickConsumeToIntake: (Double) -> Unit,
    onThaw: (InventoryItem) -> Unit,
    onView: (InventoryItem) -> Unit,
    nameByFdcId: Map<Int, String> = emptyMap(),
    nameById: Map<String, String> = emptyMap()
) {
    val dte = daysToExpiry(item.expiryDate, today)
    val (badgeText, badgeColor, txtColor) = expiryBadge(dte)
    val alerts = remember(item.note, item.quantity, item.unit, item.location, nameById) {
        InventoryAlertEngine.alertsForItem(item, nameById)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable { onView(item) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左：到期徽章
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(badgeText, color = txtColor, style = MaterialTheme.typography.labelMedium)
            }
            if (alerts.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    alerts.forEach { alert ->
                        InventoryAlertBadge(alert = alert)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // 中：信息
            Column(modifier = Modifier.weight(1f)) {
                val strike = item.quantity <= BigDecimal.ZERO

                val mainName = item.name.ifBlank { stringResource(R.string.common_unnamed_label) }
                val sysName = remember(item.fdcId, nameByFdcId) {
                    item.fdcId
                        ?.let { id -> nameByFdcId[id] }
                        ?.let { stripBrackets(it) }
                }?.takeIf { it.isNotBlank() && !it.equals(mainName, ignoreCase = true) }

                // 第一行：主名，优先，单行省略
                Text(
                    text = mainName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (strike) TextDecoration.LineThrough else null
                )

                // 第二行：系统名（小字），单行省略（若没有 sysName 则不占高度）
                if (sysName != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = sysName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 第三行：数量 / 单位 / 位置 / 到期（尽量单行）
                val quantityText = if (item.expiryDate != null) {
                    stringResource(
                        R.string.storage_quantity_location_expiry_desc,
                        fmt(item.quantity),
                        item.unit,
                        locLabel(item.location),
                        item.expiryDate
                    )
                } else {
                    stringResource(
                        R.string.quantity_unit_location_fmt,
                        fmt(item.quantity),
                        item.unit,
                        locLabel(item.location)
                    )
                }
                Text(
                    text = quantityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (alerts.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = alerts.joinToString("\n") { it.subtitle },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 右：操作（平板保留横排；手机收成 ⋮ 菜单）
            val compact = isCompactPhone()
            val baseStep = stepFor(item.unit) // g=50, ml=100, 其它=1
            InventoryRowActions(
                compact = compact,
                onQuickConsumeToIntake = { onQuickConsumeToIntake(baseStep) },
                onThaw = { onThaw(item) },
                onEdit = { onEdit(item) },
                onDelete = { onDelete(item) },
                quickConsumeLabel = stringResource(
                    R.string.storage_quick_consume_desc,
                    fmt(baseStep),
                    item.unit
                )
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryItemDetailDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onQuickConsume: (Double) -> Unit,
    onThaw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { startOfDay(Date()) }
    val days = daysToExpiry(item.expiryDate, today)
    val (badgeText, badgeColor, txtColor) = expiryBadge(days)

    // Location Colors
    val locColor = when(item.location) {
        "Pantry" -> StorageColors.Pantry
        "Fridge" -> StorageColors.Fridge
        "Freezer" -> StorageColors.Freezer
        else -> StorageColors.Other
    }

    val per100 = item.standardNutrientsPer100g
    val currencySymbol = remember(item.currency) { PriceCalcUtils.currencySymbol(item.currency) }
    var cpRefUnit by remember(item) { mutableStateOf(PriceCalcUtils.defaultCpRefUnit(item.currency)) }
    val costInfo = remember(item, cpRefUnit) { item.toCostInfo(cpRefUnit) }
    val cpDenominatorLabel = remember(currencySymbol, cpRefUnit) {
        PriceCalcUtils.formatCpDenominator(currencySymbol, cpRefUnit)
    }

    // Dietary Analysis
    val foodContexts = remember(per100) {
        if (per100 != null) {
            val scoreDetail = NutritionScorer.scorePer100g(per100)
            buildFoodContexts(per100, scoreDetail)
        } else {
            emptyList()
        }
    }

    // Total Estimation Logic
    val totalGrams: Double? = when (item.unit) {
        "g", "ml" -> item.quantity.toDouble()
        "pcs" -> item.gramsPerUnit?.let { it.toDouble() * item.quantity.toDouble() }
        else -> null
    }

    val totalNutrients: Map<String, Double>? = if (per100 != null && totalGrams != null) {
        per100.entries.associate { it.key to it.value * totalGrams / 100.0 }
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 1. Header: Name & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Price display (Total Cost)
                    item.cost?.let { cost ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.common_currency_amount_compact_desc,
                                    currencySymbol,
                                    costInfo.totalCost
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Status Badges (Location + Expiry + Category)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    // Location Badge
                    Surface(
                        color = locColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, locColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val icon = when(item.location) {
                                "Pantry" -> Icons.Outlined.AllInbox
                                "Fridge" -> Icons.Outlined.Kitchen
                                "Freezer" -> Icons.Outlined.AcUnit
                                else -> Icons.Outlined.Inventory2
                            }
                            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = locLabel(item.location),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.8f)
                            )
                        }
                    }
                    if (!item.category.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = item.category,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Expiry Badge
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.expiryDate?.let {
                                stringResource(R.string.storage_badge_with_date_desc, badgeText, it)
                            } ?: badgeText,
                            color = txtColor,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3. Quantity Display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StorageColors.Bg, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = fmt(item.quantity),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(Modifier.weight(1f))

                    // Consume Button inside quantity card for better context
                    if (item.quantity > BigDecimal.ZERO) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onQuickConsume(stepFor(item.unit).toDouble()) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.consume), style = MaterialTheme.typography.labelSmall)
                            }
                            if(item.location== "Freezer"){
                                FilledTonalButton(
                                    onClick = onThaw,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Outlined.AcUnit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.storage_thaw_action), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                if (item.gramsPerUnit != null && item.unit == "pcs") {
                    Text(
                        text = stringResource(R.string.storage_grams_per_piece_desc, fmt(item.gramsPerUnit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 0.dp)
                    )
                }

                // 4. Note
                if (!item.note.isNullOrBlank()) {
                    Column {
                        Text(stringResource(R.string.note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // 5. Cost / CP Analysis Section (Re-designed)
                if (item.cost != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StorageColors.Bg, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // --- 5a. Unit Cost & Per 100g ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Cost Per Unit
                            val unitLabel = stringResource(
                                R.string.storage_unit_label_desc,
                                costInfo.quantityLabel,
                                costInfo.unit
                            )
                            costInfo.costPerUnit?.let { perUnit ->
                                CostStatColumn(
                                    label = stringResource(R.string.storage_cost_per_unit_label, unitLabel),
                                    value = stringResource(
                                        R.string.common_currency_amount_compact_desc,
                                        currencySymbol,
                                        perUnit
                                    )
                                )
                            }

                            // Vertical Divider
                            if (costInfo.costPerUnit != null && costInfo.costPer100g != null) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(36.dp)
                                        .background(StorageColors.Outline.copy(alpha = 0.8f))
                                        .align(Alignment.CenterVertically)
                                )
                            }

                            // Right: Cost Per 100g
                            costInfo.costPer100g?.let { per100g ->
                                CostStatColumn(
                                    label = stringResource(R.string.per100g),
                                    value = stringResource(
                                        R.string.common_currency_amount_compact_desc,
                                        currencySymbol,
                                        per100g
                                    ),
                                    alignment = Alignment.End
                                )
                            }
                        }

                        // --- 5b. CP Values ---
                        if (costInfo.cpValues.isNotEmpty()) {
                            HorizontalDivider(color = StorageColors.Outline.copy(alpha = 0.5f))

                            // CP Header + Unit Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.LocalOffer,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.storage_cp_analysis_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Compact Chips for CP Base
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    PriceCalcUtils.CpRefUnit.PER_1,
                                    PriceCalcUtils.CpRefUnit.PER_100,
                                    PriceCalcUtils.CpRefUnit.PER_1000,
                                ).forEach { unit ->
                                    val isSelected = cpRefUnit == unit
                                    // Custom compact chip for better visual in dialog
                                    Surface(
                                        onClick = { cpRefUnit = unit },
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = if (isSelected) null else BorderStroke(1.dp, StorageColors.Outline),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                    text = stringResource(R.string.shopping_price_per_unit_desc, unit.multiplier),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // CP Data Card
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StorageColors.Outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    val cp = costInfo.cpValues
                                    fun v(t: CPMetricType) = PriceCalcUtils.formatCpValue(cp[t])

                                    NutrientCol(stringResource(R.string.kcal), v(CPMetricType.ENERGY), StorageColors.Energy)
                                    NutrientCol(stringResource(R.string.storage_cp_protein_label), v(CPMetricType.PROTEIN), StorageColors.Protein)
                                    NutrientCol(stringResource(R.string.storage_cp_carbs_label), v(CPMetricType.CARB), StorageColors.Carb)
                                    NutrientCol(stringResource(R.string.storage_cp_fat_label),  v(CPMetricType.FAT),  StorageColors.Fat)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = StorageColors.Outline)

                // 6. Nutrition Data (Total / Per 100g)
                if (totalNutrients != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.estimate_total_amount, totalGrams ?: 0.0),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        MacroPreviewBox(totalNutrients, Modifier.fillMaxWidth())
                    }
                } else if (per100 != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.nutritionPer100g),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        MacroPreviewBox(per100, Modifier.fillMaxWidth())
                    }
                } else {
                    Text(
                        text = stringResource(R.string.storage_no_nutrition_data_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // 7. Context Tags
                if (foodContexts.isNotEmpty()) {
                    Column {
                        Text(
                            text = stringResource(R.string.meal_picker_dietary_analysis_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            foodContexts.forEach { ctx ->
                                StorageContextTag(ctx)
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.delete))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit))
                }
            }
        }
    )
}
// --- Helper Components for the New Style ---
@Composable
private fun StorageContextTag(context: FoodContext) {
    val (bgColor, contentColor) = when (context.level) {
        ContextLevel.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ContextLevel.WARNING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(context.icon, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text(
                text = context.title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
@Composable
private fun SelectFoodForInventoryDialog(
    onDismiss: () -> Unit,
    onPicked: (fdcId: Int, standardPer100g: Map<String, Double>, displayName: String) -> Unit
) {
    val context = LocalContext.current
    val foodRepo = remember { FoodDbRepository.getInstance(context) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodSelectionCandidate>>(emptyList()) }
    var neat by remember { mutableStateOf(true) } // ✅ 新增：是否去括弧显示
    var isSeeded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            foodRepo.ensureSeeded(context)
        }
        isSeeded = true
    }

    LaunchedEffect(query, isSeeded) {
        if (!isSeeded) return@LaunchedEffect
        val list = withContext(Dispatchers.IO) {
            foodRepo.searchFoods(query)
        }
        results = list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.Use_system_data)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // ✅ 开关：去括弧显示
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { neat = !neat },
                        label = { Text(if (neat) stringResource(R.string.bracket_display_on) else stringResource(R.string.bracket_display_off)) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(results) { f ->
                        val rawName = f.name
                        val showName = if (neat) stripBrackets(rawName) else rawName
                        val std = f.nutrientsPer100g
                        Log.d("Storage","Food:$std")
                        // ✅ 提取能量（kcal）
                        val energy = f.nutrientsPer100g["Energy"] ?: 0.0
                        val energyStr = stringResource(R.string.common_kcal_desc, energy.roundToInt())
                        Log.d("Storage","energyStr:$energyStr")

                        TextButton(
                            onClick = {
                                f.id.toIntOrNull()?.let { onPicked(it, std, showName) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(showName, modifier = Modifier.weight(1f))
                                // ✅ 在右边显示热量
                                Text(
                                    energyStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

// ---------- Helpers ----------
private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private fun parseDate(s: String): Date? = try { DATE_FMT.parse(s) } catch (_: Exception) { null }
private fun fmtDate(d: Date): String = DATE_FMT.format(d)
private fun startOfDay(d: Date): Date = Calendar.getInstance().apply {
    time = d; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.time

/** 距离到期的天数（负值表示已过期） */
private fun daysToExpiry(expiry: String?, today: Date): Int? {
    val e = expiry?.let { parseDate(it) } ?: return null
    val diff = e.time - today.time
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

@Composable
private fun InventoryAlertBadge(alert: InventoryAlert) {
    val (bgColor, textColor, label) = when (alert.type) {
        AlertType.THAW_ALERT -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            stringResource(R.string.storage_alert_thaw_label)
        )
        AlertType.LOW_STOCK_ALERT -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            stringResource(R.string.storage_alert_low_label)
        )
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InventoryAlertsSection(alerts: List<InventoryAlert>) {
    val topAlerts = remember(alerts) { alerts.take(3) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, StorageColors.Outline, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.storage_alerts_title, alerts.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (alerts.size > 3) {
                Text(
                    text = stringResource(R.string.common_more_count_desc, alerts.size - 3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topAlerts.forEach { alert ->
                InventoryAlertRow(alert = alert)
            }
        }
    }
}

@Composable
private fun InventoryAlertRow(alert: InventoryAlert) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InventoryAlertBadge(alert = alert)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = alert.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun expiryBadge(days: Int?): Triple<String, Color, Color> {
    return when {
        days == null -> Triple(
            stringResource(R.string.exp_no_date),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        days < 0 -> Triple(
            stringResource(R.string.exp_expired_days, -days),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        days == 0 -> Triple(
            stringResource(R.string.exp_today),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        days in 1..3 -> Triple(
            stringResource(R.string.exp_remaining_days, days),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            stringResource(R.string.exp_remaining_days, days),
            Color(0xFFA5D6A7).copy(alpha = if (isSystemInDarkTheme()) 0.2f else 1f),
            if (isSystemInDarkTheme()) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
        )
    }
}

fun fmt(v: Double?) = if (v?.rem(1.0) == 0.0) v?.toInt().toString() else String.format(Locale.US, "%.1f", v)

/** 针对单位给一个合理的快速消耗步进 */
private fun stepFor(unit: String) = when (unit) {
    "g" -> 50.0
    "ml" -> 100.0
    else -> 1.0
}
private fun locLabel(context: Context, loc: String): String = when (loc) {
    "Pantry" -> context.getString(R.string.loc_pantry)
    "Fridge" -> context.getString(R.string.loc_fridge)
    "Freezer" -> context.getString(R.string.loc_freezer)
    else -> loc
}

@Composable
private fun locLabel(loc: String): String = locLabel(LocalContext.current, loc)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = label,
        colors = AssistChipDefaults.assistChipColors(
            labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    )
}
@Composable
private fun EmptyHint(
    filter: FilterType,
    query: String,
    onClearQuery: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val msg = when {
        query.isNotBlank() && filter == FilterType.ALL ->
            stringResource(R.string.storage_search_no_match_all_desc, query)
        query.isNotBlank() && filter == FilterType.SOON ->
            stringResource(R.string.storage_search_no_match_expiring_desc, query)
        query.isNotBlank() && filter == FilterType.EXPIRED ->
            stringResource(R.string.storage_search_no_match_expired_desc, query)

        filter == FilterType.ALL ->
            context.getString(R.string.empty_food_all)
        filter == FilterType.SOON ->
            context.getString(R.string.empty_food_expiring)
        filter == FilterType.EXPIRED ->
            context.getString(R.string.empty_food_expired)

        else -> stringResource(R.string.no_data)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (query.isNotBlank()) {
                OutlinedButton(onClick = onClearQuery) { Text(stringResource(R.string.clean_filter)) }
            }
            if (filter == FilterType.ALL && query.isBlank()) {
                OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.add_new_item)) }
            }
        }
    }
}

// 把整道菜换算成每100g营养（更健壮的版本）
private fun recipePer100g(r: RecipeEntry): Map<String, Double>? {
    // ① 先拿总重量：没有 totalWeightGram 就回退到配料总克数
    val total = (r.totalWeightGram?.takeIf { it > 0.0 }
        ?: r.ingredients.sumOf { it.amount }.takeIf { it > 0.0 })
        ?: return null

    // ② 优先用 RecipeEntry 上的总营养；全为 0 才回退按配料算一次
    var kcal = r.kcalPerServing
    var protein = r.protein
    var carbs = r.carbs
    var fats = r.fats
    if (kcal <= 0.0 && protein <= 0.0 && carbs <= 0.0 && fats <= 0.0) {
        r.ingredients.forEach { ing ->
            val g = ing.amount
            val n = ing.nutrientsPer100g ?: emptyMap()
            kcal   += (n["Energy"] ?: 0.0) * g / 100.0
            protein+= (n["Protein"] ?: 0.0) * g / 100.0
            val c   =  n["Available carbohydrate"]
                ?: n["Carbohydrate, by difference"] ?: 0.0
            carbs  += c * g / 100.0
            fats   += (n["Total lipid (fat)"] ?: 0.0) * g / 100.0
        }
    }

    // ③ 统一回传英文键；“碳水”沿用 UI 里使用的 Available carbohydrate
    return mapOf(
        "Energy" to kcal * 100.0 / total,
        "Protein" to protein * 100.0 / total,
        "Available carbohydrate" to carbs * 100.0 / total,
        "Total lipid (fat)" to fats * 100.0 / total
    )
}
private fun addDays(d: Date, days: Int): Date =
    Calendar.getInstance().apply { time = d; add(Calendar.DAY_OF_YEAR, days) }.time
@Composable
fun MealSelectDialog(
    mealDefs: List<MealDef>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_meal)) },
        text = {
            Column {
                mealDefs.forEach { def ->
                    TextButton(onClick = { onPick(def.id) }) { Text(def.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
// ---------- helper: 状态枚举 + 计算函数 ----------
private enum class EventState { ON_NOW, STARTS_IN, ENDED, UNKNOWN }

/**
 * 根据 start/end 字符串推断状态（解析 ISO yyyy-MM-dd），返回 Pair(state, displayText)
 * - displayText 已包含中文说明（例如 "On now · 还剩 3 天" / "Starts in 2 天 · 2025-10-10 – 2025-10-18" / "Ended · 2025-10-06 – 2025-10-08"）
 */
private fun deriveEventStateAndText(
    context: Context,
    startStr: String?,
    endStr: String?
): Pair<EventState, String> {
    fun parse(s: String?): LocalDate? = runCatching { s?.let { LocalDate.parse(it) } }.getOrNull()
    val today = LocalDate.now()
    val s = parse(startStr)
    val e = parse(endStr)
    val rangeText = if (!endStr.isNullOrBlank()) {
        context.getString(R.string.common_date_range_desc, startStr ?: "", endStr)
    } else {
        startStr.orEmpty()
    }

    return when {
        s != null && e != null && (today.isEqual(s) || today.isAfter(s)) && !today.isAfter(e) -> {
            // ongoing: today in [s, e]
            val remaining = ChronoUnit.DAYS.between(today, e.plusDays(1)) // 包含今天到结束日的天数
            EventState.ON_NOW to context.getString(
                R.string.shopping_event_state_on_now_desc,
                remaining
            )
        }
        s != null && today.isBefore(s) -> {
            val days = ChronoUnit.DAYS.between(today, s)
            EventState.STARTS_IN to context.getString(
                R.string.shopping_event_state_starts_in_desc,
                days,
                rangeText
            )
        }
        e != null && (s == null || today.isAfter(e)) -> {
            EventState.ENDED to context.getString(
                R.string.shopping_event_state_ended_desc,
                rangeText
            )
        }
        else -> {
            EventState.UNKNOWN to (startStr ?: endStr ?: "")
        }
    }
}
@Composable
fun ruleLabel(rule: EventRule): String {
    return when (rule) {
        is EventRule.None ->
            stringResource(R.string.event_rule_none)

        is EventRule.PercentOffEach ->
            stringResource(
                R.string.event_rule_percent_each_fmt,
                stripZero(rule.percent)
            )

        is EventRule.AmountOffEach ->
            stringResource(
                R.string.event_rule_amount_each_fmt,
                stripZero(rule.amount)
            )

        is EventRule.SecondPercent ->
            stringResource(
                R.string.event_rule_second_percent_fmt,
                stripZero(rule.percent)
            )

        is EventRule.SecondMinus ->
            stringResource(
                R.string.event_rule_second_minus_fmt,
                stripZero(rule.amount)
            )

        is EventRule.BuyXGetY -> {
            val buy = stripZero(rule.buyQty)
            val get = stripZero(rule.getQty)
            val pricing = stripZero(rule.priceOfGetPercent)

            if (rule.priceOfGetPercent > BigDecimal.ZERO) {
                stringResource(
                    R.string.event_rule_buy_x_get_y_with_pricing_fmt,
                    buy, get, pricing
                )
            } else {
                stringResource(
                    R.string.event_rule_buy_x_get_y_fmt,
                    buy, get
                )
            }
        }
        is EventRule.BundleFixedPrice ->
            stringResource(
                R.string.event_rule_bundle_fixed_price_fmt,
                stripZero(rule.bundleQty),
                stripZero(rule.bundlePrice)
            )
        is EventRule.TierByQty ->
            stringResource(R.string.event_rule_tier_by_qty)

        is EventRule.TierBySpend ->
            stringResource(R.string.event_rule_tier_by_spend)

        is EventRule.Conditional ->
            stringResource(R.string.event_rule_conditional)

        is EventRule.FreeShipping ->
            stringResource(R.string.event_rule_free_shipping)
    }
}
private fun BigDecimal.toPlainStringSafe(): String =
    this.stripTrailingZeros().toPlainString()

fun stripZero(n: Number): String =
    when (n) {
        is Int, is Long -> n.toString()
        is Double, is Float -> {
            val bd = BigDecimal(n.toString()).stripTrailingZeros()
            bd.toPlainString()
        }
        is BigDecimal -> n.stripTrailingZeros().toPlainString()
        else -> n.toString()
    }
@Composable
private fun CartItemThumbnail(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data: Any? = remember(imageUri) {
        imageUri?.takeIf { it.isNotBlank() }?.let { uri ->
            // 允许传入 content:// / file:// / http(s)://
            if (uri.startsWith("content://") || uri.startsWith("file://")) {
                Uri.parse(uri)
            } else uri
        }
    }

    if (data == null) {
        // 空图：显示占位
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun PriceWithCurrencyRow(
    price: String,
    currency: String,
    onChange: (currency: String, price: String) -> Unit,
){
    // 常见币别（可按需增删）
    val currencies = AppCurrency.entries.map { it.code }
    val symbol = AppCurrency.fromCode(currency).symbol
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        // ▼ 左侧：币别
        Box(Modifier
            .widthIn(min = 88.dp)
            .weight(0.7f)) {
            CompactDropdownFieldMenu(
                value = currency,
                onValueChange = { newCurrency -> onChange(newCurrency, price) },
                label = stringResource(R.string.currency),
                placeholder = stringResource(R.string.choose_currency),
                options = currencies,
                leadingIcon = { Text(symbol, style = MaterialTheme.typography.bodyMedium, color = Color.White) },
                fieldHeight = 44.dp,
                rounded = 12.dp,
                containerColor = MealooTechColors.GlassCard
            )
        }
        // ▼ 右侧：金额
        Box(Modifier.weight(1f)) {
            CompactTextField(
                value = price,
                onValueChange = { newPrice -> onChange(currency, newPrice) },
                label = stringResource(R.string.price),
                placeholder = stringResource(R.string.common_price_placeholder),
                leadingIcon = { Text(symbol, style = MaterialTheme.typography.bodyMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                fieldHeight = 44.dp,
                rounded = 12.dp
            )
        }
    }
}
// =================== 工具函数 ===================

private fun normName(s: String?): String =
    s?.lowercase()
        ?.replace("（", "(")
        ?.replace("）", ")")
        ?.replace(Regex("\\([^)]*\\)"), "") // 去括号内注记
        ?.trim()
        .orEmpty()

/** 统一用于 pricing 的 key：优先 FDC/内部 id，其次归一化名称 */
private fun skuForShopping(item: ShoppingListItem): String {
    // 若你的 ShoppingListItem 有 fdcId/colorId 等稳定 id，请优先使用
    val fdcId = try {
        // 反射兜底：若无则返回 null
        (item.javaClass.methods.firstOrNull { it.name == "getFdcId" }?.invoke(item) as? Int)
    } catch (_: Throwable) { null }
    return fdcId?.toString()
        ?: normName(item.name).ifBlank { item.id }
}

private fun Number.toBD2(): BigDecimal =
    when (this) {
        is BigDecimal -> this.setScale(2, RoundingMode.HALF_UP)
        else -> BigDecimal(this.toString()).setScale(2, RoundingMode.HALF_UP)
    }

private fun fmtQty(q: BigDecimal): String =
    q.stripTrailingZeros().toPlainString()



// 新的數量步進器：數字欄左、垂直三角按鈕在右
@Composable
private fun QuantityStepperTri(
    value: Double,
    onChange: (Double) -> Unit,
    minValue: Double = 1.0, // 預設最小 1，可改
    widthDp: Dp = 72.dp,
    heightDp: Dp = 44.dp
) {
    // 顯示字串（整數則不顯示小數）
    val displayText = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 數字欄：用 BasicTextField 放在一個有邊框的 Box 內，文字置中
        Box(
            modifier = Modifier
                .width(widthDp)
                .height(heightDp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = displayText,
                onValueChange = { s ->
                    // 允許像 "3" 或 "3.5" 的輸入，過濾非數字
                    val filtered = s.filter { it.isDigit() || it == '.' }
                    val parsed = filtered.toDoubleOrNull()
                    if (parsed != null) {
                        onChange(parsed.coerceAtLeast(minValue))
                    } else if (filtered.isEmpty()) {
                        // 若清空則不改變（或可選當成 0）
                    }
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp), // 讓字離邊框有點距離
                decorationBox = { innerTextField ->
                    // 若為空顯示 placeholder
                    Box(contentAlignment = Alignment.Center) {
                        if (displayText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.common_empty_placeholder),
                                style = LocalTextStyle.current.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // 上下箭頭：放在數字欄旁、垂直排列；按鈕尺寸固定且一致
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .height(heightDp)
                .wrapContentWidth()
        ) {
            IconButton(
                onClick = { onChange((value + 1.0).coerceAtLeast(minValue)) },
                modifier = Modifier
                    .size(20.dp)
                    .padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp, // 可换成更“尖”的图标，如果需要
                    contentDescription = stringResource(R.string.common_increase_action),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            IconButton(
                onClick = { onChange((value - 1.0).coerceAtLeast(minValue)) },
                modifier = Modifier
                    .size(20.dp)
                    .padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.common_decrease_action),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCheckoutDialog(
    selectedItems: List<ShoppingListItem>,
    shops: List<ShopInfo>,
    onDismiss: () -> Unit,
    onConfirm: (List<CheckoutConfig>) -> Unit
) {
    val configs = remember(selectedItems) {
        mutableStateMapOf<String, CheckoutConfig>().apply {
            selectedItems.forEach { item ->
                this[item.id] = CheckoutConfig(
                    itemId = item.id,
                    location = "Pantry",
                    gramsPerPc = ""
                )
            }
        }
    }

    // [修正] 將 O(N*M) 的搜尋移出 Composition 階段，預先計算每個 item 是否需要輸入重量
    val needGramsMap = remember(selectedItems, shops) {
        val shopMap = shops.associateBy { it.id }
        selectedItems.associate { item ->
            val shop = item.shopId?.let { shopMap[it] }
            val product = shop?.catalog?.find { it.id == item.shopProductId }
            val isFood = !product?.nutrients.isNullOrEmpty()
            item.id to (isFood && item.unit == "pcs")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.storage_checkin_confirm_title, selectedItems.size)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(selectedItems, key = { it.id }) { item ->
                    val needGramsInput = needGramsMap[item.id] == true
                    val config = configs[item.id]!!
                    CheckoutItemCard(
                        item = item,
                        config = config,
                        needGramsInput = needGramsInput,
                        onConfigChange = { newConfig -> configs[item.id] = newConfig }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(configs.values.toList()) }) {
                Text(stringResource(R.string.storage_confirm_checkin_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutItemCard(
    item: ShoppingListItem,
    config: CheckoutConfig,
    needGramsInput: Boolean,
    onConfigChange: (CheckoutConfig) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.common_quantity_unit_desc, fmt(item.quantity), item.unit),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.storage_location), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                listOf("Pantry", "Fridge", "Freezer").forEach { loc ->
                    val selected = config.location == loc
                    FilterChip(
                        selected = selected,
                        onClick = { onConfigChange(config.copy(location = loc)) },
                        label = { Text(locLabel(loc)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when(loc) {
                                "Fridge" -> StorageColors.Fridge
                                "Freezer" -> StorageColors.Freezer
                                else -> StorageColors.Pantry
                            },
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            if (needGramsInput) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactTextField(
                        value = config.gramsPerPc,
                        onValueChange = { onConfigChange(config.copy(gramsPerPc = it)) },
                        placeholder = stringResource(R.string.storage_grams_per_piece_hint),
                        label = stringResource(R.string.storage_weight_label),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.storage_grams_per_piece_help_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
// 辅助数据类
data class CheckoutConfig(
    val itemId: String,
    val location: String,
    val gramsPerPc: String
)
private fun String.toNonNegDoubleOrZero(): Double =
    toDoubleOrNull()?.takeIf { it >= 0 } ?: 0.0

private fun String.toNonNegIntOrTwo(): Int =
    toIntOrNull()?.takeIf { it >= 2 } ?: 2

private fun Double?.toStringSafe(): String = this?.let {
    if (it % 1.0 == 0.0) it.toInt().toString() else "%.2f".format(it)
} ?: ""

private fun Int?.toStringSafe(): String = this?.toString() ?: ""

@Composable
private fun CostStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
// --- ✨ 新增這些 BigDecimal 輔助函式以解決型別錯誤 ---

// 1. 解決 toStringSafe 找不到的問題
private fun BigDecimal?.toStringSafe(): String =
    this?.stripTrailingZeros()?.toPlainString() ?: ""

// 2. 解決 String.toBigDecimalOrNull 找不到的問題
private fun String.toBigDecimalOrNull(): BigDecimal? {
    return try {
        if (this.isBlank()) null else BigDecimal(this.trim().replace(",", ""))
    } catch (e: Exception) {
        null
    }
}

// 3. 讓 fmt 也能吃 BigDecimal (解決 StoragePage.kt:4563, 4797 等錯誤)
fun fmt(v: BigDecimal?): String =
    v?.stripTrailingZeros()?.toPlainString() ?: "0"

@Composable
private fun DefrostSuggestionCard(
    items: List<InventoryItem>,
    onMoveToFridge: (InventoryItem) -> Unit,
    onIgnore: () -> Unit
) {
    if (items.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD), // Ice blue background
            contentColor = Color(0xFF0D47A1)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF90CAF9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AcUnit,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Thaw Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prep for tomorrow's meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1565C0).copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onIgnore, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Close, null, tint = Color(0xFF1565C0))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFBBDEFB)
            )

            // Items List
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current: ${item.location}", // Likely "Freezer"
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1E88E5)
                        )
                    }

                    // Action Button
                    Button(
                        onClick = { onMoveToFridge(item) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Kitchen, // Fridge Icon
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Move to Fridge", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
suspend fun optimizeAndNavigate(
    context: Context,
    currentLat: Double?,
    currentLng: Double?,
    shops: List<ShopInfo>
) {
    withContext(Dispatchers.IO) {
        var finalRoute = shops

        // 1. 如果有當前座標，且站點超過 1 個，執行 Geocoder 經緯度解析 + Greedy TSP
        if (currentLat != null && currentLng != null && shops.size > 1) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val shopGeos = shops.mapNotNull { shop ->
                try {
                    val query = shop.address?.takeIf { it.isNotBlank() } ?: shop.name
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocationName(query, 1)
                    if (!results.isNullOrEmpty()) {
                        Triple(shop, results[0].latitude, results[0].longitude)
                    } else null
                } catch (e: Exception) { null }
            }

            if (shopGeos.isNotEmpty()) {
                var currPos = currentLat to currentLng
                val unvisited = shopGeos.toMutableList()
                val sorted = mutableListOf<ShopInfo>()

                // 貪婪演算法：永遠找離當前點最近的下一站
                while (unvisited.isNotEmpty()) {
                    val next = unvisited.minByOrNull {
                        calculateHaversineDistance(currPos.first, currPos.second, it.second, it.third)
                    }!!
                    sorted.add(next.first)
                    unvisited.remove(next)
                    currPos = next.second to next.third
                }

                // 補上那些 Google 找不到經緯度的店家（放最後面）
                val resolvedIds = sorted.map { it.id }.toSet()
                finalRoute = sorted + shops.filter { it.id !in resolvedIds }
            }
        }

        // 2. 組合 Google Maps 原生導航 URL (Directions API format)
        if (finalRoute.isNotEmpty()) {
            val destination = finalRoute.last().let { it.address?.takeIf { a -> a.isNotBlank() } ?: it.name }
            val waypoints = finalRoute.dropLast(1).map { it.address?.takeIf { a -> a.isNotBlank() } ?: it.name }

            val encodedDest = URLEncoder.encode(destination, "UTF-8")
            val encodedWps = waypoints.joinToString("|") { URLEncoder.encode(it, "UTF-8") }

            var uriStr = "https://www.google.com/maps/dir/?api=1&destination=$encodedDest&travelmode=driving"
            if (waypoints.isNotEmpty()) uriStr += "&waypoints=$encodedWps"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // 3. 切回 Main Thread 啟動
            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Fallback: 如果沒裝 Google Maps，丟給系統瀏覽器處理
                    intent.setPackage(null)
                    context.startActivity(intent)
                }
            }
        }
    }
}

// 直線距離計算 (Haversine formula)
fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // 地球半徑(公尺)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}