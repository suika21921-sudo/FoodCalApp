@file:Suppress("ProduceStateDoesNotAssignValue")

package com.mealoo.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mealoo.app.data.DailyMealConfigStore
import com.mealoo.app.data.IntakeDataStore
import com.mealoo.app.data.MealDefsStore
import com.mealoo.app.data.MealNamesStore
import com.mealoo.app.data.RecipeDataStore
import com.mealoo.app.data.UserPreference
import com.mealoo.app.data.UserPreferenceDataStore
import com.mealoo.app.db.AppDatabase
import com.mealoo.app.db.InventoryRepository
import com.mealoo.app.db.MealPlanRepository
import com.mealoo.app.db.SavingsRepository
import com.mealoo.app.ui.CalendarPage
import com.mealoo.app.ui.EvaluatePage
import com.mealoo.app.ui.MainScreen
import com.mealoo.app.ui.RecipePage
import com.mealoo.app.ui.SettingPage
import com.mealoo.app.ui.StoragePage
import com.mealoo.app.ui.theme.MealooTheme
import kotlinx.serialization.json.Json
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.mealoo.app.data.PreferenceDriveRepository
import com.mealoo.app.data.repository.SubscriptionRepository
import com.mealoo.app.ui.LoginScreen
import com.mealoo.app.ui.MenuPlanningPage
import com.mealoo.app.ui.SettingsDestination
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val MEAL_MIGRATION_PREFS = "meal_migration_prefs"
private const val MEAL_MIGRATION_VERSION_KEY = "meal_id_migration_version"
private const val MEAL_MIGRATION_VERSION = 1
private const val TAG = "Mealoo_Debug_Activity"

class DeepLinkViewModel : ViewModel() {
    private val _importEvent = MutableSharedFlow<String>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val importEvent = _importEvent.asSharedFlow()

    fun triggerImport(json: String) {
        val success = _importEvent.tryEmit(json)
        Log.d("Mealoo_VM", "Emit payload success: $success")
    }
}

class MainActivity : ComponentActivity() {
    private val deepLinkViewModel: DeepLinkViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("Mealoo_DEBUG", "Activity onCreate: ${intent.action}")
        handleDeepLinkIntent(intent)

        // 1. 初始化 RevenueCat
        // 雖然 debug 模式可以用 public key，但建議還是去後台申請
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration.Builder(this, "goog_ZXKrQIEWNNTCFXEGpBkZhofbBER").build())

        setContent {
            val dataStore = IntakeDataStore(applicationContext)
            val recipeDataStore = RecipeDataStore(applicationContext)
            val subscriptionRepository = remember { SubscriptionRepository() }

            AppRoot(
                deepLinkViewModel = deepLinkViewModel,
                dataStore = dataStore,
                recipeDataStore = recipeDataStore,
                subscriptionRepository = subscriptionRepository
            )
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent: Action=${intent.action}, Data=${intent.dataString}")
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "mealoo") {
                val data = intent.data
                val payload = data?.getQueryParameter("payload")

                Log.e("Mealoo_DEBUG", "DeepLink 攔截成功！Payload 長度: ${payload?.length ?: 0}")

                if (!payload.isNullOrBlank()) {
                    // 2. 修正：必須觸發 ViewModel 轉發，UI 才會動
                    deepLinkViewModel.triggerImport(payload)
                    Log.e("Mealoo_DEBUG", "事件已發射至 ViewModel")

                    // 處理完後清除 Data，避免 Activity 重組時重複觸發
                    intent.data = null
                } else {
                    Log.e("Mealoo_DEBUG", "DeepLink Payload 為空")
                }
            }
        } catch (e: Exception) {
            // 3. 關鍵：防止 malformed URI 導致的 crash
            Log.e("Mealoo_DEBUG", "解析 DeepLink 失敗 (可能是 Gemini 格式損毀): ${e.message}")
        }
    }
}
private suspend fun runMealIdMigrationIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences(MEAL_MIGRATION_PREFS, Context.MODE_PRIVATE)
    val current = prefs.getInt(MEAL_MIGRATION_VERSION_KEY, 0)
    if (current >= MEAL_MIGRATION_VERSION) return

    val appContext = context.applicationContext
    val intakeStore = IntakeDataStore(appContext)
    val planRepo = MealPlanRepository.getInstance(appContext)
    val dailyStore = DailyMealConfigStore(appContext)
    val mealDefsStore = MealDefsStore(appContext)
    val mealNamesStore = MealNamesStore(appContext)
    val prefStore = UserPreferenceDataStore(appContext)

    intakeStore.clearAll()
    planRepo.clearAll()
    dailyStore.clearAll()
    mealNamesStore.setMeals(emptyList())
    mealDefsStore.clearAll()

    appContext.getSharedPreferences("daily_meal_snapshot", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()

    val mealDefs = mealDefsStore.getAll()
    val defaultPref = UserPreferenceDataStore.createLocalizedDefaultPreference(appContext)
    val equalRatio = if (mealDefs.isNotEmpty()) 100.0 / mealDefs.size else 0.0
    val ratioMap = mealDefs.associate { it.id to equalRatio }
    val oldPref = prefStore.getPreference()
    val resetPref = oldPref.copy(
        mealTimesById = mealDefs.associate { def ->
            def.id to (defaultPref.mealTimesById[def.id] ?: "")
        },
        mealRatioMap = ratioMap,
        breakfastTime = defaultPref.breakfastTime,
        lunchTime = defaultPref.lunchTime,
        dinnerTime = defaultPref.dinnerTime
    )
    prefStore.savePreference(resetPref)

    prefs.edit().putInt(MEAL_MIGRATION_VERSION_KEY, MEAL_MIGRATION_VERSION).apply()
}

// 建立 Drive service 的工具函数
fun buildDriveService(account: GoogleSignInAccount, context: Context): Drive {
    val credential = GoogleAccountCredential.usingOAuth2(
        context,
        listOf("https://www.googleapis.com/auth/drive.appdata")
    )
    credential.selectedAccount = account.account

    return Drive.Builder(
        AndroidHttp.newCompatibleTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("Mealoo")
        .build()
}
// 輔助函式：判斷資料是否完整 (身高、體重、年齡不為 0)
fun isValidProfile(pref: UserPreference): Boolean {
    return pref.heightCm > 0 && pref.weightKg > 0f && pref.age > 0
}
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppRoot(
    deepLinkViewModel: DeepLinkViewModel,
    dataStore: IntakeDataStore,
    recipeDataStore: RecipeDataStore,
    subscriptionRepository: SubscriptionRepository
) {
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val appCtx = context.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    var migrationDone by remember { mutableStateOf(false) }

    var drive by remember { mutableStateOf<Drive?>(null) }
    val savingsRepo = remember(appCtx) { SavingsRepository.getInstance(appCtx) }
    val inventoryRepo = remember(appCtx) { InventoryRepository.getInstance(appCtx) }
    val storageItems by inventoryRepo.itemsFlow().collectAsState(initial = emptyList())
    var initialRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val prefStore = UserPreferenceDataStore(context)
        val pref = prefStore.getPreference()
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        drive = lastAccount?.let { buildDriveService(it, context) }
        val ageOk = (pref.age ?: 0) > 0
        val heightOk = (pref.heightCm ?: 0) > 0
        val weightOk = (pref.weightKg.toDouble() ?: 0.0) > 0.0
        val hasProfile = ageOk && heightOk && weightOk
        initialRoute = if (hasProfile) "main" else "login"
    }

    LaunchedEffect(db) {
        withContext(Dispatchers.IO) {
            runMealIdMigrationIfNeeded(context)
        }
        migrationDone = true
    }

    MealooTheme(darkTheme = true, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // initialRoute 还没算出来先给个简单 loading，避免 NavHost 用到 null
            if (initialRoute == null || !migrationDone) {
                // 这里你也可以放自家 Logo / Splash
                androidx.compose.material3.Text("Loading...")
            } else {
                NavHost(
                    navController = navController,
                    startDestination = initialRoute!!
                ) {
                    composable("login") {
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
                        // 取得 DataStore
                        val prefStore = remember { UserPreferenceDataStore(context) }

                        LoginScreen(
                            navController = navController,
                            onSkipLogin = {
                                // 跳過登入：直接看本地是否有資料，決定去 Settings 還是 Main
                                scope.launch {
                                    val localPref = prefStore.getPreference()
                                    if (isValidProfile(localPref)) {
                                        navController.navigate("main") { popUpTo("login") { inclusive = true } }
                                    } else {
                                        navController.navigate("settings?start=personal")
                                    }
                                }
                            },
                            onLoginSuccess = { account ->
                                // --- 登入成功後的攔截邏輯 ---
                                scope.launch {
                                    val newDriveService = buildDriveService(account, context)
                                    drive = newDriveService

                                    val prefRepo = PreferenceDriveRepository(newDriveService)

                                    val cloudPref = prefRepo.fetchPreference()

                                    if (cloudPref != null && isValidProfile(cloudPref)) {
                                        prefStore.savePreference(cloudPref)
                                        prefStore.setOnboarded()
                                        navController.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        if (cloudPref != null) {
                                            prefStore.savePreference(cloudPref)
                                        }
                                        navController.navigate("settings?start=personal")
                                    }
                                }
                            }
                        )
                    }

                    composable("main") {
                        MainScreen(
                            deepLinkViewModel = deepLinkViewModel,
                            dataStore = dataStore,
                            navController = navController,
                            drive = drive
                        )
                    }

                    composable("recipe") {
                        RecipePage(
                            dataStore = recipeDataStore
                        )
                    }

                    composable(
                        route = "settings?start={start}",
                        arguments = listOf(navArgument("start") { defaultValue = "menu" })
                    ) { backStackEntry ->
                        val context = LocalContext.current
                        val prefStore = remember { UserPreferenceDataStore(context) }

                        val start = backStackEntry.arguments?.getString("start") ?: "menu"
                        val startDest = when (start) {
                            "personal" -> SettingsDestination.PERSONAL_DATA
                            "account" -> SettingsDestination.ACCOUNT_AND_SYNC
                            else -> SettingsDestination.MENU
                        }

                        // snapshot 逻辑保持不变...
                        val snapshotJson = remember {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<String>("pref_snapshot_json")
                        }

                        var userPref by remember {
                            mutableStateOf<UserPreference?>(
                                snapshotJson?.let { Json.decodeFromString(UserPreference.serializer(), it) }
                            )
                        }

                        LaunchedEffect(snapshotJson) {
                            if (snapshotJson == null) {
                                userPref = prefStore.getPreference()
                            }
                        }

                        SettingPage(
                            userPref = userPref,
                            prefStore = prefStore,
                            navController = navController,
                            onBack = { navController.popBackStack() },
                            drive = drive,
                            intakeStore = remember(context) { IntakeDataStore(context) }, // Pass IntakeDataStore here
                            onLoginRequest = { navController.navigate("login") },
                            onLogoutRequest = {
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                ).build()
                                val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                client.signOut().addOnCompleteListener {
                                    drive = null // 清除 MainActivity 的狀態
                                }
                            },
                            startDestination = startDest,
                            subscriptionRepository = subscriptionRepository
                        )
                    }

                    composable("calendar") {
                        CalendarPage(navController = navController)
                    }
                    composable(
                        route = "menu_planning/{dates}",
                        arguments = listOf(
                            navArgument("dates") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val dateString = backStackEntry.arguments?.getString("dates") ?: ""
                        MenuPlanningPage(
                            navController = navController,
                            dateString = dateString
                        )
                    }

                    composable("storage") {
                        StoragePage(
                            savingsRepo = savingsRepo,
                            store = inventoryRepo,
                            allItems = storageItems
                        )
                    }

                    composable("evaluate") {
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
                            drive = drive,
                        )
                    }
                }
            }
        }
    }
}
