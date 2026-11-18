package com.smartstock.app.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.data.PaymentMode
import com.smartstock.app.data.User
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ReceiptRoute
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.MessageBox
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.products.ProductRow
import com.smartstock.app.ui.theme.numeric
import java.math.BigDecimal

@Composable
fun PosScreen(user: User, nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val vm = viewModel { PosViewModel(backend) }
	val snackbar = remember { SnackbarHostState() }
	var query by rememberSaveable { mutableStateOf("") }
	var pickCustomer by rememberSaveable { mutableStateOf(false) }
	LaunchedEffect(vm.message) {
		vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
	}

	Scaffold(
		modifier = modifier,
		topBar = {
			TopAppBar(
				title = { Text("Sell", style = MaterialTheme.typography.headlineMedium) },
				actions = {
					if (vm.cart.isNotEmpty() || vm.customer != null) {
						IconButton(onClick = vm::clear) { Icon(Icons.Outlined.DeleteSweep, "Clear sale") }
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbar) },
	) { padding ->
		Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
			OutlinedTextField(
				query, { query = it },
				Modifier.fillMaxWidth().padding(horizontal = 16.dp),
				placeholder = { Text("Search or scan a barcode") },
				leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
				singleLine = true,
				shape = RoundedCornerShape(28.dp),
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
				keyboardActions = KeyboardActions(onDone = {
					if (vm.addByBarcode(query)) query = "" else if (query.isNotBlank()) vm.message = "No sellable product with that barcode"
				}),
			)
			Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
				val buyer = vm.customer
				if (buyer == null) {
					AssistChip(
						onClick = { pickCustomer = true },
						label = { Text("Choose customer") },
						leadingIcon = { Icon(Icons.Outlined.PersonSearch, null) },
					)
				} else {
					InputChip(
						selected = true,
						onClick = { pickCustomer = true },
						label = { Text(buyer.fullName + (buyer.phone?.let { " · $it" } ?: "")) },
						leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) },
					)
				}
			}
			Column(Modifier.weight(1f)) {
				if (query.isNotBlank()) {
					LoadableContent(vm.products, onRetry = vm::loadProducts) { products ->
						val needle = query.trim().lowercase()
						val matches = products.filter { it.name.lowercase().contains(needle) || it.barcode == query.trim() }
						if (matches.isEmpty()) {
							MessageBox(message = "No sellable products match")
						} else {
							LazyColumn {
								items(matches, key = { it.id }) { product ->
									ProductRow(product, showStock = true) {
										vm.add(product)
										query = ""
									}
								}
							}
						}
					}
				} else {
					Cart(vm)
				}
			}
			if (query.isBlank()) {
				Summary(vm, user, onCheckout = { vm.checkout { id -> nav.navigate(ReceiptRoute(id)) } })
			}
		}
	}

	if (pickCustomer) {
		CustomerSheet(vm, onDismiss = { pickCustomer = false })
	}
}

@Composable
private fun Cart(vm: PosViewModel) {
	if (vm.cart.isEmpty()) {
		MessageBox(Icons.Outlined.ShoppingCart, "Search or scan products to add them to the sale")
		return
	}
	val lines = vm.quote?.items?.associateBy { it.productId }
	LazyColumn {
		items(vm.cart.keys.toList(), key = { it }) { id ->
			val quantity = vm.cart[id] ?: 0
			val product = vm.product(id)
			ListItem(
				content = { Text(lines?.get(id)?.productName ?: product?.name ?: "Product #$id") },
				supportingContent = {
					val price = lines?.get(id)?.unitPrice ?: product?.price
					price?.let { Text("${money(it)} each") }
				},
				trailingContent = {
					Row(verticalAlignment = Alignment.CenterVertically) {
						FilledTonalIconButton(onClick = { vm.setQuantity(id, quantity - 1) }) {
							Icon(Icons.Outlined.Remove, "Remove one")
						}
						Text(
							"$quantity",
							Modifier.padding(horizontal = 8.dp),
							style = MaterialTheme.typography.titleMedium.numeric,
						)
						FilledTonalIconButton(
							onClick = { vm.setQuantity(id, quantity + 1) },
							enabled = product == null || quantity < product.stock,
						) { Icon(Icons.Outlined.Add, "Add one") }
					}
				},
			)
		}
	}
}

@Composable
private fun Summary(vm: PosViewModel, user: User, onCheckout: () -> Unit) {
	if (vm.cart.isEmpty()) return
	ElevatedCard(Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(28.dp)) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			val q = vm.quote
			if (q != null) {
				Amount("Subtotal", q.subtotal)
				if (q.discount.signum() > 0) {
					Amount(if (q.discountSource == "RULE") "Discount (offer)" else "Discount", q.discount.negate())
				}
				Amount("GST ${q.taxRate.stripTrailingZeros().toPlainString()}%", q.tax)
				Amount("Total", q.total, emphasis = true)
			} else {
				vm.quoteError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: LoadingIndicator(Modifier.size(32.dp))
			}
			SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
				PaymentMode.entries.forEachIndexed { index, mode ->
					SegmentedButton(
						selected = vm.payment == mode,
						onClick = { vm.payment = mode },
						shape = SegmentedButtonDefaults.itemShape(index, PaymentMode.entries.size),
					) { Text(mode.label) }
				}
			}
			if (user.role.isManagement) {
				OutlinedTextField(
					vm.discount, vm::updateDiscount, Modifier.fillMaxWidth(),
					label = { Text("Manual discount (₹)") },
					supportingText = { Text("Leave empty to apply the best offer") },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
				)
			}
			Button(
				onClick = onCheckout,
				Modifier.fillMaxWidth(),
				enabled = vm.customer != null && q != null && !vm.busy,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.tertiaryContainer,
					contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
				),
			) {
				when {
					vm.busy -> LoadingIndicator(Modifier.size(24.dp))
					vm.customer == null -> Text("Choose a customer to check out")
					else -> Text("Charge ${q?.let { money(it.total) }.orEmpty()}")
				}
			}
		}
	}
}

@Composable
private fun Amount(label: String, value: BigDecimal, emphasis: Boolean = false) {
	val style = if (emphasis) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
	Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(label, style = style)
		Text(money(value), style = style.numeric)
	}
}
