package mobile.project.escrowx

import com.google.gson.annotations.SerializedName

data class UserDetailsResponse(
    val id: String,
    val phone: String,
    val email: String,
    val role: String,
    val status: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("businessName") val businessName: String? = null,
    @SerializedName("deliveryAddress") val deliveryAddress: String? = null,   // added
    @SerializedName("shopLocation") val shopLocation: String? = null,         // added
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String? = null
)