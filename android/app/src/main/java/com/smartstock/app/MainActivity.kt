package com.smartstock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartstock.app.data.AuthState
import com.smartstock.app.ui.AppNavigation
import com.smartstock.app.ui.theme.SmartStockTheme

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		val backend = (application as SmartStockApplication).backend
		installSplashScreen().setKeepOnScreenCondition { backend.auth.value == AuthState.Unknown }
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		setContent {
			SmartStockTheme {
				AppNavigation(backend)
			}
		}
	}
}
