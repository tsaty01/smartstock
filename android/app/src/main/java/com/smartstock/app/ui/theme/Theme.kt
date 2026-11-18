package com.smartstock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.smartstock.app.R

// Provisional palette: replace with the verified brand palette (GOAL.md section 9) when it is chosen.
private val Light = lightColorScheme(
	primary = Color(0xFF2E5E4E),
	onPrimary = Color(0xFFFFFFFF),
	primaryContainer = Color(0xFFB4EDD6),
	onPrimaryContainer = Color(0xFF002117),
	secondary = Color(0xFF4C6359),
	onSecondary = Color(0xFFFFFFFF),
	secondaryContainer = Color(0xFFCEE9DB),
	onSecondaryContainer = Color(0xFF092017),
	tertiary = Color(0xFF3F6375),
	onTertiary = Color(0xFFFFFFFF),
	tertiaryContainer = Color(0xFFC3E8FD),
	onTertiaryContainer = Color(0xFF001F2A),
	error = Color(0xFFBA1A1A),
	onError = Color(0xFFFFFFFF),
	errorContainer = Color(0xFFFFDAD6),
	onErrorContainer = Color(0xFF410002),
	background = Color(0xFFF7FAF7),
	onBackground = Color(0xFF181D1A),
	surface = Color(0xFFF7FAF7),
	onSurface = Color(0xFF181D1A),
	surfaceVariant = Color(0xFFDBE5DE),
	onSurfaceVariant = Color(0xFF404944),
	surfaceContainerLowest = Color(0xFFFFFFFF),
	surfaceContainerLow = Color(0xFFF1F5F1),
	surfaceContainer = Color(0xFFEBEFEB),
	surfaceContainerHigh = Color(0xFFE6E9E6),
	surfaceContainerHighest = Color(0xFFE0E3E0),
	outline = Color(0xFF707973),
	outlineVariant = Color(0xFFBFC9C2),
)

private val Dark = darkColorScheme(
	primary = Color(0xFF98D1BA),
	onPrimary = Color(0xFF003828),
	primaryContainer = Color(0xFF114F3C),
	onPrimaryContainer = Color(0xFFB4EDD6),
	secondary = Color(0xFFB2CCBF),
	onSecondary = Color(0xFF1E352C),
	secondaryContainer = Color(0xFF344C42),
	onSecondaryContainer = Color(0xFFCEE9DB),
	tertiary = Color(0xFFA7CCE0),
	onTertiary = Color(0xFF0A3445),
	tertiaryContainer = Color(0xFF264B5C),
	onTertiaryContainer = Color(0xFFC3E8FD),
	error = Color(0xFFFFB4AB),
	onError = Color(0xFF690005),
	errorContainer = Color(0xFF93000A),
	onErrorContainer = Color(0xFFFFDAD6),
	background = Color(0xFF0F1512),
	onBackground = Color(0xFFDEE4DF),
	surface = Color(0xFF0F1512),
	onSurface = Color(0xFFDEE4DF),
	surfaceVariant = Color(0xFF404944),
	onSurfaceVariant = Color(0xFFBFC9C2),
	surfaceContainerLowest = Color(0xFF0A0F0D),
	surfaceContainerLow = Color(0xFF171D1A),
	surfaceContainer = Color(0xFF1B211E),
	surfaceContainerHigh = Color(0xFF262B28),
	surfaceContainerHighest = Color(0xFF303633),
	outline = Color(0xFF89938D),
	outlineVariant = Color(0xFF404944),
)

/** Stock-status colours, kept apart from the brand scheme so they always mean the same thing. */
@Immutable
data class StatusColors(val ok: Color, val onOk: Color, val warning: Color, val onWarning: Color, val danger: Color, val onDanger: Color)

private val LightStatus = StatusColors(Color(0xFFD3F0DC), Color(0xFF0B3A1D), Color(0xFFFFE2A8), Color(0xFF3E2A00), Color(0xFFFFDAD6), Color(0xFF410002))
private val DarkStatus = StatusColors(Color(0xFF1F4D2F), Color(0xFFC0EBCB), Color(0xFF5A4300), Color(0xFFFFDF9A), Color(0xFF93000A), Color(0xFFFFDAD6))

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private fun inter(weight: Int) = Font(
	R.font.inter_variable,
	weight = FontWeight(weight),
	variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun serif(weight: Int) = Font(
	R.font.source_serif_4_variable,
	weight = FontWeight(weight),
	variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))
val SourceSerif = FontFamily(serif(400), serif(500), serif(600), serif(700))

private fun TextStyle.inter(weight: FontWeight? = fontWeight) = copy(fontFamily = Inter, fontWeight = weight)

private fun TextStyle.serif() = copy(fontFamily = SourceSerif)

private val Base = Typography()

/** Source Serif for display and headline roles; Inter for everything functional. */
private val AppTypography = Typography(
	displayLarge = Base.displayLarge.serif(),
	displayMedium = Base.displayMedium.serif(),
	displaySmall = Base.displaySmall.serif(),
	headlineLarge = Base.headlineLarge.serif(),
	headlineMedium = Base.headlineMedium.serif(),
	headlineSmall = Base.headlineSmall.serif(),
	titleLarge = Base.titleLarge.inter(FontWeight.SemiBold),
	titleMedium = Base.titleMedium.inter(FontWeight.SemiBold),
	titleSmall = Base.titleSmall.inter(),
	bodyLarge = Base.bodyLarge.inter(),
	bodyMedium = Base.bodyMedium.inter(),
	bodySmall = Base.bodySmall.inter(),
	labelLarge = Base.labelLarge.inter(),
	labelMedium = Base.labelMedium.inter(),
	labelSmall = Base.labelSmall.inter(),
)

/** Tabular figures so amounts and counts line up in columns. */
val TextStyle.numeric: TextStyle get() = copy(fontFeatureSettings = "tnum")

@Composable
fun SmartStockTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
	CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
		MaterialExpressiveTheme(
			colorScheme = if (dark) Dark else Light,
			motionScheme = MotionScheme.expressive(),
			typography = AppTypography,
			content = content,
		)
	}
}

val statusColors: StatusColors
	@Composable get() = LocalStatusColors.current
