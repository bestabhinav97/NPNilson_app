package com.example.np_nilson.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ReportsApi {
    @GET("api/categories")
    suspend fun getCategories(@Header("Authorization") auth: String,
        @Query("includeArchived") includeArchived: Boolean = false): Response<List<CategoryDto>>

    @POST("api/admin/categories")
    suspend fun createCategory(@Header("Authorization") auth: String, @Body category: CategoryDto): Response<CategoryDto>

    @PUT("api/admin/categories/{id}")
    suspend fun updateCategory(@Header("Authorization") auth: String, @Path("id") id: String,
        @Body category: CategoryDto): Response<CategoryDto>

    @GET("api/reports/{id}")
    suspend fun getReport(@Header("Authorization") auth: String, @Path("id") id: String): Response<ReportDto>

    @GET("api/reports/{id}/messages")
    suspend fun getMessages(@Header("Authorization") auth: String, @Path("id") id: String,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null): Response<List<ReportMessageDto>>

    @POST("api/reports/{id}/messages")
    suspend fun sendMessage(@Header("Authorization") auth: String, @Path("id") id: String,
        @Body message: SendReportMessageRequest): Response<ReportMessageDto>
    @Multipart
    @POST("api/reports")
    suspend fun createReport(
        @Header("Authorization") authHeader: String,
        @Part("category") category: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("fieldValues") fieldValues: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<ReportDto>

    @GET("api/reports")
    suspend fun getReports(
        @Header("Authorization") authHeader: String
    ): Response<List<ReportDto>>

    @PATCH("api/reports/{reportId}/status")
    suspend fun updateReportStatus(
        @Header("Authorization") authHeader: String,
        @Path("reportId") reportId: String,
        @Body request: UpdateReportStatusRequest
    ): Response<ReportDto>

    // Image URLs come from ReportImageDto.url (e.g. "/api/reports/{id}/images/{imageId}")
    @Streaming
    @GET
    suspend fun getReportImage(
        @Header("Authorization") authHeader: String,
        @Url url: String
    ): Response<ResponseBody>
}
