package com.example.np_nilson.ui.report

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.np_nilson.data.api.ApiClient
import com.example.np_nilson.data.api.ErrorResponse
import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.api.ReportDto
import com.example.np_nilson.data.report.ReportImageLoader
import com.example.np_nilson.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

object ReportValidation {
    const val MAX_TITLE_LENGTH = 150
    const val MAX_PHOTOS = 5

    /** Returns an error message to show, or null if the report can be sent. */
    fun validate(category: ReportCategory?, title: String, photoCount: Int): String? = when {
        category == null -> "Välj en kategori"
        title.isBlank() -> "Ange en rubrik"
        title.trim().length > MAX_TITLE_LENGTH -> "Rubriken får vara högst $MAX_TITLE_LENGTH tecken"
        photoCount > MAX_PHOTOS -> "Du kan bifoga högst $MAX_PHOTOS bilder"
        else -> null
    }
}

class CreateReportViewModel(application: Application) : AndroidViewModel(application) {

    var category by mutableStateOf<ReportCategory?>(null)
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    val photos = mutableStateListOf<Uri>()

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var submittedReport by mutableStateOf<ReportDto?>(null)
        private set

    val remainingPhotoSlots: Int
        get() = ReportValidation.MAX_PHOTOS - photos.size

    private var started = false

    // The ViewModel outlives the screen, so the form is only set up once per visit
    fun start(initialCategory: ReportCategory?) {
        if (started) return
        started = true
        category = initialCategory
    }

    fun clear() {
        started = false
        category = null
        title = ""
        description = ""
        photos.clear()
        errorMessage = null
        submittedReport = null
        photoDir().deleteRecursively()
    }

    fun addPhotos(uris: List<Uri>) {
        val toAdd = uris.filterNot { it in photos }.take(remainingPhotoSlots)
        photos.addAll(toAdd)
        if (toAdd.size < uris.size) {
            errorMessage = "Du kan bifoga högst ${ReportValidation.MAX_PHOTOS} bilder"
        }
    }

    fun removePhoto(uri: Uri) {
        photos.remove(uri)
    }

    /** Creates a file the camera app can write a new photo into. */
    fun createCameraPhotoUri(): Uri {
        val context = getApplication<Application>()
        val file = File(photoDir().apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun submit(sessionManager: SessionManager) {
        val token = sessionManager.getToken() ?: return
        val selectedCategory = category
        val validationError = ReportValidation.validate(selectedCategory, title, photos.size)
        if (validationError != null || selectedCategory == null) {
            errorMessage = validationError
            return
        }

        errorMessage = null
        isSubmitting = true
        viewModelScope.launch {
            val imageParts = try {
                withContext(Dispatchers.IO) {
                    photos.mapIndexed { index, uri ->
                        val bytes = ReportImageLoader.compressForUpload(getApplication(), uri)
                        MultipartBody.Part.createFormData(
                            "images",
                            "photo_${index + 1}.jpg",
                            bytes.toRequestBody(JPEG)
                        )
                    }
                }
            } catch (e: Exception) {
                isSubmitting = false
                errorMessage = "Kunde inte läsa en av bilderna. Ta bort den och försök igen."
                return@launch
            }

            try {
                val response = ApiClient.getReportsApi().createReport(
                    authHeader = "Bearer $token",
                    category = selectedCategory.apiValue.toRequestBody(TEXT),
                    title = title.trim().toRequestBody(TEXT),
                    description = description.trim().toRequestBody(TEXT),
                    images = imageParts
                )
                isSubmitting = false
                if (response.isSuccessful && response.body() != null) {
                    submittedReport = response.body()
                } else {
                    val errorStr = response.errorBody()?.string()
                    val parsed = try { Gson().fromJson(errorStr, ErrorResponse::class.java)?.error } catch (e: Exception) { null }
                    errorMessage = parsed ?: "Kunde inte skicka rapporten"
                }
            } catch (e: Exception) {
                isSubmitting = false
                errorMessage = "Kunde inte ansluta till servern"
            }
        }
    }

    private fun photoDir() = File(getApplication<Application>().cacheDir, "report_photos")

    companion object {
        private val TEXT = "text/plain; charset=utf-8".toMediaType()
        private val JPEG = "image/jpeg".toMediaType()
    }
}
