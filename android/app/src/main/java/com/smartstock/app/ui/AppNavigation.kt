package com.smartstock.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
import com.smartstock.app.ui.more.AboutScreen
import com.smartstock.app.ui.more.AlertsScreen
import com.smartstock.app.ui.more.ReportsScreen
import com.smartstock.app.ui.more.UsersScreen
import com.smartstock.app.ui.orders.ReceiptScreen
import com.smartstock.app.ui.products.ProductDetailScreen
import com.smartstock.app.ui.products.ProductEditScreen
import com.smartstock.app.ui.theme.paper
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
@Serializable data object AboutRoute

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
	NavHost(
		nav, startDestination = LoginRoute,
		enterTransition = { pageEnter() }, exitTransition = { ExitTransition.None },
		popEnterTransition = { EnterTransition.None }, popExitTransition = { pageExit() },
		predictivePopEnterTransition = { _ -> EnterTransition.None },
		predictivePopExitTransition = { _ -> pageExit() },
	) {
		composable<LoginRoute> { PageSurface { LoginScreen(onRegister = { nav.navigate(RegisterRoute) }) } }
		composable<RegisterRoute> { PageSurface { RegisterScreen(onBack = nav::popBackStack) } }
	}
}

@Composable
private fun SignedInNavigation(user: User) {
	val nav = rememberNavController()
	NavHost(
		nav, startDestination = HomeRoute,
		enterTransition = { pageEnter() }, exitTransition = { ExitTransition.None },
		popEnterTransition = { EnterTransition.None }, popExitTransition = { pageExit() },
		predictivePopEnterTransition = { _ -> EnterTransition.None },
		predictivePopExitTransition = { _ -> pageExit() },
	) {
		composable<HomeRoute> { PageSurface { HomeScreen(user, nav) } }
		composable<ProductRoute> { entry ->
			PageSurface { ProductDetailScreen(entry.toRoute<ProductRoute>().id, user, nav) }
		}
		composable<ProductEditRoute> { entry ->
			PageSurface { ProductEditScreen(entry.toRoute<ProductEditRoute>().id, onDone = nav::popBackStack) }
		}
		composable<ReceiptRoute> { entry ->
			PageSurface { ReceiptScreen(entry.toRoute<ReceiptRoute>().id, onBack = nav::popBackStack) }
		}
		composable<AlertsRoute> { PageSurface { AlertsScreen(nav) } }
		composable<ReportsRoute> { PageSurface { ReportsScreen(onBack = nav::popBackStack) } }
		composable<UsersRoute> { PageSurface { UsersScreen(user, onBack = nav::popBackStack) } }
		composable<AboutRoute> { PageSurface { AboutScreen(onBack = nav::popBackStack) } }
	}
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnter(): EnterTransition =
	slideIntoContainer(SlideDirection.Left, tween(240, easing = FastOutSlowInEasing))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExit(): ExitTransition =
	slideOutOfContainer(SlideDirection.Right, tween(240, easing = FastOutSlowInEasing))

@Composable
private fun PageSurface(content: @Composable () -> Unit) {
	Box(Modifier.fillMaxSize().paper(isSystemInDarkTheme())) { content() }
}
