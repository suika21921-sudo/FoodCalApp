@file:OptIn(ExperimentalMaterial3Api::class)

package com.mealoo.app.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import android.graphics.*
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import android.location.Geocoder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.unit.sp
import com.mealoo.app.R
import com.mealoo.app.pricing.EventRule
import com.mealoo.app.pricing.Step
import com.mealoo.app.model.FoodItem
import com.mealoo.app.db.FoodDbRepository
import com.mealoo.app.model.LocalizedNutrientEntry
import com.mealoo.app.model.NutrientEntry
import com.mealoo.app.model.NutrientKeys
import com.mealoo.app.model.UnifiedFoodDisplayItem
import com.mealoo.app.model.UserFoodStorage
import com.mealoo.app.ui.localizePredefinedTag
import com.mealoo.app.ui.predefinedTags
import com.mealoo.app.model.DrinkEntry
import com.mealoo.app.model.EatEntry
import com.mealoo.app.data.MealPlanSummaryDto
import java.net.HttpURLConnection
import java.net.URL
import androidx.annotation.StringRes
import java.util.Calendar
import kotlin.math.ln
import kotlinx.serialization.Serializable
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import kotlin.collections.plus
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.model.RecipeEntry
import kotlinx.serialization.KSerializer
import android.Manifest
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mealoo.app.model.AppCurrency
import com.mealoo.app.ui.design.CompactTextField
import kotlinx.serialization.json.jsonObject
import kotlin.math.roundToInt
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        // 寫入：統一轉為 String，避免科學記號，預設保留原始精度或由業務層控制 scale
        // 這裡為了存儲整潔，建議去除末尾 0，但在計算時務必 setScale
        encoder.encodeString(value.stripTrailingZeros().toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return try {
            // 讀取：支援 String ("10.5") 和 Number (10.5)
            val input = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content
                ?: decoder.decodeString()

            if (input.isBlank()) BigDecimal.ZERO else BigDecimal(input)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}
private val jsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
// 擴充函式：方便全域使用統一精度
fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
fun BigDecimal.qty(): BigDecimal = this.setScale(3, RoundingMode.HALF_UP)
fun Double.toBD(): BigDecimal = BigDecimal.valueOf(this)
enum class UnifiedFoodSourceScope {
    ALL,
    DATA,
    USER,
    STORAGE,
    RECIPE
}
suspend fun queryUnifiedFoods(
    context: Context,
    keyword: String,
    scope: UnifiedFoodSourceScope,
    storageItems: List<InventoryItem>,
    recipeEntries: List<RecipeEntry>
): List<UnifiedFoodDisplayItem> {

    // 1. DATA 来源（JP + data.json merge）
    val locale = Locale.getDefault()
    val foodRepo = FoodDbRepository.getInstance(context)
    foodRepo.ensureSeeded(context)

    val dataFoods = foodRepo.searchLocalizedFoods(keyword, limit = 200).map { food ->
        UnifiedFoodDisplayItem(
            id = food.fdcId.toString(),
            name = LocaleUtil.getLocalizedName(food, locale),   // ✅ 根据系统语言返回 nameEn / nameJa / nameZh
            source = "DATA",
            // ✅ 用英文标准 key（Energy / Protein / Available carbohydrate...）
            nutrients = food.nutrients,
            // ✅ 从 JSON 读取的 category 字段，经 LocaleUtil 做本地化
            category = LocaleUtil.getLocalizedCategory(food, locale)
        )
    }

    // 2. USER 来源（UserFoodItem）
    val userFoods = UserFoodStorage.loadUserFoods(context).map { uf ->
        UnifiedFoodDisplayItem(
            id = uf.id,
            name = uf.name,
            source = "USER",
            nutrients = uf.nutrientsPer100g,
            category = null   // 暂时没有类别信息
        )
    }

    // 3. STORAGE 来源（InventoryItem）
    val storageFoods = storageItems.mapNotNull { inv ->
        val nut = inv.standardNutrientsPer100g ?: return@mapNotNull null
        UnifiedFoodDisplayItem(
            id = inv.id,
            name = inv.name,
            source = "STORAGE",
            nutrients = nut,
            category = null   // 暂时没有类别信息
        )
    }

    // 4. RECIPE 来源（RecipeEntry 转成“100g 营养素”）
    val recipeFoods = recipeEntries.mapNotNull { r ->
        if (r.totalWeightGram <= 0.0) return@mapNotNull null

        val per100g = mapOf(
            "Energy" to (r.kcalPerServing / r.totalWeightGram * 100.0),
            "Protein" to (r.protein / r.totalWeightGram * 100.0),
            "Available carbohydrate" to (r.carbs / r.totalWeightGram * 100.0),
            "Total lipid (fat)" to (r.fats / r.totalWeightGram * 100.0)
        )

        UnifiedFoodDisplayItem(
            id = r.id,
            name = r.name,
            source = "RECIPE",
            nutrients = per100g,
            category = null   // 食谱暂时不挂类别
        )
    }

    // 根据 scope 选择来源
    val allFoods = when (scope) {
        UnifiedFoodSourceScope.DATA -> dataFoods
        UnifiedFoodSourceScope.USER -> userFoods
        UnifiedFoodSourceScope.STORAGE -> storageFoods
        UnifiedFoodSourceScope.RECIPE -> recipeFoods
        UnifiedFoodSourceScope.ALL -> dataFoods + userFoods + storageFoods + recipeFoods
    }

    // 关键字过滤（name + category 一起搜）
    if (keyword.isBlank()) return allFoods

    val kwLower = keyword.lowercase()
    return allFoods.filter { item ->
        item.source == "DATA" ||
            item.name.lowercase().contains(kwLower) ||
            item.category?.lowercase()?.contains(kwLower) == true
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagMultiSelectDropdown(
    selectedIds: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault().language
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "tag_arrow"
    )

    // 让菜单宽度跟头部 input 一样
    var anchorWidth by remember { mutableStateOf(0) }
    val widthDp = with(LocalDensity.current) { anchorWidth.toDp() }

    val placeholder = stringResource(R.string.please_select_tags)
    val displayText = if (selectedIds.isEmpty()) {
        ""
    } else {
        selectedIds.joinToString("、") { localizePredefinedTag(it, locale) }
    }

    Column(modifier) {
        // label
        Text(
            text = stringResource(R.string.tags),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { anchorWidth = it.width }
        ) {
            CompactTextField(
                value = displayText,
                onValueChange = { /* read-only */ },
                label = null,
                placeholder = placeholder,
                leadingIcon = null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                },
                singleLine = true,
                editable = false,
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            )

            // ▼▼ DropdownMenu：风格贴近 CompactDropdownFieldMenu ▼▼
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(widthDp)
                    .heightIn(max = 280.dp)
                    .border(
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        RoundedCornerShape(
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp,
                            topStart = 0.dp,
                            topEnd = 0.dp
                        )
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        RoundedCornerShape(
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp,
                            topStart = 0.dp,
                            topEnd = 0.dp
                        )
                    )
            ) {
                predefinedTags.forEach { tag ->
                    val picked = selectedIds.contains(tag.id)
                    val bg = if (picked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        Color.Transparent
                    }
                    val rowShape = RoundedCornerShape(8.dp)

                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg, rowShape)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = picked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.surface)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = localizePredefinedTag(tag.id, locale),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            val newSelection =
                                if (picked) selectedIds - tag.id else selectedIds + tag.id
                            onSelectedChange(newSelection)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateField(
    expiry: String?,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(com.mealoo.app.R.string.expiration_data),
    placeholderText: String = stringResource(com.mealoo.app.R.string.choose_expiration_data) + stringResource(R.string.optional),
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    // 弹出 DatePickerDialog 的副作用（保留你原来的解析逻辑）
    LaunchedEffect(showPicker) {
        if (!showPicker) return@LaunchedEffect
        val cal = Calendar.getInstance()
        if (!expiry.isNullOrBlank()) {
            runCatching {
                val parts = expiry.split("-").map { it.toInt() }
                cal.set(parts[0], (parts[1] - 1).coerceIn(0, 11), parts[2])
            }
        }
        val dialog = DatePickerDialog(
            context,
            R.style.MealooDatePickerDark,   // ✅ 强制走暗色主题
            { _, y, m, d ->
                val picked = String.format("%04d-%02d-%02d", y, m + 1, d)
                onDateSelected(picked)
                showPicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        dialog.setOnCancelListener { showPicker = false }
        dialog.setOnDismissListener { showPicker = false }
        dialog.show()
    }

    // UI: Compact 风格（pill + 浅灰占位 + border），整行可点
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        val fieldHeight = 44.dp
        val shape = RoundedCornerShape(12.dp)
        val emptyFill = (containerColor ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.04f)
        val baseFill = containerColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        val borderColor = (contentColor ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.12f)
        val finalContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .clip(shape)
                .background(if (expiry.isNullOrBlank()) emptyFill else baseFill)
                .border(BorderStroke(1.dp, borderColor), shape)
                .clickable { showPicker = true } // 整行点开选择器
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                IconButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = finalContentColor)
                }
                Spacer(Modifier.width(8.dp))
                // 显示 value 或 placeholder（placeholder 覆盖样式）
                if (expiry.isNullOrBlank()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = finalContentColor.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = expiry,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = finalContentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
enum class DiscountRuleType {
    PERCENT_EACH,
    AMOUNT_EACH,
    SECOND_PERCENT,
    SECOND_MINUS,
    BUY_X,
    GET_Y,
    GIFT_PRICING,
    BUNDLE_QTY,
    BUNDLE_PRICE,
    QTY_THRESHOLD,
    PERCENT,
    SPEND_THRESHOLD,
    AMOUNT,
    FREE_SHIPPING
}

val DiscountRuleType.labelResId: Int
    get() = when (this) {
        DiscountRuleType.PERCENT_EACH -> R.string.discount_percent_each_label
        DiscountRuleType.AMOUNT_EACH -> R.string.discount_amount_each_label
        DiscountRuleType.SECOND_PERCENT -> R.string.discount_second_percent_label
        DiscountRuleType.SECOND_MINUS -> R.string.discount_second_minus_label
        DiscountRuleType.BUY_X -> R.string.discount_buy_x_label
        DiscountRuleType.GET_Y -> R.string.discount_get_y_label
        DiscountRuleType.GIFT_PRICING -> R.string.discount_gift_pricing_label
        DiscountRuleType.BUNDLE_QTY -> R.string.discount_bundle_qty_label
        DiscountRuleType.BUNDLE_PRICE -> R.string.discount_bundle_price_label
        DiscountRuleType.QTY_THRESHOLD -> R.string.discount_qty_threshold_label
        DiscountRuleType.PERCENT -> R.string.discount_percent_label
        DiscountRuleType.SPEND_THRESHOLD -> R.string.discount_spend_threshold_label
        DiscountRuleType.AMOUNT -> R.string.discount_amount_label
        DiscountRuleType.FREE_SHIPPING -> R.string.discount_free_shipping_note
    }

@Composable
fun DiscountRuleDropdown(
    value: EventRule,
    onValueChange: (EventRule) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.discount_rule_label),
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    // ✅ 1) 选项：displayTitle 给主框显示，subtitle 只在 picker 里显示
    val options = listOf(
        RuleOption(
            id = "NONE",
            title = stringResource(R.string.discount_rule_none_title),
            subtitle = stringResource(R.string.discount_rule_none_subtitle)
        ),
        RuleOption(
            id = "PercentOffEach",
            title = stringResource(R.string.discount_rule_percent_off_each_title),
            subtitle = stringResource(R.string.discount_rule_percent_off_each_subtitle)
        ),
        RuleOption(
            id = "AmountOffEach",
            title = stringResource(R.string.discount_rule_amount_off_each_title),
            subtitle = stringResource(R.string.discount_rule_amount_off_each_subtitle)
        ),
        RuleOption(
            id = "SecondPercent",
            title = stringResource(R.string.discount_rule_second_percent_title),
            subtitle = stringResource(R.string.discount_rule_second_percent_subtitle)
        ),
        RuleOption(
            id = "SecondMinus",
            title = stringResource(R.string.discount_rule_second_minus_title),
            subtitle = stringResource(R.string.discount_rule_second_minus_subtitle)
        ),
        RuleOption(
            id = "BuyXGetY",
            title = stringResource(R.string.discount_rule_buy_x_get_y_title),
            subtitle = stringResource(R.string.discount_rule_buy_x_get_y_subtitle)
        ),
        RuleOption(
            id = "BundleFixedPrice",
            title = stringResource(R.string.discount_rule_bundle_fixed_price_title),
            subtitle = stringResource(R.string.discount_rule_bundle_fixed_price_subtitle)
        ),
        RuleOption(
            id = "TierByQty",
            title = stringResource(R.string.discount_rule_tier_by_qty_title),
            subtitle = stringResource(R.string.discount_rule_tier_by_qty_subtitle)
        ),
        RuleOption(
            id = "TierBySpend",
            title = stringResource(R.string.discount_rule_tier_by_spend_title),
            subtitle = stringResource(R.string.discount_rule_tier_by_spend_subtitle)
        ),
        RuleOption(
            id = "Conditional",
            title = stringResource(R.string.discount_rule_conditional_title),
            subtitle = stringResource(R.string.discount_rule_conditional_subtitle)
        ),
        RuleOption(
            id = "FreeShipping",
            title = stringResource(R.string.discount_rule_free_shipping_title),
            subtitle = stringResource(R.string.discount_rule_free_shipping_subtitle)
        )
    )

    // ✅ 2) 当前 rule -> id
    val selectedId = when (value) {
        is EventRule.None             -> "NONE"
        is EventRule.PercentOffEach   -> "PercentOffEach"
        is EventRule.AmountOffEach    -> "AmountOffEach"
        is EventRule.SecondPercent    -> "SecondPercent"
        is EventRule.SecondMinus      -> "SecondMinus"
        is EventRule.BuyXGetY         -> "BuyXGetY"
        is EventRule.BundleFixedPrice -> "BundleFixedPrice"
        is EventRule.TierByQty        -> "TierByQty"
        is EventRule.TierBySpend      -> "TierBySpend"
        is EventRule.Conditional      -> "Conditional"
        is EventRule.FreeShipping     -> "FreeShipping"
    }

    // ✅ 3) 主框显示短标题（不会爆字）
    val displayTitle = options.firstOrNull { it.id == selectedId }?.title ?: stringResource(R.string.discount_rule_none_title)

    var showSheet by remember { mutableStateOf(false) }

    CompactTextField(
        value = displayTitle,
        onValueChange = {},
        label = label,
        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = contentColor ?: MaterialTheme.colorScheme.primary) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant) },
        editable = false,
        singleLine = true,
        containerColor = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
    )
    if (showSheet) {
        DiscountRulePickerSheet(
            visible = showSheet,
            rules = options,
            selectedId = selectedId,
            onConfirm = { id ->
                onValueChange(mapIdToDefaultRule(id))
                showSheet = false
            },
            onCancel = { showSheet = false },
            title = label
        )
    }
}

@Composable
private fun DiscountRulePickerSheet(
    visible: Boolean,
    rules: List<RuleOption>,
    selectedId: String?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    title: String,
) {
    if (!visible) return

    val initIndex = remember(selectedId, rules) {
        rules.indexOfFirst { it.id == (selectedId ?: "NONE") }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIndex)
    var pickedIndex by remember { mutableStateOf(initIndex) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.cancel), color = Color.Gray)
                        }
                        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = {
                            val pickedId = rules.getOrNull(pickedIndex)?.id ?: rules.first().id
                            onConfirm(pickedId)
                        }) {
                            Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp
                        )
                    ) {
                        itemsIndexed(rules) { index, opt ->
                            val selected = (index == pickedIndex)
                            DropdownMenuItem(
                                onClick = { pickedIndex = index },
                                text = {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Black
                                        )
                                        opt.subtitle?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                },
                                trailingIcon = {
                                    if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun mapIdToDefaultRule(id: String): EventRule = when (id) {
    "NONE" -> EventRule.None

    "PercentOffEach" -> EventRule.PercentOffEach(
        percent = BigDecimal("10.0")
    )

    "AmountOffEach" -> EventRule.AmountOffEach(
        amount = BigDecimal("10.0")
    )

    "SecondPercent" -> EventRule.SecondPercent(
        percent = BigDecimal("50.0")
    )

    "SecondMinus" -> EventRule.SecondMinus(
        amount = BigDecimal("5.0")
    )

    "BuyXGetY" -> EventRule.BuyXGetY(
        buyQty = BigDecimal.ONE,
        getQty = BigDecimal.ONE,
        priceOfGetPercent = BigDecimal.ZERO // 0 表示贈品免費
    )

    "BundleFixedPrice" -> EventRule.BundleFixedPrice(
        bundleQty = BigDecimal("2"),
        bundlePrice = BigDecimal("100.0")
    )

    "TierByQty" -> EventRule.TierByQty(
        steps = listOf(
            Step(
                threshold = BigDecimal("2"),
                percent = BigDecimal("10.0"),
                amount = null
            )
        )
    )

    "TierBySpend" -> EventRule.TierBySpend(
        steps = listOf(
            Step(
                threshold = BigDecimal("100.0"),
                amount = BigDecimal("10.0"),
                percent = null
            )
        )
    )

    "Conditional" -> EventRule.Conditional(
        eligiblePaymentMethods = null,
        eligibleStores = null,
        stackable = true
    )
    "FreeShipping" -> EventRule.FreeShipping

    else -> EventRule.None
}

data class RuleOption(
    val id: String,
    val title: String,
    val subtitle: String? = null
)
/**
 * 将任意食材的营养数据标准化为固定字段
 */
fun buildStandardNutrientMap(
    raw: Map<String, NutrientEntry>
): Map<String, Double> {
    val carbs = raw["Carbohydrate, by difference"]?.amount ?: 0.0
    val fiber = raw["Fiber, total dietary"]?.amount ?: 0.0
    val availableCarbs = if (raw.containsKey("Available carbohydrate")) {
        raw["Available carbohydrate"]?.amount ?: (carbs - fiber)
    } else carbs - fiber

    // ✅ 改成返回英文键
    return NutrientKeys.STANDARD_KEYS.associate { key ->
        val value = when (key) {
            "Available carbohydrate" -> availableCarbs
            "Fiber, total dietary" -> fiber
            else -> raw[key]?.amount ?: 0.0
        }
        key to value
    }
}
fun normalizeToEnglish(per100: Map<String, Double>): Map<String, Double> {
    val reverse = NutrientKeys.STANDARD_KEYS.associateBy { NutrientNameMapper.localize(it) }
    return per100.mapKeys { (k, _) -> reverse[k] ?: k }
}
object NutrientNameMapper {
    fun localize(nutrientKey: String): String {
        return when (Locale.getDefault().language) {
            "zh" -> zhMap[nutrientKey] ?: nutrientKey
            "ja" -> jaMap[nutrientKey] ?: nutrientKey
            else -> nutrientKey // 英文原样
        }
    }

    private val zhMap = mapOf(
        "Energy" to "熱量",
        "Protein" to "蛋白質",
        "Available carbohydrate" to "可利用碳水",
        "Carbohydrate, by difference" to "碳水（差值）",
        "Fiber, total dietary" to "膳食纖維",
        "Total lipid (fat)" to "脂肪",
        "Sodium, Na" to "鈉",
        "Cholesterol" to "膽固醇"
    )

    private val jaMap = mapOf(
        "Energy" to "エネルギー",
        "Protein" to "たんぱく質",
        "Available carbohydrate" to "利用可能炭水化物",
        "Carbohydrate, by difference" to "炭水化物（差）",
        "Fiber, total dietary" to "食物繊維",
        "Total lipid (fat)" to "脂質",
        "Sodium, Na" to "ナトリウム",
        "Cholesterol" to "コレステロール"
    )
}
fun estimateCaffeineClearTime(
    intakeMg: Double,
    intakeTimeMillis: Long,
    halfLifeHours: Double = 5.0,
    thresholdMg: Double = 20.0
): Calendar {
    if (intakeMg <= thresholdMg) {
        return Calendar.getInstance().apply { timeInMillis = intakeTimeMillis }
    }
    val hoursToClear = halfLifeHours * (ln(intakeMg / thresholdMg) / ln(2.0))
    return Calendar.getInstance().apply {
        timeInMillis = intakeTimeMillis
        add(Calendar.MINUTE, (hoursToClear * 60).toInt())
    }
}
fun alcoholGramFromVolume(volumeMl: Double, percent: Double): Double {
    val density = 0.789f // 酒精密度 g/mL
    return (volumeMl * percent / 100f * density)
}
object LocaleUtil {

    fun getLocalizedName(food: FoodItem, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "zh" -> food.nameZh
            "ja" -> food.nameJa
            else -> food.nameEn
        }
    }

    fun getLocalizedCategory(food: LocalizedNutrientEntry, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "zh" -> food.categoryZh ?: food.categoryEn ?: food.categoryJa.orEmpty()
            "ja" -> food.categoryJa ?: food.categoryEn ?: food.categoryZh.orEmpty()
            else -> food.categoryEn ?: food.categoryZh ?: food.categoryJa.orEmpty()
        }
    }

    fun getLocalizedCategory(food: FoodItem, locale: Locale = Locale.getDefault()): String? {
        return when (locale.language) {
            "zh" -> food.categoryZh
            "ja" -> food.categoryJa
            else -> food.categoryEn
        }
    }

    // ✅ 新增：支援 LocalizedFoodItem
    fun getLocalizedName(food: LocalizedNutrientEntry, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "zh" -> food.nameZh
            "ja" -> food.nameJa
            else -> food.nameEn
        }
    }
}
private val BRACKET_REGEXES by lazy {
    listOf(
        "\\(.*?\\)", "\\[.*?\\]", "\\{.*?\\}", "<.*?>",          // 英文 () [] {} <>
        "（.*?）", "【.*?】", "［.*?］", "｛.*?｝",               // 全形（）【】［］｛｝
        "《.*?》", "〈.*?〉", "〔.*?〕", "「.*?」", "『.*?』"     // CJK 括号
    ).map { Regex(it) }
}

/** 移除各种括号及其内容，压缩空白；若清空则回退到原文 */
fun stripBrackets(src: String): String {
    var s = src
    repeat(3) {
        BRACKET_REGEXES.forEach { rx -> s = s.replace(rx, "") }
    }
    s = s.replace(Regex("\\s+"), "-")
        .trim(' ', '-', '·', '•', '，', ',', ';', '；', '。', '、', '・', '（', '）', '［', '］', '【', '】', '《', '》')
        .trim()

    return if (s.isNotEmpty()) s else src
}

private const val TAG = "ImageIOUtils"


private fun enforceImageCacheLimit(imageDir: File, maxTotalBytes: Long = 100L * 1024L * 1024L, maxFiles: Int = 300) {
    try {
        if (!imageDir.exists()) return
        val files = imageDir.listFiles()?.filter { it.isFile }?.toMutableList() ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxTotalBytes && files.size <= maxFiles) return

        // 以 lastModified 升序（最旧的先删）
        files.sortBy { it.lastModified() }
        var idx = 0
        while ((total > maxTotalBytes || files.size - idx > maxFiles) && idx < files.size) {
            val f = files[idx]
            val len = f.length()
            if (f.delete()) {
                total -= len
            }
            idx++
        }
    } catch (e: Exception) {
        Log.e(TAG, "enforceImageCacheLimit failed: ${e.message}", e)
    }
}
enum class TagCat(val key: String) {
    MEAL("meal"), DIET("diet"), CUISINE("cuisine"), COURSE("course"),
    TASTE("taste"), METHOD("method"), DEVICE("device"), OCCASION("occasion"),
    ALLERGEN("allergen"), DIFFICULTY("difficulty"), TIME("time"), OTHER("other");

    companion object { fun fromKey(k: String) = entries.find { it.key == k } ?: OTHER }
}

@Serializable
data class TagRef(val cat: String, val id: String)

object TagCatalog {
    // 仅示例：上线时放完整表
    val meal = setOf("breakfast","lunch","dinner","am_snack","pm_snack","night_snack")
    val diet = setOf("low_sugar","low_sodium","keto","vegan",
        "low_carb","low_fat","low_kcal","high_protein")   // ✅ 扩充
    val cuisine = setOf("taiwanese","japanese","korean","italian","chinese") // ✅ 扩充
    val course = setOf("main","side","soup","dessert","drink")
    val taste = setOf("sweet","sour","spicy","savory","bitter","umami")
    val method = setOf("stir_fry","steam","boil","bake","air_fry")
    val device = setOf("oven","air_fryer","pressure_cooker")
    val occasion = setOf("quick","meal_prep","party","kids")
    val allergen = setOf("peanuts","tree_nuts","gluten","dairy","egg")
    val difficulty = setOf("easy","medium","hard")
    private val aliases = mapOf( // ✅ 简单别名表
        "chinese_food" to "chinese"
    )

    fun categorize(token: String): Pair<String,String> {
        val t = token.lowercase()
        return when {
            t in meal      -> TagCat.MEAL.key to t
            t in diet      -> TagCat.DIET.key to t
            t in cuisine   -> TagCat.CUISINE.key to t
            t in course    -> TagCat.COURSE.key to t
            t in taste     -> TagCat.TASTE.key to t
            t in method    -> TagCat.METHOD.key to t
            t in device    -> TagCat.DEVICE.key to t
            t in occasion  -> TagCat.OCCASION.key to t
            t in allergen  -> TagCat.ALLERGEN.key to t
            t in difficulty-> TagCat.DIFFICULTY.key to t
            else           -> TagCat.OTHER.key to t
        }
    }
}
// 传入类别key（如 "meal"）与该类下的标签id（如 "breakfast"）
fun localizeTag(catKey: String, id: String, locale: java.util.Locale = java.util.Locale.getDefault()): String {
    val lang = locale.language
    return when (catKey) {
        "meal" -> when (id) {
            "breakfast" -> if (lang=="ja") "朝ごはん" else if (lang=="zh") "早餐" else "Breakfast"
            "lunch"     -> if (lang=="ja") "昼ごはん" else if (lang=="zh") "午餐" else "Lunch"
            "dinner"    -> if (lang=="ja") "夕食"     else if (lang=="zh") "晚餐" else "Dinner"
            "am_snack"  -> if (lang=="ja") "午前間食" else if (lang=="zh") "上午加餐" else "AM Snack"
            "pm_snack"  -> if (lang=="ja") "午後間食" else if (lang=="zh") "下午加餐" else "PM Snack"
            "night_snack"->if (lang=="ja") "夜食"     else if (lang=="zh") "宵夜" else "Night Snack"
            else -> id
        }
        "diet" -> when (id) {
            "low_carb" -> if (lang=="ja") "低炭水化物" else if (lang=="zh") "低碳水" else "Low Carb"
            "low_fat"  -> if (lang=="ja") "低脂肪"     else if (lang=="zh") "低脂" else "Low Fat"
            "low_kcal" -> if (lang=="ja") "低カロリー" else if (lang=="zh") "低卡" else "Low Calorie"
            "high_protein" -> if (lang=="ja") "高たんぱく" else if (lang=="zh") "高蛋白" else "High Protein"
            "low_sugar" -> if (lang=="ja") "低糖" else if (lang=="zh") "低糖" else "Low Sugar"
            "low_sodium"-> if (lang=="ja") "低ナトリウム" else if (lang=="zh") "低钠" else "Low Sodium"
            "keto" -> if (lang=="ja") "ケト" else if (lang=="zh") "生酮" else "Keto"
            "vegan"-> if (lang=="ja") "ヴィーガン" else if (lang=="zh") "纯素" else "Vegan"
            else -> id
        }
        "cuisine" -> when (id) {
            "chinese"  -> if (lang=="ja") "中華"   else if (lang=="zh") "中式" else "Chinese"
            "japanese" -> if (lang=="ja") "和食"   else if (lang=="zh") "日式" else "Japanese"
            "korean"   -> if (lang=="ja") "韓国料理" else if (lang=="zh") "韩式" else "Korean"
            "italian"  -> if (lang=="ja") "イタリアン" else if (lang=="zh") "意式" else "Italian"
            "taiwanese"-> if (lang=="ja") "台湾料理" else if (lang=="zh") "台式" else "Taiwanese"
            else -> id
        }
        "course" -> when (id) {
            "main"   -> if (lang=="ja") "主菜" else if (lang=="zh") "主菜" else "Main"
            "side"   -> if (lang=="ja") "副菜" else if (lang=="zh") "配菜" else "Side"
            "soup"   -> if (lang=="ja") "スープ" else if (lang=="zh") "汤品" else "Soup"
            "dessert"-> if (lang=="ja") "デザート" else if (lang=="zh") "甜点" else "Dessert"
            "drink"  -> if (lang=="ja") "飲み物" else if (lang=="zh") "饮料" else "Drink"
            else -> id
        }
        // 其它类别先回退到 id；需要时继续补表
        else -> id
    }
}

// 类别名字本地化（用于类别筛选条上显示 "餐次/饮食法/菜系" 等）
fun localizeTagCategory(catKey: String, locale: java.util.Locale = java.util.Locale.getDefault()): String {
    val lang = locale.language
    return when (catKey) {
        "meal" -> if (lang=="ja") "食事区分" else if (lang=="zh") "餐次" else "Meal"
        "diet" -> if (lang=="ja") "食事制限" else if (lang=="zh") "饮食法" else "Diet"
        "cuisine" -> if (lang=="ja") "料理系統" else if (lang=="zh") "菜系" else "Cuisine"
        "course" -> if (lang=="ja") "コース" else if (lang=="zh") "菜品" else "Course"
        "taste" -> if (lang=="ja") "味" else if (lang=="zh") "口味" else "Taste"
        "method" -> if (lang=="ja") "調理法" else if (lang=="zh") "烹饪法" else "Method"
        "device" -> if (lang=="ja") "器具" else if (lang=="zh") "器具" else "Device"
        "occasion" -> if (lang=="ja") "場面" else if (lang=="zh") "场景" else "Occasion"
        "allergen" -> if (lang=="ja") "アレルゲン" else if (lang=="zh") "过敏原" else "Allergen"
        "difficulty" -> if (lang=="ja") "難易度" else if (lang=="zh") "难度" else "Difficulty"
        else -> catKey
    }
}
fun normalizeToTarget(
    values: List<Double>,
    target: Double = 100.0,
    step: Double = 0.1,
): List<Double> {
    if (values.isEmpty()) return values
    val clamped = values.map { it.coerceIn(0.0, target) }
    val sum = clamped.sum()
    if (sum <= 0.0) return List(values.size) { 0.0 }

    // 比例缩放到 target
    val scaled = clamped.map { it * (target / sum) }

    // 量化到 step（例如 0.1%）
    val quantized = scaled.map { kotlin.math.round(it / step) * step }.toMutableList()

    // 修正量化后的残差（把最小单位分配出去，确保总和严格等于 target）
    val diff = target - quantized.sum()
    val units = kotlin.math.round(diff / step).toInt()
    val inc = if (units >= 0) step else -step
    repeat(kotlin.math.abs(units)) { i ->
        val idx = i % quantized.size
        quantized[idx] = (quantized[idx] + inc).coerceIn(0.0, target)
    }
    return quantized
}

// 依据新餐次数重建 ratios（尽量保留旧值；必要时缩放/分配剩余；最后正规化）
fun recomputeMealRatios(
    newNames: List<String>,
    oldRatios: List<Pair<String, Double>>,
    step: Double = 0.1
): List<Pair<String, Double>> {
    val oldMap = oldRatios.toMap()
    val kept = newNames.filter { it in oldMap }
    val added = newNames.filter { it !in oldMap }

    // 先取保留部分的旧值
    val keptVals = kept.map { oldMap[it] ?: 0.0 }
    val keptSum = keptVals.sum()

    val baseVals: List<Double> = if (added.isNotEmpty()) {
        val leftover = 100.0 - keptSum
        val seed = step                      // 每个新增餐至少分到 step（默认 0.1%）
        val addTotal = seed * added.size

        if (leftover >= addTotal) {
            // 剩余足够：先给每个新增餐种子，再把剩余均分给新增餐
            val extra = (leftover - addTotal) / added.size
            keptVals + List(added.size) { seed + extra }
        } else {
            // 剩余不够：把保留部分按比例缩小，给新增餐种子
            val scale = if (keptSum > 0.0) (100.0 - addTotal) / keptSum else 0.0
            val scaledKept = keptVals.map { it * scale }
            scaledKept + List(added.size) { seed }
        }
    } else {
        keptVals
    }

    val normalized = normalizeToTarget(baseVals, target = 100.0, step = step)

    // 重新组装成 <name, ratio>
    return newNames.mapIndexed { i, name -> name to normalized[i] }
}
data class FavoriteTask(
    val id: String,
    val name: String,
    val imageUrl: String?
)

fun String.toLocalizedNutrientName(ctx: Context): String = when (this) {
    "Energy" -> ctx.getString(R.string.energy)          // 例如「能量」
    "Protein" -> ctx.getString(R.string.protein)        // 「蛋白质」
    "Available carbohydrate",
    "Carbohydrate, by difference" ->
        ctx.getString(R.string.carbs)                   // 「碳水化合物」
    "Total lipid (fat)" -> ctx.getString(R.string.fats)  // 「脂肪」
    else -> this  // 没有定义就先原样显示
}
data class ScannedFood(val name: String, val nutrients: Map<String, Double>)

private val httpClient = OkHttpClient()

@Composable
fun ImagePickerTile(
    uri: String?,
    onPick: () -> Unit,
    onReCrop: () -> Unit,
    size: Dp,
    corner: Dp = 14.dp
) {
    val tfColors = TextFieldDefaults.colors()
    val containerColor: Color = tfColors.unfocusedContainerColor

    val borderColor = MaterialTheme.colorScheme.outlineVariant ?: MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .border(1.dp, borderColor, RoundedCornerShape(corner))
            .clickable { if (uri == null) onPick() else onReCrop() },
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(corner),
        color = containerColor // ✅ 与 CompactTextField 背景一致
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text(text = stringResource(id = R.string.add_image))
                }
            } else {
                val context = LocalContext.current
                val thumb by remember(uri) {
                    mutableStateOf(
                        runCatching {
                            val parsed = Uri.parse(uri)
                            context.contentResolver.openInputStream(parsed).use { input ->
                                input?.let { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                            }
                        }.getOrNull()
                    )
                }
                if (thumb != null) {
                    Image(bitmap = thumb!!, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                AssistChip(
                    onClick = onReCrop,
                    label = { Text(stringResource(id = R.string.edit)) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
    }
}
@Composable
fun rememberPickImageWithInAppCrop(
    onCropped: (Uri) -> Unit,
    onError: (Throwable) -> Unit = {}
): Pair<() -> Unit, (Uri) -> Unit> {
    val context = LocalContext.current
    var pendingSrc by remember { mutableStateOf<Uri?>(null) }
    var showCrop by remember { mutableStateOf(false) }


    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingSrc = uri
            showCrop = true
        }
    }


    if (showCrop && pendingSrc != null) {
        InAppCropSheet(
            source = pendingSrc!!,
            onDismiss = { showCrop = false },
            onConfirm = { bm, exportRect ->
                try {
                    val outUri = exportCroppedBitmap(context, bm, exportRect)
                    onCropped(outUri)
                } catch (t: Throwable) {
                    onError(t)
                } finally {
                    showCrop = false
                }
            },
            onError = {
                onError(it)
                showCrop = false
            }
        )
    }


    val pick = { launcher.launch("image/*") }
    val cropAgain: (Uri) -> Unit = { src -> pendingSrc = src; showCrop = true }
    return pick to cropAgain
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppCropSheet(
    source: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap, android.graphics.Rect) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(source) {
        runCatching { loadDownscaledBitmap(context, source, 2048) }
            .onSuccess { bitmap = it }
            .onFailure { error = it }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("Image error") },
            text = { Text(error!!.localizedMessage ?: "Failed to load image") }
        )
        return
    }

    val bm = bitmap
    // 平移/缩放状态
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pinch to zoom, drag to move", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // 预览区域固定为正方形视口
            val boxSize = 320.dp
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        // 手势：缩放 + 平移
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 10f)
                            offset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bm == null) {
                    CircularProgressIndicator()
                } else {
                    // 画布渲染（带平移/缩放），以及方形裁切遮罩
                    Canvas(Modifier.fillMaxSize()) {
                        val iw = bm.width.toFloat()
                        val ih = bm.height.toFloat()
                        val vw = size.width
                        val vh = size.height

                        // 让图片以“居中适配”作为基础尺寸，然后叠加 scale、offset
                        val fit = minOf(vw / iw, vh / ih)
                        val drawW = iw * fit * scale
                        val drawH = ih * fit * scale
                        val left = (vw - drawW) / 2f + offset.x
                        val top  = (vh - drawH) / 2f + offset.y

                        drawImage(
                            image = bm.asImageBitmap(),
                            srcSize = androidx.compose.ui.unit.IntSize(bm.width, bm.height),
                            dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt()),
                            dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
                        )

                        // 方形裁切框 = 视口的中心正方形
                        val side = minOf(vw, vh)
                        val cropLeft = (vw - side) / 2f
                        val cropTop  = (vh - side) / 2f
                        val cropRect = androidx.compose.ui.geometry.Rect(cropLeft, cropTop, cropLeft + side, cropTop + side)

                        // 遮罩：外部半透 + 白色描边
                        val path = androidx.compose.ui.graphics.Path().apply {
                            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, vw, vh))
                            addRect(cropRect)
                            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                        }
                        drawPath(path, color = Color(0f, 0f, 0f, 0.55f))
                        drawRect(
                            color = Color.White,
                            topLeft = cropRect.topLeft,
                            size = cropRect.size,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // 简单的重置按钮（可选）
                TextButton(onClick = { scale = 1f; offset = androidx.compose.ui.geometry.Offset.Zero }) {
                    Text("Reset")
                }
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    val density = LocalDensity.current
                    Button(onClick = {
                        // 1) 先做所有组合性读取 / 空值检查（这些不能被 try/catch 包裹）
                        val viewportPx = with(density) { boxSize.toPx().toInt() } // LocalDensity.current 的读取已经完成
                        val bmNonNull = bm
                        if (bmNonNull == null) {
                            onError(IllegalStateException("Bitmap not loaded"))
                            return@Button
                        }

                        // 2) 把可能抛的纯逻辑放到 runCatching（或 try/catch），不会触碰到 composable API
                        kotlin.runCatching {
                            computeCropRectInBitmap(
                                bm = bmNonNull,
                                scale = scale,
                                offset = offset,
                                viewportPx = viewportPx
                            )
                        }.onSuccess { rect ->
                            // 成功：回调裁切确认（非 composable）
                            onConfirm(bmNonNull, rect)
                        }.onFailure { t ->
                            // 失败：回调错误处理
                            onError(t)
                        }
                    }) {
                        Text("Crop")
                    }
                }
            }
        }
    }
}

private suspend fun loadDownscaledBitmap(
    context: Context,
    uri: Uri,
    maxSide: Int
): Bitmap = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri).use { input ->
        val bytes = input!!.readBytes()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val (w, h) = opts.outWidth to opts.outHeight
        var sample = 1
        var side = maxOf(w, h)
        while (side / (sample * 2) > maxSide) sample *= 2
        val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts2)
            ?: error("Decode failed")
    }
}

private fun exportCroppedBitmap(
    context: Context,
    bm: Bitmap,
    rect: Rect
): Uri {
    val safe = Rect(0, 0, bm.width, bm.height).apply { intersect(rect) }
    val cropped = Bitmap.createBitmap(bm, safe.left, safe.top, safe.width(), safe.height())
    val out = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
    FileOutputStream(out).use { fos ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 92, fos)
    }
    return Uri.fromFile(out) // 仅在 app 内使用足够；若要对外分享，再换成 FileProvider
}
private fun computeCropRectInBitmap(
    bm: Bitmap,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    viewportPx: Int
): android.graphics.Rect {
    val iw = bm.width.toFloat()
    val ih = bm.height.toFloat()
    val vw = viewportPx.toFloat()
    val vh = viewportPx.toFloat()

    // 与 Canvas 里一致：先 fit center，再叠加 scale、offset
    val fit = minOf(vw / iw, vh / ih)
    val drawW = iw * fit * scale
    val drawH = ih * fit * scale
    val leftOnView = (vw - drawW) / 2f + offset.x
    val topOnView  = (vh - drawH) / 2f + offset.y

    // 方形裁切框在视口中心
    val cropSide = minOf(vw, vh)
    val cropLeft = (vw - cropSide) / 2f
    val cropTop  = (vh - cropSide) / 2f

    // 把裁切框左上角在 view 中的位置反算到 bitmap 坐标
    val xInBitmap = ((cropLeft - leftOnView) / (fit * scale)).coerceIn(0f, iw)
    val yInBitmap = ((cropTop  - topOnView)  / (fit * scale)).coerceIn(0f, ih)
    val sideInBitmap = (cropSide / (fit * scale))
        .coerceIn(1f, minOf(iw - xInBitmap, ih - yInBitmap))

    val l = xInBitmap.toInt()
    val t = yInBitmap.toInt()
    val s = sideInBitmap.toInt()
    return android.graphics.Rect(l, t, (l + s).coerceAtMost(bm.width), (t + s).coerceAtMost(bm.height))
}
@Composable
fun formatMoney(value: BigDecimal): String {
    val df = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.TAIWAN)
    return df.format(value)
}
fun trimQty(q: Double): String {
    return if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()
}

object PriceCalcUtils {
    enum class CpRefUnit(val multiplier: Int) {
        PER_1(1),
        PER_100(100),
        PER_1000(1000),
    }

    fun defaultCpRefUnit(currencyCode: String?): CpRefUnit {
        return when (currencyCode?.uppercase()) {
            "JPY", "JP¥", "¥" -> CpRefUnit.PER_100
            else -> CpRefUnit.PER_1
        }
    }

    fun currencySymbol(code: String?): String {
        return when (code?.uppercase()) {
            "NTD", "TWD", "NT$", "NT" -> "NT$"
            "JPY", "JP¥", "¥" -> "¥"
            "USD", "$", "US$" -> "$"
            "CNY", "RMB" -> "¥"
            "EUR", "€" -> "€"
            else -> code?.takeIf { it.isNotBlank() } ?: "$"
        }
    }

    fun formatCpDenominator(currencySymbol: String, refUnit: CpRefUnit): String {
        return if (refUnit == CpRefUnit.PER_1) currencySymbol else currencySymbol + refUnit.multiplier.toString()
    }

    fun scaleCpValue(value: Double?, refUnit: CpRefUnit): Double? {
        val v = value ?: return null
        if (!v.isFinite() || v <= 0.0) return null
        return v * refUnit.multiplier.toDouble()
    }

    fun formatCpValue(value: Double?, fallback: String = "-"): String {
        val v = value ?: return fallback
        if (!v.isFinite() || v <= 0.0) return fallback

        val scale = when {
            v >= 10.0 -> 1
            v >= 1.0 -> 2
            v >= 0.1 -> 3
            v >= 0.01 -> 4
            else -> 6
        }
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }

    fun formatCpValue(value: Double?, refUnit: CpRefUnit, fallback: String = "-"): String {
        return formatCpValue(scaleCpValue(value, refUnit), fallback)
    }
}

data class InventoryCostInfo(
    val totalCost: BigDecimal,
    val costPerUnit: BigDecimal?,
    val costPer100g: BigDecimal?,
    val currency: String?,
    val quantityLabel: String,
    val unit: String,
    val cpValues: Map<CPMetricType, Double>,
    val highlightMetrics: List<CPMetricType>,
)

data class RecipeCostInfo(
    val totalCost: BigDecimal,
    val costPer100g: BigDecimal?,
    val costPer100Kcal: BigDecimal?,
    val currency: String?,
    val cpValues: Map<CPMetricType, Double>,
)

enum class CPMetricType(
    @StringRes val labelResId: Int,
    val baseUnit: String,
) {
    ENERGY(R.string.energy, "kcal"),
    PROTEIN(R.string.protein, "g"),
    CARB(R.string.carbs, "g"),
    FAT(R.string.fats, "g"),
    FIBER(R.string.fiber, "g"),
    WEIGHT(R.string.total_weight, "g"),
    COST(R.string.total_cost, ""),
}

fun RecipeEntry.toCostInfo(
    cpRefUnit: PriceCalcUtils.CpRefUnit = PriceCalcUtils.defaultCpRefUnit(this.currency),
): RecipeCostInfo {
    // 1. 基底成本 (已是 BD)
    val baseCost: BigDecimal = this.cost ?: BigDecimal.ZERO

    // 2. 食材成本累加 (Ingredient.cost 已是 BD)
    val ingredientsCost: BigDecimal = ingredients.fold(BigDecimal.ZERO) { acc, ing ->
        val c = ing.cost ?: BigDecimal.ZERO
        acc.add(c)
    }

    val rawTotalCost: BigDecimal = baseCost.add(ingredientsCost)
    val totalCost: BigDecimal = rawTotalCost.max(BigDecimal.ZERO) // 使用 max(BigDecimal.ZERO)
    val totalCostRounded = totalCost.money() // 使用 helper extension

    // 重量計算保持 Double -> BigDecimal 轉換 (因為 grams 還是 Double)
    val rawTotalWeight: BigDecimal = when {
        this.totalWeightGram > 0.0 -> BigDecimal.valueOf(this.totalWeightGram)
        ingredients.isNotEmpty() -> ingredients.fold(BigDecimal.ZERO) { acc, ing ->
            val amt = ing.amount
            if (amt > 0.0) acc.add(BigDecimal.valueOf(amt)) else acc
        }
        else -> BigDecimal.ZERO
    }
    val totalWeight: BigDecimal = rawTotalWeight.max(BigDecimal.ZERO)

    // 每 100g 成本
    val costPer100g: BigDecimal? = if (totalWeight > BigDecimal.ZERO) {
        totalCost.divide(totalWeight, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100")).money()
    } else null

    // 每 100kcal 成本
    val kcal = this.kcalPerServing
    val costPer100Kcal: BigDecimal? = if (kcal > 0.0) {
        totalCost.divide(BigDecimal.valueOf(kcal), 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100")).money()
    } else null

    // CP 值計算 (轉為 Double 計算即可，因為是用於顯示圖表或排序)
    val cpMap = mutableMapOf<CPMetricType, Double>()
    val costDouble = totalCost.toDouble()

    if (costDouble > 0.001) {
        val mul = cpRefUnit.multiplier.toDouble()
        cpMap[CPMetricType.ENERGY] = (this.kcalPerServing / costDouble) * mul
        cpMap[CPMetricType.PROTEIN] = (this.protein / costDouble) * mul
        cpMap[CPMetricType.CARB] = (this.carbs / costDouble) * mul
        cpMap[CPMetricType.FAT] = (this.fats / costDouble) * mul
    }

    return RecipeCostInfo(
        totalCost = totalCostRounded,
        costPer100g = costPer100g,
        costPer100Kcal = costPer100Kcal,
        currency = this.currency,
        cpValues = cpMap,
    )
}

fun InventoryItem.toCostInfo(
    cpRefUnit: PriceCalcUtils.CpRefUnit = PriceCalcUtils.defaultCpRefUnit(this.currency),
): InventoryCostInfo {
    // cost 已經是 BD
    val rawTotalCost: BigDecimal = this.cost ?: BigDecimal.ZERO
    val totalCost: BigDecimal = rawTotalCost.max(BigDecimal.ZERO)
    val totalCostRounded = totalCost.money()

    // quantity 已經是 BD
    val qty: BigDecimal = this.quantity

    // 單價計算
    val costPerUnit: BigDecimal? = if (qty > BigDecimal.ZERO) {
        totalCost.divide(qty, 6, RoundingMode.HALF_UP).money()
    } else null

    // 每 100g 成本
    val gPerUnit = this.gramsPerUnit?.takeIf { it > 0.0 }
    val costPer100g: BigDecimal? = if (gPerUnit != null && qty > BigDecimal.ZERO) {
        val totalGrams = BigDecimal.valueOf(gPerUnit).multiply(qty)
        if (totalGrams > BigDecimal.ZERO) {
            totalCost.divide(totalGrams, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100")).money()
        } else null
    } else null

    // CP 值計算
    val cpMap = mutableMapOf<CPMetricType, Double>()
    if (costPer100g != null && costPer100g.toDouble() > 0.001 && this.standardNutrientsPer100g != null) {
        val costDouble = costPer100g.toDouble()
        val nuts = this.standardNutrientsPer100g

        val mul = cpRefUnit.multiplier.toDouble()
        fun calc(key: String): Double {
            val amount = nuts[key] ?: 0.0
            return if (amount > 0) (amount / costDouble) * mul else 0.0
        }

        // ... (中間的 calcCarb 與 highlightMetrics 邏輯保持不變) ...

        fun calcCarb(): Double {
            val amount = nuts["Carbohydrate, by difference"] ?: nuts["Available carbohydrate"] ?: 0.0
            return if (amount > 0) (amount / costDouble) * mul else 0.0
        }

        cpMap[CPMetricType.ENERGY] = calc("Energy")
        cpMap[CPMetricType.PROTEIN] = calc("Protein")
        cpMap[CPMetricType.FAT] = calc("Total lipid (fat)")
        cpMap[CPMetricType.FIBER] = calc("Fiber, total dietary")
        cpMap[CPMetricType.CARB] = calcCarb()
    }

    // ... (Highlight 邏輯保持不變) ...
    val categoryEn = this.category ?: ""
    val highlights = when (categoryEn) {
        "Vegetables", "Mushrooms", "Seaweeds" -> listOf(CPMetricType.FIBER, CPMetricType.ENERGY)
        "Meats", "Fish and Shellfish", "Eggs", "Legumes", "Dairy Products" -> listOf(CPMetricType.PROTEIN, CPMetricType.ENERGY)
        "Cereals", "Potatoes and Starches", "Sugars and Sweeteners" -> listOf(CPMetricType.ENERGY, CPMetricType.CARB)
        "Fruits" -> listOf(CPMetricType.FIBER, CPMetricType.CARB)
        "Fats and Oils" -> listOf(CPMetricType.FAT, CPMetricType.ENERGY)
        "Nuts and Seeds" -> listOf(CPMetricType.FAT, CPMetricType.PROTEIN)
        else -> listOf(CPMetricType.ENERGY, CPMetricType.PROTEIN)
    }

    // 使用 BigDecimal 的格式化字串 (stripTrailingZeros)
    val qtyLabel = qty.stripTrailingZeros().toPlainString()

    return InventoryCostInfo(
        totalCost = totalCostRounded,
        costPerUnit = costPerUnit,
        costPer100g = costPer100g,
        currency = this.currency,
        quantityLabel = qtyLabel,
        unit = this.unit,
        cpValues = cpMap,
        highlightMetrics = highlights,
    )
}

// 判斷邏輯也需更新
fun RecipeEntry.hasCostInfo(): Boolean {
    // 判斷 cost > 0
    if ((this.cost ?: BigDecimal.ZERO) > BigDecimal.ZERO) return true
    // 判斷 ingredient cost
    if (this.ingredients.any { (it.cost ?: BigDecimal.ZERO) > BigDecimal.ZERO }) return true
    return false
}

fun InventoryItem.hasCostInfo(): Boolean = (this.cost ?: BigDecimal.ZERO) > BigDecimal.ZERO

/**
 * 智能導航：支援 Place ID、直接貼上的 URL 連結、或純文字地址搜尋
 */
fun navigateToShop(context: Context, shopName: String, address: String?, googlePlaceId: String?) {
    if (address.isNullOrBlank() && googlePlaceId.isNullOrBlank()) {
        Toast.makeText(context, context.getString(R.string.shop_no_address), Toast.LENGTH_SHORT).show()
        return
    }

    val finalUri: Uri = when {
        // 1. 最優先：如果有 Place ID (通常來自 Places API 自動填入)，這是最精準的
        !googlePlaceId.isNullOrBlank() -> {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(shopName)}&query_place_id=$googlePlaceId")
        }

        // 2. 判斷 Address 是否為連結 (使用者直接貼上了 Maps 連結)
        !address.isNullOrBlank() && (
                address.startsWith("http://", ignoreCase = true) ||
                        address.startsWith("https://", ignoreCase = true) ||
                        address.startsWith("geo:", ignoreCase = true)
                ) -> {
            // 直接解析使用者貼上的 URL
            Uri.parse(address)
        }

        // 3. 兜底：視為普通文字地址，構建搜尋 URL
        else -> {
            val query = if (!address.isNullOrBlank()) address else shopName
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
        }
    }

    val intent = Intent(Intent.ACTION_VIEW, finalUri)

    // 嘗試指定 Google Maps App
    intent.setPackage("com.google.android.apps.maps")

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 沒裝 App 則清除 package，讓系統彈出瀏覽器選項
        intent.setPackage(null)
        try {
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, context.getString(R.string.no_map_app_found), Toast.LENGTH_SHORT).show()
        }
    }
}
//============AI prompt Generator==============
@Serializable
data class AiRecipeIngredient(
    val storageId: String?,   // 如果是從庫存來的，就放 InventoryItem.id；額外食材就 null
    val name: String,
    val grams: Double
)

@Serializable
data class AiRecipeResponse(
    val title: String,
    val description: String? = null,
    val servings: Int = 1,
    val steps: List<String>,
    val ingredients: List<AiRecipeIngredient>
)
private val aiJson = Json {
    ignoreUnknownKeys = true   // AI 多回欄位也沒關係
    prettyPrint = false
}

/** 把 AI 回的 JSON 字串轉成 AiRecipeResponse */
fun parseAiRecipeResponse(json: String): Result<AiRecipeResponse, Throwable> {
    return try {
        val obj = aiJson.decodeFromString(AiRecipeResponse.serializer(), json)
        Result.Ok(obj)
    } catch (e: Throwable) {
        Result.Err(e)
    }
}
private fun InventoryItem.formatForAiPrompt(): String {

    val qtyDouble = this.quantity.toDouble()
    val gramsDouble = this.gramsPerUnit ?: 0.0
    val approxGrams = gramsDouble * qtyDouble

    // 標準化營養 per 100g（已經是英文 key）
    val nut = this.standardNutrientsPer100g

    return if (nut == null || nut.isEmpty()) {
        // 沒營養資料的情況
        "- id: ${this.id}, " +
                "name: ${this.name}, " +
                "quantity: ${this.quantity ?: 0.0} ${this.unit.orEmpty()}, " +
                "approx_grams: ${"%.0f".format(approxGrams)}, " +
                "nutrients_per100g: null"
    } else {
        val energy = nut["Energy"] ?: 0.0               // kcal / 100g
        val protein = nut["Protein"] ?: 0.0             // g / 100g
        val carbs = nut["Available carbohydrate"] ?: 0.0// g / 100g
        val fat = nut["Total lipid (fat)"] ?: 0.0       // g / 100g
        val sodium = nut["Sodium, Na"] ?: 0.0           // mg / 100g
        val chol = nut["Cholesterol"] ?: 0.0            // mg / 100g

        "- id: ${this.id}, " +
                "name: ${this.name}, " +
                "quantity: ${this.quantity ?: 0.0} ${this.unit.orEmpty()}, " +
                "approx_grams: ${"%.0f".format(approxGrams)}, " +
                "nutrients_per100g: {" +
                "Energy_kcal: ${"%.1f".format(energy)}, " +
                "Protein_g: ${"%.1f".format(protein)}, " +
                "Carbs_g: ${"%.1f".format(carbs)}, " +
                "Fat_g: ${"%.1f".format(fat)}, " +
                "Sodium_mg: ${"%.0f".format(sodium)}, " +
                "Cholesterol_mg: ${"%.0f".format(chol)}" +
                "}"
    }
}
/**
 * 粗估 OpenAI 類模型的 token 數：
 * - ASCII 約 4 字元 ≈ 1 token
 * - 非 ASCII（中日文等）約 2 字元 ≈ 1 token
 * 只用來估算費用/上限，不保證精準。
 */
fun estimateTokensApprox(text: String): Int {
    if (text.isEmpty()) return 0
    var asciiCount = 0
    var otherCount = 0
    text.forEach { ch ->
        if (ch.code < 128) asciiCount++ else otherCount++
    }
    val est = asciiCount / 4.0 + otherCount / 2.0
    return est.roundToInt().coerceAtLeast(1)
}

data class CalculatedTotalPrice(
    val originalTotal: BigDecimal,
    val finalTotal: BigDecimal,
    val label: String? = null
)

private val BD_100 = BigDecimal("100")

fun calculateDiscountedTotal(
    basePrice: BigDecimal,
    qty: BigDecimal,
    rule: EventRule?
): CalculatedTotalPrice {
    val q = qty.max(BigDecimal.ZERO)
    val original = basePrice.multiply(q) // 原價總額

    if (q <= BigDecimal.ZERO || rule == null || rule is EventRule.None) {
        return CalculatedTotalPrice(original, original, null)
    }

    return when (rule) {
        is EventRule.PercentOffEach -> {
            // 公式：原價 * (100 - percent) / 100
            val multiplier = BD_100.subtract(rule.percent).divide(BD_100)
            val final = original.multiply(multiplier)
            CalculatedTotalPrice(original, final, "-${rule.percent.stripTrailingZeros().toPlainString()}%")
        }

        is EventRule.AmountOffEach -> {
            // 公式：(原價 - (每件減額 * 數量)).max(0)
            val totalDeduction = rule.amount.multiply(q)
            val final = original.subtract(totalDeduction).max(BigDecimal.ZERO)
            CalculatedTotalPrice(original, final, "-${rule.amount.stripTrailingZeros().toPlainString()}")
        }

        is EventRule.BuyXGetY -> {
            val x = rule.buyQty.max(BigDecimal.ZERO)
            val y = rule.getQty.max(BigDecimal.ZERO)
            val group = x.add(y)

            if (group <= BigDecimal.ZERO) return CalculatedTotalPrice(original, original, null)

            // 完整組數 = q / (x+y) 取整數
            val fullGroups = q.divideToIntegralValue(group)
            // 剩餘件數 = q % (x+y)
            val rem = q.remainder(group)

            // 贈品計價比例 (0~1)
            val giftRate = rule.priceOfGetPercent.divide(BD_100).max(BigDecimal.ZERO).min(BigDecimal.ONE)

            // 每組計價件數 = x + y * giftRate
            val payableInGroup = x.add(y.multiply(giftRate))

            // 剩餘部分：先算買的(x)，再算送的(y)
            val remBuy = rem.min(x)
            val remGift = rem.subtract(remBuy).max(BigDecimal.ZERO)
            val payableRem = remBuy.add(remGift.multiply(giftRate))

            // 總計價件數 = 完整組 * 每組計價 + 剩餘計價
            val totalPayableQty = fullGroups.multiply(payableInGroup).add(payableRem)
            val final = basePrice.multiply(totalPayableQty)

            CalculatedTotalPrice(original, final, "買${x.toInt()}送${y.toInt()}")
        }

        is EventRule.SecondPercent -> {
            // 每兩件一組
            val two = BigDecimal("2")
            val groups = q.divideToIntegralValue(two)
            val rem = q.remainder(two)

            // 第二件折扣率 (例如 50% off -> 0.5)
            val offRate = rule.percent.divide(BD_100)
            // 第二件實付比例 (1 - 0.5 = 0.5)
            val secondPayRate = BigDecimal.ONE.subtract(offRate)

            // 每組實付 = 1(第一件) + secondPayRate(第二件)
            val groupRate = BigDecimal.ONE.add(secondPayRate)

            // 總額 = (組數 * 組費率 + 剩餘件數) * 單價
            val final = basePrice.multiply(groups.multiply(groupRate).add(rem))

            CalculatedTotalPrice(original, final, "第2件${rule.percent.toInt()}%") // 這裡可保留 Int 顯示較簡潔
        }

        is EventRule.SecondMinus -> {
            val two = BigDecimal("2")
            val groups = q.divideToIntegralValue(two)
            val rem = q.remainder(two)

            // 第二件實付價格 = (單價 - 減免額).max(0)
            val secondPrice = basePrice.subtract(rule.amount).max(BigDecimal.ZERO)

            // 總額 = 組數 * (單價 + 第二件價) + 剩餘 * 單價
            val groupPrice = basePrice.add(secondPrice)
            val final = groups.multiply(groupPrice).add(rem.multiply(basePrice))

            CalculatedTotalPrice(original, final, "第2件-${rule.amount.stripTrailingZeros().toPlainString()}")
        }

        is EventRule.BundleFixedPrice -> {
            val n = rule.bundleQty.max(BigDecimal.ZERO)
            if (n <= BigDecimal.ZERO) return CalculatedTotalPrice(original, original, null)

            val groups = q.divideToIntegralValue(n)
            val rem = q.remainder(n)

            val final = groups.multiply(rule.bundlePrice).add(rem.multiply(basePrice))
            CalculatedTotalPrice(original, final, "${n.toInt()}件${rule.bundlePrice.toInt()}")
        }

        // 複雜規則暫時回傳原價 (待結算)
        is EventRule.TierByQty,
        is EventRule.TierBySpend,
        is EventRule.Conditional,
        is EventRule.FreeShipping -> {
            CalculatedTotalPrice(original, original, "待結算")
        }

        EventRule.None -> CalculatedTotalPrice(original, original, null)
    }
}
@Composable
fun OsmLocationPicker(
    onLocationPicked: (String) -> Unit, // 回傳地址字串
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 初始化 OSM 設定 (重要：避免緩存問題)
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentAddress by remember { mutableStateOf("移動地圖以選取位置...") }
    var isGeocoding by remember { mutableStateOf(false) }

    val fetchCurrentLocation = {
        try {
            // 使用 CancellationTokenSource 產生一個空的 token 傳入，滿足非空限制
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    mapView?.controller?.animateTo(geoPoint)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // 權限請求 Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            fetchCurrentLocation()
        }
    }

    // 畫面載入時檢查權限
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchCurrentLocation()
        } else {
            // 尚未授權，跳出系統權限詢問框
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OSM 地圖視圖
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0) // 稍微放大一點，街區級別
                    // 預設先放台灣中心或台北車站，等定位到了會自動 animateTo 過去
                    controller.setCenter(GeoPoint(25.0478, 121.5170))
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. 中央的「大頭針」圖示
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 24.dp)
        )

        // --- 🔥 新增：右下角「回到目前位置」的浮動按鈕 ---
        FloatingActionButton(
            onClick = {
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) fetchCurrentLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(bottom = 140.dp, end = 16.dp), // 避開底部的 Card
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
        }

        // 3. 底部操作區
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 48.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顯示當前解析的地址 (或提示)
                Text(
                    text = currentAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            // 點擊確認時，抓取當前地圖中心並解析地址
                            mapView?.let { map ->
                                val center = map.mapCenter as GeoPoint
                                scope.launch {
                                    isGeocoding = true
                                    currentAddress = "解析中..."
                                    val address = getAddressFromLocation(context, center.latitude, center.longitude)
                                    isGeocoding = false
                                    onLocationPicked(address) // 回傳結果並關閉
                                }
                            }
                        },
                        enabled = !isGeocoding
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("確認此位置")
                    }
                }
            }
        }
    }
}

// 輔助函式：Android 原生 Geocoder (免費，不需 API Key)
suspend fun getAddressFromLocation(context: android.content.Context, lat: Double, lon: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // 取得 1 筆結果
            @Suppress("DEPRECATION") // 新版 Android 有新 API，但舊版兼容性較好先用這個
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // 組合地址字串 (例如：台南市新營區...)
                // getAddressLine(0) 通常包含完整地址
                address.getAddressLine(0) ?: "未知地址"
            } else {
                "無法解析此位置"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "網路錯誤或無法解析"
        }
    }
}
