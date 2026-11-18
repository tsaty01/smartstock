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

/** Brand palette: Navy, Glacier Blue and Lemon (GOAL.md section 9). */
val Navy = Color(0xFF0C1B48)
val GlacierBlue = Color(0xFF62A7EA)
val Lemon = Color(0xFFF2EA1E)

private val Light = lightColorScheme(
	primary = Navy,
	onPrimary = Color(0xFFFFFFFF),
	primaryContainer = Color(0xFFD6E7FB),
	onPrimaryContainer = Color(0xFF001B3D),
	secondary = Color(0xFF4E5F7C),
	onSecondary = Color(0xFFFFFFFF),
	secondaryContainer = Color(0xFFCFE3FA),
	onSecondaryContainer = Color(0xFF0A1C36),
	tertiary = Color(0xFF636000),
	onTertiary = Color(0xFFFFFFFF),
	tertiaryContainer = Lemon,
	onTertiaryContainer = Navy,
	error = Color(0xFFBA1A1A),
	onError = Color(0xFFFFFFFF),
	errorContainer = Color(0xFFFFDAD6),
	onErrorContainer = Color(0xFF410002),
	background = Color(0xFFF7F9FD),
	onBackground = Color(0xFF191C22),
	surface = Color(0xFFF7F9FD),
	onSurface = Color(0xFF191C22),
	surfaceVariant = Color(0xFFE0E2EC),
	onSurfaceVariant = Color(0xFF444852),
	surfaceContainerLowest = Color(0xFFFFFFFF),
	surfaceContainerLow = Color(0xFFF1F4FA),
	surfaceContainer = Color(0xFFEBEFF6),
	surfaceContainerHigh = Color(0xFFE5E9F1),
	surfaceContainerHighest = Color(0xFFDFE3EC),
	outline = Color(0xFF747883),
	outlineVariant = Color(0xFFC4C7D1),
)

private val Dark = darkColorScheme(
	primary = GlacierBlue,
	onPrimary = Color(0xFF00213F),
	primaryContainer = Color(0xFF1D3A6E),
	onPrimaryContainer = Color(0xFFD6E7FB),
	secondary = Color(0xFFB6C6E4),
	onSecondary = Color(0xFF20304C),
	secondaryContainer = Color(0xFF2A3C63),
	onSecondaryContainer = Color(0xFFDCE6F9),
	tertiary = Color(0xFFE5DD3A),
	onTertiary = Color(0xFF333100),
	tertiaryContainer = Lemon,
	onTertiaryContainer = Navy,
	error = Color(0xFFFFB4AB),
	onError = Color(0xFF690005),
	errorContainer = Color(0xFF93000A),
	onErrorContainer = Color(0xFFFFDAD6),
	background = Color(0xFF0B1430),
	onBackground = Color(0xFFE1E6F5),
	surface = Color(0xFF0B1430),
	onSurface = Color(0xFFE1E6F5),
	surfaceVariant = Color(0xFF3F4762),
	onSurfaceVariant = Color(0xFFC2C7D8),
	surfaceContainerLowest = Color(0xFF070E24),
	surfaceContainerLow = Color(0xFF111A38),
	surfaceContainer = Color(0xFF15203F),
	surfaceContainerHigh = Color(0xFF1F2A4A),
	surfaceContainerHighest = Color(0xFF2A3556),
	outline = Color(0xFF8D93A6),
	outlineVariant = Color(0xFF3F4762),
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
