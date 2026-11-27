package com.smartstock.app.data

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/** Money travels as a JSON number and is held exactly. */
object BigDecimalSerializer : KSerializer<BigDecimal> {
	override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

	override fun deserialize(decoder: Decoder): BigDecimal =
		BigDecimal((decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content)

	override fun serialize(encoder: Encoder, value: BigDecimal) =
		(encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
}

typealias Money = @Serializable(with = BigDecimalSerializer::class) BigDecimal

enum class Role { ADMIN, MANAGER, CASHIER, CUSTOMER;

	val isStaff get() = this != CUSTOMER
	val isManagement get() = this == ADMIN || this == MANAGER
}

enum class PaymentMode(val label: String) { CASH("Cash"), CARD("Card"), UPI("UPI"), OTHER("Other") }

@Serializable
data class User(
	val id: Long,
	val username: String,
	val role: Role,
	val fullName: String,
	val phone: String? = null,
	val email: String? = null,
	val active: Boolean,
	val createdAt: String,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String, val expiresAt: String, val user: User)

@Serializable
data class RegisterRequest(
	val username: String,
	val password: String,
	val fullName: String,
	val phone: String? = null,
	val email: String? = null,
)

@Serializable
data class CreateUserRequest(
	val username: String,
	val password: String,
	val role: Role,
	val fullName: String,
	val phone: String? = null,
	val email: String? = null,
)

@Serializable
data class UpdateUserRequest(
	val role: Role,
	val fullName: String,
	val phone: String? = null,
	val email: String? = null,
	val active: Boolean,
	val password: String? = null,
)

@Serializable
data class Category(val id: Long, val name: String, val description: String? = null)

@Serializable
data class Product(
	val id: Long,
	val categoryId: Long,
	val categoryName: String,
	val name: String,
	val description: String? = null,
	val barcode: String? = null,
	val price: Money,
	val stock: Int,
	val reorderLevel: Int,
	val expiryDate: String? = null,
	val daysToExpiry: Long? = null,
	val warningLabel: String? = null,
	val imageUrl: String? = null,
	val active: Boolean,
	val lowStock: Boolean,
	val expired: Boolean,
)

@Serializable
data class ProductRequest(
	val categoryId: Long,
	val name: String,
	val description: String? = null,
	val barcode: String? = null,
	val price: Money,
	val reorderLevel: Int,
	val expiryDate: String? = null,
	val warningLabel: String? = null,
	val active: Boolean,
	val initialStock: Int? = null,
)

@Serializable
data class RestockRequest(val quantity: Int, val notes: String? = null)

@Serializable
data class AdjustmentRequest(val change: Int, val reason: String, val notes: String)

@Serializable
data class Movement(
	val id: Long,
	val productId: Long,
	val productName: String,
	val change: Int,
	val reason: String,
	val orderId: Long? = null,
	val performedBy: String,
	val notes: String? = null,
	val createdAt: String,
)

@Serializable
data class PageMeta(val size: Int, val number: Int, val totalElements: Long, val totalPages: Int)

@Serializable
data class Page<T>(val content: List<T>, val page: PageMeta)

@Serializable
data class Notification(
	val id: Long,
	val type: String,
	val message: String,
	val productId: Long? = null,
	val read: Boolean,
	val createdAt: String,
)

@Serializable
data class CartItem(val productId: Long, val quantity: Int)

@Serializable
data class QuoteRequest(val items: List<CartItem>, val discount: Money? = null)

@Serializable
data class CheckoutRequest(
	val customerId: Long,
	val items: List<CartItem>,
	val paymentMode: PaymentMode,
	val discount: Money? = null,
)

@Serializable
data class Line(
	val productId: Long,
	val productName: String,
	val quantity: Int,
	val unitPrice: Money,
	val lineTotal: Money,
)

@Serializable
data class Quote(
	val items: List<Line>,
	val subtotal: Money,
	val discount: Money,
	val discountSource: String,
	val taxRate: Money,
	val tax: Money,
	val total: Money,
)

@Serializable
data class Receipt(
	val id: Long,
	val createdAt: String,
	val customerId: Long,
	val customerName: String,
	val customerPhone: String? = null,
	val cashierName: String,
	val paymentMode: PaymentMode,
	val items: List<Line>,
	val subtotal: Money,
	val discount: Money,
	val taxRate: Money,
	val tax: Money,
	val total: Money,
)

@Serializable
data class OrderSummary(
	val id: Long,
	val createdAt: String,
	val customerId: Long,
	val customerName: String,
	val cashierName: String,
	val paymentMode: PaymentMode,
	val itemCount: Int,
	val total: Money,
)

@Serializable
data class Totals(val orders: Long, val revenue: Money, val tax: Money, val discount: Money, val customers: Long)

@Serializable
data class MonthlySales(val month: String, val revenue: Money, val orders: Long)

@Serializable
data class TopProduct(val id: Long, val name: String, val units: Long, val revenue: Money)

@Serializable
data class StockAlert(
	val id: Long,
	val name: String,
	val stock: Int,
	val reorderLevel: Int,
	val expiryDate: String? = null,
)

@Serializable
data class Dashboard(
	val today: Totals,
	val monthlySales: List<MonthlySales>,
	val topProducts: List<TopProduct>,
	val lowStock: List<StockAlert>,
	val expiringSoon: List<StockAlert>,
	val expired: List<StockAlert>,
	val unreadNotifications: Long,
)

@Serializable
data class ProductSales(
	val id: Long,
	val name: String,
	val category: String,
	val price: Money,
	val stock: Int,
	val units: Long,
	val revenue: Money,
	val orders: Long,
)

@Serializable
data class CategorySales(val id: Long, val name: String, val units: Long, val revenue: Money)

@Serializable
data class Problem(val title: String? = null, val status: Int? = null, val detail: String? = null)
