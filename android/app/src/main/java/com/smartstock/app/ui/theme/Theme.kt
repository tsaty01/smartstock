package com.smartstock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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

/** Forest ink, paper and warm brass. The icon uses the same forest green. */
val Forest = Color(0xFF1A3A2A)
val Paper = Color(0xFFF5EDDA)
val Brass = Color(0xFFE9AE62)

private val Light = lightColorScheme(
	primary = Forest,
	onPrimary = Color.White,
	primaryContainer = Color(0xFFD1E6D3),
	onPrimaryContainer = Color(0xFF0B2C1C),
	secondary = Color(0xFF475F4C),
	onSecondary = Color.White,
	secondaryContainer = Color(0xFFDAE9D8),
	onSecondaryContainer = Color(0xFF1A3922),
	tertiary = Color(0xFF704D19),
	onTertiary = Color.White,
	tertiaryContainer = Brass,
	onTertiaryContainer = Color(0xFF301B00),
	error = Color(0xFFBA1A1A),
	onError = Color(0xFFFFFFFF),
	errorContainer = Color(0xFFFFDAD6),
	onErrorContainer = Color(0xFF410002),
	// Screens sit on the paper texture drawn by SmartStockTheme, so the base surfaces are see-through.
	background = Color.Transparent,
	onBackground = Color(0xFF202A20),
	surface = Color.Transparent,
	onSurface = Color(0xFF202A20),
	surfaceVariant = Color(0xFFE5DDC9),
	onSurfaceVariant = Color(0xFF4B5646),
	surfaceContainerLowest = Color(0xFFFFFAEE),
	surfaceContainerLow = Color(0xFFF1E9D7),
	surfaceContainer = Color(0xFFEBE1CC),
	surfaceContainerHigh = Color(0xFFE4DAC4),
	surfaceContainerHighest = Color(0xFFDCD0B8),
	outline = Color(0xFF677463),
	outlineVariant = Color(0xFFC6CFBE),
)

private val Dark = darkColorScheme(
	primary = Color(0xFFB9DABA),
	onPrimary = Color(0xFF102B1B),
	primaryContainer = Color(0xFF244A32),
	onPrimaryContainer = Color(0xFFD5EAD3),
	secondary = Color(0xFFC5D7BD),
	onSecondary = Color(0xFF253A27),
	secondaryContainer = Color(0xFF304937),
	onSecondaryContainer = Color(0xFFE1EEDB),
	tertiary = Brass,
	onTertiary = Color(0xFF301B00),
	tertiaryContainer = Brass,
	onTertiaryContainer = Color(0xFF301B00),
	error = Color(0xFFFFB4AB),
	onError = Color(0xFF690005),
	errorContainer = Color(0xFF93000A),
	onErrorContainer = Color(0xFFFFDAD6),
	background = Color.Transparent,
	onBackground = Color(0xFFF1EAD8),
	surface = Color.Transparent,
	onSurface = Color(0xFFF1EAD8),
	surfaceVariant = Color(0xFF334535),
	onSurfaceVariant = Color(0xFFC6D1C0),
	surfaceContainerLowest = Color(0xFF101811),
	surfaceContainerLow = Color(0xFF2C4232),
	surfaceContainer = Color(0xFF334A39),
	surfaceContainerHigh = Color(0xFF3C5340),
	surfaceContainerHighest = Color(0xFF465E49),
	outline = Color(0xFFABBDA9),
	outlineVariant = Color(0xFF465E48),
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
		) {
			Box(if (dark) Modifier.fillMaxSize().paper(dark = true) else Modifier.fillMaxSize().paper()) { content() }
		}
	}
}

val statusColors: StatusColors
	@Composable get() = LocalStatusColors.current
