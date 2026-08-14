package com.mealoo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.mealoo.app.R
import com.mealoo.app.ui.design.DarkMealooColors
import com.mealoo.app.ui.design.LightMealooColors
import com.mealoo.app.ui.design.LocalMealooColors
@Composable
fun MealooTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val LightColorScheme = lightColorScheme(
        primary = colorResource(id = R.color.md_theme_light_primary),
        onPrimary = colorResource(id = R.color.md_theme_light_onPrimary),
        secondary = colorResource(id = R.color.md_theme_light_secondary),
        onSecondary = colorResource(id = R.color.md_theme_light_onSecondary),
        tertiary = colorResource(id = R.color.md_theme_light_tertiary),
        onTertiary = colorResource(id = R.color.md_theme_light_onTertiary),
        background = colorResource(id = R.color.md_theme_light_background),
        onBackground = colorResource(id = R.color.md_theme_light_onBackground),
        surface = colorResource(id = R.color.md_theme_light_surface),
        onSurface = colorResource(id = R.color.md_theme_light_onSurface),
        surfaceVariant = colorResource(id = R.color.md_theme_light_surfaceVariant),
        onSurfaceVariant = colorResource(id = R.color.md_theme_light_onSurfaceVariant)
    )
    val DarkColorScheme = darkColorScheme(
        primary = colorResource(id = R.color.md_theme_dark_primary),
        onPrimary = colorResource(id = R.color.md_theme_dark_onPrimary),
        secondary = colorResource(id = R.color.md_theme_dark_secondary),
        onSecondary = colorResource(id = R.color.md_theme_dark_onSecondary),
        tertiary = colorResource(id = R.color.md_theme_dark_tertiary),
        onTertiary = colorResource(id = R.color.md_theme_dark_onTertiary),
        background = colorResource(id = R.color.md_theme_dark_background),
        onBackground = colorResource(id = R.color.md_theme_dark_onBackground),
        surface = colorResource(id = R.color.md_theme_dark_surface),
        onSurface = colorResource(id = R.color.md_theme_dark_onSurface),
        surfaceVariant = colorResource(id = R.color.md_theme_dark_surfaceVariant),
        onSurfaceVariant = colorResource(id = R.color.md_theme_dark_onSurfaceVariant)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkMealooColors else LightMealooColors

    CompositionLocalProvider(LocalMealooColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
