package com.example.np_nilson.data.api

import com.google.gson.annotations.SerializedName

enum class ReportCategory(val apiValue: String, val label: String) {
    DAMAGED_GOODS("DAMAGED_GOODS", "Skadat gods"),
    SAFETY("SAFETY", "Säkerhet"),
    MAINTENANCE("MAINTENANCE", "Underhåll"),
    OTHER("OTHER", "Övrigt");

    companion object {
        fun fromApiValue(value: String): ReportCategory? = entries.firstOrNull { it.apiValue == value }
    }
}

data class UpdateReportStatusRequest(
    @SerializedName("status") val status: String
)

data class ReportImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("sizeBytes") val sizeBytes: Long
)

data class ReportAuthorDto(
    @SerializedName("id") val id: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String
)

data class ReportDto(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("storeId") val storeId: Long? = null,
    @SerializedName("storeName") val storeName: String? = null,
    @SerializedName("author") val author: ReportAuthorDto,
    @SerializedName("images") val images: List<ReportImageDto> = emptyList()
)
