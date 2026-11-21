package com.kaminova.novalogistikmobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val LightColorScheme = expressiveLightColorScheme().copy(
    primary = Color(0xFF005746),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB1F1D6),
    onPrimaryContainer = Color(0xFF052D23),
    secondary = Color(0xFF495F56),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4ECE0),
    onSecondaryContainer = Color(0xFF21372E),
    tertiary = Color(0xFF6D521B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3A7),
    onTertiaryContainer = Color(0xFF372500),
    background = Color(0xFFF4F9F5),
    onBackground = Color(0xFF15231D),
    surface = Color(0xFFF4F9F5),
    onSurface = Color(0xFF15231D),
    surfaceVariant = Color(0xFFDDE8E1),
    onSurfaceVariant = Color(0xFF42564C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF5EF),
    surfaceContainer = Color(0xFFE7F0E9),
    surfaceContainerHigh = Color(0xFFE0EBE3),
    surfaceContainerHighest = Color(0xFFD8E6DD),
    outline = Color(0xFF65796E),
    outlineVariant = Color(0xFFBBCBC1),
    surfaceTint = Color(0xFF005746),
    inverseSurface = Color(0xFF293930),
    inverseOnSurface = Color(0xFFEBF5ED),
    inversePrimary = Color(0xFF8EDABD)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8EDABD),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF135342),
    onPrimaryContainer = Color(0xFFB1F1D6),
    secondary = Color(0xFFB8D0C4),
    onSecondary = Color(0xFF23382F),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFD4ECE0),
    tertiary = Color(0xFFEDC577),
    onTertiary = Color(0xFF3C2C06),
    tertiaryContainer = Color(0xFF534017),
    onTertiaryContainer = Color(0xFFFFE3A7),
    background = Color(0xFF101A16),
    onBackground = Color(0xFFE0EDE4),
    surface = Color(0xFF101A16),
    onSurface = Color(0xFFE0EDE4),
    surfaceVariant = Color(0xFF3C4C44),
    onSurfaceVariant = Color(0xFFBBCBC1),
    surfaceContainerLowest = Color(0xFF0B1511),
    surfaceContainerLow = Color(0xFF17221C),
    surfaceContainer = Color(0xFF1B2821),
    surfaceContainerHigh = Color(0xFF25332B),
    surfaceContainerHighest = Color(0xFF303F36),
    outline = Color(0xFF879A8F),
    outlineVariant = Color(0xFF43564B),
    surfaceTint = Color(0xFF8EDABD),
    inverseSurface = Color(0xFFE0EDE4),
    inverseOnSurface = Color(0xFF293930),
    inversePrimary = Color(0xFF005746)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
    largeIncreased = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(44.dp),
    extraExtraLarge = RoundedCornerShape(52.dp)
)

internal val supportsPhoneColors: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NovaLogistikMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    usePhoneColors: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        usePhoneColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = NovaShapes,
        typography = Typography,
        content = content
    )
}
