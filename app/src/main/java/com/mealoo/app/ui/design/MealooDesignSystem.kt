package com.mealoo.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealoo.app.R

// ==========================================
// 0. Tech Design Tokens (科技風設計語彙)
// ==========================================
object MealooTechColors {
    val DeepBackground = Color(0xFF030508)
    val GlassCard = Color(0xFF0D1117).copy(alpha = 0.85f)
    val AccentCyan = Color(0xFF00D2FF)
    val AccentMagenta = Color(0xFFFF2D55)
    val TechBorder = Color(0xFF30363D)
    val CardGlow = Color(0xFF00D2FF).copy(alpha = 0.1f)
    val NeonP = Color(0xFF00FF88)
    val NeonC = Color(0xFF00D2FF)
    val NeonF = Color(0xFFFF9F0A)
}

@Composable
fun Modifier.mealooTechGlassBorder(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    borderColor: Color = MealooTechColors.TechBorder
) = this
    .clip(shape)
    .border(1.dp, borderColor, shape)
// ==========================================
// 1. Design Tokens (設計變數)
// ==========================================
object MealooDimens {
    val RadiusSmall = 8.dp
    val RadiusMedium = 12.dp  // 標準圓角
    val RadiusLarge = 16.dp

    val FieldHeight = 48.dp   // 標準輸入框高度 (符合觸控標準)
    val ButtonHeight = 44.dp  // 標準按鈕高度

    val PaddingSmall = 8.dp
    val PaddingMedium = 12.dp

    const val DisabledAlpha = 0.38f
    const val ContentAlpha = 0.7f
}
data class MealooExtendedColors(
    val protein: Color,
    val carbs: Color,
    val fats: Color,
    val water: Color,

    val breakfast: Color,
    val lunch: Color,
    val dinner: Color,
    val snack: Color,

    val cardBgEnergy: Color,
    val cardBgWater: Color,
    val cardBgMeal: Color
)

// 2. 淺色模式色板 (你原本的顏色)
val LightMealooColors = MealooExtendedColors(
    protein = Color(0xFF81C784),
    carbs = Color(0xFF64B5F6),
    fats = Color(0xFFFFB74D),
    water = Color(0xFF26C6DA),
    breakfast = Color(0xFFFFB74D),
    lunch = Color(0xFF66BB6A),
    dinner = Color(0xFF5C6BC0),
    snack = Color(0xFFAB47BC),
    cardBgEnergy = Color(0xFFFFF8E1),
    cardBgWater = Color(0xFFE0F7FA),
    cardBgMeal = Color(0xFFF5F5F5)
)

// 3. 深色模式色板 (🌟 關鍵：提高明度、調整背景色避免髒色)
val DarkMealooColors = MealooExtendedColors(
    protein = Color(0xFFA5D6A7),    // 更亮的綠
    carbs = Color(0xFF90CAF9),      // 更亮的藍
    fats = Color(0xFFFFCC80),       // 更亮的橘
    water = Color(0xFF4DD0E1),      // 更亮的青
    breakfast = Color(0xFFFFCC80),
    lunch = Color(0xFFA5D6A7),
    dinner = Color(0xFF7986CB),
    snack = Color(0xFFCE93D8),
    // 🌟 在深色模式下，大面積卡片背景不要用原本的淺黃/淺藍，改用帶有極低透明度的主題色或深灰
    cardBgEnergy = Color(0xFF2B281D), // 帶有一點點暖黃的深灰色
    cardBgWater = Color(0xFF1D282A),  // 帶有一點點青藍的深灰色
    cardBgMeal = Color(0xFF202124)    // 純深灰
)

// 4. 建立 CompositionLocal
val LocalMealooColors = staticCompositionLocalOf { LightMealooColors }

// 5. 提供一個類似 MaterialTheme 的進入點
object MealooTheme {
    val colors: MealooExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMealooColors.current
}
/**
 * 統一的容器樣式：處理圓角、背景色、邊框動畫
 * 適用於：TextField, Button, SelectTrigger
 */
@Composable
private fun MealooBaseContainer(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(MealooDimens.RadiusMedium),
    backgroundColor: Color,
    borderColor: Color,
    contentAlignment: Alignment = Alignment.CenterStart,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val borderAnim by animateColorAsState(targetValue = borderColor, label = "border")

    Box(
        modifier = modifier
            .heightIn(min = MealooDimens.ButtonHeight)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderAnim), shape)
            .let { baseMod ->
                if (onClick != null && enabled) {
                    baseMod.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    baseMod
                }
            }
            .padding(horizontal = MealooDimens.PaddingMedium),
        contentAlignment = contentAlignment,
        content = content
    )
}

// ==========================================
// 3. Input Components (輸入組件)
// ==========================================

@Composable
fun MealooTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textAlign: TextAlign = TextAlign.Start,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onClick: (() -> Unit)? = null // 若傳入，則視為點擊觸發(如日期選擇)
) {
    var focused by remember { mutableStateOf(false) }

    // 狀態顏色邏輯
    val baseFill = MaterialTheme.colorScheme.surfaceVariant
    val focusedBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val normalBorder = Color.Transparent

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }

        MealooBaseContainer(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(MealooDimens.FieldHeight) else Modifier.heightIn(min = MealooDimens.FieldHeight)),
            backgroundColor = baseFill,
            borderColor = if (focused) focusedBorder else normalBorder,
            onClick = if (!enabled || readOnly) onClick else null, // 唯讀時允許點擊容器
            enabled = enabled
        ) {
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                if (leadingIcon != null) {
                    Box(Modifier.size(20.dp).padding(top = if (singleLine) 0.dp else 12.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                    Spacer(Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = if (singleLine) 0.dp else 12.dp)) {
                    if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (readOnly && onClick != null) {
                        Text(
                            text = value,
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = if(singleLine) 1 else Int.MAX_VALUE
                        )
                    } else {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused },
                            singleLine = singleLine,
                            enabled = enabled,
                            readOnly = readOnly,
                            textStyle = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = keyboardOptions,
                            keyboardActions = keyboardActions,
                            visualTransformation = visualTransformation
                        )
                    }
                }

                if (trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(20.dp).padding(top = if (singleLine) 0.dp else 12.dp), contentAlignment = Alignment.Center) { trailingIcon() }
                }
            }
        }
    }
}

/**
 * 支援 TextFieldValue 的 MealooTextField 版本，適用於需要精確控制游標或語法高亮的場景
 */
@Composable
fun MealooTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textAlign: TextAlign = TextAlign.Start,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onClick: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }

    val baseFill = MaterialTheme.colorScheme.surfaceVariant
    val focusedBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val normalBorder = Color.Transparent

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }

        MealooBaseContainer(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(MealooDimens.FieldHeight) else Modifier.heightIn(min = MealooDimens.FieldHeight)),
            backgroundColor = baseFill,
            borderColor = if (focused) focusedBorder else normalBorder,
            onClick = if (!enabled || readOnly) onClick else null,
            enabled = enabled
        ) {
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                if (leadingIcon != null) {
                    Box(Modifier.size(20.dp).padding(top = if (singleLine) 0.dp else 12.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                    Spacer(Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = if (singleLine) 0.dp else 12.dp)) {
                    if (value.text.isEmpty() && !placeholder.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                        singleLine = singleLine,
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation
                    )
                }

                if (trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(20.dp).padding(top = if (singleLine) 0.dp else 12.dp), contentAlignment = Alignment.Center) { trailingIcon() }
                }
            }
        }
    }
}

// ==========================================
// 4. Button Components (按鈕組件)
// ==========================================

enum class MealooButtonStyle {
    Primary,   // 實心主色
    Secondary, // 淺色背景/邊框
    Ghost      // 透明背景
}

@Composable
fun MealooButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MealooButtonStyle = MealooButtonStyle.Secondary,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val colors = when (style) {
        MealooButtonStyle.Primary -> Pair(
            MaterialTheme.colorScheme.primary, // Bg
            MaterialTheme.colorScheme.onPrimary // Text
        )
        MealooButtonStyle.Secondary -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface
        )
        MealooButtonStyle.Ghost -> Pair(
            Color.Transparent,
            MaterialTheme.colorScheme.primary
        )
    }

    val borderColor = if (style == MealooButtonStyle.Secondary && enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else Color.Transparent

    val finalBg = if (enabled) colors.first else colors.first.copy(alpha = 0.12f)
    val finalContentColor = if (enabled) colors.second else MaterialTheme.colorScheme.onSurface.copy(alpha = MealooDimens.DisabledAlpha)

    MealooBaseContainer(
        modifier = modifier.height(MealooDimens.ButtonHeight),
        backgroundColor = finalBg,
        borderColor = borderColor,
        onClick = onClick,
        enabled = enabled && !isLoading,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = finalContentColor
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = finalContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

// ==========================================
// 5. Select/Dropdown Trigger (選擇觸發器)
// ==========================================

@Composable
fun MealooSelectField(
    value: String?,
    label: String? = null,
    placeholder: String = "請選擇",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    expanded: Boolean = false // 控制箭頭旋轉
) {
    // 復用 MealooTextField 的外觀，但強制 ReadOnly 並處理箭頭動畫
    MealooTextField(
        value = value ?: "",
        onValueChange = {},
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        readOnly = true,
        enabled = true,
        onClick = onClick,
        leadingIcon = leadingIcon,
        trailingIcon = {
            val rotation by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (expanded) 180f else 0f, label = "arrow"
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Expand",
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    )
}

// ==========================================
// 6. List/Card Helper (列表項目)
// ==========================================

@Composable
fun MealooListTile(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MealooDimens.RadiusMedium))
            .background(containerColor)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(all = MealooDimens.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supporting != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealooCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
                disabledContainerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
                disabledContainerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
            content = content
        )
    }
}

@Composable
fun ProFeatureDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.pro_feature_title))
        },
        text = {
            Text(stringResource(R.string.pro_feature_message))
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text(stringResource(R.string.pro_feature_upgrade))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fieldHeight: Dp = 48.dp, // 稍微增高一点，符合触控标准
    editable: Boolean = true,
    rounded: Dp = 12.dp,
    showBorder: Boolean = true,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    textAlign: TextAlign = TextAlign.Start,
    onFocusChange: (Boolean) -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val clickHandler: (() -> Unit)? = onClick
    val fill = containerColor ?: MaterialTheme.colorScheme.surfaceVariant

    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else Color.Transparent
    )

    Column(modifier = modifier) {
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp) // 稍微对齐
            )
        }

        val shape = RoundedCornerShape(rounded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .clip(shape)
                .background(fill)
                .then(if (showBorder) Modifier.border(BorderStroke(1.dp, borderColor), shape) else Modifier)
                .let {
                    if (!editable && clickHandler != null) {
                        it.clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            clickHandler?.invoke()
                        }
                    } else it
                }
                .padding(horizontal = 12.dp), // 移除垂直 padding，靠 Alignment.CenterVertically
            contentAlignment = Alignment.CenterStart
        ){
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                if (leadingIcon != null) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { leadingIcon() }
                    Spacer(Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp, // 字体微调
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = textAlign
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                focused = it.isFocused
                                onFocusChange(it.isFocused)
                            },
                        readOnly = !editable,
                        enabled = editable
                    )
                }

                if (trailingIcon != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { trailingIcon() }
                }
            }
        }
    }
}
@Composable
fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = stringResource(id = R.string.note_placeholder), // or provide a default like "Your message..."
    minHeight: Dp = 120.dp,
    maxHeight: Dp = 240.dp,
    rounded: Dp = 12.dp,
    maxChars: Int? = 400,
    showCounter: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
) {
    var focused by remember { mutableStateOf(false) }

    // colors
    val emptyFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    val baseFill = MaterialTheme.colorScheme.surfaceVariant
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
        }

        val shape = RoundedCornerShape(rounded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight)
                .clip(shape)
                .background(if (value.isEmpty()) emptyFill else baseFill)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // content row: leading | editor | trailing
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp).size(24.dp), contentAlignment = Alignment.TopCenter) {
                        leading()
                    }
                }

                // Scrollable text area
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .onFocusChanged { focused = it.isFocused }
                ) {
                    if (value.isEmpty()) {
                        // placeholder covers the editable area when empty
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            val newVal = if (maxChars != null) it.take(maxChars) else it
                            onValueChange(newVal)
                        },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.TopCenter) {
                        trailing()
                    }
                }
            }

            // char counter (右下角)
            if (showCounter && maxChars != null) {
                Text(
                    text = "${value.length}/${maxChars}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 6.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDropdownFieldMenu(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    options: List<String>,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    onCustomClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    fieldHeight: Dp = 44.dp,
    rounded: Dp = 12.dp,
    maxMenuHeight: Dp = 320.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    var anchorWidth by remember { mutableStateOf(0) }
    val widthDp = with(LocalDensity.current) { anchorWidth.toDp() }

    val displayText = value?.takeIf { it.isNotBlank() } ?: (placeholder ?: "")

    Column(modifier) {
        // label（可空就不画）
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
        }

        // 头：你的 CompactTextField，只读+点击打开菜单
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorWidth = it.size.width } // 记录宽度
        ) {
            CompactTextField(
                value = displayText,
                onValueChange = {},               // read-only
                label = null,
                placeholder = null,              // 上面已经有 label/placeholder
                leadingIcon = leadingIcon,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                },
                singleLine = true,
                editable = false,
                fieldHeight = fieldHeight,
                containerColor = containerColor,
                rounded = rounded,
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(widthDp)
                    .heightIn(max = maxMenuHeight)
                    .border(
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        RoundedCornerShape(bottomStart = rounded, bottomEnd = rounded, topStart = 0.dp, topEnd = 0.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(bottomStart = rounded, bottomEnd = rounded, topStart = 0.dp, topEnd = 0.dp)
                    )
            ) {
                options.forEach { opt ->
                    CompactDropdownMenuItem(
                        text = opt,
                        selected = (opt == value),
                        onClick = {
                            onValueChange(opt)
                            expanded = false
                        }
                    )
                }

                if (onCustomClick != null) {
                    HorizontalDivider()
                    CompactDropdownMenuItem(
                        text = "自订…",
                        selected = false,
                        leading = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            expanded = false
                            onCustomClick()
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun CompactDropdownMenuItem(
    text: String,
    selected: Boolean,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val containerShape = RoundedCornerShape(8.dp)

    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when {
                    trailing != null -> {
                        Spacer(Modifier.width(8.dp)); trailing()
                    }
                }
            }
        },
        onClick = onClick,
        leadingIcon = null,
        trailingIcon = null
    )
}