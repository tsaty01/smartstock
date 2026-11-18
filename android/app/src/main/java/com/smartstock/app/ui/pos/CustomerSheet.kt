package com.smartstock.app.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.RegisterRequest
import com.smartstock.app.data.User
import com.smartstock.app.ui.auth.PasswordField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Find a customer, or register one without leaving the sale. */
@Composable
fun CustomerSheet(vm: PosViewModel, onDismiss: () -> Unit) {
	var registering by rememberSaveable { mutableStateOf(false) }
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			if (registering) {
				RegisterCustomer(vm, onDone = { vm.customer = it; onDismiss() }, onCancel = { registering = false })
			} else {
				FindCustomer(vm, onPick = { vm.customer = it; onDismiss() }, onRegister = { registering = true })
			}
		}
	}
}

@Composable
private fun FindCustomer(vm: PosViewModel, onPick: (User) -> Unit, onRegister: () -> Unit) {
	var query by rememberSaveable { mutableStateOf("") }
	var results by remember { mutableStateOf<List<User>>(emptyList()) }
	var error by remember { mutableStateOf<String?>(null) }
	LaunchedEffect(query) {
		if (query.trim().length < 2) {
			results = emptyList()
			return@LaunchedEffect
		}
		delay(300)
		try {
			results = vm.searchCustomers(query.trim())
			error = null
		} catch (e: ApiError) {
			error = e.message
		}
	}
	Text("Customer", style = MaterialTheme.typography.headlineSmall)
	OutlinedTextField(
		query, { query = it }, Modifier.fillMaxWidth(),
		placeholder = { Text("Name, phone or username") },
		leadingIcon = { Icon(Icons.Outlined.Search, null) },
		singleLine = true,
	)
	error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
	LazyColumn(Modifier.heightIn(max = 320.dp)) {
		items(results, key = { it.id }) { customer ->
			ListItem(
				content = { Text(customer.fullName) },
				supportingContent = { Text(listOfNotNull(customer.phone, "@${customer.username}").joinToString(" · ")) },
				leadingContent = { Icon(Icons.Outlined.Person, null) },
				onClick = { onPick(customer) },
			)
		}
	}
	if (query.trim().length >= 2 && results.isEmpty() && error == null) {
		Text("No matching customer", color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
	TextButton(onClick = onRegister) {
		Icon(Icons.Outlined.PersonAdd, null)
		Text("  Register a new customer")
	}
}

@Composable
private fun RegisterCustomer(vm: PosViewModel, onDone: (User) -> Unit, onCancel: () -> Unit) {
	val scope = rememberCoroutineScope()
	var fullName by rememberSaveable { mutableStateOf("") }
	var phone by rememberSaveable { mutableStateOf("") }
	var username by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	var error by remember { mutableStateOf<String?>(null) }
	var busy by remember { mutableStateOf(false) }
	val valid = fullName.isNotBlank() && username.length >= 3 && password.length >= 8 &&
		(phone.isEmpty() || phone.length == 10)

	Text("New customer", style = MaterialTheme.typography.headlineSmall)
	OutlinedTextField(fullName, { fullName = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
	OutlinedTextField(
		phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(),
		label = { Text("Mobile number") }, singleLine = true,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
	)
	OutlinedTextField(username, { username = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
	PasswordField(password, { password = it }, label = "Temporary password", supporting = "The customer signs in with this")
	error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
	Button(
		onClick = {
			busy = true
			scope.launch {
				try {
					onDone(vm.registerCustomer(RegisterRequest(username, password, fullName.trim(), phone.ifBlank { null })))
				} catch (e: ApiError) {
					error = e.message
				}
				busy = false
			}
		},
		Modifier.fillMaxWidth(),
		enabled = valid && !busy,
	) { Text("Register and select") }
	TextButton(onClick = onCancel) { Text("Back to search") }
}
