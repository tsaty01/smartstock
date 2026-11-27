package com.smartstock.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.smartstock.app.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.RegisterRequest
import com.smartstock.app.ui.LocalBackend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(private val backend: Backend) : ViewModel() {

	var busy by mutableStateOf(false)
		private set

	var error by mutableStateOf<String?>(null)
		private set

	var serverUrl by mutableStateOf("")

	init {
		viewModelScope.launch { serverUrl = backend.serverUrl.first { it != null }!! }
	}

	fun signIn(username: String, password: String) = run { backend.signIn(username.trim(), password) }

	fun register(request: RegisterRequest) = run { backend.register(request) }

	private fun run(action: suspend () -> Unit) {
		busy = true
		error = null
		viewModelScope.launch {
			try {
				backend.setServerUrl(serverUrl)
				action()
			} catch (e: ApiError) {
				error = e.message
			} finally {
				busy = false
			}
		}
	}
}

@Composable
fun LoginScreen(onRegister: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { AuthViewModel(backend) }
	var username by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	var showServer by rememberSaveable { mutableStateOf(false) }
	val submit = { if (username.isNotBlank() && password.isNotEmpty()) vm.signIn(username, password) }

	Scaffold { padding ->
		Column(
			Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
		) {
			Image(painterResource(R.drawable.brand_mark), contentDescription = null, modifier = Modifier.size(72.dp))
			Text("SmartStock", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
			Text(
				"Inventory and billing for your store",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
				OutlinedTextField(
					username, { username = it }, Modifier.fillMaxWidth(),
					label = { Text("Username") },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
				)
				PasswordField(password, { password = it }, onDone = submit)
				AnimatedVisibility(showServer) {
					OutlinedTextField(
						vm.serverUrl, { vm.serverUrl = it }, Modifier.fillMaxWidth(),
						label = { Text("Server address") },
						supportingText = { Text("For example https://smartstock.example.com") },
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
					)
				}
				vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
				Button(onClick = submit, Modifier.fillMaxWidth(), enabled = !vm.busy) {
					if (vm.busy) LoadingIndicator(Modifier.size(24.dp)) else Text("Sign in")
				}
				TextButton(onClick = onRegister, Modifier.fillMaxWidth()) { Text("New customer? Create an account") }
				TextButton(onClick = { showServer = !showServer }, Modifier.fillMaxWidth()) {
					Icon(Icons.Outlined.Dns, contentDescription = null)
					Text("  Server: ${vm.serverUrl}", maxLines = 1)
				}
			}
		}
	}
}

@Composable
fun RegisterScreen(onBack: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { AuthViewModel(backend) }
	var fullName by rememberSaveable { mutableStateOf("") }
	var username by rememberSaveable { mutableStateOf("") }
	var phone by rememberSaveable { mutableStateOf("") }
	var email by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	val valid = fullName.isNotBlank() && username.length >= 3 && password.length >= 8 &&
		(phone.isEmpty() || phone.matches(Regex("[0-9]{10}")))
	val submit = {
		if (valid) {
			vm.register(RegisterRequest(username.trim(), password, fullName.trim(), phone.ifBlank { null }, email.ifBlank { null }))
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Create account") },
				navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
	) { padding ->
		Column(
			Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("See your purchases and receipts.", style = MaterialTheme.typography.bodyLarge)
			OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
			OutlinedTextField(
				username, { username = it }, Modifier.fillMaxWidth(),
				label = { Text("Username") },
				supportingText = { Text("At least 3 letters, digits, dots, dashes or underscores") },
				singleLine = true,
			)
			OutlinedTextField(
				phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(),
				label = { Text("Mobile number (optional)") },
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
			)
			OutlinedTextField(
				email, { email = it }, Modifier.fillMaxWidth(),
				label = { Text("Email (optional)") },
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
			)
			PasswordField(password, { password = it }, supporting = "At least 8 characters", onDone = submit)
			vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
			Button(onClick = submit, Modifier.fillMaxWidth(), enabled = valid && !vm.busy) {
				if (vm.busy) LoadingIndicator(Modifier.size(24.dp)) else Text("Create account")
			}
		}
	}
}

@Composable
fun PasswordField(
	value: String,
	onChange: (String) -> Unit,
	label: String = "Password",
	supporting: String? = null,
	onDone: () -> Unit = {},
) {
	var visible by rememberSaveable { mutableStateOf(false) }
	OutlinedTextField(
		value, onChange, Modifier.fillMaxWidth(),
		label = { Text(label) },
		supportingText = supporting?.let { { Text(it) } },
		singleLine = true,
		visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
		keyboardActions = KeyboardActions(onDone = { onDone() }),
		trailingIcon = {
			IconButton(onClick = { visible = !visible }) {
				Icon(
					if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
					contentDescription = if (visible) "Hide password" else "Show password",
				)
			}
		},
	)
}
