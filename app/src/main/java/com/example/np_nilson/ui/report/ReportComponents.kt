package com.example.np_nilson.ui.report

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.api.ReportDto
import com.example.np_nilson.data.report.ReportImageRepository
import com.example.np_nilson.ui.theme.*

private const val THUMBNAIL_MAX_DIMENSION = 300
private const val DETAIL_MAX_DIMENSION = 1280

fun categoryIcon(apiValue: String): ImageVector = when (ReportCategory.fromApiValue(apiValue)) {
    ReportCategory.DAMAGED_GOODS -> Icons.Outlined.Inventory2
    ReportCategory.SAFETY -> Icons.Outlined.Warning
    ReportCategory.MAINTENANCE -> Icons.Outlined.Build
    else -> Icons.Default.MoreHoriz
}

/** Shows a report image from the backend, which requires the session token. */
@Composable
fun ReportImage(
    url: String,
    token: String?,
    maxDimension: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap by produceState(ReportImageRepository.getCached(url, maxDimension), url, token) {
        if (value == null && token != null) {
            value = ReportImageRepository.loadImage(url, token, maxDimension)
        }
    }

    Box(
        modifier = modifier.background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        val loaded: Bitmap? = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Rapportbild",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = NpTextSecondary)
        }
    }
}

@Composable
fun ReportStatusBadge(status: String) {
    val (background, foreground) = when (status) {
        "RESOLVED" -> Color(0xFFDCFCE7) to Color(0xFF166534)
        "IN_PROGRESS" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFE8F2FC) to NpBluePrimary
    }
    Text(
        text = ReportFormatting.statusLabel(status),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = foreground,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun ReportListItem(
    report: ReportDto,
    token: String?,
    showAuthor: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstImage = report.images.firstOrNull()
            if (firstImage != null) {
                ReportImage(
                    url = firstImage.url,
                    token = token,
                    maxDimension = THUMBNAIL_MAX_DIMENSION,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F2FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIcon(report.category), contentDescription = null, tint = NpBluePrimary)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ReportStatusBadge(report.status)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ReportFormatting.categoryLabel(report.category)} · ${ReportFormatting.formatDate(report.createdAt)}",
                    fontSize = 12.sp,
                    color = NpTextSecondary
                )
                val details = buildList {
                    if (showAuthor) add("${report.author.firstname} ${report.author.lastname}")
                    if (report.images.isNotEmpty()) add("${report.images.size} bild(er)")
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        fontSize = 12.sp,
                        color = NpBluePrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailDialog(
    report: ReportDto,
    token: String?,
    canChangeStatus: Boolean,
    isUpdatingStatus: Boolean,
    statusError: String?,
    onChangeStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rapport", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = NpBackgroundLight
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F2FC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon(report.category), contentDescription = null, tint = NpBluePrimary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = ReportFormatting.categoryLabel(report.category),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NpBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatusBadge(report.status)
                }

                Text(
                    text = report.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NpTextPrimary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow("Skapad", ReportFormatting.formatDate(report.createdAt))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        DetailRow("Rapporterad av", "${report.author.firstname} ${report.author.lastname}")
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        DetailRow("Butik", report.storeName ?: "Ingen butik")
                    }
                }

                if (canChangeStatus) {
                    StatusSelector(
                        currentStatus = report.status,
                        isUpdating = isUpdatingStatus,
                        error = statusError,
                        onChangeStatus = onChangeStatus
                    )
                }

                if (report.description.isNotBlank()) {
                    Column {
                        Text("Beskrivning", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NpTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(report.description, fontSize = 14.sp, color = NpTextPrimary, lineHeight = 20.sp)
                    }
                }

                if (report.images.isNotEmpty()) {
                    Text(
                        "Bilder (${report.images.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )
                    report.images.forEach { image ->
                        ReportImage(
                            url = image.url,
                            token = token,
                            maxDimension = DETAIL_MAX_DIMENSION,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSelector(
    currentStatus: String,
    isUpdating: Boolean,
    error: String?,
    onChangeStatus: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ändra status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NpTextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isUpdating) {
                CircularProgressIndicator(
                    color = NpBluePrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReportFormatting.STATUSES.forEachIndexed { index, status ->
                SegmentedButton(
                    selected = status == currentStatus,
                    onClick = { onChangeStatus(status) },
                    enabled = !isUpdating,
                    shape = SegmentedButtonDefaults.itemShape(index, ReportFormatting.STATUSES.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = NpBluePrimary,
                        activeContentColor = Color.White
                    ),
                    label = { Text(ReportFormatting.statusLabel(status), maxLines = 1) }
                )
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = NpErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium, color = NpTextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.Bold, color = NpTextPrimary)
    }
}
