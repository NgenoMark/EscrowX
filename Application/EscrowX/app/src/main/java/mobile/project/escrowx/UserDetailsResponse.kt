package mobile.project.escrowx

import com.google.gson.annotations.SerializedName

data class UserDetailsResponse(
    val id: String,
    val phone: String,
    val email: String,
    val role: String,
    val status: String,
    @SerializedName(value = "displayName", alternate = ["display_name"]) val displayName: String? = null,
    @SerializedName("businessName") val businessName: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("shopLocation") val shopLocation: String? = null,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String? = null
)