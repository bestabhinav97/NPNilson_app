package com.example.np_nilson.data.api

import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String
    ): Response<UserDto>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authHeader: String
    ): Response<ApiResponse>

    @GET("api/admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") authHeader: String
    ): Response<List<UserDto>>

    @POST("api/admin/users")
    suspend fun createUser(
        @Header("Authorization") authHeader: String,
        @Body request: CreateUserRequest
    ): Response<UserDto>

    @POST("api/admin/users/{userId}/reset-password")
    suspend fun resetPassword(
        @Header("Authorization") authHeader: String,
        @Path("userId") userId: String,
        @Body request: ResetPasswordRequest
    ): Response<UserDto>
}
