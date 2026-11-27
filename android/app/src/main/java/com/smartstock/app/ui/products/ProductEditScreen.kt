package com.smartstock.app.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.Category
import com.smartstock.app.data.ProductRequest
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.ScanButton
import com.smartstock.app.ui.components.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

val WARNING_LABELS = listOf(
	"Prescription Required", "Harmful to Kids", "Age 18+ Only", "Allergen: Contains Nuts",
	"Refrigerate After Opening", "Restricted Drug",
)

/** Editable product fields as text, so partial input survives recomposition and process death. */
data class ProductForm(
	val name: String = "",
	val categoryId: Long? = null,
	val price: String = "",
	val initialStock: String = "0",
	val reorderLevel: String = "10",
	val barcode: String = "",
	val expiryDate: String? = null,
	val warningLabel: String? = null,
	val description: String = "",
	val active: Boolean = true,
) {
	val priceValue get() = price.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
	val valid get() = name.isNotBlank() && categoryId != null && priceValue != null &&
		reorderLevel.toIntOrNull() != null && initialStock.toIntOrNull() != null

	fun toRequest(isNew: Boolean) = ProductRequest(
		categoryId = categoryId!!,
		name = name.trim(),
		description = description.ifBlank { null },
		barcode = barcode.trim().ifBlank { null },
		price = priceValue!!,
		reorderLevel = reorderLevel.toInt(),
		expiryDate = expiryDate,
		warningLabel = warningLabel,
		active = active,
		initialStock = if (isNew) initialStock.toInt() else null,
	)
}

class ProductEditViewModel(private val backend: Backend, private val id: Long) : ViewModel() {

	var categories by mutableStateOf<Loadable<List<Category>>>(Loadable.Loading)
		private set

	var form by mutableStateOf(ProductForm())

	var busy by mutableStateOf(false)
		private set

	var error by mutableStateOf<String?>(null)
		private set

	init {
		load()
	}

	fun load() {
		viewModelScope.launch {
			categories = try {
				if (id != 0L) {
					val p = backend.call { product(id) }
					form = ProductForm(
						p.name, p.categoryId, p.price.toPlainString(), "0", p.reorderLevel.toString(), p.barcode.orEmpty(),
						p.expiryDate, p.warningLabel, p.description.orEmpty(), p.active,
					)
				}
				Loadable.Ready(backend.call { categories() })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}

	fun save(onDone: () -> Unit) {
		busy = true
		error = null
		viewModelScope.launch {
			try {
				val request = form.toRequest(isNew = id == 0L)
				if (id == 0L) backend.call { createProduct(request) } else backend.call { updateProduct(id, request) }
				onDone()
			} catch (e: ApiError) {
				error = e.message
			}
			busy = false
		}
	}
}

@Composable
fun ProductEditScreen(id: Long, onDone: () -> Unit) {
	val backend = LocalBackend.current
	val vm = viewModel { ProductEditViewModel(backend, id) }
	val form = vm.form
	fun update(change: ProductForm.() -> ProductForm) {
		vm.form = vm.form.change()
	}
	var pickDate by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(if (id == 0L) "New product" else "Edit product") },
				navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
			)
		},
	) { padding ->
		LoadableContent(vm.categories, onRetry = vm::load) { categories ->
			Column(
				Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				OutlinedTextField(form.name, { v -> update { copy(name = v) } }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
				Choice(
					label = "Category",
					selected = categories.firstOrNull { it.id == form.categoryId }?.name,
					options = categories.map { it.name },
					onSelect = { index -> update { copy(categoryId = categories[index].id) } },
				)
				Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					NumberField("Price (₹)", form.price, Modifier.weight(1f), decimal = true) { v -> update { copy(price = v) } }
					NumberField("Reorder level", form.reorderLevel, Modifier.weight(1f)) { v -> update { copy(reorderLevel = v) } }
				}
				if (id == 0L) {
					NumberField("Opening stock", form.initialStock, Modifier.fillMaxWidth()) { v -> update { copy(initialStock = v) } }
				}
				OutlinedTextField(
					form.barcode, { v -> update { copy(barcode = v) } }, Modifier.fillMaxWidth(),
					label = { Text("Barcode") },
					trailingIcon = { ScanButton { code -> update { copy(barcode = code) } } },
					singleLine = true,
				)
				OutlinedTextField(
					value = form.expiryDate?.let(::formatDate) ?: "No expiry",
					onValueChange = {},
					readOnly = true,
					modifier = Modifier.fillMaxWidth(),
					label = { Text("Expiry date") },
					trailingIcon = {
						Row {
							if (form.expiryDate != null) {
								IconButton(onClick = { update { copy(expiryDate = null) } }) { Icon(Icons.Outlined.Clear, "Clear expiry") }
							}
							IconButton(onClick = { pickDate = true }) { Icon(Icons.Outlined.CalendarMonth, "Pick expiry date") }
						}
					},
				)
				Choice(
					label = "Warning label",
					selected = form.warningLabel ?: "None",
					options = listOf("None") + WARNING_LABELS,
					onSelect = { index -> update { copy(warningLabel = if (index == 0) null else WARNING_LABELS[index - 1]) } },
				)
				OutlinedTextField(form.description, { v -> update { copy(description = v) } }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2)
				ListItem(
					content = { Text("For sale") },
					supportingContent = { Text("Inactive products are hidden from the till") },
					trailingContent = { Switch(form.active, { v -> update { copy(active = v) } }) },
				)
				vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
				Button(onClick = { vm.save(onDone) }, Modifier.fillMaxWidth(), enabled = form.valid && !vm.busy) {
					if (vm.busy) LoadingIndicator(Modifier.size(24.dp)) else Text("Save")
				}
			}
		}
	}

	if (pickDate) {
		val initial = form.expiryDate?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
		val state = rememberDatePickerState(initialSelectedDateMillis = initial)
		DatePickerDialog(
			onDismissRequest = { pickDate = false },
			confirmButton = {
				TextButton(onClick = {
					state.selectedDateMillis?.let { millis ->
						val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
						update { copy(expiryDate = date.toString()) }
					}
					pickDate = false
				}) { Text("OK") }
			},
			dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
		) { DatePicker(state) }
	}
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, decimal: Boolean = false, onChange: (String) -> Unit) {
	OutlinedTextField(
		value,
		{ v -> onChange(v.filter { it.isDigit() || (decimal && it == '.') }) },
		modifier,
		label = { Text(label) },
		singleLine = true,
		keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
	)
}

@Composable
private fun Choice(label: String, selected: String?, options: List<String>, onSelect: (Int) -> Unit) {
	var expanded by rememberSaveable { mutableStateOf(false) }
	ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
		OutlinedTextField(
			value = selected.orEmpty(),
			onValueChange = {},
			readOnly = true,
			label = { Text(label) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
			modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
		)
		DropdownMenu(expanded, onDismissRequest = { expanded = false }, Modifier.exposedDropdownSize()) {
			options.forEachIndexed { index, option ->
				DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
			}
		}
	}
}
