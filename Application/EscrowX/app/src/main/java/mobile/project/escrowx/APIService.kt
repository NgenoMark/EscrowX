package mobile.project.escrowx

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import mobile.project.escrowx.auth.ConfirmRequest
import mobile.project.escrowx.auth.ConfirmResponse
import mobile.project.escrowx.auth.LoginRequest
import mobile.project.escrowx.auth.LoginResponse
import mobile.project.escrowx.auth.PasswordResetConfirmRequest
import mobile.project.escrowx.auth.PasswordResetConfirmResponse
import mobile.project.escrowx.auth.PasswordResetRequestDto
import mobile.project.escrowx.auth.PasswordResetRequestResponse
import mobile.project.escrowx.auth.RegisterRequest
import mobile.project.escrowx.auth.RegisterResponse

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
    suspend fun getUserByEmail(@Path("email") email: String): Response<UserProfileResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.190:8081/"

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

data class UserProfileResponse(
    val id: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
