@file:OptIn(ExperimentalMaterial3Api::class)

package com.mealoo.app.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.api.services.drive.Drive
import com.mealoo.app.data.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.mealoo.app.R
import com.mealoo.app.model.MealDef
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.util.Locale
import com.mealoo.app.util.recomputeMealRatios
import com.mealoo.app.util.normalizeToTarget
import com.mealoo.app.utils.dailyMacroTargets
import kotlinx.coroutines.withContext
import com.mealoo.app.ui.design.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.mealoo.app.model.AppCurrency
import com.mealoo.app.data.IntakeDataStore
import com.mealoo.app.data.IntakeRepository
import com.mealoo.app.model.MealKind
import com.mealoo.app.data.repository.SubscriptionRepository // 請確認你的 package 路徑
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode

const val RESULT_SETTINGS_UPDATED = "settings_updated"

// --- 🎨 Tech Design Tokens & Styles ---
private object SettingTechColors {
    val DeepBackground @Composable get() = if (isSystemInDarkTheme()) Color(0xFF030508) else Color(0xFFF9F9F9)
    val GlassCard @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0D1117).copy(alpha = 0.85f) else Color(0xFFFFFFFF)
    val TechBorder @Composable get() = if (isSystemInDarkTheme()) Color(0xFF30363D) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val AccentCyan = Color(0xFF00D2FF)
    val AccentMagenta = Color(0xFFFF2D55)
    val GlowColor @Composable get() = if (isSystemInDarkTheme()) Color(0xFF00D2FF).copy(alpha = 0.08f) else Color.Transparent
}

@Composable
private fun Modifier.settingTechCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    borderColor: Color = SettingTechColors.TechBorder
) = this
    .clip(shape)
    .background(SettingTechColors.GlassCard)
    .border(1.dp, borderColor, shape)

@Composable
private fun Modifier.settingTechGlow(
    color: Color = SettingTechColors.AccentCyan,
    alpha: Float = 0.1f
) = if (isSystemInDarkTheme()) {
    this.background(
        androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            radius = 1200f
        )
    )
} else this

private object SettingColors {
    val ProfileBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0D47A1).copy(alpha = 0.2f) else Color(0xFFE3F2FD)
    val ProfileIcon = Color(0xFF1565C0)

    val TargetBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
    val TargetIcon = Color(0xFF2E7D32)

    val DietBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFF3E0)
    val DietIcon = Color(0xFFEF6C00)

    val TrainingBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
    val TrainingIcon = Color(0xFFC62828)

    val RoutineBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4A148C).copy(alpha = 0.2f) else Color(0xFFF3E5F5)
    val RoutineIcon = Color(0xFF6A1B9A)

    val MacroBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF006064).copy(alpha = 0.2f) else Color(0xFFE0F7FA)
    val MacroIcon = Color(0xFF00838F)

    val InputBg @Composable get() = if (isSystemInDarkTheme()) Color(0xFF161B22) else Color(0xFFFAFAFA)
    val InputBorder @Composable get() = if (isSystemInDarkTheme()) Color(0xFF30363D) else Color(0xFFEEEEEE)
}
// 定義頁面層級
enum class SettingsDestination {
    MENU,           // 主選單
    PERSONAL_DATA,
    ACCOUNT_AND_SYNC,
    CURRENCY_SETTINGS,
}

@Composable
fun SettingPage(
    userPref: UserPreference?,
    prefStore: UserPreferenceDataStore,
    navController: NavController,
    onBack: () -> Unit,
    drive: Drive?,
    intakeStore: IntakeDataStore,
    subscriptionRepository: SubscriptionRepository,
    onLoginRequest: () -> Unit,
    onLogoutRequest: () -> Unit,
    startDestination: SettingsDestination = SettingsDestination.MENU
) {
    if (userPref == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var currentDestination by remember(startDestination) {
        mutableStateOf(startDestination)
    }

    BackHandler(enabled = currentDestination != SettingsDestination.MENU) {
        currentDestination = SettingsDestination.MENU
    }
    if (currentDestination == SettingsDestination.MENU) {
        BackHandler(enabled = true, onBack = onBack)
    }

    AnimatedContent(
        targetState = currentDestination,
        label = "SettingsTransition",
        transitionSpec = {
            if (targetState == SettingsDestination.MENU) {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }
        }
    ) { destination ->
        when (destination) {
            SettingsDestination.MENU -> {
                SettingsMenuScreen(
                    onBack = onBack,
                    onNavigateToPersonal = { currentDestination = SettingsDestination.PERSONAL_DATA },
                    onNavigateToAccount = { currentDestination = SettingsDestination.ACCOUNT_AND_SYNC },
                    onNavigateToCurrency = { currentDestination = SettingsDestination.CURRENCY_SETTINGS }
                )
            }
            SettingsDestination.PERSONAL_DATA -> {
                // 原本龐大的邏輯封裝在此
                PersonalDataEditor(
                    userPref = userPref,
                    prefStore = prefStore,
                    navController = navController,
                    drive = drive, // Pass drive
                    onBack = { currentDestination = SettingsDestination.MENU } // 返回到選單
                )
            }
            SettingsDestination.ACCOUNT_AND_SYNC -> {
                AccountAndSyncScreen(
                    drive = drive,
                    intakeStore = intakeStore,
                    subscriptionRepository = subscriptionRepository,
                    onLoginRequest = onLoginRequest,
                    onLogoutRequest = onLogoutRequest,
                    onBack = { currentDestination = SettingsDestination.MENU }
                )
            }
            SettingsDestination.CURRENCY_SETTINGS -> {
                CurrencySettingsManager(
                    userPref = userPref,
                    onBack = { currentDestination = SettingsDestination.MENU }
                )
            }
        }
    }
}

// --- 1. 新增：設定主選單 (分層入口) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    onBack: () -> Unit,
    onNavigateToPersonal: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToCurrency: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.setting_title), 
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = SettingTechColors.DeepBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .settingTechGlow()
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                
                // 個人資料入口卡片
                TechSettingCard(
                    title = stringResource(R.string.profile),
                    subtitle = stringResource(R.string.profile_settings_subtitle),
                    icon = Icons.Outlined.Person,
                    accentColor = SettingTechColors.AccentCyan,
                    onClick = onNavigateToPersonal
                )

                TechSettingCard(
                    title = stringResource(R.string.account_sync_title),
                    subtitle = stringResource(R.string.account_sync_subtitle),
                    icon = Icons.Outlined.ManageAccounts,
                    accentColor = Color(0xFFA5D6A7),
                    onClick = onNavigateToAccount
                )

                TechSettingCard(
                    title = stringResource(R.string.currency_settings_title),
                    subtitle = stringResource(R.string.currency_settings_subtitle),
                    icon = Icons.Outlined.Paid,
                    accentColor = Color(0xFFFFCC80),
                    onClick = onNavigateToCurrency
                )
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAndSyncScreen(
    drive: Drive?,
    intakeStore: IntakeDataStore,
    subscriptionRepository: SubscriptionRepository,
    onLoginRequest: () -> Unit,
    onLogoutRequest: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Google Drive / Cloud State ---
    val isConnected = drive != null
    // 只有有 Drive 時才建立 Repository
    val backupRepo = remember(drive, context) { drive?.let { BackupRepository(context, it) } }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedDate by remember { mutableStateOf(today) }

    // --- Subscription State ---
    // 取得 Activity 用於發起購買
    val activity = context as? android.app.Activity
    val isPro by subscriptionRepository.isPro.collectAsState()
    val offerings by subscriptionRepository.offerings.collectAsState()
    var subMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.account_sync_title), 
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = SettingTechColors.DeepBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ==========================================
            // 1. Google 帳號管理區 (含登入/登出)
            // ==========================================
            item {
                SectionHeader(stringResource(R.string.google_account_title), Icons.Default.AccountCircle)
                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().settingTechCard()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isConnected) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone, 
                                contentDescription = null, 
                                tint = Color(0xFFA5D6A7), 
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.cloud_connected_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.cloud_connected_desc), 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))

                            // 登出按鈕
                            MealooButton(
                                text = stringResource(R.string.cloud_logout_action),
                                onClick = onLogoutRequest,
                                style = MealooButtonStyle.Ghost,
                                leadingIcon = { Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudOff, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.cloud_not_connected_short), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.cloud_disconnected_desc), 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))

                            // 登入按鈕
                            MealooButton(
                                text = stringResource(R.string.cloud_login_action),
                                onClick = onLoginRequest,
                                style = MealooButtonStyle.Primary,
                                leadingIcon = { Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. Pro 會員訂閱區
            // ==========================================
            item {
                SectionHeader(stringResource(R.string.pro_membership_title), Icons.Default.CardMembership)
                Spacer(Modifier.height(8.dp))

                // 狀態卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingTechCard()
                        .let { 
                            if (isPro) it.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            else it
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPro) Icons.Default.Verified else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isPro) Color(0xFFA5D6A7) else SettingTechColors.AccentCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(
                                text = if (isPro) stringResource(R.string.pro_member_active) else stringResource(R.string.free_version),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPro) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.onSurface
                            )
                            if (!isPro) {
                                Text(stringResource(R.string.upgrade_unlock_features), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 購買選項 (如果不是 Pro)
                if (!isPro && offerings.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    offerings.forEach { pkg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .settingTechCard()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pkg.product.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(pkg.product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            subMessage = "處理中..."
                                            subscriptionRepository.purchase(activity, pkg) { errorMsg ->
                                                subMessage = errorMsg ?: "購買成功！"
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SettingTechColors.AccentCyan)
                                ) {
                                    Text(pkg.product.price.formatted, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 恢復購買與訊息
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        subMessage = "恢復中..."
                        subscriptionRepository.restorePurchases { success ->
                            subMessage = if (success) context.getString(R.string.restore_success) else context.getString(R.string.restore_no_record)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_purchases))
                }

                if (subMessage.isNotEmpty()) {
                    Text(
                        text = subMessage,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ==========================================
            // 3. 雲端備份操作 (僅在已連結時顯示)
            // ==========================================
                item {
                    SectionHeader(stringResource(R.string.cloud_backup_operations_title), Icons.Default.CloudSync)
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingTechCard()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var isBackingUp by remember { mutableStateOf(false) }
                            var isRestoring by remember { mutableStateOf(false) }
                            var dbMessage by remember { mutableStateOf<String?>(null) }
                            var cloudTime by remember { mutableStateOf("-") }
                            var cloudVer by remember { mutableStateOf("-") }
                            var localTime by remember { mutableStateOf("-") }
                            var showRestoreConfirm by remember { mutableStateOf(false) }
                            var showDebugViewer by remember { mutableStateOf(false) }

                            LaunchedEffect(isConnected, isBackingUp, isRestoring) {
                                if (isConnected) {
                                    val meta = backupRepo?.getLastBackupMetadata()
                                    if (meta != null) {
                                        cloudTime = if (meta.first == "0") "未知" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(meta.first.toLongOrNull() ?: 0))
                                        cloudVer = meta.second
                                        localTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(meta.third.toLongOrNull() ?: 0))
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "本地最後修改: $localTime", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "雲端備份時間: $cloudTime (版本 $cloudVer)", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Advanced backup toggle
                            var advancedBackup by remember { mutableStateOf(false) }
                            var selectedCategories by remember { mutableStateOf(com.mealoo.app.data.BackupCategory.entries.toSet()) }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Switch(
                                    checked = advancedBackup,
                                    onCheckedChange = { advancedBackup = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = SettingTechColors.AccentCyan)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("進階備份 (選擇部分資料)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            if (advancedBackup) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("勾選您想備份的項目：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    com.mealoo.app.data.BackupCategory.entries.forEach { category ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp)) {
                                            Checkbox(
                                                checked = selectedCategories.contains(category),
                                                onCheckedChange = { isChecked ->
                                                    selectedCategories = if (isChecked) {
                                                        selectedCategories + category
                                                    } else {
                                                        selectedCategories - category
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = SettingTechColors.AccentCyan)
                                            )
                                            Text(category.label, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            if (isBackingUp || isRestoring) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SettingTechColors.AccentCyan)
                            }

                            // 備份按鈕
                            Button(
                                onClick = {
                                    scope.launch {
                                        isBackingUp = true
                                        dbMessage = null
                                        try {
                                            if (advancedBackup) {
                                                if (selectedCategories.isEmpty()) {
                                                    dbMessage = "請至少選擇一項備份項目"
                                                } else {
                                                    dbMessage = backupRepo?.backupSelectedDatabase(selectedCategories)
                                                }
                                            } else {
                                                dbMessage = backupRepo?.backupDatabase()
                                            }
                                        } catch (e: Exception) {
                                            dbMessage = e.message
                                        } finally {
                                            isBackingUp = false
                                        }
                                    }
                                },
                                enabled = !isBackingUp && !isRestoring,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SettingTechColors.AccentCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
                                Spacer(Modifier.width(10.dp))
                                Text(if (advancedBackup) "執行部分備份" else "立即備份至 Google Drive", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            // 還原按鈕
                            Button(
                                onClick = { showRestoreConfirm = true },
                                enabled = !isBackingUp && !isRestoring,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("從雲端還原資料庫", fontWeight = FontWeight.Bold)
                            }

                            if (dbMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (dbMessage!!.contains("成功") || dbMessage!!.contains("最新")) 
                                            Color(0xFF1B5E20).copy(alpha = 0.1f) else Color(0xFFB71C1C).copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dbMessage!!,
                                        modifier = Modifier.padding(8.dp),
                                        color = if (dbMessage!!.contains("成功") || dbMessage!!.contains("最新")) 
                                            Color(0xFFA5D6A7) else Color(0xFFFFCDD2),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = SettingTechColors.TechBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = SettingTechColors.AccentCyan)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "備份已加密保護",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = { showDebugViewer = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("調試視窗", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (showDebugViewer && drive != null) {
                                DriveDebugViewer(drive = drive, onDismiss = { showDebugViewer = false })
                            }

                            if (showRestoreConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showRestoreConfirm = false },
                                    title = { Text("警告：即將覆蓋本地資料") },
                                    text = { Text("还原操作將清除所有當前資料並即刻重啟應用。建議在還原前先進行備份。確定要繼續嗎？") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showRestoreConfirm = false
                                                scope.launch {
                                                    isRestoring = true
                                                    dbMessage = null
                                                    try {
                                                        dbMessage = backupRepo?.restoreDatabase()
                                                        if (dbMessage?.contains("成功") == true) {
                                                            com.jakewharton.processphoenix.ProcessPhoenix.triggerRebirth(context)
                                                        }
                                                    } catch (e: Exception) {
                                                        dbMessage = e.message
                                                    } finally {
                                                        isRestoring = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) { Text("確認還原") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRestoreConfirm = false }) { Text("取消") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
}
@Composable
fun TechSettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingTechCard()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DriveDebugViewer(
    drive: Drive,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var logText by remember { mutableStateOf(context.getString(R.string.drive_scanning_files)) }
    var isLoading by remember { mutableStateOf(true) }

    // 執行非同步讀取
    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        withContext(Dispatchers.IO) {
            try {
                val sb = StringBuilder()
                sb.append(appContext.getString(R.string.drive_scan_start))

                // 1. 列出所有檔案
                val listRequest = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name, size, modifiedTime)")
                    .setQ("trashed = false")

                val files = listRequest.execute().files ?: emptyList()
                sb.append(appContext.getString(R.string.drive_files_found, files.size))

                // 2. 逐一讀取內容
                files.sortedByDescending { it.name }.forEachIndexed { index, file ->
                    sb.append(appContext.getString(R.string.drive_file_name, index + 1, file.name))
                    sb.append(appContext.getString(R.string.drive_file_id, file.id))
                    sb.append(appContext.getString(R.string.drive_file_size, file.getSize() ?: 0))
                    sb.append(appContext.getString(R.string.drive_file_modified_time, file.modifiedTime))

                    try {
                        val inputStream = drive.files().get(file.id).executeMediaAsInputStream()
                        val content = inputStream.bufferedReader().use { it.readText() }
                        // 為了避免 Log 太長，只顯示前 500 個字元，或是完整顯示視需求而定
                        // 這裡選擇完整顯示但做一點格式化
                        sb.append(appContext.getString(R.string.drive_content_preview, content))
                    } catch (e: Exception) {
                        sb.append(appContext.getString(R.string.drive_read_content_failed, e.message))
                    }
                    sb.append(appContext.getString(R.string.drive_log_separator))
                }

                sb.append(appContext.getString(R.string.drive_scan_complete))
                logText = sb.toString()
            } catch (e: Exception) {
                logText += appContext.getString(R.string.drive_severe_error, e.localizedMessage)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drive_raw_data_viewer_title)) },
        text = {
            Column {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }

                // 顯示日誌的捲動區域
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 佔用剩餘空間
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .verticalScroll(scrollState)
                ) {
                    // 使用等寬字體方便閱讀 JSON
                    Text(
                        text = logText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color.Black
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        modifier = Modifier.fillMaxHeight(0.85f) // 讓 Dialog 高一點
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsManager(
    userPref: UserPreference?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefStore = remember { UserPreferenceDataStore(context) }
    val scope = rememberCoroutineScope()
    val currentHomeCurrency = userPref?.homeCurrency ?: AppCurrency.USD
    var homeCurrencyState by remember(currentHomeCurrency) { mutableStateOf(currentHomeCurrency) }
    val exchangeRates = remember {
        mutableStateMapOf<String, String>()
    }

    var showCurrencyPicker by remember { mutableStateOf(false) }
    // --- 新增：同步狀態 ---
    var isSyncing by remember { mutableStateOf(false) }

    // 1. 宣告狀態
    var availableCurrencies by remember { mutableStateOf<List<ExchangeRateFileManager.CurrencyRateEntry>>(emptyList()) }

    LaunchedEffect(homeCurrencyState) {
        isSyncing = true
        var ratesList = ExchangeRateFileManager.loadLocalCache(context, homeCurrencyState.code)
        if (ratesList == null) {
            ratesList = ExchangeRateFileManager.fetchAndSave(context, homeCurrencyState.code)
        }

        if (ratesList != null) {
            availableCurrencies = ratesList // 存下來給 BottomSheet 的 Picker 用

            ratesList.forEach { entry ->
                // 更新匯率表
                exchangeRates[entry.code] = BigDecimal.valueOf(entry.inverseRate)
                    .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            }
        }
        isSyncing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.currency_settings_title), 
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = SettingTechColors.DeepBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Section 1: 主幣別 (Home Currency) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.primary_currency_title), Icons.Default.AccountBalanceWallet)
                Text(
                    stringResource(R.string.primary_currency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingTechCard()
                        .clickable { showCurrencyPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 直接顯示 Enum 的屬性
                            Text(homeCurrencyState.flagEmoji, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(homeCurrencyState.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = stringResource(id = homeCurrencyState.displayName), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 修改 Header：加入同步按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SectionHeader(stringResource(R.string.exchange_rates_title), Icons.Default.CurrencyExchange)
                        Text(
                            stringResource(R.string.exchange_rates_desc, homeCurrencyState.code),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // --- 手動同步按鈕 ---
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                val rates = ExchangeRateFileManager.fetchAndSave(context, homeCurrencyState.code)
                                rates?.forEach { entry ->
                                    exchangeRates[entry.code] = BigDecimal.valueOf(entry.inverseRate)
                                        .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                                }
                                isSyncing = false
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync rates", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                AppCurrency.entries.filter { it != homeCurrencyState && it != AppCurrency.OTHER }.forEach { target ->
                    ExchangeRateRow(
                        targetCurrency = target,
                        homeCurrency = homeCurrencyState,
                        rate = exchangeRates[target.code].orEmpty(),
                        onRateChange = { newRate ->
                            exchangeRates[target.code] = newRate
                        }
                    )
                }
            }
        }
    }

    // Currency Picker BottomSheet
    if (showCurrencyPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCurrencyPicker = false }
        ) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    stringResource(R.string.select_primary_currency_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                LazyColumn {
                    // 建議用 entries 取代 values()
                    items(AppCurrency.entries) { currency ->
                        ListItem(
                            headlineContent = { Text("${currency.code} - ${stringResource(currency.displayName)}") },
                            leadingContent = { Text(currency.flagEmoji, style = MaterialTheme.typography.titleMedium) },
                            trailingContent = {
                                if (currency == homeCurrencyState) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                // 修正 2: 更新邏輯
                                homeCurrencyState = currency
                                showCurrencyPicker = false
                                scope.launch {
                                    userPref?.let {
                                        // 這裡直接傳入 Enum 物件，而非 currency.code
                                        prefStore.savePreference(it.copy(homeCurrency = currency))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Composable: 單行匯率設定 ---
@Composable
fun ExchangeRateRow(
    targetCurrency: AppCurrency,
    homeCurrency: AppCurrency,
    rate: String,
    onRateChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingTechCard()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：外幣
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(80.dp)
            ) {
                Text(targetCurrency.flagEmoji)
                Spacer(Modifier.width(8.dp))
                Text("1 ${targetCurrency.code}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }

            Text("≈", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)

            // 中間：輸入框
            OutlinedTextField(
                value = rate,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onRateChange(it) },
                placeholder = { Text("") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                )
            )

            // 右側：主幣別
            Text(
                text = homeCurrency.code,
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String, 
    icon: ImageVector, 
    accentColor: Color = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.primary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
fun PersonalDataEditor(
    userPref: UserPreference?,
    prefStore: UserPreferenceDataStore,
    navController: NavController,
    drive: Drive?,
    onBack: () -> Unit
) {
    if (userPref == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pref = remember { mutableStateOf(userPref) }
    val targetWeight = remember { mutableStateOf(userPref.weightKg) }
    val targetDays = remember { mutableStateOf(60) }

    val bmr by remember { derivedStateOf { calculateBMR(pref.value) } }
    val tdee by remember { derivedStateOf { calculateTDEE(bmr, pref.value.activityLevel) } }
    val targetKcal by remember { derivedStateOf { calculateTargetKcal(tdee, pref.value.goal) } }

    val dailyTargetsState = remember {
        derivedStateOf {
            dailyMacroTargets(
                weightKg = pref.value.weightKg.toDouble(),
                dailyKcalTarget = targetKcal,
                proteinGPerKg = pref.value.proteinGPerKg,
                fatGPerKg = pref.value.fatGPerKg
            )
        }
    }
    val proteinG = dailyTargetsState.value.proteinG
    val carbsG = dailyTargetsState.value.carbsG
    val fatsG = dailyTargetsState.value.fatG

    val dailyStore = remember { DailyMealConfigStore(context) }
    val selectedDate = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("selectedDate")

    val dailyCfg by produceState<DailyMealConfig?>(initialValue = null, selectedDate) {
        value = selectedDate?.let { date ->
            withContext(Dispatchers.IO) {
                dailyStore.get(date)
            }
        }
    }
    // Form States
    val dietType = remember { mutableStateOf(userPref.dietType) }
    val carbFocusMeal = remember { mutableStateOf(userPref.carbFocusMeal) }
    val snackHabitType = remember { mutableStateOf(userPref.snackHabitType) }
    val snackTimes = remember { mutableStateOf(userPref.snackTimes) }
    val snackPreferences = remember { mutableStateOf(userPref.snackPreferences) }
    val trainingDaysPerWeek = remember { mutableStateOf(userPref.trainingDaysPerWeek) }
    val trainingTime = remember { mutableStateOf(userPref.trainingTime) }
    val fastedTraining = remember { mutableStateOf(userPref.fastedTraining) }
    val sleepHours = remember { mutableStateOf(userPref.sleepHours) }
    val hasNightSnack = remember { mutableStateOf(userPref.hasNightSnack) }
    var isRatioValid by remember { mutableStateOf(true) }

    val mealDefsStore = remember { MealDefsStore(context) }
    val mealDefs by mealDefsStore.mealDefsFlow().collectAsState(initial = emptyList())
    val mealDefsById = remember(mealDefs) { mealDefs.associateBy { it.id } }

    LaunchedEffect(Unit) {
        mealDefsStore.getAll()
    }

    val baseMealIds = remember(mealDefs) { mealDefs.map { it.id } }
    val mealIds = remember(baseMealIds, dailyCfg) {
        val ordered = dailyCfg?.mealIds?.takeIf { it.isNotEmpty() } ?: baseMealIds
        ordered.filter { it in baseMealIds }
    }
    val mealDefsForUi = remember(mealIds, mealDefsById) { mealIds.mapNotNull { mealDefsById[it] } }

    var mealRatios: List<Pair<String, Double>> by remember(userPref, mealIds, dailyCfg) {
        mutableStateOf(
            when {
                !dailyCfg?.ratioByMealId.isNullOrEmpty() ->
                    recomputeMealRatios(mealIds, dailyCfg!!.ratioByMealId.toList(), step = 0.1)
                !userPref?.mealRatioMap.isNullOrEmpty() ->
                    recomputeMealRatios(mealIds, userPref!!.mealRatioMap.toList(), step = 0.1)
                else -> mealIds.map { it to (100.0 / maxOf(1, mealIds.size)) }
            }
        )
    }
    val mealTimesById = remember { mutableStateOf(userPref.mealTimesById) }
    LaunchedEffect(mealDefs) {
        val updated = mealTimesById.value.toMutableMap()
        val validIds = mealDefs.map { it.id }.toSet()
        updated.keys.retainAll(validIds)
        mealDefs.forEach { def ->
            if (!updated.containsKey(def.id)) {
                updated[def.id] = ""
            }
        }
        mealTimesById.value = updated
    }

    LaunchedEffect(mealIds) {
        val oldRatiosMap = mealRatios.toMap()
        val equal = if (mealIds.isEmpty()) 0.0 else 100.0 / mealIds.size
        mealRatios = mealIds.map { id -> id to (oldRatiosMap[id] ?: equal) }
        mealRatios = recomputeMealRatios(newNames = mealIds, oldRatios = mealRatios, step = 0.1)
    }

    fun pendingPref(): UserPreference {
        val fixed = normalizeToTarget(mealRatios.map { it.second }, target = 100.0, step = 0.1)
        val ratiosForSaving: List<Pair<String, Double>> = mealRatios.mapIndexed { i, p -> p.first to fixed[i] }
        val dailyTargets = dailyMacroTargets(
            weightKg = pref.value.weightKg.toDouble(),
            dailyKcalTarget = targetKcal,
            proteinGPerKg = pref.value.proteinGPerKg,
            fatGPerKg = pref.value.fatGPerKg
        )
        return pref.value.copy(
            dietType = dietType.value,
            carbFocusMeal = carbFocusMeal.value,
            snackHabitType = snackHabitType.value,
            snackTimes = snackTimes.value,
            snackPreferences = snackPreferences.value,
            trainingDaysPerWeek = trainingDaysPerWeek.value,
            trainingTime = trainingTime.value,
            fastedTraining = fastedTraining.value,
            sleepHours = sleepHours.value,
            mealTimesById = mealTimesById.value,
            proteinGPerKg = pref.value.proteinGPerKg,
            fatGPerKg = pref.value.fatGPerKg,
            carbsMode = "FILL_REST",
            dailyKcalTarget = targetKcal,
            dailyProteinTarget = dailyTargets.proteinG,
            dailyCarbsTarget = dailyTargets.carbsG,
            dailyFatsTarget = dailyTargets.fatG,
            mealRatioMap = ratiosForSaving.toMap()
        )
    }

    val isDirty by remember(
        pref.value, dietType.value, carbFocusMeal.value,
        snackHabitType.value, snackTimes.value, snackPreferences.value,
        trainingDaysPerWeek.value, trainingTime.value, fastedTraining.value,
        sleepHours.value, mealTimesById.value,
        hasNightSnack.value, mealRatios, targetKcal
    ) {
        mutableStateOf(userPref != null && pendingPref() != userPref)
    }

    var showLeaveDialog by remember { mutableStateOf(false) }

    fun tryLeave() {
        if (isDirty) showLeaveDialog = true else onBack()
    }
    fun saveAndExit() {
        val newPref = pendingPref()

        // 1. 處理導航回傳數據 (維持原樣)
        val prevEntry = navController.previousBackStackEntry
        val prevRoute = prevEntry?.destination?.route
        if (prevEntry != null && prevRoute != "login") {
            val json = Json.encodeToString(UserPreference.serializer(), newPref)
            prevEntry.savedStateHandle.set("settings_updated_pref_json", json)
            prevEntry.savedStateHandle.set(RESULT_SETTINGS_UPDATED, true)
        }

        // 2. 執行導航 (維持原樣，這會導致頁面銷毀)
        if (prevRoute == "login") {
            navController.navigate("main") { popUpTo("login") { inclusive = true } }
        } else {
            val navigated = navController.navigateUp()
            if (!navigated) onBack()
        }

        // 3. 本地儲存 (快速操作，可以用原本的 scope，但為了保險建議一起移入 global/io scope)
        // 4. 雲端上傳 (耗時操作，必須脫離 UI scope)

        // ✅ 修正：創建一個獨立的 Scope 來執行儲存與上傳，不受頁面銷毀影響
        CoroutineScope(Dispatchers.IO).launch {
            // A. 本地儲存
            try {
                prefStore.savePreference(newPref)
                selectedDate?.let { d ->
                    dailyStore.save(d, DailyMealConfig(mealIds = mealIds, ratioByMealId = mealRatios.toMap()))
                }
            } catch (e: Exception) {
                Log.e("SettingPage", "Local save failed", e)
            }

            // B. 雲端同步
            if (drive != null) {
                try {
                    val driveRepo = PreferenceDriveRepository(drive)
                    val success = driveRepo.uploadPreference(newPref)
                    if (success) {
                        Log.d("SettingPage", "User preference synced to Drive successfully.")
                    } else {
                        Log.e("SettingPage", "Failed to upload preference to Drive.")
                    }
                } catch (e: Exception) {
                    Log.e("SettingPage", "Exception syncing preference to Drive", e)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.profile), 
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { tryLeave() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = if (isSystemInDarkTheme()) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = SettingTechColors.DeepBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ColorfulSection(
                    title = stringResource(R.string.profile),
                    icon = Icons.Default.Person,
                    color = SettingColors.ProfileBg,
                    iconColor = SettingColors.ProfileIcon
                ) {
                    ProfileSection(
                        name = pref.value.name,
                        gender = pref.value.gender,
                        age = pref.value.age,
                        heightCm = pref.value.heightCm,
                        weightKg = pref.value.weightKg,
                        onNameChange = { pref.value = pref.value.copy(name = it) },
                        onGenderChange = { g -> pref.value = pref.value.copy(gender = g) },
                        onAgeChange = { a -> pref.value = pref.value.copy(age = a) },
                        onHeightChange = { h -> pref.value = pref.value.copy(heightCm = h) },
                        onWeightInputChange = { w -> pref.value = pref.value.copy(weightKg = w) }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.target_setting),
                    icon = Icons.Default.Flag,
                    color = SettingColors.TargetBg,
                    iconColor = SettingColors.TargetIcon
                ) {
                    TargetWeightSection(
                        goal = pref.value.goal,
                        currentWeight = pref.value.weightKg,
                        targetWeight = targetWeight.value,
                        onTargetWeightChange = { targetWeight.value = it },
                        targetDays = targetDays.value,
                        onTargetDaysChange = { targetDays.value = it },
                        onGoalChanged = { pref.value = pref.value.copy(goal = it) }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.diet_and_snack),
                    icon = Icons.Default.RestaurantMenu,
                    color = SettingColors.DietBg,
                    iconColor = SettingColors.DietIcon
                ) {
                    DietAndSnackSection(
                        dietType = dietType.value,
                        onDietTypeChange = { dietType.value = it },
                        carbFocusMeal = carbFocusMeal.value,
                        onCarbFocusMealChange = { carbFocusMeal.value = it },
                        snackHabitType = snackHabitType.value,
                        onSnackHabitTypeChange = { snackHabitType.value = it },
                        snackTimes = snackTimes.value,
                        onSnackTimesChange = { snackTimes.value = it },
                        snackPreferences = snackPreferences.value,
                        onSnackPreferencesChange = { snackPreferences.value = it }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.training_habit),
                    icon = Icons.Default.FitnessCenter,
                    color = SettingColors.TrainingBg,
                    iconColor = SettingColors.TrainingIcon
                ) {
                    TrainingHabitSection(
                        trainingDaysPerWeek = trainingDaysPerWeek.value,
                        onTrainingDaysChange = { trainingDaysPerWeek.value = it },
                        trainingTime = trainingTime.value,
                        onTrainingTimeChange = { trainingTime.value = it },
                        fastedTraining = fastedTraining.value,
                        onFastedTrainingChange = { fastedTraining.value = it }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.routine_habit_title),
                    icon = Icons.Default.Bedtime,
                    color = SettingColors.RoutineBg,
                    iconColor = SettingColors.RoutineIcon
                ) {
                    RoutineHabitSection(
                        sleepHours = sleepHours.value,
                        onSleepHoursChange = { sleepHours.value = it },
                        mealDefs = mealDefsForUi,
                        mealTimesById = mealTimesById.value,
                        onMealTimeChange = { mealId, newTime ->
                            mealTimesById.value = mealTimesById.value.toMutableMap().apply {
                                this[mealId] = newTime
                            }
                        }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.meal_management_title),
                    icon = Icons.Default.RestaurantMenu,
                    color = SettingColors.RoutineBg,
                    iconColor = SettingColors.RoutineIcon
                ) {
                    MealDefManagerSection(
                        mealDefs = mealDefsForUi,
                        onAdd = { name ->
                            scope.launch { mealDefsStore.add(name) }
                        },
                        onDelete = { id ->
                            scope.launch { mealDefsStore.delete(id) }
                        },
                        onUpdate = { updatedDef ->
                            scope.launch {
                                val current = mealDefsStore.getAll()
                                val updatedList = current.map { if (it.id == updatedDef.id) updatedDef else it }
                                mealDefsStore.setAll(updatedList)
                            }
                        }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.meal_ratio_title),
                    icon = Icons.Default.PieChart,
                    color = SettingColors.MacroBg,
                    iconColor = SettingColors.MacroIcon
                ) {
                    val ratioById = mealRatios.toMap()
                    MealRatioEditor(
                        mealDefs = mealDefsForUi,
                        initialRatios = mealDefsForUi.map { def -> ratioById[def.id] ?: 0.0 },
                        onRatiosChanged = { ratios, valid ->
                            mealRatios = ratios
                            isRatioValid = valid
                        }
                    )
                }
            }

            item {
                ColorfulSection(
                    title = stringResource(R.string.macro_mode_label),
                    icon = Icons.Default.Tune,
                    color = SettingColors.MacroBg,
                    iconColor = SettingColors.MacroIcon
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacroPerKgInput(
                            label = "${stringResource(R.string.protein)} (g/kg)",
                            value = pref.value.proteinGPerKg,
                            onValueChange = { pref.value = pref.value.copy(proteinGPerKg = it) }
                        )
                        MacroPerKgInput(
                            label = "${stringResource(R.string.fats)} (g/kg)",
                            value = pref.value.fatGPerKg,
                            onValueChange = { pref.value = pref.value.copy(fatGPerKg = it) }
                        )
                        Text(
                            text = "${stringResource(R.string.carbs)} (auto): ${carbsG.toInt()} g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            item {
                // Recommendation Dashboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingTechCard()
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(SettingTechColors.AccentCyan.copy(alpha = 0.05f), Color.Transparent)
                        ))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = SettingTechColors.AccentCyan)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.recommendation_title),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem("BMR", bmr.toInt().toString(), SettingTechColors.AccentCyan)
                            StatItem("TDEE", tdee.toInt().toString(), Color(0xFFA5D6A7))
                            StatItem("Target", targetKcal.toInt().toString(), Color(0xFFFFCC80))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem(stringResource(R.string.protein), "${proteinG.toInt()}g", Color(0xFFEF9A9A))
                            StatItem(stringResource(R.string.carbs), "${carbsG.toInt()}g", SettingTechColors.AccentCyan)
                            StatItem(stringResource(R.string.fats), "${fatsG.toInt()}g", Color(0xFFCE93D8))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { saveAndExit() },
                    enabled = isRatioValid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    BackHandler(enabled = true) { tryLeave() }
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_msg)) },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; saveAndExit() }) {
                    Text(stringResource(R.string.save_and_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false; onBack() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.leave_without_saving))
                }
            }
        )
    }
}

// --- 🎨 UI Helper Components ---

@Composable
fun ColorfulSection(
    title: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingTechCard()
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iconColor.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            // Content
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    MealooTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine
    )
}

@Composable
fun ModernDropdown(
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MealooSelectField(
            value = selected,
            label = label,
            onClick = { expanded = !expanded },
            expanded = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelectedChange(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) SettingTechColors.AccentCyan.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (checked) SettingTechColors.AccentCyan.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked, 
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = SettingTechColors.AccentCyan)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = color
        )
    }
}

// --- Section Implementations (Refactored) ---

@Composable
fun ProfileSection(
    name: String, gender: Gender, age: Int, heightCm: Int, weightKg: Float,
    onNameChange: (String) -> Unit, onGenderChange: (Gender) -> Unit,
    onAgeChange: (Int) -> Unit, onHeightChange: (Int) -> Unit, onWeightInputChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernTextField(value = name, onValueChange = onNameChange, label = stringResource(R.string.nickname))

        Column {
            Text(stringResource(R.string.gender), style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Gender.MALE to stringResource(R.string.gender_male), Gender.FEMALE to stringResource(R.string.gender_female), Gender.OTHER to stringResource(R.string.gender_unknown)).forEach { (g, l) ->
                    FilterChip(selected = gender == g, onClick = { onGenderChange(g) }, label = { Text(l) })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernTextField(
                value = if (age == 0) "" else age.toString(),
                onValueChange = { onAgeChange(it.toIntOrNull() ?: 0) },
                label = stringResource(R.string.age),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            ModernTextField(
                value = if (heightCm == 0) "" else heightCm.toString(),
                onValueChange = { onHeightChange(it.toIntOrNull() ?: 0) },
                label = stringResource(R.string.height_cm),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Weight 使用 FloatInputField
            FloatInputField(
                value = weightKg,
                onValueChange = onWeightInputChange,
                label = stringResource(R.string.weight_kg),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
@Composable
fun FloatInputField(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
) {
    var text by remember(value) {
        mutableStateOf(if (value == 0f) "" else value.toString().removeSuffix(".0"))
    }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (!isFocused) {
            text = if (value == 0f) "" else value.toString().removeSuffix(".0")
        }
    }

    MealooTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val parsed = newText.toFloatOrNull() ?: 0f
            if (parsed != value) onValueChange(parsed)
        },
        label = label,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}
@Composable
fun TargetWeightSection(
    goal: Goal, currentWeight: Float, targetWeight: Float, onTargetWeightChange: (Float) -> Unit,
    targetDays: Int, onTargetDaysChange: (Int) -> Unit, onGoalChanged: (Goal) -> Unit
) {
    val context = LocalContext.current
    val goalGain = stringResource(R.string.goal_gain)
    val goalLoss = stringResource(R.string.goal_loss)
    val goalMaintain = stringResource(R.string.goal_maintain)
    val detectedGoal by remember(currentWeight, targetWeight) {
        derivedStateOf { if (abs(targetWeight - currentWeight) < 2f) goalMaintain else if (targetWeight > currentWeight) goalGain else goalLoss }
    }
    val options = listOf(Goal.MAINTAIN to goalMaintain, Goal.GAIN to goalGain, Goal.LOSS to goalLoss)
    val selectedLabel = options.firstOrNull { it.first == goal }?.second ?: detectedGoal

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(context.getString(R.string.current_weight_fmt, currentWeight), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FloatInputField(
                value = targetWeight,
                onValueChange = onTargetWeightChange,
                label = stringResource(R.string.target_weight_label),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            ModernTextField(
                value = if (targetDays == 0) "" else targetDays.toString(),
                onValueChange = { onTargetDaysChange(it.toIntOrNull() ?: 0) },
                label = stringResource(R.string.target_days_label),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        ModernDropdown(options.map { it.second }, selectedLabel, { label -> onGoalChanged(options.first { it.second == label }.first) }, stringResource(R.string.goal_label))
    }
}

@Composable
fun DietAndSnackSection(
    dietType: String, onDietTypeChange: (String) -> Unit,
    carbFocusMeal: String, onCarbFocusMealChange: (String) -> Unit,
    snackHabitType: String, onSnackHabitTypeChange: (String) -> Unit,
    snackTimes: List<String>, onSnackTimesChange: (List<String>) -> Unit,
    snackPreferences: List<String>, onSnackPreferencesChange: (List<String>) -> Unit
) {
    val res = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernDropdown(res.getStringArray(R.array.diet_options).toList(), dietType, onDietTypeChange, stringResource(R.string.diet_type))
        ModernDropdown(res.getStringArray(R.array.meal_options).toList(), carbFocusMeal, onCarbFocusMealChange, stringResource(R.string.carb_focus_meal))

        HorizontalDivider(color = SettingColors.InputBorder)

        ModernDropdown(res.getStringArray(R.array.snack_habit_options).toList(), snackHabitType, onSnackHabitTypeChange, stringResource(R.string.snack_type))

        if (snackHabitType == res.getStringArray(R.array.snack_habit_options)[1]) {
            Text(stringResource(R.string.snack_time), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            res.getStringArray(R.array.snack_time_options).forEach { time ->
                CheckRow(time, snackTimes.contains(time)) { checked -> onSnackTimesChange(if (checked) snackTimes + time else snackTimes - time) }
            }
        }
        if (snackHabitType != res.getStringArray(R.array.snack_habit_options)[0]) {
            Text(stringResource(R.string.snack_pref), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            res.getStringArray(R.array.snack_pref_options).forEach { pref ->
                CheckRow(pref, snackPreferences.contains(pref)) { checked -> onSnackPreferencesChange(if (checked) snackPreferences + pref else snackPreferences - pref) }
            }
        }
    }
}

@Composable
fun TrainingHabitSection(
    trainingDaysPerWeek: Int, onTrainingDaysChange: (Int) -> Unit,
    trainingTime: String, onTrainingTimeChange: (String) -> Unit,
    fastedTraining: Boolean, onFastedTrainingChange: (Boolean) -> Unit
) {
    val timeOptions = listOf(stringResource(R.string.morning), stringResource(R.string.noon), stringResource(R.string.evening))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernTextField(value = trainingDaysPerWeek.toString(), onValueChange = { onTrainingDaysChange(it.toIntOrNull() ?: 0) }, label = stringResource(R.string.training_days_per_week), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ModernDropdown(timeOptions, trainingTime, onTrainingTimeChange, stringResource(R.string.training_time_of_day))
        CheckRow(stringResource(R.string.fasted_training), fastedTraining, onFastedTrainingChange)
    }
}

@Composable
fun RoutineHabitSection(
    sleepHours: Int, onSleepHoursChange: (Int) -> Unit,
    mealDefs: List<MealDef>,
    mealTimesById: Map<String, String>,
    onMealTimeChange: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernTextField(value = sleepHours.toString(), onValueChange = { onSleepHoursChange(it.toIntOrNull() ?: 0) }, label = stringResource(R.string.sleep_duration_label), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        mealDefs.forEach { def ->
            val name = when (def.kind) {
                MealKind.BREAKFAST -> stringResource(R.string.breakfast)
                MealKind.LUNCH -> stringResource(R.string.lunch)
                MealKind.DINNER -> stringResource(R.string.dinner)
                MealKind.SNACK -> stringResource(R.string.snack)
                else -> def.name
            }
            ModernTextField(
                value = mealTimesById[def.id].orEmpty(),
                onValueChange = { onMealTimeChange(def.id, it) },
                label = name,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MacroPerKgInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    step: Double = 0.1,
    min: Double = 0.0,
    max: Double = 5.0
) {
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", value)) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            text = String.format(Locale.getDefault(), "%.1f", value)
        }
    }

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onValueChange((value - step).coerceAtLeast(min))
                }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            MealooTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    newText.toDoubleOrNull()?.let { parsed ->
                        onValueChange(parsed.coerceIn(min, max))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    onValueChange((value + step).coerceAtMost(max))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun MealDefManagerSection(
    mealDefs: List<MealDef>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (MealDef) -> Unit
) {
    var newMealName by remember { mutableStateOf("") }
    val canDelete = mealDefs.size > 1
    var editingMeal by remember { mutableStateOf<MealDef?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 現有餐次列表
        mealDefs.forEach { def ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SettingColors.InputBg)
                    .border(1.dp, SettingColors.InputBorder, RoundedCornerShape(10.dp))
                    .clickable { editingMeal = def }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val name = when (def.kind) {
                    MealKind.BREAKFAST -> stringResource(R.string.breakfast)
                    MealKind.LUNCH     -> stringResource(R.string.lunch)
                    MealKind.DINNER    -> stringResource(R.string.dinner)
                    MealKind.SNACK     -> stringResource(R.string.snack)
                    else -> def.name
                }

                // Show color dot if configured
                def.trackColorHex?.let { hex ->
                    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()?.let { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                }

                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { editingMeal = def },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "編輯",
                        tint = SettingTechColors.AccentCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete(def.id) },
                    enabled = canDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "刪除",
                        tint = if (canDelete) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 新增餐次輸入列
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MealooTextField(
                value = newMealName,
                onValueChange = { newMealName = it },
                label = "新餐次名稱",
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    val name = newMealName.trim()
                    if (name.isNotEmpty()) {
                        onAdd(name)
                        newMealName = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(SettingTechColors.AccentCyan.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增",
                    tint = SettingTechColors.AccentCyan
                )
            }
        }
    }

    if (editingMeal != null) {
        MealDefEditDialog(
            mealDef = editingMeal!!,
            onDismiss = { editingMeal = null },
            onSave = { updated ->
                onUpdate(updated)
                editingMeal = null
            }
        )
    }
}

@Composable
fun MealRatioEditor(
    mealDefs: List<MealDef>,
    initialRatios: List<Double>,
    onRatiosChanged: (List<Pair<String, Double>>, Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var locked by remember { mutableStateOf(setOf<Int>()) }
    fun lock(i: Int) { locked = locked + i }
    fun unlock(i: Int) { locked = locked - i }
    fun isLocked(i: Int) = i in locked

    var values by remember(mealDefs, initialRatios) { mutableStateOf(mealDefs.indices.map { i -> initialRatios.getOrNull(i) ?: 0.0 }) }
    var texts by remember(mealDefs, initialRatios) { mutableStateOf(values.map { v -> String.format(Locale.getDefault(), "%.1f", v) }) }

    LaunchedEffect(mealDefs, initialRatios) {
        if (focusedIndex == null) {
            values = mealDefs.indices.map { i -> initialRatios.getOrNull(i) ?: 0.0 }
            texts = values.map { v -> String.format(Locale.getDefault(), "%.1f", v) }
            locked = emptySet()
        }
    }

    fun approxEqual(a: Double, b: Double, tol: Double = 0.05) = kotlin.math.abs(a - b) <= tol
    fun quantize(v: Double, step: Double = 0.1) = (kotlin.math.round(v / step) * step).coerceIn(0.0, 100.0)

    fun rebalance(commitIndex: Int? = null, step: Double = 0.1) {
        val current = values.toMutableList()
        val total = current.sum()
        var remain = 100.0 - total
        if (approxEqual(remain, 0.0)) {
            val q = current.map { quantize(it, step) }
            values = q
            texts = q.map { String.format(Locale.getDefault(), "%.1f", it) }
            onRatiosChanged(mealDefs.mapIndexed { index, def -> def.id to values[index] }, true)
            return
        }
        val adjustable = current.indices.filter { it != commitIndex && !isLocked(it) }.toMutableList()
        if (adjustable.isNotEmpty()) {
            val base = remain / adjustable.size
            adjustable.forEach { idx ->
                val before = current[idx]
                val after = (before + base).coerceIn(0.0, 100.0)
                current[idx] = after
                remain -= (after - before)
            }
            var guard = 0
            while (!approxEqual(remain, 0.0, tol = step / 2) && guard++ < 400) {
                for (idx in adjustable) {
                    val tryVal = (current[idx] + kotlin.math.sign(remain) * step).coerceIn(0.0, 100.0)
                    val applied = tryVal - current[idx]
                    if (kotlin.math.abs(applied) < 1e-9) continue
                    current[idx] = tryVal
                    remain -= applied
                    if (approxEqual(remain, 0.0, tol = step / 2)) break
                }
            }
        }
        val quant = current.map { quantize(it, step) }.toMutableList()
        var qRem = 100.0 - quant.sum()
        if (!approxEqual(qRem, 0.0)) {
            val targetIdx = when {
                adjustable.isNotEmpty() -> adjustable.last()
                commitIndex != null     -> commitIndex
                else                    -> current.indices.last()
            }
            quant[targetIdx] = (quant[targetIdx] + qRem).coerceIn(0.0, 100.0)
        }
        values = quant
        texts  = quant.map { String.format(Locale.getDefault(), "%.1f", it) }
        onRatiosChanged(mealDefs.mapIndexed { index, def -> def.id to values[index] }, approxEqual(values.sum(), 100.0))
    }

    val total = values.sum()
    val remaining = 100.0 - total
    val isValid = approxEqual(total, 100.0)

    // Visual Bar Logic
    val animatedValues = values.map { animateFloatAsState(targetValue = it.toFloat() / 100f).value }
    val colors = listOf(Color(0xFFEF9A9A), Color(0xFFCE93D8), Color(0xFF9FA8DA), Color(0xFF80DEEA), Color(0xFFA5D6A7), Color(0xFFFFF59D))

    Column {
        // Visual Percentage Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEEEEEE))
        ) {
            animatedValues.forEachIndexed { index, percent ->
                if (percent > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(percent)
                            .background(colors.getOrElse(index) { Color.Gray })
                    )
                    if (index < animatedValues.lastIndex) {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White))
                    }
                }
            }
            if (total < 99.9) {
                Spacer(modifier = Modifier.weight(((100.0 - total) / 100.0).toFloat()))
            }
        }
        Spacer(Modifier.height(16.dp))

        mealDefs.forEachIndexed { index, def ->
            val name = when (def.kind) {
                MealKind.BREAKFAST -> stringResource(R.string.breakfast)
                MealKind.LUNCH -> stringResource(R.string.lunch)
                MealKind.DINNER -> stringResource(R.string.dinner)
                MealKind.SNACK -> stringResource(R.string.snack)
                else -> def.name
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Color Dot
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.getOrElse(index) { Color.Gray }))
                Spacer(Modifier.width(8.dp))

                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = texts.getOrNull(index) ?: "",
                    onValueChange = { newText ->
                        texts = texts.toMutableList().apply { if (index < size) this[index] = newText else add(newText) }
                        newText.toDoubleOrNull()?.let { parsed ->
                            val v = parsed.coerceIn(0.0, 100.0)
                            values = values.toMutableList().apply { if (index < size) this[index] = v else add(v) }
                            onRatiosChanged(mealDefs.mapIndexed { idx, item -> item.id to values[idx] }, approxEqual(values.sum(), 100.0))
                        }
                    },
                    trailingIcon = { Text("%", style = MaterialTheme.typography.bodySmall, color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .width(100.dp)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                focusedIndex = index
                                unlock(index)
                            } else if (focusedIndex == index) {
                                focusedIndex = null
                                lock(index)
                                rebalance(commitIndex = index, step = 0.1)
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SettingColors.InputBg,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = SettingColors.InputBorder,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = String.format(Locale.getDefault(), "%s %.1f%%", if (remaining >= 0) stringResource(R.string.remaining) else stringResource(R.string.over), abs(remaining)),
                color = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { rebalance(commitIndex = focusedIndex, step = 0.1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) { Text(stringResource(R.string.auto_fix_to_100)) }
        }
    }
}

// Logic Functions preserved
fun calculateBMR(pref: UserPreference): Double {
    return when (pref.gender) {
        Gender.MALE -> 10 * pref.weightKg + 6.25 * pref.heightCm - 5 * pref.age + 5
        else -> 10 * pref.weightKg + 6.25 * pref.heightCm - 5 * pref.age - 161
    }
}
fun calculateTDEE(bmr: Double, activity: ActivityLevel): Double {
    return when (activity) {
        ActivityLevel.LOW -> bmr * 1.2
        ActivityLevel.MEDIUM -> bmr * 1.55
        ActivityLevel.HIGH -> bmr * 1.8
    }
}
fun calculateTargetKcal(tdee: Double, goal: Goal): Double {
    return when (goal) {
        Goal.GAIN -> tdee * 1.15
        Goal.LOSS -> tdee * 0.8
        Goal.MAINTAIN -> tdee
    }
}
object ExchangeRateFileManager {
    data class CurrencyRateEntry(
        val code: String,
        val floatName: String, // FloatRates 提供的英文名稱
        val inverseRate: Double // 1 單位外幣 = 多少基準幣
    )
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun getCacheFile(context: android.content.Context, baseCode: String): File {
        return File(context.filesDir, "rates_${baseCode.lowercase()}.json")
    }

    // 回傳 List<CurrencyRateEntry>
    suspend fun loadLocalCache(context: android.content.Context, baseCode: String): List<CurrencyRateEntry>? = withContext(Dispatchers.IO) {
        val file = getCacheFile(context, baseCode)
        if (!file.exists()) return@withContext null
        try {
            parseFloatRatesJson(file.readText(), baseCode)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAndSave(context: android.content.Context, baseCode: String): List<CurrencyRateEntry>? = withContext(Dispatchers.IO) {
        val file = getCacheFile(context, baseCode)
        val url = "https://www.floatrates.com/daily/${baseCode.lowercase()}.json"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bodyString = response.body?.string() ?: return@use null

                // 存檔
                file.writeText(bodyString)

                return@withContext parseFloatRatesJson(bodyString, baseCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 解析邏輯：同時抓取 name 與 inverseRate
    private fun parseFloatRatesJson(jsonString: String, baseCode: String): List<CurrencyRateEntry> {
        val rootObj = json.parseToJsonElement(jsonString).jsonObject
        val results = mutableListOf<CurrencyRateEntry>()

        // 1. 手動塞入基準幣別自己 (1:1)
        results.add(
            CurrencyRateEntry(
                code = baseCode.uppercase(),
                floatName = "Base Currency",
                inverseRate = 1.0
            )
        )

        // 2. 遍歷 JSON 內的所有貨幣
        rootObj.keys.forEach { key ->
            val node = rootObj[key]?.jsonObject
            val code = node?.get("code")?.jsonPrimitive?.content ?: key.uppercase()
            val name = node?.get("name")?.jsonPrimitive?.content ?: code
            val rateToBase = node?.get("inverseRate")?.jsonPrimitive?.doubleOrNull

            if (rateToBase != null) {
                results.add(CurrencyRateEntry(code, name, rateToBase))
            }
        }

        // 依代碼排序後回傳
        return results.sortedBy { it.code }
    }
}
@Composable
fun MealDefEditDialog(
    mealDef: MealDef,
    onDismiss: () -> Unit,
    onSave: (MealDef) -> Unit
) {
    var name by remember { mutableStateOf(mealDef.name) }
    var selectedColor by remember { mutableStateOf(mealDef.trackColorHex ?: "#00D2FF") }
    var selectedIcon by remember { mutableStateOf(mealDef.iconName ?: "Fastfood") }

    val techColors = listOf(
        "#00D2FF", // Cyan
        "#FF2D55", // Magenta
        "#00FF88", // Neon Green
        "#FF9F0A", // Orange
        "#BF5AF2", // Purple
        "#FFD60A", // Yellow
        "#64D2FF", // Light Blue
        "#FF375F"  // Crimson
    )

    val iconOptions = listOf(
        "Breakfast", "Lunch", "Dinner", "Snack",
        "Restaurant", "Coffee", "LocalBar", "Icecream",
        "Egg", "BakeryDining", "Cake", "LocalPizza",
        "LocalDrink", "RamenDining"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SettingTechColors.DeepBackground,
        title = { Text(stringResource(R.string.edit_meal_customization), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MealooTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.meal_name),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.track_color), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    techColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Text(stringResource(R.string.progress_bar_icon), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Column {
                    val chunks = iconOptions.chunked(5)
                    chunks.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { iconName ->
                                val icon = mealIconForChoice(iconName)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedIcon == iconName) SettingTechColors.AccentCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedIcon = iconName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = iconName,
                                        tint = if (selectedIcon == iconName) SettingTechColors.AccentCyan else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(mealDef.copy(name = name, trackColorHex = selectedColor, iconName = selectedIcon))
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.Gray)
            }
        }
    )
}

private fun mealIconForChoice(name: String): ImageVector = when (name) {
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
    else -> Icons.Default.Fastfood
}
