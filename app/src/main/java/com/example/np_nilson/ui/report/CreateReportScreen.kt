package com.example.np_nilson.ui.report

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.report.ReportImageLoader
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    sessionManager: SessionManager,
    initialCategory: ReportCategory?,
    onClose: () -> Unit,
    viewModel: CreateReportViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.start(initialCategory)
    }

    val close = {
        viewModel.clear()
        onClose()
    }
    BackHandler(enabled = !viewModel.isSubmitting) { close() }

    // Camera writes into a file we create; remember its Uri until the result comes back
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) viewModel.addPhotos(listOf(uri))
        pendingCameraUri = null
    }
    val pickPhotosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReportValidation.MAX_PHOTOS)
    ) { uris ->
        viewModel.addPhotos(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ny rapport", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = close, enabled = !viewModel.isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.submit(sessionManager) },
                    enabled = !viewModel.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(50.dp)
                ) {
                    if (viewModel.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Skickar...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skicka rapport", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = NpBackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Category
            Column {
                SectionTitle("Kategori")
                Spacer(modifier = Modifier.height(8.dp))
                ReportCategory.entries.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCategories.forEach { category ->
                            FilterChip(
                                selected = viewModel.category == category,
                                onClick = { viewModel.category = category },
                                label = { Text(category.label) },
                                enabled = !viewModel.isSubmitting,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NpBluePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Title & description
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Beskrivning")
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it.take(ReportValidation.MAX_TITLE_LENGTH) },
                    label = { Text("Rubrik") },
                    placeholder = { Text("T.ex. Trasig pall i lager 3") },
                    singleLine = true,
                    enabled = !viewModel.isSubmitting,
                    supportingText = { Text("${viewModel.title.length}/${ReportValidation.MAX_TITLE_LENGTH}") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Beskrivning (valfritt)") },
                    placeholder = { Text("Vad har hänt och var?") },
                    minLines = 4,
                    enabled = !viewModel.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Photos
            Column {
                SectionTitle("Bilder (${viewModel.photos.size}/${ReportValidation.MAX_PHOTOS})")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val canAddPhotos = viewModel.remainingPhotoSlots > 0 && !viewModel.isSubmitting
                    OutlinedButton(
                        onClick = {
                            val uri = viewModel.createCameraPhotoUri()
                            pendingCameraUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        enabled = canAddPhotos,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ta foto")
                    }
                    OutlinedButton(
                        onClick = {
                            pickPhotosLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = canAddPhotos,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galleri")
                    }
                }

                if (viewModel.photos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(viewModel.photos, key = { it.toString() }) { uri ->
                            PhotoThumbnail(
                                uri = uri,
                                enabled = !viewModel.isSubmitting,
                                onRemove = { viewModel.removePhoto(uri) }
                            )
                        }
                    }
                }
            }

            viewModel.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = NpErrorRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Success Dialog
    viewModel.submittedReport?.let { report ->
        AlertDialog(
            onDismissRequest = close,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NpBluePrimary) },
            title = { Text("Rapporten har skickats") },
            text = {
                Text(
                    "\"${report.title}\" har skickats in" +
                        if (report.images.isNotEmpty()) " med ${report.images.size} bild(er)." else "."
                )
            },
            confirmButton = {
                Button(
                    onClick = close,
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
                ) {
                    Text("Klar")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = NpTextPrimary
    )
}

@Composable
private fun PhotoThumbnail(
    uri: Uri,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                ReportImageLoader.loadBitmap(context, uri, maxDimension = 300)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Bifogad bild",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ta bort bild",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
