package mobile.project.escrowx.auth

// POST /api/v1/auth/confirm request structure
data class ConfirmRequest(
    val email: String,
    val otp: String
)

// POST /api/v1/auth/confirm response structure [cite: 21, 29]
data class ConfirmResponse(
    val email: String,
    val status: String,
    val confirmed: Boolean
)

// POST /api/v1/auth/login request payload
data class LoginRequest(
    val email: String,
    val password: String
)

// POST /api/v1/auth/login response payload
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserProfileData // Resolves the 'user' reference issue
)
// Embedded user profile details inside the login response object
data class UserProfileData(
    val id: String,
    val email: String,
    val phone: String,
    val role: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
data class RegisterRequest(
    val phone: String,       // e.g., "+254712345678"
    val email: String,       // e.g., "newuser@escrowx.local"
    val password: String,    // e.g., "Strong@123"
    val displayName: String, // e.g., "New User"
    val businessName: String?,// Optional field (Nullable)
    val role: String         // BUYER or SELLER
)

data class RegisterResponse(
    val userId: String,
    val phone: String,
    val status: String,
    val role: String,
    val otpPreview: String
)

data class PasswordResetRequestDto(
    val email: String
)

data class PasswordResetRequestResponse(
    val email: String,
    val message: String,
    val otpPreview: String?
)

data class PasswordResetConfirmRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

data class PasswordResetConfirmResponse(
    val email: String? = null,
    val message: String? = null,
    val passwordUpdated: Boolean? = null
)
