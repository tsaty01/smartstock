package com.smartstock.app.ui.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.BuildConfig
import com.smartstock.app.R
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.CategorySales
import com.smartstock.app.data.Notification
import com.smartstock.app.data.ProductSales
import com.smartstock.app.data.User
import com.smartstock.app.ui.AboutRoute
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
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun MoreScreen(user: User, nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val scope = rememberCoroutineScope()
	val server by backend.serverUrl.collectAsStateWithLifecycle()
	var editingServer by rememberSaveable { mutableStateOf(false) }
	var serverInput by rememberSaveable { mutableStateOf("") }
	LazyColumn(modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
		item { Entry(Icons.Outlined.Dns, "Server", server.orEmpty()) {
			serverInput = server.orEmpty()
			editingServer = true
		} }
		item { Entry(Icons.Outlined.Info, "About", "SmartStock ${BuildConfig.VERSION_NAME}") { nav.navigate(AboutRoute) } }
		item { Entry(Icons.AutoMirrored.Outlined.Logout, "Sign out", null) { scope.launch { backend.signOut() } } }
	}
	if (editingServer) {
		AlertDialog(
			onDismissRequest = { editingServer = false },
			title = { Text("Server address") },
			text = {
				OutlinedTextField(
					serverInput, { serverInput = it },
					label = { Text("HTTP or HTTPS address") },
					placeholder = { Text("http://127.0.0.1:8080") },
					singleLine = true,
				)
			},
			confirmButton = {
				TextButton(
					onClick = { scope.launch { backend.setServerUrl(serverInput); editingServer = false } },
					enabled = serverInput.trim().toHttpUrlOrNull() != null,
				) { Text("Save") }
			},
			dismissButton = { TextButton(onClick = { editingServer = false }) { Text("Cancel") } },
		)
	}
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("About") },
				navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
	) { padding ->
		Column(
			Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Image(painterResource(R.drawable.brand_mark), contentDescription = null, modifier = Modifier.size(64.dp))
			Text("SmartStock", style = MaterialTheme.typography.headlineLarge)
			Text("Inventory and billing for your store", style = MaterialTheme.typography.bodyLarge)
			Text(
				"Manage products, stock, sales and receipts on your Android device. Your SmartStock server decides prices and permissions.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
			Text("Project team", style = MaterialTheme.typography.titleMedium)
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text("Ishant Mehndiratta")
				Text("Satyam Tiwari")
				Text("Anshaj")
				Text("Aayush Bindal")
			}
			HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
			Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
			Text(
				"Inter and Source Serif 4 are provided under the SIL Open Font License.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
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
				val products = backend.call { productSales() }
				val categories = backend.call { categorySales() }
				Loadable.Ready(Reports(products, categories))
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
