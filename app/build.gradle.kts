import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.mealoo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mealoo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 21
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            // 直接引用開發環境預設的 debug 金鑰
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // 🚀 新增：將上面定義的簽名套用到 release 模式
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = false // 保持目前的設定 [cite: 4]
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.all {
    exclude(group = "org.apache.httpcomponents", module = "httpclient")
    exclude(group = "org.apache.httpcomponents", module = "httpcore")
}

dependencies {
    // --- 核心與導航 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // --- Compose UI ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // 補回 Icons

    // --- 資料存儲與序列化 ---
    implementation(libs.kotlinx.serialization.json) // 補回 Serialization
    implementation(libs.androidx.datastore.preferences) // 補回 DataStore
    implementation(libs.androidx.datastore)

    // --- Google Drive & API ---
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // MediaPipe 已移除，改用 llama.cpp JNI 橋接

    // --- 訂閱與支付 (RevenueCat) ---
    implementation(libs.revenuecat.purchases)

    // --- 第三方工具 ---
    implementation(libs.ucrop)
    implementation(libs.coil.compose)
    implementation(libs.mpandroidchart)
    implementation(libs.accompanist.flowlayout)
    implementation("com.jakewharton:process-phoenix:2.1.2")

    // --- Firebase ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // --- 資料庫 (Room) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Changed to implementation and removed the ksp declaration below
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    implementation(libs.androidx.room.runtime) {
        exclude(group = "androidx.sqlite", module = "sqlite")
    }
//    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- 測試 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.google.http.client.android)
}