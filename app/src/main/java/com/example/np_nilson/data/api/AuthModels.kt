package com.example.np_nilson.data.api

import com.google.gson.annotations.SerializedName

data class StoreDto(
    @SerializedName("storeId")
    val storeId: Long,

    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("address")
    val address: String
)

data class UserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("firstname")
    val firstname: String,

    @SerializedName("lastname")
    val lastname: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("isActive")
    val isActive: Boolean = true,

    @SerializedName("store")
    val store: StoreDto? = null
)

data class AdminStoreStatsDto(
    @SerializedName("storeId")
    val storeId: Long,

    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("userCount")
    val userCount: Int = 0,

    @SerializedName("openReports")
    val openReports: Int = 0
)

data class AdminActivityDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("action")
    val action: String,

    @SerializedName("targetType")
    val targetType: String,

    @SerializedName("targetId")
    val targetId: String? = null,

    @SerializedName("details")
    val details: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("actorName")
    val actorName: String
)

data class AdminDashboardDto(
    @SerializedName("totalUsers")
    val totalUsers: Int = 0,

    @SerializedName("activeUsers")
    val activeUsers: Int = 0,

    @SerializedName("adminCount")
    val adminCount: Int = 0,

    @SerializedName("storeCount")
    val storeCount: Int = 0,

    @SerializedName("openReports")
    val openReports: Int = 0,

    @SerializedName("inProgressReports")
    val inProgressReports: Int = 0,

    @SerializedName("resolvedReports")
    val resolvedReports: Int = 0,

    @SerializedName("stores")
    val stores: List<AdminStoreStatsDto> = emptyList(),

    @SerializedName("recentActivity")
    val recentActivity: List<AdminActivityDto> = emptyList()
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: UserDto
)

data class CreateUserRequest(
    @SerializedName("firstname")
    val firstname: String,

    @SerializedName("lastname")
    val lastname: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String = "USER",

    @SerializedName("storeId")
    val storeId: Long? = null
)

data class ResetPasswordRequest(
    @SerializedName("newPassword")
    val newPassword: String
)

data class SetUserActiveRequest(
    @SerializedName("active")
    val active: Boolean
)

data class UpdateUserRequest(
    @SerializedName("firstname")
    val firstname: String,

    @SerializedName("lastname")
    val lastname: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("storeId")
    val storeId: Long? = null
)

data class CreateStoreRequest(
    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("address")
    val address: String
)

data class UpdateStoreRequest(
    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("address")
    val address: String
)

data class ApiResponse(
    @SerializedName("message")
    val message: String
)

data class ErrorResponse(
    @SerializedName("error")
    val error: String? = null
)