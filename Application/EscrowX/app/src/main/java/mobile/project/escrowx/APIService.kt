package mobile.project.escrowx

// FIXED: Added missing Retrofit and Gson imports
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/auth/confirm")
    suspend fun confirmAccount(@Body request: ConfirmRequest): Response<ConfirmResponse>

    @POST("api/v1/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8081/"

    val instance: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}