package com.example.np_nilson.ui.report

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.np_nilson.data.api.ReportImageDto
import java.util.Locale

@Composable
fun PhotoInformation(image: ReportImageDto) {
    val metadata = image.metadata
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Fotoinformation", style = MaterialTheme.typography.titleSmall)
        Text("Tagen: ${metadata?.takenAt?.let { ReportFormatting.formatDate(it) } ?: "Uppgift saknas"}")
        val camera = listOfNotNull(metadata?.cameraMake,metadata?.cameraModel).distinct().joinToString(" ")
        Text("Kamera: ${camera.ifBlank { "Uppgift saknas" }}")
        Text("Fil: ${image.contentType} · ${image.sizeBytes / 1024} kB",style = MaterialTheme.typography.bodySmall)
        val latitude = metadata?.latitude
        val longitude = metadata?.longitude
        if (latitude != null && longitude != null) {
            Text(String.format(Locale.US,"GPS: %.6f, %.6f",latitude,longitude))
            metadata.altitude?.let { Text(String.format(locale,"Höjd: %.1f m",it)) }
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            TextButton(onClick = {
                try { context.startActivity(mapIntent) }
                catch (_: android.content.ActivityNotFoundException) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")))
                    } catch (_: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context,"Ingen kartapp eller webbläsare tillgänglig",android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }) { Text("Öppna plats i kartan") }
        } else Text("GPS: Uppgift saknas", style = MaterialTheme.typography.bodySmall)
        Text("Fototid och inskickningstid är separata uppgifter.",style = MaterialTheme.typography.bodySmall)
    }
}
