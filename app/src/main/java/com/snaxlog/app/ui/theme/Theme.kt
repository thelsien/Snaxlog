package com.snaxlog.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    scrim = ScrimColor,
    surfaceTint = SurfaceTintLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = ScrimColor,
    surfaceTint = SurfaceTintDark
)

/**
 * Custom colors not provided by Material 3 default scheme.
 * Includes semantic colors for progress states and macro nutrient indicators.
 */
@Immutable
data class CustomColors(
    val success: Color = Color.Unspecified,
    val onSuccess: Color = Color.Unspecified,
    val successContainer: Color = Color.Unspecified,
    val warning: Color = Color.Unspecified,
    val onWarning: Color = Color.Unspecified,
    val warningContainer: Color = Color.Unspecified,
    val protein: Color = Color.Unspecified,
    val fat: Color = Color.Unspecified,
    val carbs: Color = Color.Unspecified
)

val LightCustomColors = CustomColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    warningContainer = WarningContainerLight,
    protein = ProteinLight,
    fat = FatLight,
    carbs = CarbsLight
)

val DarkCustomColors = CustomColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    warningContainer = WarningContainerDark,
    protein = ProteinDark,
    fat = FatDark,
    carbs = CarbsDark
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

@Composable
fun SnaxlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Extension property to access custom colors from MaterialTheme.
 */
object SnaxlogThemeExtras {
    val customColors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
