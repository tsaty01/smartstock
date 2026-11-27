package com.smartstock.app.data

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

sealed interface AuthState {
	data object Unknown : AuthState
	data object SignedOut : AuthState
	data class SignedIn(val session: Session) : AuthState
}

/** A failed call, with a message fit to show the user. */
class ApiError(message: String, val status: Int? = null) : Exception(message)

/**
 * Network access to the SmartStock server. The server address and token come from [SessionStore]; a 401
 * on an authenticated call ends the session so the app returns to sign-in.
 */
class Backend(private val sessions: SessionStore, private val json: Json, private val scope: CoroutineScope) {

	val auth: StateFlow<AuthState> = sessions.session
		.map { session -> session?.let(AuthState::SignedIn) ?: AuthState.SignedOut }
		.stateIn(scope, SharingStarted.Eagerly, AuthState.Unknown)

	val currentUser: User? get() = (auth.value as? AuthState.SignedIn)?.session?.user

	val serverUrl: StateFlow<String?> = sessions.serverUrl.stateIn(scope, SharingStarted.Eagerly, null)

	val http: OkHttpClient = OkHttpClient.Builder()
		.addInterceptor(Interceptor { chain ->
			val request = chain.request()
			val base = (serverUrl.value ?: runBlocking { sessions.serverUrl.first() }).toHttpUrl()
			val url = request.url.newBuilder().scheme(base.scheme).host(base.host).port(base.port).build()
			val session = (auth.value as? AuthState.SignedIn)?.session
			val token = session?.takeIf { it.serverUrl.toHttpUrl() == base }?.token
			val response = chain.proceed(
				request.newBuilder().url(url).apply { token?.let { header("Authorization", "Bearer $it") } }.build(),
			)
			if (response.code == 401 && token != null) {
				scope.launch { sessions.clear() }
			}
			response
		})
		.build()

	val api: SmartStockApi = Retrofit.Builder()
		.baseUrl("http://localhost/")
		.client(http)
		.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
		.build()
		.create(SmartStockApi::class.java)

	/** Absolute URL for server-relative paths such as product image links. */
	fun absolute(path: String): String = (serverUrl.value ?: "") + path

	/** Runs an API call and converts failures into an [ApiError] with the server's explanation. */
	suspend fun <T> call(block: suspend SmartStockApi.() -> T): T = try {
		api.block()
	} catch (e: HttpException) {
		val body = e.response()?.errorBody()?.string()
		val detail = body?.let { runCatching { json.decodeFromString(Problem.serializer(), it).detail }.getOrNull() }
		throw ApiError(detail ?: defaultMessage(e.code()), e.code())
	} catch (e: IOException) {
		throw ApiError("Cannot reach the server. Check the connection and server address.")
	} catch (e: IllegalArgumentException) {
		throw ApiError("Invalid server address")
	}

	suspend fun signIn(username: String, password: String) {
		val response = call { login(LoginRequest(username, password)) }
		sessions.save(response.token, response.user)
	}

	suspend fun register(request: RegisterRequest) {
		val response = call { register(request) }
		sessions.save(response.token, response.user)
	}

	suspend fun setServerUrl(url: String) = sessions.setServerUrl(url)

	suspend fun signOut() = sessions.clear()

	private fun defaultMessage(status: Int) = when (status) {
		401 -> "Your session has ended. Sign in again."
		403 -> "You do not have permission for this."
		404 -> "Not found."
		else -> "The server could not complete the request ($status)."
	}
}
