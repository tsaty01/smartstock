package com.smartstock.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.smartstock.app.data.Backend
import com.smartstock.app.data.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

class SmartStockApplication : Application(), SingletonImageLoader.Factory {

	lateinit var backend: Backend
		private set

	override fun onCreate() {
		super.onCreate()
		val json = Json {
			ignoreUnknownKeys = true
			explicitNulls = false
		}
		val scope = CoroutineScope(SupervisorJob())
		backend = Backend(SessionStore(this, json, BuildConfig.DEFAULT_SERVER_URL), json, scope)
	}

	/** Product images need the same server address and token as API calls. */
	override fun newImageLoader(context: android.content.Context): ImageLoader = ImageLoader.Builder(context)
		.components { add(OkHttpNetworkFetcherFactory(callFactory = { backend.http })) }
		.build()
}
