package com.smartstock.app.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** The SmartStock REST API; see docs/api.md. */
interface SmartStockApi {

	@POST("api/auth/login")
	suspend fun login(@Body request: LoginRequest): TokenResponse

	@POST("api/auth/register")
	suspend fun register(@Body request: RegisterRequest): TokenResponse

	@GET("api/auth/me")
	suspend fun me(): User

	@GET("api/users")
	suspend fun users(): List<User>

	@POST("api/users")
	suspend fun createUser(@Body request: CreateUserRequest): User

	@PUT("api/users/{id}")
	suspend fun updateUser(@Path("id") id: Long, @Body request: UpdateUserRequest): User

	@GET("api/customers")
	suspend fun customers(@Query("q") query: String): List<User>

	@POST("api/customers")
	suspend fun registerCustomer(@Body request: RegisterRequest): User

	@GET("api/categories")
	suspend fun categories(): List<Category>

	@GET("api/products")
	suspend fun products(
		@Query("q") query: String? = null,
		@Query("categoryId") categoryId: Long? = null,
		@Query("sellable") sellable: Boolean = false,
		@Query("includeInactive") includeInactive: Boolean = false,
	): List<Product>

	@GET("api/products/{id}")
	suspend fun product(@Path("id") id: Long): Product

	@POST("api/products")
	suspend fun createProduct(@Body request: ProductRequest): Product

	@PUT("api/products/{id}")
	suspend fun updateProduct(@Path("id") id: Long, @Body request: ProductRequest): Product

	@POST("api/products/{id}/restock")
	suspend fun restock(@Path("id") id: Long, @Body request: RestockRequest): Product

	@POST("api/products/{id}/adjustments")
	suspend fun adjust(@Path("id") id: Long, @Body request: AdjustmentRequest): Product

	@Multipart
	@PUT("api/products/{id}/image")
	suspend fun uploadImage(@Path("id") id: Long, @Part file: MultipartBody.Part)

	@GET("api/stock-movements")
	suspend fun movements(@Query("productId") productId: Long, @Query("size") size: Int = 50): Page<Movement>

	@GET("api/notifications")
	suspend fun notifications(@Query("unread") unread: Boolean = false): List<Notification>

	@POST("api/notifications/{id}/read")
	suspend fun markRead(@Path("id") id: Long)

	@POST("api/orders/quote")
	suspend fun quote(@Body request: QuoteRequest): Quote

	@POST("api/orders")
	suspend fun checkout(@Body request: CheckoutRequest): Receipt

	@GET("api/orders")
	suspend fun orders(@Query("page") page: Int = 0, @Query("size") size: Int = 30): Page<OrderSummary>

	@GET("api/orders/{id}")
	suspend fun receipt(@Path("id") id: Long): Receipt

	@GET("api/reports/dashboard")
	suspend fun dashboard(): Dashboard

	@GET("api/reports/products")
	suspend fun productSales(): List<ProductSales>

	@GET("api/reports/categories")
	suspend fun categorySales(): List<CategorySales>
}
