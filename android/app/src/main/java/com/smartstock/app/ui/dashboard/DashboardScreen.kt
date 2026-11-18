package com.smartstock.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.Dashboard
import com.smartstock.app.data.StockAlert
import com.smartstock.app.ui.AlertsRoute
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ProductRoute
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.StatusPill
import com.smartstock.app.ui.components.formatDate
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.theme.numeric
import com.smartstock.app.ui.theme.statusColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class DashboardViewModel(private val backend: Backend) : ViewModel() {

	var state by mutableStateOf<Loadable<Dashboard>>(Loadable.Loading)
		private set

	var refreshing by mutableStateOf(false)
		private set

	init {
		load()
	}

	fun load(refresh: Boolean = false) {
		if (refresh) refreshing = true
		viewModelScope.launch {
			state = try {
				Loadable.Ready(backend.call { dashboard() })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
			refreshing = false
		}
	}
}

@Composable
fun DashboardScreen(nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val vm = viewModel { DashboardViewModel(backend) }
	val unread = (vm.state as? Loadable.Ready)?.value?.unreadNotifications ?: 0
	Column(modifier.fillMaxSize()) {
		TopAppBar(
			title = { Text("Today", style = MaterialTheme.typography.headlineMedium) },
			actions = {
				IconButton(onClick = { nav.navigate(AlertsRoute) }) {
					BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
						Icon(Icons.Outlined.Notifications, contentDescription = "Alerts, $unread unread")
					}
				}
			},
		)
		PullToRefreshBox(vm.refreshing, onRefresh = { vm.load(refresh = true) }, Modifier.fillMaxSize()) {
			LoadableContent(vm.state, onRetry = { vm.load() }) { dashboard ->
				DashboardContent(dashboard, onProduct = { nav.navigate(ProductRoute(it)) })
			}
		}
	}
}

@Composable
private fun DashboardContent(d: Dashboard, onProduct: (Long) -> Unit) {
	LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
		item {
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Metric("Revenue", money(d.today.revenue), Modifier.weight(1f), highlight = true)
				Metric("Orders", d.today.orders.toString(), Modifier.weight(1f))
			}
		}
		item {
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				Metric("Customers", d.today.customers.toString(), Modifier.weight(1f))
				Metric("Discounts", money(d.today.discount), Modifier.weight(1f))
				Metric("Tax", money(d.today.tax), Modifier.weight(1f))
			}
		}
		item { SalesChart(d) }
		if (d.topProducts.isNotEmpty()) {
			item { SectionTitle("Top sellers, last 30 days") }
			items(d.topProducts, key = { "top-${it.id}" }) { p ->
				ListItem(
					content = { Text(p.name) },
					supportingContent = { Text("${p.units} sold") },
					trailingContent = { Text(money(p.revenue), style = MaterialTheme.typography.bodyLarge.numeric) },
					onClick = { onProduct(p.id) },
				)
			}
		}
		alertSection("Expired, remove from shelf", d.expired, onProduct) { alert ->
			StatusPill("Expired ${alert.expiryDate?.let(::formatDate).orEmpty()}", statusColors.danger, statusColors.onDanger)
		}
		alertSection("Expiring soon", d.expiringSoon, onProduct) { alert ->
			StatusPill(alert.expiryDate?.let(::formatDate).orEmpty(), statusColors.warning, statusColors.onWarning)
		}
		alertSection("Low stock", d.lowStock, onProduct) { alert ->
			StatusPill("${alert.stock} / ${alert.reorderLevel}", statusColors.warning, statusColors.onWarning)
		}
	}
}

private fun LazyListScope.alertSection(
	title: String,
	alerts: List<StockAlert>,
	onProduct: (Long) -> Unit,
	trailing: @Composable (StockAlert) -> Unit,
) {
	if (alerts.isEmpty()) return
	item { SectionTitle(title) }
	items(alerts, key = { "$title-${it.id}" }) { alert ->
		ListItem(
			content = { Text(alert.name) },
			supportingContent = { Text("${alert.stock} in stock") },
			trailingContent = { trailing(alert) },
			onClick = { onProduct(alert.id) },
		)
	}
}

@Composable
private fun SectionTitle(text: String) {
	Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
	val colors = MaterialTheme.colorScheme
	Card(modifier, shape = RoundedCornerShape(24.dp)) {
		Surface(color = if (highlight) colors.primaryContainer else colors.surfaceContainerHigh) {
			Column(Modifier.fillMaxWidth().padding(16.dp)) {
				Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
				Text(
					value,
					style = MaterialTheme.typography.titleLarge.numeric,
					maxLines = 1,
					autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize),
				)
			}
		}
	}
}

/** Revenue bars for the last six months, including months without sales. */
@Composable
private fun SalesChart(d: Dashboard) {
	val byMonth = d.monthlySales.associateBy { it.month }
	val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
	val revenue = months.map { byMonth[it.toString()]?.revenue ?: BigDecimal.ZERO }
	val max = revenue.max().takeIf { it.signum() > 0 } ?: BigDecimal.ONE
	ElevatedCard(shape = RoundedCornerShape(24.dp)) {
		Column(Modifier.padding(16.dp)) {
			Text("Monthly revenue", style = MaterialTheme.typography.titleMedium)
			Spacer(Modifier.height(12.dp))
			Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				months.forEachIndexed { index, month ->
					val fraction = revenue[index].divide(max, 4, RoundingMode.HALF_UP).toFloat().coerceAtLeast(0.02f)
					Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
						Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
							Surface(
								color = MaterialTheme.colorScheme.primary,
								shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
								modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction),
							) {}
						}
						Text(month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), style = MaterialTheme.typography.labelSmall)
					}
				}
			}
		}
	}
}
