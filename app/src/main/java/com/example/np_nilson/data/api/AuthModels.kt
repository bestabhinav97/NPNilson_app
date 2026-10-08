package com.example.np_nilson.data.api

import com.google.gson.annotations.SerializedName

data class StoreDto(
    @SerializedName("storeId") val storeId: Long,
    @SerializedName("storeName") val storeName: String,
    @SerializedName("address") val address: String
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("store") val store: StoreDto? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class CreateUserRequest(
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "USER",
    @SerializedName("storeId") val storeId: Long? = null
)

data class ResetPasswordRequest(
    @SerializedName("newPassword") val newPassword: String
)

data class ApiResponse(
    @SerializedName("message") val message: String
)

data class ErrorResponse(
    @SerializedName("error") val error: String? = null
)
