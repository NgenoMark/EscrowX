package mobile.project.escrowx

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import mobile.project.escrowx.auth.*
import mobile.project.escrowx.dash.DashboardResponse
import java.net.URI
import java.util.concurrent.TimeUnit

interface AuthApiService {

    // AUTH ENDPOINTS
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

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<LogoutResponse>

    @POST("api/v1/notifications/devices/register")
    suspend fun registerDeviceToken(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Body request: RegisterDeviceTokenRequest
    ): Response<RegisterDeviceTokenResponse>

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null
    ): Response<PageResponse<InAppNotificationResponse>>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadNotificationsCount(
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<Map<String, Long>>

    @GET("api/v1/notifications/counts")
    suspend fun getNotificationCounts(
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<Map<String, Long>>

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") notificationId: String
    ): Response<NotificationStatusUpdateResponse>

    @PATCH("api/v1/notifications/{id}/archive")
    suspend fun archiveNotification(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") notificationId: String
    ): Response<NotificationStatusUpdateResponse>

    // USER ENDPOINTS
    @GET("api/v1/users/by-email/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): Response<UserDetailsResponse>

    @GET("api/v1/users/by-phone/{phone}")
    suspend fun getUserByPhone(@Path("phone") phone: String): Response<UserDetailsResponse>

    @GET("api/v1/users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserDetailsResponse>

    // UPDATE USER PROFILE (PATCH)
    @PATCH("api/v1/users/{id}/update_profile")
    suspend fun updateProfile(
        @Path("id") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserDetailsResponse>

    @Multipart
    @POST("api/v1/uploads/users/profile")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part,
        @Query("userId") userId: String?
    ): Response<UploadImageResponse>

    // DASHBOARD ENDPOINT (if available)
    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardData(): Response<DashboardResponse>

    // TRANSACTION/ESCROW ENDPOINTS (as per your backend)
    @POST("api/v1/transactions/create")
    suspend fun createEscrow(@Body request: CreateEscrowRequest): Response<EscrowResponse>
    @GET("api/v1/transactions/buyer/{buyerId}")
    suspend fun getTransactionsByBuyer(
        @Path("buyerId") buyerId: String,
        @Query("status") status: String? = null
    ): Response<List<EscrowResponse>>

    @GET("api/v1/transactions/seller/{sellerId}")
    suspend fun getTransactionsBySeller(
        @Path("sellerId") sellerId: String,
        @Query("status") status: String? = null
    ): Response<List<EscrowResponse>>

    @GET("api/v1/transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: String): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/decline-transaction")
    suspend fun declineTransaction(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/approve-transaction")
    suspend fun approveTransaction(
        @Path("id") id:String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>


    @POST("api/v1/transactions/{id}/accept-transaction")
    suspend fun acceptTransaction(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/cancel")
    suspend fun cancelTransaction(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/mark-in-delivery")
    suspend fun markInDelivery(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/confirm-receipt")
    suspend fun confirmReceipt(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    // Optional separate endpoints for buyer/seller confirm delivery
    @POST("api/v1/transactions/{id}/buyer-confirm-delivery")
    suspend fun buyerConfirmDelivery(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/transactions/{id}/seller-confirm-delivery")
    suspend fun sellerConfirmDelivery(
        @Path("id") id: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<EscrowResponse>

    @POST("api/v1/payments/escrows/{escrowId}/stk-push")
    suspend fun initiateStkPush(
        @Path("escrowId") escrowId: String,
        @Header("X-Actor-User-Id") actorUserId: String,
        @Body request: InitiateStkPushRequest
    ): Response<InitiateStkPushResponse>

    @GET("api/v1/payments/{paymentId}")
    suspend fun getPayment(
        @Path("paymentId") paymentId: String
    ): Response<PaymentResponse>

    @GET("api/v1/payments/intents/me")
    suspend fun getMyPaymentIntents(
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<List<PaymentIntentFinanceResponse>>

    @GET("api/v1/payments/payouts/me")
    suspend fun getMyPayouts(
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<List<PayoutFinanceResponse>>

    @POST("api/v1/payments/escrows/{escrowId}/release")
    suspend fun releasePayout(
        @Path("escrowId") escrowId: String,
        @Header("X-Actor-User-Id") actorUserId: String
    ): Response<ReleasePayoutResponse>

    // DISPUTE ENDPOINTS
    @POST("api/v1/disputes")
    suspend fun raiseDispute(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Body request: RaiseDisputeRequest
    ): Response<DisputeResponse>

    @Multipart
    @POST("api/v1/uploads/disputes")
    suspend fun uploadDisputeImage(
        @Part file: MultipartBody.Part,
        @Query("referenceId") referenceId: String?
    ): Response<UploadImageResponse>

    @GET("api/v1/disputes/{id}")
    suspend fun getDisputeById(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") id: String
    ): Response<DisputeDetailsResponse>

    @GET("api/v1/disputes/transaction/{transactionId}")
    suspend fun getDisputeByTransactionId(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("transactionId") transactionId: String
    ): Response<DisputeDetailsResponse>

    @POST("api/v1/disputes/{id}/evidence")
    suspend fun addDisputeEvidence(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") disputeId: String,
        @Body request: UpdateDisputeEvidenceRequest
    ): Response<DisputeDetailsResponse>

    @POST("api/v1/disputes/{id}/evidence/remove")
    suspend fun removeDisputeEvidence(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") disputeId: String,
        @Body request: UpdateDisputeEvidenceRequest
    ): Response<DisputeDetailsResponse>

    @POST("api/v1/disputes/{id}/close")
    suspend fun closeDispute(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") disputeId: String,
        @Body request: CloseDisputeRequest
    ): Response<DisputeDetailsResponse>

    @POST("api/v1/admin/disputes/{id}/action-required")
    suspend fun assignDisputeActionRequired(
        @Header("X-Actor-User-Id") actorUserId: String,
        @Path("id") disputeId: String,
        @Body request: ActionRequiredRequest
    ): Response<DisputeDetailsResponse>
}

object RetrofitClient {
    private const val BASE_URL = "https://mullets-handset-pampered.ngrok-free.dev"

    fun getBaseUrl(): String = BASE_URL

    fun resolveApiUrl(urlOrPath: String?): String? {
        val value = urlOrPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val parsedPath = try {
            URI(value).path
        } catch (_: Exception) {
            null
        }

        if (!parsedPath.isNullOrBlank() && parsedPath.startsWith("/uploads/")) {
            return BASE_URL.removeSuffix("/") + parsedPath
        }

        return when {
            value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("/") -> BASE_URL.removeSuffix("/") + value
            else -> BASE_URL.removeSuffix("/") + "/" + value
        }
    }

    fun toBackendRelativePath(urlOrPath: String?): String? {
        val value = urlOrPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val base = BASE_URL.removeSuffix("/")
        return if (value.startsWith(base, ignoreCase = true)) {
            value.removePrefix(base).ifBlank { "/" }
        } else {
            value
        }
    }

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: AuthApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApiService::class.java)

    fun authenticated(token: String): AuthApiService {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        val authClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}

// ---------- DATA CLASSES ----------
data class CreateEscrowRequest(
    val buyerId: String,
    val sellerId: String,
    val title: String,
    val productDescription: String,
    val amount: Double,
    val deliveryAddress: String,
    val currency: String? = null,
    val initialDepositAmount: Double? = null,
    val deliveryDueAt: String? = null
)

data class EscrowResponse(
    val id: String,
    val reference: String?,
    val buyerId: String,
    val sellerId: String,
    val title: String,
    val productDescription: String,
    val amount: Double,
    val deliveryAddress: String,
    val initialDepositAmount: Double?,
    val currency: String?,
    val status: String,
    val deliveryDueAt: String,
    val autoReleaseAt: String?,
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

data class UpdateProfileRequest(
    val displayName: String? = null,
    val phone: String? = null,
    val businessName: String? = null,
    val address: String? = null,          // ✅ match backend field name
    val shopLocation: String? = null,
    val avatarUrl: String? = null
)

// Dispute related
enum class DisputeCategory {
    NON_DELIVERY,
    ITEM_NOT_AS_DESCRIBED,
    DAMAGED_ITEM,
    WRONG_ITEM,
    SELLER_UNRESPONSIVE,
    OTHER
}

data class RaiseDisputeRequest(
    val transactionId: String,
    val category: String,
    val description: String,
    val evidenceUrls: List<String> = emptyList()
)

data class DisputeResponse(
    val id: String,
    val transactionId: String,
    val raisedById: String,
    val category: String,
    val status: String,
    val evidenceUrls: List<String>? = null,
    val createdAt: String
)

data class DisputeDetailsResponse(
    val id: String,
    val transactionId: String,
    val transactionReference: String? = null,
    val raisedById: String,
    val raisedByName: String? = null,
    val category: String,
    val description: String? = null,
    val status: String,
    val assignedAdminId: String? = null,
    val resolution: String? = null,
    val resolvedAt: String? = null,
    val evidenceUrls: List<String>? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

data class UploadImageResponse(
    val url: String
)

data class UpdateDisputeEvidenceRequest(
    val evidenceUrl: String
)

data class CloseDisputeRequest(
    val reason: String? = null
)

data class ActionRequiredRequest(
    val action: String
)

data class InitiateStkPushRequest(
    val phoneNumber: String
)

data class InitiateStkPushResponse(
    val paymentId: String,
    val escrowId: String?,
    val status: String?,
    val checkoutRequestId: String?,
    val merchantRequestId: String?,
    val message: String?
)

data class PaymentResponse(
    val paymentId: String,
    val escrowId: String?,
    val amount: Double?,
    val currency: String?,
    val status: String,
    val phoneNumber: String?,
    val checkoutRequestId: String?,
    val merchantRequestId: String?,
    val mpesaReceiptNumber: String?,
    val paidAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class PaymentIntentFinanceResponse(
    val paymentId: String,
    val transactionId: String?,
    val transactionReference: String?,
    val buyerId: String?,
    val sellerId: String?,
    val amount: Double?,
    val currency: String?,
    val status: String,
    val paymentMethod: String?,
    val phoneNumber: String?,
    val mpesaReceiptNumber: String?,
    val paidAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class PayoutFinanceResponse(
    val payoutId: String,
    val transactionId: String?,
    val transactionReference: String?,
    val sellerId: String?,
    val amount: Double?,
    val currency: String?,
    val status: String,
    val conversationId: String?,
    val originatorConversationId: String?,
    val resultCode: String?,
    val resultDescription: String?,
    val paidAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class ReleasePayoutResponse(
    val payoutId: String,
    val escrowId: String,
    val status: String,
    val conversationId: String?,
    val originatorConversationId: String?,
    val message: String?
)

// Additional auth DTOs (if not already present elsewhere)
data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(val accessToken: String, val refreshToken: String, val tokenType: String, val expiresIn: Long)
data class LogoutRequest(val refreshToken: String)
data class LogoutResponse(val loggedOut: Boolean, val message: String)