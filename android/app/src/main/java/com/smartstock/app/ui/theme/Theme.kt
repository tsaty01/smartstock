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

/** Brand palette: Deep Teal, Cream and Orange (GOAL.md section 9). */
val DeepTeal = Color(0xFF013230)
val Cream = Color(0xFFF3EBDD)
val Orange = Color(0xFFFF6A1F)

private val Light = lightColorScheme(
	primary = DeepTeal,
	onPrimary = Cream,
	primaryContainer = Color(0xFFB3E3DC),
	onPrimaryContainer = Color(0xFF00201E),
	secondary = Color(0xFF4A6361),
	onSecondary = Color(0xFFFFFFFF),
	secondaryContainer = Color(0xFFD3E6DF),
	onSecondaryContainer = Color(0xFF051F1D),
	tertiary = Color(0xFFA33F00),
	onTertiary = Color(0xFFFFFFFF),
	tertiaryContainer = Orange,
	onTertiaryContainer = Color(0xFF1F0800),
	error = Color(0xFFBA1A1A),
	onError = Color(0xFFFFFFFF),
	errorContainer = Color(0xFFFFDAD6),
	onErrorContainer = Color(0xFF410002),
	background = Cream,
	onBackground = Color(0xFF1C1B17),
	surface = Cream,
	onSurface = Color(0xFF1C1B17),
	surfaceVariant = Color(0xFFE5DCCB),
	onSurfaceVariant = Color(0xFF4A4740),
	surfaceContainerLowest = Color(0xFFFFFBF5),
	surfaceContainerLow = Color(0xFFEEE5D6),
	surfaceContainer = Color(0xFFE8DFCF),
	surfaceContainerHigh = Color(0xFFE2D9C9),
	surfaceContainerHighest = Color(0xFFDCD3C3),
	outline = Color(0xFF7B776C),
	outlineVariant = Color(0xFFCFC6B6),
)

private val Dark = darkColorScheme(
	primary = Color(0xFF8CCFC6),
	onPrimary = Color(0xFF003733),
	primaryContainer = Color(0xFF1B4A47),
	onPrimaryContainer = Color(0xFFB3E3DC),
	secondary = Color(0xFFB1CCC8),
	onSecondary = Color(0xFF1C3532),
	secondaryContainer = Color(0xFF334B48),
	onSecondaryContainer = Color(0xFFD3E6DF),
	tertiary = Color(0xFFFFB596),
	onTertiary = Color(0xFF5A1B00),
	tertiaryContainer = Orange,
	onTertiaryContainer = Color(0xFF1F0800),
	error = Color(0xFFFFB4AB),
	onError = Color(0xFF690005),
	errorContainer = Color(0xFF93000A),
	onErrorContainer = Color(0xFFFFDAD6),
	background = Color(0xFF0B1A19),
	onBackground = Color(0xFFECE3D5),
	surface = Color(0xFF0B1A19),
	onSurface = Color(0xFFECE3D5),
	surfaceVariant = Color(0xFF3F4947),
	onSurfaceVariant = Color(0xFFBFC8C5),
	surfaceContainerLowest = Color(0xFF071413),
	surfaceContainerLow = Color(0xFF112221),
	surfaceContainer = Color(0xFF152726),
	surfaceContainerHigh = Color(0xFF1F3231),
	surfaceContainerHighest = Color(0xFF2A3D3B),
	outline = Color(0xFF8A9290),
	outlineVariant = Color(0xFF3F4947),
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
