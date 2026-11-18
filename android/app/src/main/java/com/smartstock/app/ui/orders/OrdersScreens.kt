package com.smartstock.app.ui.orders

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.OrderSummary
import com.smartstock.app.data.Receipt
import com.smartstock.app.data.User
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ReceiptRoute
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.MessageBox
import com.smartstock.app.ui.components.formatInstant
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.theme.numeric
import java.math.BigDecimal
import kotlinx.coroutines.launch

class OrdersViewModel(private val backend: Backend) : ViewModel() {

	var state by mutableStateOf<Loadable<List<OrderSummary>>>(Loadable.Loading)
		private set

	var refreshing by mutableStateOf(false)
		private set

	var hasMore by mutableStateOf(false)
		private set

	private var page = 0

	init {
		load()
	}

	fun load(refresh: Boolean = false) {
		if (refresh) refreshing = true
		viewModelScope.launch {
			state = try {
				val result = backend.call { orders(page = 0) }
				page = 0
				hasMore = result.page.totalPages > 1
				Loadable.Ready(result.content)
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
			refreshing = false
		}
	}

	fun loadMore() {
		val current = (state as? Loadable.Ready)?.value ?: return
		viewModelScope.launch {
			runCatching { backend.call { orders(page = page + 1) } }.onSuccess { result ->
				page += 1
				hasMore = page + 1 < result.page.totalPages
				state = Loadable.Ready(current + result.content)
			}
		}
	}
}

@Composable
fun OrdersScreen(user: User, nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val vm = viewModel { OrdersViewModel(backend) }
	Column(modifier.fillMaxSize()) {
		TopAppBar(title = {
			Text(if (user.role.isStaff) "Orders" else "My purchases", style = MaterialTheme.typography.headlineMedium)
		})
		PullToRefreshBox(vm.refreshing, onRefresh = { vm.load(refresh = true) }, Modifier.fillMaxSize()) {
			LoadableContent(vm.state, onRetry = { vm.load() }) { orders ->
				if (orders.isEmpty()) {
					MessageBox(Icons.AutoMirrored.Outlined.ReceiptLong, if (user.role.isStaff) "No sales yet" else "No purchases yet")
				} else {
					LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
						items(orders, key = { it.id }) { order ->
							ListItem(
								onClick = { nav.navigate(ReceiptRoute(order.id)) },
								content = {
									Text(if (user.role.isStaff) "#${order.id} · ${order.customerName}" else "Bill #${order.id}")
								},
								supportingContent = {
									Text("${formatInstant(order.createdAt)} · ${order.itemCount} items · ${order.paymentMode.label}")
								},
								trailingContent = { Text(money(order.total), style = MaterialTheme.typography.titleMedium.numeric) },
							)
						}
						if (vm.hasMore) {
							item {
								OutlinedButton(onClick = vm::loadMore, Modifier.fillMaxWidth().padding(16.dp)) { Text("Load more") }
							}
						}
					}
				}
			}
		}
	}
}

class ReceiptViewModel(private val backend: Backend, private val id: Long) : ViewModel() {

	var state by mutableStateOf<Loadable<Receipt>>(Loadable.Loading)
		private set

	init {
		load()
	}

	fun load() {
		viewModelScope.launch {
			state = try {
				Loadable.Ready(backend.call { receipt(id) })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}
}

@Composable
fun ReceiptScreen(id: Long, onBack: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { ReceiptViewModel(backend, id) }
	val context = LocalContext.current
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Bill #$id") },
				navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
				actions = {
					(vm.state as? Loadable.Ready)?.value?.let { receipt ->
						IconButton(onClick = {
							val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receiptText(receipt))
							context.startActivity(Intent.createChooser(send, "Share receipt"))
						}) { Icon(Icons.Outlined.Share, "Share receipt") }
					}
				},
			)
		},
	) { padding ->
		LoadableContent(vm.state, onRetry = vm::load) { r ->
			LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
				item {
					ElevatedCard(shape = RoundedCornerShape(28.dp)) {
						Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							Text("SmartStock", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
							Text(formatInstant(r.createdAt), style = MaterialTheme.typography.bodyMedium)
							Text("Customer: ${r.customerName}" + (r.customerPhone?.let { " · $it" } ?: ""))
							Text("Cashier: ${r.cashierName}")
							Text("Payment: ${r.paymentMode.label}")
							HorizontalDivider(Modifier.padding(vertical = 8.dp))
							r.items.forEach { line ->
								Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
									Column(Modifier.weight(1f)) {
										Text(line.productName)
										Text(
											"${line.quantity} × ${money(line.unitPrice)}",
											style = MaterialTheme.typography.bodySmall.numeric,
											color = MaterialTheme.colorScheme.onSurfaceVariant,
										)
									}
									Text(money(line.lineTotal), style = MaterialTheme.typography.bodyLarge.numeric)
								}
							}
							HorizontalDivider(Modifier.padding(vertical = 8.dp))
							Total("Subtotal", r.subtotal)
							if (r.discount.signum() > 0) Total("Discount", r.discount.negate())
							Total("GST ${r.taxRate.stripTrailingZeros().toPlainString()}%", r.tax)
							Total("Total", r.total, emphasis = true)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun Total(label: String, amount: BigDecimal, emphasis: Boolean = false) {
	val style = if (emphasis) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
	Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(label, style = style)
		Text(money(amount), style = style.numeric)
	}
}

private fun receiptText(r: Receipt) = buildString {
	appendLine("SmartStock · Bill #${r.id}")
	appendLine(formatInstant(r.createdAt))
	appendLine("Customer: ${r.customerName}")
	appendLine()
	r.items.forEach { appendLine("${it.productName}  ${it.quantity} × ${money(it.unitPrice)} = ${money(it.lineTotal)}") }
	appendLine()
	appendLine("Subtotal ${money(r.subtotal)}")
	if (r.discount.signum() > 0) appendLine("Discount -${money(r.discount)}")
	appendLine("GST ${money(r.tax)}")
	append("Total ${money(r.total)} (${r.paymentMode.label})")
}
