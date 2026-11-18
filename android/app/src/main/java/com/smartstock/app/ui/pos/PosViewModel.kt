package com.smartstock.app.ui.pos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartstock.app.data.ApiError
import com.smartstock.app.data.Backend
import com.smartstock.app.data.CartItem
import com.smartstock.app.data.CheckoutRequest
import com.smartstock.app.data.PaymentMode
import com.smartstock.app.data.Product
import com.smartstock.app.data.Quote
import com.smartstock.app.data.QuoteRequest
import com.smartstock.app.data.RegisterRequest
import com.smartstock.app.data.User
import com.smartstock.app.ui.components.Loadable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Till state. Prices always come from the server's quote, so the app never computes totals, discounts
 * or tax itself.
 */
class PosViewModel(private val backend: Backend) : ViewModel() {

	var products by mutableStateOf<Loadable<List<Product>>>(Loadable.Loading)
		private set

	/** Product id to quantity, in the order items were added. */
	val cart = mutableStateMapOf<Long, Int>()

	var customer by mutableStateOf<User?>(null)

	var payment by mutableStateOf(PaymentMode.CASH)

	/** Manual discount text; blank applies the automatic discount rules. */
	var discount by mutableStateOf("")
		private set

	var quote by mutableStateOf<Quote?>(null)
		private set

	var quoteError by mutableStateOf<String?>(null)
		private set

	var message by mutableStateOf<String?>(null)

	var busy by mutableStateOf(false)
		private set

	private var quoteJob: Job? = null

	init {
		loadProducts()
	}

	fun loadProducts() {
		viewModelScope.launch {
			products = try {
				Loadable.Ready(backend.call { products(sellable = true) })
			} catch (e: ApiError) {
				Loadable.Failed(e.message.orEmpty())
			}
		}
	}

	fun product(id: Long): Product? = (products as? Loadable.Ready)?.value?.firstOrNull { it.id == id }

	fun add(product: Product) = setQuantity(product.id, (cart[product.id] ?: 0) + 1)

	/** Adds the product whose barcode matches exactly; returns false when none does. */
	fun addByBarcode(code: String): Boolean {
		val match = (products as? Loadable.Ready)?.value?.firstOrNull { it.barcode == code.trim() } ?: return false
		add(match)
		return true
	}

	fun setQuantity(productId: Long, quantity: Int) {
		if (quantity <= 0) cart.remove(productId) else cart[productId] = quantity
		requote()
	}

	fun updateDiscount(value: String) {
		discount = value.filter { it.isDigit() || it == '.' }
		requote()
	}

	fun clear() {
		cart.clear()
		customer = null
		discount = ""
		payment = PaymentMode.CASH
		requote()
	}

	private fun items() = cart.map { (id, qty) -> CartItem(id, qty) }

	private fun requote() {
		quoteJob?.cancel()
		if (cart.isEmpty()) {
			quote = null
			quoteError = null
			return
		}
		quoteJob = viewModelScope.launch {
			delay(250)
			try {
				quote = backend.call { quote(QuoteRequest(items(), discount.toBigDecimalOrNull())) }
				quoteError = null
			} catch (e: ApiError) {
				quote = null
				quoteError = e.message
			}
		}
	}

	fun checkout(onDone: (Long) -> Unit) {
		val buyer = customer ?: return
		busy = true
		viewModelScope.launch {
			try {
				val receipt = backend.call {
					checkout(CheckoutRequest(buyer.id, items(), payment, discount.toBigDecimalOrNull()))
				}
				clear()
				loadProducts()
				onDone(receipt.id)
			} catch (e: ApiError) {
				message = e.message
				requote()
			}
			busy = false
		}
	}

	suspend fun searchCustomers(query: String): List<User> = backend.call { customers(query) }

	suspend fun registerCustomer(request: RegisterRequest): User = backend.call { registerCustomer(request) }
}
