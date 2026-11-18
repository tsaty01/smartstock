package com.smartstock.app.ui.products

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartstock.app.data.AdjustmentRequest
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.Movement
import com.smartstock.app.data.Product
import com.smartstock.app.data.RestockRequest
import com.smartstock.app.data.User
import com.smartstock.app.ui.LocalBackend
import com.smartstock.app.ui.ProductEditRoute
import com.smartstock.app.ui.components.Loadable
import com.smartstock.app.ui.components.LoadableContent
import com.smartstock.app.ui.components.StockPill
import com.smartstock.app.ui.components.formatDate
import com.smartstock.app.ui.components.formatInstant
import com.smartstock.app.ui.components.money
import com.smartstock.app.ui.theme.numeric
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class ProductDetail(val product: Product, val movements: List<Movement>)

class ProductDetailViewModel(private val backend: Backend, private val id: Long, private val management: Boolean) :
	ViewModel() {

	var state by mutableStateOf<Loadable<ProductDetail>>(Loadable.Loading)
		private set

	var message by mutableStateOf<String?>(null)

	var busy by mutableStateOf(false)
		private set

	init {
		load()
	}

	fun load() {
		viewModelScope.launch {
			state = try {
				val movements = async { if (management) backend.call { movements(id) }.content else emptyList() }
				Loadable.Ready(ProductDetail(backend.call { product(id) }, movements.await()))
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}

	fun restock(quantity: Int, notes: String) = act("Stock added") {
		backend.call { restock(id, RestockRequest(quantity, notes.ifBlank { null })) }
	}

	fun adjust(change: Int, reason: String, notes: String) = act("Stock adjusted") {
		backend.call { adjust(id, AdjustmentRequest(change, reason, notes)) }
	}

	fun uploadImage(context: Context, uri: Uri) = act("Image updated") {
		val resolver = context.contentResolver
		val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ApiError("Could not read the image")
		val body = bytes.toRequestBody(resolver.getType(uri)?.toMediaTypeOrNull())
		backend.call { uploadImage(id, MultipartBody.Part.createFormData("file", "image", body)) }
	}

	private fun act(done: String, action: suspend () -> Unit) {
		busy = true
		viewModelScope.launch {
			message = try {
				action()
				load()
				done
			} catch (e: ApiError) {
				e.message
			}
			busy = false
		}
	}
}

@Composable
fun ProductDetailScreen(id: Long, user: User, nav: NavController) {
	val backend = LocalBackend.current
	val management = user.role.isManagement
	val vm = viewModel { ProductDetailViewModel(backend, id, management) }
	val snackbar = remember { SnackbarHostState() }
	val context = LocalContext.current
	var sheet by rememberSaveable { mutableStateOf<String?>(null) }
	val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
		uri?.let { vm.uploadImage(context, it) }
	}
	LaunchedEffect(vm.message) {
		vm.message?.let { snackbar.showSnackbar(it); vm.message = null }
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Product") },
				navigationIcon = { IconButton(onClick = nav::popBackStack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
				actions = {
					if (management) {
						IconButton(onClick = { nav.navigate(ProductEditRoute(id)) }) { Icon(Icons.Outlined.Edit, "Edit product") }
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbar) },
	) { padding ->
		LoadableContent(vm.state, onRetry = vm::load) { detail ->
			val p = detail.product
			LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
				item {
					ProductImage(p, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
					Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(p.name, style = MaterialTheme.typography.headlineMedium)
						Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
							Text(money(p.price), style = MaterialTheme.typography.titleLarge.numeric)
							if (user.role.isStaff) StockPill(p)
						}
						Fact("Category", p.categoryName)
						p.barcode?.let { Fact("Barcode", it) }
						p.expiryDate?.let { Fact("Expires", formatDate(it) + (p.daysToExpiry?.let { d -> " ($d days)" } ?: "")) }
						p.warningLabel?.let { Fact("Warning", it) }
						if (user.role.isStaff) Fact("Reorder level", p.reorderLevel.toString())
						if (!p.active) Fact("Status", "Inactive, not for sale")
						p.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
					}
					if (management) {
						FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							Button(onClick = { sheet = "restock" }, enabled = !vm.busy && p.active) {
								Icon(Icons.Outlined.Inventory, null); Text("  Restock")
							}
							FilledTonalButton(onClick = { sheet = "adjust" }, enabled = !vm.busy) {
								Icon(Icons.Outlined.Tune, null); Text("  Adjust")
							}
							OutlinedButton(
								onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
								enabled = !vm.busy,
							) { Icon(Icons.Outlined.AddPhotoAlternate, null); Text("  Photo") }
						}
					}
				}
				if (detail.movements.isNotEmpty()) {
					item { Text("Stock history", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp)) }
					items(detail.movements, key = { it.id }) { m ->
						ListItem(
							content = { Text(m.reason.lowercase().replaceFirstChar(Char::titlecase).replace('_', ' ')) },
							supportingContent = {
								Text(listOfNotNull(m.performedBy, formatInstant(m.createdAt), m.notes, m.orderId?.let { "Order #$it" }).joinToString(" · "))
							},
							trailingContent = {
								Text(
									(if (m.change > 0) "+" else "") + m.change,
									style = MaterialTheme.typography.titleMedium.numeric,
									color = if (m.change > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
								)
							},
						)
					}
				}
			}
		}
	}

	when (sheet) {
		"restock" -> RestockSheet(onDismiss = { sheet = null }) { qty, notes -> vm.restock(qty, notes); sheet = null }
		"adjust" -> AdjustSheet(onDismiss = { sheet = null }) { change, reason, notes -> vm.adjust(change, reason, notes); sheet = null }
	}
}

@Composable
private fun Fact(label: String, value: String) {
	Row {
		Text("$label  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		Text(value, style = MaterialTheme.typography.bodyMedium)
	}
}

@Composable
private fun RestockSheet(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
	var quantity by rememberSaveable { mutableStateOf("") }
	var notes by rememberSaveable { mutableStateOf("") }
	val qty = quantity.toIntOrNull()?.takeIf { it > 0 }
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Text("Restock", style = MaterialTheme.typography.headlineSmall)
			OutlinedTextField(
				quantity, { quantity = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(),
				label = { Text("Quantity received") }, singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			)
			OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes, e.g. supplier invoice") })
			Button(onClick = { qty?.let { onConfirm(it, notes) } }, Modifier.fillMaxWidth(), enabled = qty != null) { Text("Add stock") }
		}
	}
}

@Composable
private fun AdjustSheet(onDismiss: () -> Unit, onConfirm: (Int, String, String) -> Unit) {
	var amount by rememberSaveable { mutableStateOf("") }
	var remove by rememberSaveable { mutableStateOf(true) }
	var expired by rememberSaveable { mutableStateOf(false) }
	var notes by rememberSaveable { mutableStateOf("") }
	val qty = amount.toIntOrNull()?.takeIf { it > 0 }
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Text("Adjust stock", style = MaterialTheme.typography.headlineSmall)
			SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
				SegmentedButton(remove && !expired, { remove = true; expired = false }, SegmentedButtonDefaults.itemShape(0, 3)) { Text("Remove") }
				SegmentedButton(!remove, { remove = false; expired = false }, SegmentedButtonDefaults.itemShape(1, 3)) { Text("Add") }
				SegmentedButton(expired, { remove = true; expired = true }, SegmentedButtonDefaults.itemShape(2, 3)) { Text("Expired") }
			}
			OutlinedTextField(
				amount, { amount = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(),
				label = { Text("Quantity") }, singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			)
			OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Reason (required)") })
			Button(
				onClick = {
					qty?.let { onConfirm(if (remove) -it else it, if (expired) "EXPIRED_REMOVAL" else "ADJUSTMENT", notes.trim()) }
				},
				Modifier.fillMaxWidth(),
				enabled = qty != null && notes.isNotBlank(),
			) { Text("Save adjustment") }
		}
	}
}
