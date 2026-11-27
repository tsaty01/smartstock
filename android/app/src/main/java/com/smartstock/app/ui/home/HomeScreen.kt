package com.smartstock.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.abs
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

private val DockTabWidth = 64.dp
private val DockTabHeight = 48.dp
private val DockPillWidth = 56.dp
private val DockPillHeight = 40.dp
private val DockTabGap = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, nav: NavController) {
	val tabs = tabsFor(user.role)
	var selected by rememberSaveable { mutableStateOf(tabs.first()) }
	val colors = MaterialTheme.colorScheme
	val barColor = if (isSystemInDarkTheme()) colors.surfaceContainerLowest else colors.surfaceContainerLow
	val pillX by animateDpAsState(
		targetValue = (DockTabWidth + DockTabGap) * tabs.indexOf(selected) + (DockTabWidth - DockPillWidth) / 2,
		animationSpec = tween(220, easing = FastOutSlowInEasing),
		label = "Dock selection",
	)
	var dockVisible by rememberSaveable { mutableStateOf(true) }
	val threshold = with(LocalDensity.current) { 12.dp.toPx() }
	val scrollConnection = remember(threshold) {
		object : NestedScrollConnection {
			private var travel = 0f
			override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
				if (source != NestedScrollSource.UserInput || consumed.y == 0f) return Offset.Zero
				if (travel * consumed.y < 0f) travel = 0f
				travel += consumed.y
				if (abs(travel) >= threshold) {
					dockVisible = travel > 0f
					travel = 0f
				}
				return Offset.Zero
			}
		}
	}
	LaunchedEffect(selected) { dockVisible = true }
	CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
		Box(Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
			val content = Modifier.fillMaxSize()
		when (selected) {
			Tab.DASHBOARD -> DashboardScreen(nav, content)
			Tab.PRODUCTS, Tab.SHOP -> ProductsScreen(user, nav, content)
			Tab.SELL -> PosScreen(user, nav, content)
			Tab.ORDERS, Tab.PURCHASES -> OrdersScreen(user, nav, content)
			Tab.MORE -> MoreScreen(user, nav, content)
		}
		Box(
			Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
			contentAlignment = Alignment.BottomCenter,
		) {
			AnimatedVisibility(
				visible = dockVisible,
				enter = slideInVertically { it } + fadeIn(),
				exit = slideOutVertically { it } + fadeOut(),
			) {
				Surface(
					shape = RoundedCornerShape(40.dp),
					color = barColor.copy(alpha = 0.94f),
					shadowElevation = 12.dp,
				) {
				Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
					Box(
						Modifier.offset(x = pillX, y = (DockTabHeight - DockPillHeight) / 2)
							.size(DockPillWidth, DockPillHeight)
							.background(colors.primaryContainer, RoundedCornerShape(DockPillHeight / 2)),
					)
					Row(horizontalArrangement = Arrangement.spacedBy(DockTabGap)) {
					tabs.forEach { tab ->
						TooltipBox(
							positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
							tooltip = { PlainTooltip { Text(tab.label) } },
							state = rememberTooltipState(),
						) {
							Box(
								Modifier.size(DockTabWidth, DockTabHeight)
									.selectable(
										selected = tab == selected,
										interactionSource = remember { MutableInteractionSource() },
										indication = null,
										role = SemanticsRole.Tab,
										onClick = { selected = tab },
									)
									.semantics { contentDescription = tab.label },
								contentAlignment = Alignment.Center,
							) {
								Icon(tab.icon, contentDescription = null, tint = if (tab == selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
							}
						}
					}
					}
				}
				}
			}
		}
	}
	}
}
