package com.smartstock.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.smartstock.app.data.AuthState
import com.smartstock.app.data.Backend
import com.smartstock.app.data.User
import com.smartstock.app.ui.auth.LoginScreen
import com.smartstock.app.ui.auth.RegisterScreen
import com.smartstock.app.ui.home.HomeScreen
import com.smartstock.app.ui.more.AlertsScreen
import com.smartstock.app.ui.more.ReportsScreen
import com.smartstock.app.ui.more.UsersScreen
import com.smartstock.app.ui.orders.ReceiptScreen
import com.smartstock.app.ui.products.ProductDetailScreen
import com.smartstock.app.ui.products.ProductEditScreen
import kotlinx.serialization.Serializable

val LocalBackend = staticCompositionLocalOf<Backend> { error("Backend not provided") }

@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object HomeRoute
@Serializable data class ProductRoute(val id: Long)

/** [id] 0 creates a new product. */
@Serializable data class ProductEditRoute(val id: Long = 0)
@Serializable data class ReceiptRoute(val id: Long)
@Serializable data object AlertsRoute
@Serializable data object ReportsRoute
@Serializable data object UsersRoute

/** Signed-out and signed-in navigation; switching account resets the signed-in graph. */
@Composable
fun AppNavigation(backend: Backend) {
	val auth by backend.auth.collectAsStateWithLifecycle()
	CompositionLocalProvider(LocalBackend provides backend) {
		when (val state = auth) {
			AuthState.Unknown -> Unit
			AuthState.SignedOut -> SignedOutNavigation()
			is AuthState.SignedIn -> key(state.session.user.id) { SignedInNavigation(state.session.user) }
		}
	}
}

@Composable
private fun SignedOutNavigation() {
	val nav = rememberNavController()
	NavHost(nav, startDestination = LoginRoute) {
		composable<LoginRoute> { LoginScreen(onRegister = { nav.navigate(RegisterRoute) }) }
		composable<RegisterRoute> { RegisterScreen(onBack = nav::popBackStack) }
	}
}

@Composable
private fun SignedInNavigation(user: User) {
	val nav = rememberNavController()
	NavHost(nav, startDestination = HomeRoute) {
		composable<HomeRoute> { HomeScreen(user, nav) }
		composable<ProductRoute> {
			ProductDetailScreen(it.toRoute<ProductRoute>().id, user, nav)
		}
		composable<ProductEditRoute> {
			ProductEditScreen(it.toRoute<ProductEditRoute>().id, onDone = nav::popBackStack)
		}
		composable<ReceiptRoute> { ReceiptScreen(it.toRoute<ReceiptRoute>().id, onBack = nav::popBackStack) }
		composable<AlertsRoute> { AlertsScreen(nav) }
		composable<ReportsRoute> { ReportsScreen(onBack = nav::popBackStack) }
		composable<UsersRoute> { UsersScreen(user, onBack = nav::popBackStack) }
	}
}
