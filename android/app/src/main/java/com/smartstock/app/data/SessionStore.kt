package com.smartstock.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class Session(val token: String, val user: User, val serverUrl: String)

private val Context.dataStore by preferencesDataStore("session")

/**
 * Persists the server address and the signed-in session. The session (token and profile) is encrypted
 * with an AES key that never leaves the Android Keystore.
 */
class SessionStore(private val context: Context, private val json: Json, private val defaultServerUrl: String) {

	val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: defaultServerUrl }

	val session: Flow<Session?> = context.dataStore.data.map { prefs ->
		prefs[SESSION]?.let { sealed -> runCatching { json.decodeFromString<StoredSession>(open(sealed)) }.getOrNull() }
			?.let { Session(it.token, it.user, it.serverUrl.ifEmpty { prefs[SERVER_URL] ?: defaultServerUrl }) }
	}

	suspend fun setServerUrl(url: String) {
		val normalized = url.trim().trimEnd('/')
		context.dataStore.edit { prefs ->
			if (normalized != (prefs[SERVER_URL] ?: defaultServerUrl)) {
				prefs.remove(SESSION)
			}
			prefs[SERVER_URL] = normalized
		}
	}

	suspend fun save(token: String, user: User) {
		context.dataStore.edit { prefs ->
			val origin = prefs[SERVER_URL] ?: defaultServerUrl
			prefs[SESSION] = seal(json.encodeToString(StoredSession.serializer(), StoredSession(token, user, origin)))
		}
	}

	suspend fun updateUser(user: User) {
		context.dataStore.edit { prefs ->
			val current = prefs[SESSION]?.let { json.decodeFromString<StoredSession>(open(it)) } ?: return@edit
			prefs[SESSION] = seal(json.encodeToString(StoredSession.serializer(), current.copy(user = user)))
		}
	}

	suspend fun clear() {
		context.dataStore.edit { it.remove(SESSION) }
	}

	private fun seal(plain: String): String {
		val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
		val encrypted = cipher.doFinal(plain.toByteArray())
		return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
	}

	private fun open(sealed: String): String {
		val bytes = Base64.decode(sealed, Base64.NO_WRAP)
		val cipher = Cipher.getInstance(TRANSFORMATION)
		cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_LENGTH))
		return String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH))
	}

	private fun key(): SecretKey {
		val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
		(keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
		return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
			init(
				KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
					.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
					.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
					.build(),
			)
			generateKey()
		}
	}

	@kotlinx.serialization.Serializable
	private data class StoredSession(val token: String, val user: User, val serverUrl: String = "")

	private companion object {
		val SERVER_URL = stringPreferencesKey("server_url")
		val SESSION = stringPreferencesKey("session")
		const val KEYSTORE = "AndroidKeyStore"
		const val KEY_ALIAS = "smartstock-session"
		const val TRANSFORMATION = "AES/GCM/NoPadding"
		const val IV_LENGTH = 12
	}
}
