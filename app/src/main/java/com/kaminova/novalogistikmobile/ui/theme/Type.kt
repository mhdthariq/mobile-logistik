package com.kaminova.novalogistikmobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.kaminova.novalogistikmobile.R

private val jakarta = FontFamily(Font(R.font.plus_jakarta_sans))
private val defaults = Typography()

val Typography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = jakarta),
    displayMedium = defaults.displayMedium.copy(fontFamily = jakarta),
    displaySmall = defaults.displaySmall.copy(fontFamily = jakarta),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = jakarta),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = jakarta),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = jakarta),
    titleLarge = defaults.titleLarge.copy(fontFamily = jakarta),
    titleMedium = defaults.titleMedium.copy(fontFamily = jakarta),
    titleSmall = defaults.titleSmall.copy(fontFamily = jakarta),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = jakarta),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = jakarta),
    bodySmall = defaults.bodySmall.copy(fontFamily = jakarta),
    labelLarge = defaults.labelLarge.copy(fontFamily = jakarta),
    labelMedium = defaults.labelMedium.copy(fontFamily = jakarta),
    labelSmall = defaults.labelSmall.copy(fontFamily = jakarta)
)
