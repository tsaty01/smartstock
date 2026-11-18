package com.smartstock.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.CreateUserRequest
import com.smartstock.app.data.Role
import com.smartstock.app.data.UpdateUserRequest
import com.smartstock.app.data.User
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.auth.PasswordField
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import kotlinx.coroutines.launch

class UsersViewModel(private val backend: Backend) : ViewModel() {

	var state by mutableStateOf<Loadable<List<User>>>(Loadable.Loading)
		private set

	var error by mutableStateOf<String?>(null)
		private set

	init {
		load()
	}

	fun load() {
		viewModelScope.launch {
			state = try {
				Loadable.Ready(backend.call { users() })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}

	fun save(existing: User?, form: UserForm, onDone: () -> Unit) {
		error = null
		viewModelScope.launch {
			try {
				if (existing == null) {
					backend.call {
						createUser(CreateUserRequest(form.username, form.password, form.role, form.fullName, form.phone.ifBlank { null }))
					}
				} else {
					backend.call {
						updateUser(
							existing.id,
							UpdateUserRequest(form.role, form.fullName, form.phone.ifBlank { null }, existing.email, form.active, form.password.ifBlank { null }),
						)
					}
				}
				load()
				onDone()
			} catch (e: ApiError) {
				error = e.message
			}
		}
	}
}

data class UserForm(
	val username: String = "",
	val fullName: String = "",
	val phone: String = "",
	val role: Role = Role.CASHIER,
	val active: Boolean = true,
	val password: String = "",
)

@Composable
fun UsersScreen(self: User, onBack: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { UsersViewModel(backend) }
	var editing by rememberSaveable { mutableStateOf<Long?>(null) }
	var creating by rememberSaveable { mutableStateOf(false) }
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Users") },
				navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = { creating = true },
				icon = { Icon(Icons.Outlined.PersonAdd, null) },
				text = { Text("Add user") },
			)
		},
	) { padding ->
		LoadableContent(vm.state, onRetry = vm::load) { users ->
			LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
				items(users, key = { it.id }) { user ->
					ListItem(
						onClick = { editing = user.id },
						content = { Text(user.fullName + if (user.id == self.id) " (you)" else "") },
						supportingContent = {
							Text("@${user.username} · ${user.role.name.lowercase()}" + if (!user.active) " · disabled" else "")
						},
					)
				}
			}
			val target = users.firstOrNull { it.id == editing }
			if (creating || target != null) {
				UserSheet(
					existing = target,
					error = vm.error,
					isSelf = target?.id == self.id,
					onDismiss = { creating = false; editing = null },
					onSave = { form -> vm.save(target, form) { creating = false; editing = null } },
				)
			}
		}
	}
}

@Composable
private fun UserSheet(existing: User?, error: String?, isSelf: Boolean, onDismiss: () -> Unit, onSave: (UserForm) -> Unit) {
	var form by remember(existing?.id) {
		mutableStateOf(
			existing?.let { UserForm(it.username, it.fullName, it.phone.orEmpty(), it.role, it.active) } ?: UserForm(),
		)
	}
	val valid = form.fullName.isNotBlank() && form.username.length >= 3 &&
		(if (existing == null) form.password.length >= 8 else form.password.isEmpty() || form.password.length >= 8)
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(
			Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(if (existing == null) "New user" else "Edit ${existing.username}", style = MaterialTheme.typography.headlineSmall)
			if (existing == null) {
				OutlinedTextField(form.username, { form = form.copy(username = it.trim()) }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
			}
			OutlinedTextField(form.fullName, { form = form.copy(fullName = it) }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
			OutlinedTextField(
				form.phone, { form = form.copy(phone = it.filter(Char::isDigit).take(10)) }, Modifier.fillMaxWidth(),
				label = { Text("Mobile number") }, singleLine = true,
			)
			SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
				Role.entries.forEachIndexed { index, role ->
					SegmentedButton(
						selected = form.role == role,
						onClick = { if (!isSelf) form = form.copy(role = role) },
						shape = SegmentedButtonDefaults.itemShape(index, Role.entries.size),
						enabled = !isSelf,
					) { Text(role.name.lowercase().replaceFirstChar(Char::titlecase), maxLines = 1) }
				}
			}
			if (existing != null) {
				ListItem(
					content = { Text("Active") },
					supportingContent = { Text(if (isSelf) "You cannot disable yourself" else "Disabled users are signed out at once") },
					trailingContent = { Switch(form.active, { form = form.copy(active = it) }, enabled = !isSelf) },
				)
			}
			PasswordField(
				form.password, { form = form.copy(password = it) },
				label = if (existing == null) "Password" else "New password (optional)",
				supporting = "At least 8 characters",
			)
			error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
			Button(onClick = { onSave(form) }, Modifier.fillMaxWidth(), enabled = valid) { Text("Save") }
		}
	}
}
