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

/** Camera metadata the server read from the photo's EXIF. Every field is null when the photo had none. */
data class ReportImageMetadataDto(
    @SerializedName("takenAt") val takenAt: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("altitude") val altitude: Double? = null,
    @SerializedName("cameraMake") val cameraMake: String? = null,
    @SerializedName("cameraModel") val cameraModel: String? = null
)

data class ReportImageDto(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("sizeBytes") val sizeBytes: Long,
    @SerializedName("metadata") val metadata: ReportImageMetadataDto? = null
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
    @SerializedName("images") val images: List<ReportImageDto> = emptyList(),
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("categoryIcon") val categoryIcon: String? = null,
    @SerializedName("fields") val fields: List<ReportFieldValueDto> = emptyList()
)

data class CategoryFieldDto(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "", val type: String = "TEXT", val required: Boolean = false,
    val options: List<String> = emptyList(), val validation: Map<String, Double> = emptyMap(), val sortOrder: Int = 0
)
data class CategoryDto(
    val id: String? = null, val code: String = "", val name: String = "", val description: String = "",
    val icon: String = "OTHER", val sortOrder: Int = 0, val isActive: Boolean = true,
    val fields: List<CategoryFieldDto> = emptyList()
)
data class ReportFieldValueDto(val definition: CategoryFieldDto, val value: com.google.gson.JsonElement)
data class ReportMessageDto(
    val id: String, val reportId: String, val senderId: String?, val senderName: String,
    val senderRole: String, val body: String, val createdAt: String
)
data class SendReportMessageRequest(val id: String, val body: String)
