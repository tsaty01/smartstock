package com.smartstock.app.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.Category
import com.smartstock.app.data.Product
import com.smartstock.app.data.User
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ProductEditRoute
import com.smartstock.app.ui.ProductRoute
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.MessageBox
import com.smartstock.app.ui.components.StockPill
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.theme.numeric
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

enum class StockFilter(val label: String, val matches: (Product) -> Boolean) {
	ALL("All", { true }),
	LOW("Low stock", { it.lowStock && it.stock > 0 }),
	OUT("Out of stock", { it.stock == 0 }),
	EXPIRING("Expiring", { (it.daysToExpiry ?: Long.MAX_VALUE) in 0..30 }),
	EXPIRED("Expired", { it.expired }),
	INACTIVE("Inactive", { !it.active }),
}

data class Catalog(val products: List<Product>, val categories: List<Category>)

class ProductsViewModel(private val backend: Backend, private val management: Boolean) : ViewModel() {

	var state by mutableStateOf<Loadable<Catalog>>(Loadable.Loading)
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
				val categories = async { backend.call { categories() } }
				val products = backend.call { products(sellable = !management, includeInactive = management) }
				Loadable.Ready(Catalog(products, categories.await()))
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
			refreshing = false
		}
	}
}

@Composable
fun ProductsScreen(user: User, nav: NavController, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	val management = user.role.isManagement
	val vm = viewModel(key = "products-$management") { ProductsViewModel(backend, management) }
	var query by rememberSaveable { mutableStateOf("") }
	var filter by rememberSaveable { mutableStateOf(StockFilter.ALL) }
	var category by rememberSaveable { mutableStateOf<Long?>(null) }
	val filters = if (management) StockFilter.entries else if (user.role.isStaff) StockFilter.entries.dropLast(1) else emptyList()

	Box(modifier.fillMaxSize()) {
		Column(Modifier.fillMaxSize()) {
			TopAppBar(title = {
				Text(if (user.role.isStaff) "Products" else "Shop", style = MaterialTheme.typography.headlineMedium)
			})
			OutlinedTextField(
				query, { query = it },
				Modifier.fillMaxWidth().padding(horizontal = 16.dp),
				placeholder = { Text("Search by name or barcode") },
				leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
				singleLine = true,
				shape = RoundedCornerShape(28.dp),
			)
			val catalog = (vm.state as? Loadable.Ready)?.value
			Row(
				Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				filters.forEach { f ->
					FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
				}
				catalog?.categories?.forEach { c ->
					FilterChip(
						selected = category == c.id,
						onClick = { category = if (category == c.id) null else c.id },
						label = { Text(c.name) },
					)
				}
			}
			PullToRefreshBox(vm.refreshing, onRefresh = { vm.load(refresh = true) }, Modifier.fillMaxSize()) {
				LoadableContent(vm.state, onRetry = { vm.load() }) { data ->
					val needle = query.trim().lowercase()
					val shown = data.products.filter { p ->
						filter.matches(p) && (category == null || p.categoryId == category) &&
							(needle.isEmpty() || p.name.lowercase().contains(needle) || p.barcode == query.trim())
					}
					if (shown.isEmpty()) {
						MessageBox(Icons.Outlined.Inventory2, "No products match")
					} else {
						LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
							items(shown, key = { it.id }) { product ->
								ProductRow(product, showStock = user.role.isStaff) { nav.navigate(ProductRoute(product.id)) }
							}
						}
					}
				}
			}
		}
		if (management) {
			ExtendedFloatingActionButton(
				onClick = { nav.navigate(ProductEditRoute()) },
				icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
				text = { Text("Add product") },
				modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
			)
		}
	}
}

@Composable
fun ProductRow(product: Product, showStock: Boolean, onClick: () -> Unit) {
	ListItem(
		onClick = onClick,
		leadingContent = { ProductImage(product, Modifier.size(56.dp)) },
		content = { Text(product.name, maxLines = 2) },
		supportingContent = {
			Text(
				listOfNotNull(product.categoryName, product.warningLabel).joinToString(" · "),
				maxLines = 1,
			)
		},
		trailingContent = {
			Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(money(product.price), style = MaterialTheme.typography.titleMedium.numeric)
				if (showStock) StockPill(product)
			}
		},
	)
}

@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier) {
	val backend = LocalBackend.current
	Surface(modifier.clip(RoundedCornerShape(16.dp)), color = MaterialTheme.colorScheme.secondaryContainer) {
		if (product.imageUrl != null) {
			AsyncImage(
				model = backend.absolute(product.imageUrl),
				contentDescription = null,
				contentScale = ContentScale.Crop,
				modifier = Modifier.fillMaxSize(),
			)
		} else {
			Box(contentAlignment = Alignment.Center) {
				Text(
					product.name.take(1).uppercase(),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.onSecondaryContainer,
				)
			}
		}
	}
}
