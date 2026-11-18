package com.smartstock.app.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.smartstock.app.data.Role
import com.smartstock.app.data.User
import com.smartstock.app.ui.dashboard.DashboardScreen
import com.smartstock.app.ui.more.MoreScreen
import com.smartstock.app.ui.orders.OrdersScreen
import com.smartstock.app.ui.pos.PosScreen
import com.smartstock.app.ui.products.ProductsScreen

enum class Tab(val label: String, val icon: ImageVector) {
	DASHBOARD("Dashboard", Icons.Outlined.Dashboard),
	PRODUCTS("Products", Icons.Outlined.Inventory2),
	SHOP("Shop", Icons.Outlined.Storefront),
	SELL("Sell", Icons.Outlined.PointOfSale),
	ORDERS("Orders", Icons.AutoMirrored.Outlined.ReceiptLong),
	PURCHASES("Purchases", Icons.AutoMirrored.Outlined.ReceiptLong),
	MORE("More", Icons.Outlined.MoreHoriz),
}

/** Bottom navigation for the signed-in role; the server enforces the same limits. */
fun tabsFor(role: Role): List<Tab> = when (role) {
	Role.ADMIN, Role.MANAGER -> listOf(Tab.DASHBOARD, Tab.PRODUCTS, Tab.SELL, Tab.ORDERS, Tab.MORE)
	Role.CASHIER -> listOf(Tab.SELL, Tab.ORDERS, Tab.PRODUCTS, Tab.MORE)
	Role.CUSTOMER -> listOf(Tab.PURCHASES, Tab.SHOP, Tab.MORE)
}

@Composable
fun HomeScreen(user: User, nav: NavController) {
	val tabs = tabsFor(user.role)
	var selected by rememberSaveable { mutableStateOf(tabs.first()) }
	Scaffold(
		bottomBar = {
			NavigationBar {
				tabs.forEach { tab ->
					NavigationBarItem(
						selected = tab == selected,
						onClick = { selected = tab },
						icon = { Icon(tab.icon, contentDescription = null) },
						label = { Text(tab.label) },
					)
				}
			}
		},
	) { padding ->
		val modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
		when (selected) {
			Tab.DASHBOARD -> DashboardScreen(nav, modifier)
			Tab.PRODUCTS -> ProductsScreen(user, nav, modifier)
			Tab.SHOP -> ProductsScreen(user, nav, modifier)
			Tab.SELL -> PosScreen(user, nav, modifier)
			Tab.ORDERS, Tab.PURCHASES -> OrdersScreen(user, nav, modifier)
			Tab.MORE -> MoreScreen(user, nav, modifier)
		}
	}
}
