// MealooApp.kt
package com.mealoo.app

import android.app.Application
import com.google.android.libraries.places.api.Places

class MealooApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 用你的 Android API Key
        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.google_maps_key))
        }
    }
}
