package com.example.np_nilson.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.np_nilson.data.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ReportConversationViewModel : ViewModel() {
    var messages by mutableStateOf<List<ReportMessageDto>>(emptyList())
        private set
    var draft by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var sendError by mutableStateOf<String?>(null)
        private set
    var hasOlder by mutableStateOf(false)
        private set
    private var pending: SendReportMessageRequest? = null
    private var lastFetchedId: String? = null

    private fun merge(incoming: List<ReportMessageDto>) {
        messages = (messages + incoming).associateBy { it.id }.values.sortedWith(compareBy({it.createdAt},{it.id}))
    }
    suspend fun refresh(token: String, reportId: String, older: Boolean = false) {
        if (loading) return
        loading = true
        try {
            val before = if (older) messages.firstOrNull()?.id else null
            var after = if (!older) lastFetchedId else null
            do {
                val response = ApiClient.getReportsApi().getMessages("Bearer $token",reportId,before,after)
                if (!response.isSuccessful) { loadError = response.apiError("Kunde inte hämta konversationen"); break }
                val page = response.body().orEmpty()
                if (older || after == null) hasOlder = page.size == 50
                if (!older && after == null) messages = page else merge(page)
                if (!older && page.isNotEmpty()) lastFetchedId = page.last().id
                loadError = null
                // Catch up in ascending pages so a busy thread cannot lose messages between refreshes.
                if (older || after == null || page.size < 50) break
                after = page.last().id
            } while (true)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { loadError = "Kunde inte hämta meddelanden. Försök igen." }
        finally { loading = false }
    }
    fun send(token: String, reportId: String) {
        if (sending || draft.isBlank() || draft.trim().length > 4000) return
        val request = pending?.takeIf { it.body == draft.trim() }
            ?: SendReportMessageRequest(UUID.randomUUID().toString(),draft.trim()).also { pending = it }
        sending = true
        sendError = null
        viewModelScope.launch {
            try {
                val response = ApiClient.getReportsApi().sendMessage("Bearer $token",reportId,request)
                val message = response.body()
                if (response.isSuccessful && message != null) {
                    merge(listOf(message)); draft = ""; pending = null
                } else sendError = response.apiError("Kunde inte skicka meddelandet")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { sendError = "Kunde inte skicka. Tryck på Skicka igen för att försöka på nytt." }
            finally { sending = false }
        }
    }
}

@Composable
fun ReportConversation(reportId: String, token: String?, currentUserId: String?) {
    val model: ReportConversationViewModel = viewModel(key = "conversation-$reportId-$currentUserId")
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(reportId, token, lifecycle) {
        if (token != null) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { model.refresh(token,reportId); delay(10000) }
        }
    }
    LaunchedEffect(model.messages.lastOrNull()?.id) {
        if (model.messages.isNotEmpty()) listState.animateScrollToItem(model.messages.lastIndex)
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Konversation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Meddelanden mellan rapportören och administratörerna.", style = MaterialTheme.typography.bodySmall)
            if (model.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            model.loadError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { token?.let { scope.launch { model.refresh(it,reportId) } } }, enabled = !model.loading) { Text("Försök igen") }
            }
            if (model.hasOlder) TextButton(onClick = { token?.let { scope.launch { model.refresh(it,reportId,true) } } }, enabled = !model.loading) { Text("Visa äldre meddelanden") }
            if (model.messages.isEmpty() && !model.loading && model.loadError == null) Text("Inga meddelanden ännu. Starta konversationen nedan.")
            if (model.messages.isNotEmpty()) LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(model.messages, key = {it.id}) { message ->
                    val own = message.senderId == currentUserId
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(0.92f)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${message.senderName} · ${if(message.senderRole == "ADMIN") "Admin" else "Rapportör"}", style = MaterialTheme.typography.labelMedium)
                                Text(message.body)
                                Text(ReportFormatting.formatDate(message.createdAt), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            OutlinedTextField(model.draft, { model.draft = it.take(4000) }, label = { Text("Skriv ett meddelande") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5, enabled = token != null && !model.sending,
                supportingText = { Text("${model.draft.length}/4000") })
            model.sendError?.let { Text(it,color = MaterialTheme.colorScheme.error) }
            Button(enabled = token != null && !model.sending && model.draft.isNotBlank(), onClick = { token?.let { model.send(it,reportId) } }, modifier = Modifier.align(Alignment.End)) {
                Text(if (model.sending) "Skickar…" else "Skicka")
            }
        }
    }
}
