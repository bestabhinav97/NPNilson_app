package com.example.np_nilson.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ReportsApi {
    @Multipart
    @POST("api/reports")
    suspend fun createReport(
        @Header("Authorization") authHeader: String,
        @Part("category") category: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
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
