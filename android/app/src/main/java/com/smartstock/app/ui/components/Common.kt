package com.smartstock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartstock.app.data.Product
import com.smartstock.app.ui.theme.numeric
import com.smartstock.app.ui.theme.statusColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Screen data that loads from the server. */
sealed interface Loadable<out T> {
	data object Loading : Loadable<Nothing>
	data class Ready<T>(val value: T) : Loadable<T>
	data class Failed(val message: String) : Loadable<Nothing>
}

@Composable
fun <T> LoadableContent(state: Loadable<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
	when (state) {
		Loadable.Loading -> LoadingBox()
		is Loadable.Failed -> MessageBox(Icons.Outlined.CloudOff, state.message, actionLabel = "Try again", onAction = onRetry)
		is Loadable.Ready -> content(state.value)
	}
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
	Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		LoadingIndicator(Modifier.size(64.dp))
	}
}

@Composable
fun MessageBox(
	icon: ImageVector = Icons.Outlined.Inventory2,
	message: String,
	modifier: Modifier = Modifier,
	actionLabel: String? = null,
	onAction: (() -> Unit)? = null,
) {
	Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
		Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
			Icon(icon, contentDescription = null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
			Text(
				message,
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
			)
			if (actionLabel != null && onAction != null) {
				FilledTonalButton(onClick = onAction) { Text(actionLabel) }
			}
		}
	}
}

/** Small status label, e.g. stock or expiry state. */
@Composable
fun StatusPill(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
	Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
		Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium.numeric)
	}
}

private const val EXPIRY_SOON_DAYS = 7

@Composable
fun StockPill(product: Product) {
	val colors = statusColors
	when {
		product.expired -> StatusPill("Expired", colors.danger, colors.onDanger)
		product.stock == 0 -> StatusPill("Out of stock", colors.danger, colors.onDanger)
		product.lowStock -> StatusPill("${product.stock} left", colors.warning, colors.onWarning)
		(product.daysToExpiry ?: Long.MAX_VALUE) <= EXPIRY_SOON_DAYS ->
			StatusPill("Expires in ${product.daysToExpiry}d", colors.warning, colors.onWarning)
		else -> StatusPill("${product.stock} in stock", colors.ok, colors.onOk)
	}
}

private val rupees: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

fun money(amount: BigDecimal): String = rupees.format(amount)

private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

fun formatInstant(value: String): String =
	runCatching { dateTime.format(Instant.parse(value).atZone(ZoneId.systemDefault())) }.getOrDefault(value)

fun formatDate(value: String): String = runCatching { date.format(LocalDate.parse(value)) }.getOrDefault(value)
