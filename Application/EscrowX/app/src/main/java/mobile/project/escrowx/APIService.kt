package mobile.project.escrowx

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import mobile.project.escrowx.auth.*
import mobile.project.escrowx.dash.*

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/auth/confirm")
    suspend fun confirmAccount(@Body request: ConfirmRequest): Response<ConfirmResponse>

    @POST("api/v1/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequestDto): Response<PasswordResetRequestResponse>

    @POST("api/v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmRequest): Response<PasswordResetConfirmResponse>

    @GET("api/v1/users/by-email/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): Response<UserDetailsResponse>

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardData(): Response<DashboardResponse>

    // Escrow endpoints
    @POST("api/v1/transactions")
    suspend fun createEscrow(@Body request: CreateEscrowRequest): Response<EscrowResponse>

    @GET("api/v1/transactions")
    suspend fun listTransactions(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("userId") userId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<EscrowResponse>>

    @GET("api/v1/transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: String): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/accept")
    suspend fun acceptTransaction(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/cancel")
    suspend fun cancelTransaction(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.3:8081/"

    val instance: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    fun authenticated(token: String): AuthApiService {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}

// Additional data classes
data class CreateEscrowRequest(
    val buyerId: String,
    val sellerId: String,
    val title: String,
    val amount: String,
    val currency: String = "KES",
    val deliveryDueAt: String? = null
)

data class EscrowResponse(
    val id: String,
    val reference: String,
    val buyerId: String,
    val sellerId: String,
    val title: String,
    val amount: String,
    val currency: String,
    val status: String,
    val deliveryDueAt: String? = null,
    val autoReleaseAt: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class PageResponse<T>(
    val content: List<T>,
    val pageable: PageableInfo,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: SortInfo,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
)

data class PageableInfo(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: SortInfo,
    val offset: Long,
    val paged: Boolean,
    val unpaged: Boolean
)

data class SortInfo(
    val empty: Boolean,
    val sorted: Boolean,
    val unsorted: Boolean
)