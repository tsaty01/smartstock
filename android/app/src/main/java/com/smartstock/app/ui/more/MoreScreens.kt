package com.smartstock.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.BuildConfig
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.CategorySales
import com.smartstock.app.data.Notification
import com.smartstock.app.data.ProductSales
import com.smartstock.app.data.User
import com.smartstock.app.ui.AlertsRoute
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ProductRoute
import com.smartstock.app.ui.ReportsRoute
import com.smartstock.app.ui.UsersRoute
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.MessageBox
import com.smartstock.app.ui.components.formatInstant
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.theme.numeric
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(user: User, nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val scope = rememberCoroutineScope()
	val server by backend.serverUrl.collectAsStateWithLifecycle()
	LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
		item {
			ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
				Column(Modifier.padding(20.dp)) {
					Text(user.fullName, style = MaterialTheme.typography.headlineSmall)
					Text("@${user.username} · ${user.role.name.lowercase().replaceFirstChar(Char::titlecase)}")
					user.phone?.let { Text(it) }
					user.email?.let { Text(it) }
				}
			}
		}
		if (user.role.isManagement) {
			item { Entry(Icons.Outlined.Notifications, "Alerts", "Low stock and expiry") { nav.navigate(AlertsRoute) } }
			item { Entry(Icons.Outlined.Insights, "Reports", "Sales by product and category") { nav.navigate(ReportsRoute) } }
		}
		if (user.role == com.smartstock.app.data.Role.ADMIN) {
			item { Entry(Icons.Outlined.Group, "Users", "Accounts and roles") { nav.navigate(UsersRoute) } }
		}
		item { Entry(Icons.Outlined.Dns, "Server", server.orEmpty()) {} }
		item { Entry(Icons.AutoMirrored.Outlined.Logout, "Sign out", null) { scope.launch { backend.signOut() } } }
		item {
			Text(
				"SmartStock ${BuildConfig.VERSION_NAME} · Fonts: Inter, Source Serif 4 (SIL Open Font License)",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 16.dp),
			)
		}
	}
}

@Composable
private fun Entry(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
	ListItem(
		onClick = onClick,
		leadingContent = { Icon(icon, null) },
		content = { Text(title) },
		supportingContent = subtitle?.let { { Text(it) } },
	)
}

class AlertsViewModel(private val backend: Backend) : ViewModel() {

	var state by mutableStateOf<Loadable<List<Notification>>>(Loadable.Loading)
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
				Loadable.Ready(backend.call { notifications() })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
			refreshing = false
		}
	}

	fun markRead(id: Long) {
		viewModelScope.launch {
			runCatching { backend.call { markRead(id) } }
			load()
		}
	}
}

@Composable
fun AlertsScreen(nav: NavController) {
	val backend = LocalBackend.current
	val vm = viewModel { AlertsViewModel(backend) }
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Alerts") },
				navigationIcon = { IconButton(onClick = nav::popBackStack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
	) { padding ->
		PullToRefreshBox(vm.refreshing, onRefresh = { vm.load(refresh = true) }, Modifier.fillMaxSize().padding(padding)) {
			LoadableContent(vm.state, onRetry = { vm.load() }) { alerts ->
				if (alerts.isEmpty()) {
					MessageBox(Icons.Outlined.NotificationsNone, "No alerts")
				} else {
					LazyColumn {
						items(alerts, key = { it.id }) { alert ->
							ListItem(
								onClick = { alert.productId?.let { nav.navigate(ProductRoute(it)) } },
								content = {
									Text(alert.message, fontWeight = if (alert.read) FontWeight.Normal else FontWeight.SemiBold)
								},
								supportingContent = { Text(formatInstant(alert.createdAt)) },
								trailingContent = {
									if (!alert.read) TextButton(onClick = { vm.markRead(alert.id) }) { Text("Mark read") }
								},
							)
						}
					}
				}
			}
		}
	}
}

data class Reports(val products: List<ProductSales>, val categories: List<CategorySales>)

class ReportsViewModel(private val backend: Backend) : ViewModel() {

	var state by mutableStateOf<Loadable<Reports>>(Loadable.Loading)
		private set

	init {
		load()
	}

	fun load() {
		viewModelScope.launch {
			state = try {
				val categories = async { backend.call { categorySales() } }
				Loadable.Ready(Reports(backend.call { productSales() }, categories.await()))
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}
}

@Composable
fun ReportsScreen(onBack: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { ReportsViewModel(backend) }
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Last 30 days") },
				navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
	) { padding ->
		LoadableContent(vm.state, onRetry = vm::load) { reports ->
			LazyColumn(Modifier.fillMaxSize().padding(padding)) {
				item { Heading("By category") }
				items(reports.categories, key = { "c${it.id}" }) { c ->
					ListItem(content = { Text(c.name) },
						supportingContent = { Text("${c.units} units") },
						trailingContent = { Text(money(c.revenue), style = MaterialTheme.typography.titleMedium.numeric) },
					)
				}
				item { Heading("By product") }
				items(reports.products, key = { "p${it.id}" }) { p ->
					ListItem(
						content = { Text(p.name) },
						supportingContent = { Text("${p.category} · ${p.units} sold in ${p.orders} orders · ${p.stock} in stock") },
						trailingContent = { Text(money(p.revenue), style = MaterialTheme.typography.titleMedium.numeric) },
					)
				}
			}
		}
	}
}

@Composable
private fun Heading(text: String) {
	Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}
