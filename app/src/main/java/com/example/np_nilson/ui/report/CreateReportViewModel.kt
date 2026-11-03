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
import com.example.np_nilson.data.api.CategoryDto
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
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
    fun validate(category: String?, title: String, photoCount: Int): String? = when {
        category == null -> "Välj en kategori"
        title.isBlank() -> "Ange en rubrik"
        title.trim().length > MAX_TITLE_LENGTH -> "Rubriken får vara högst $MAX_TITLE_LENGTH tecken"
        photoCount > MAX_PHOTOS -> "Du kan bifoga högst $MAX_PHOTOS bilder"
        else -> null
    }
}

class CreateReportViewModel(application: Application) : AndroidViewModel(application) {

    var category by mutableStateOf<CategoryDto?>(null)
        private set
    var categories by mutableStateOf<List<CategoryDto>>(emptyList())
        private set
    var isLoadingCategories by mutableStateOf(false)
        private set
    private var categoriesJob: kotlinx.coroutines.Job? = null
    private var categoriesGeneration = 0
    val fieldValues = mutableStateMapOf<String, String>()

    fun selectCategory(value: CategoryDto) {
        category = value
        fieldValues.clear()
        value.fields.filter { it.type == "CHECKBOX" }.forEach { fieldValues[it.id] = "false" }
    }

    fun loadCategories(sessionManager: SessionManager, initialCode: String?) {
        val token = sessionManager.getToken() ?: return
        if (isLoadingCategories) return
        isLoadingCategories = true
        val generation = ++categoriesGeneration
        categoriesJob = viewModelScope.launch {
            try {
                val response = ApiClient.getReportsApi().getCategories("Bearer $token")
                if (generation != categoriesGeneration) return@launch
                if (!response.isSuccessful) throw IllegalStateException("Kunde inte hämta kategorier (HTTP ${response.code()})")
                categories = response.body().orEmpty()
                if (category == null) categories.find { it.code == initialCode }?.let(::selectCategory)
                else categories.find { it.id == category?.id }?.let { category = it }
                    ?: run { category = null; fieldValues.clear() }
                category?.fields?.filter { it.type == "CHECKBOX" }?.forEach { fieldValues.putIfAbsent(it.id,"false") }
                errorMessage = if (categories.isEmpty()) "Inga aktiva kategorier. Kontakta administratören." else null
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { errorMessage = e.message ?: "Kunde inte hämta kategorier" }
            finally { if (generation == categoriesGeneration) isLoadingCategories = false }
        }
    }
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
    fun start(initialCategory: String?, sessionManager: SessionManager) {
        if (started) return
        started = true
        loadCategories(sessionManager, initialCategory)
    }

    fun clear() {
        categoriesGeneration++
        categoriesJob?.cancel()
        isLoadingCategories = false
        started = false
        category = null
        categories = emptyList()
        fieldValues.clear()
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
        if (isSubmitting) return
        val token = sessionManager.getToken() ?: return
        val selectedCategory = category
        val validationError = ReportValidation.validate(selectedCategory?.code, title, photos.size)
        if (validationError != null || selectedCategory == null) {
            errorMessage = validationError
            return
        }

        val values = try { DynamicFieldValidation.values(selectedCategory.fields, fieldValues) }
        catch (e: IllegalArgumentException) { errorMessage = e.message; return }
        val uploadPhotos = photos.toList()
        val uploadTitle = title.trim()
        val uploadDescription = description.trim()

        errorMessage = null
        isSubmitting = true
        viewModelScope.launch {
            val imageParts = try {
                withContext(Dispatchers.IO) {
                    uploadPhotos.mapIndexed { index, uri ->
                        val bytes = ReportImageLoader.compressForUpload(getApplication(), uri)
                        MultipartBody.Part.createFormData(
                            "images",
                            "photo_${index + 1}.jpg",
                            bytes.toRequestBody(JPEG)
                        )
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                isSubmitting = false
                errorMessage = "Kunde inte läsa en av bilderna. Ta bort den och försök igen."
                return@launch
            }

            try {
                val response = ApiClient.getReportsApi().createReport(
                    authHeader = "Bearer $token",
                    category = selectedCategory.code.toRequestBody(TEXT),
                    title = uploadTitle.toRequestBody(TEXT),
                    description = uploadDescription.toRequestBody(TEXT),
                    fieldValues = Gson().toJson(values).toRequestBody(TEXT),
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
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                isSubmitting = false
                errorMessage = "Kunde inte ansluta till servern"
            } finally {
                isSubmitting = false
            }
        }
    }

    private fun photoDir() = File(getApplication<Application>().cacheDir, "report_photos")

    companion object {
        private val TEXT = "text/plain; charset=utf-8".toMediaType()
        private val JPEG = "image/jpeg".toMediaType()
    }
}
