package com.example.np_nilson.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.np_nilson.data.api.*
import com.example.np_nilson.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun <T> retrofit2.Response<T>.apiError(fallback: String): String = runCatching {
    Gson().fromJson(errorBody()?.string(), ErrorResponse::class.java)?.error
}.getOrNull() ?: "$fallback (HTTP ${code()})"

private val fieldTypes = listOf("TEXT" to "Text", "MULTILINE" to "Lång text", "NUMBER" to "Tal",
    "DATE" to "Datum", "SELECT" to "Rullista", "CHECKBOX" to "Kryssruta")
private val icons = listOf("OTHER" to "Övrigt", "SAFETY" to "Säkerhet", "DAMAGED_GOODS" to "Gods", "MAINTENANCE" to "Underhåll")

@Composable
fun CategoryManagement(sessionManager: SessionManager, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CategoryDto?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        if (loading) return
        scope.launch {
            loading = true
            try {
                val response = ApiClient.getReportsApi().getCategories("Bearer ${sessionManager.getToken()}", true)
                if (!response.isSuccessful) error = response.apiError("Kunde inte hämta kategorier")
                else { categories = response.body().orEmpty(); error = null }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "Kunde inte ansluta till servern" }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rapportkategorier", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Anpassa kategorier och formulär. Arkiverade kategorier kan inte väljas för nya rapporter. Befintliga rapporter behåller sina ursprungliga uppgifter.")
        Button(onClick = { editing = CategoryDto(sortOrder = categories.size); editError = null }) { Text("Ny kategori") }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = ::refresh, enabled = !loading) { Text("Uppdatera kategorier") }
        categories.forEach { category ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                    Text("${if (category.isActive) "Aktiv" else "Arkiverad"} · ${category.fields.size} fält · ordning ${category.sortOrder}")
                    if (category.description.isNotBlank()) Text(category.description)
                    TextButton(onClick = { editing = category; editError = null }) { Text("Redigera") }
                }
            }
        }
    }
    editing?.let { initial ->
        key(initial.id) {
            CategoryEditor(initial, saving, editError, onClose = { if (!saving) editing = null }, onSave = { category ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        try {
                            val api = ApiClient.getReportsApi()
                            val auth = "Bearer ${sessionManager.getToken()}"
                            val response = if (category.id == null) api.createCategory(auth, category)
                                else api.updateCategory(auth, category.id, category)
                            if (!response.isSuccessful) editError = response.apiError("Kunde inte spara kategorin")
                            else { editing = null; refresh(); onChanged() }
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { editError = "Kunde inte ansluta. Dina ändringar finns kvar." }
                        finally { saving = false }
                    }
                }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditor(initial: CategoryDto, saving: Boolean, serverError: String?, onClose: () -> Unit, onSave: (CategoryDto) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var order by remember { mutableStateOf(initial.sortOrder.toString()) }
    var localError by remember { mutableStateOf<String?>(null) }
    val rules = remember { mutableStateMapOf<String,String>().apply {
        initial.fields.forEach { field -> field.validation.forEach { (name,value) -> put("${field.id}:$name",value.toString()) } }
    } }
    fun updateField(id: String, transform: (CategoryFieldDto) -> CategoryFieldDto) {
        draft = draft.copy(fields = draft.fields.map { if (it.id == id) transform(it) else it })
    }
    Dialog(onDismissRequest = { if (!saving) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(topBar = { TopAppBar(title = { Text(if (initial.id == null) "Ny kategori" else "Redigera kategori") },
            navigationIcon = { TextButton(onClick = onClose, enabled = !saving) { Text("Stäng") } }) },
            bottomBar = { Surface {
                Button(enabled = !saving, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), onClick = {
                    try {
                        require(draft.name.isNotBlank()) { "Ange ett kategorinamn" }
                        val sortOrder = order.toIntOrNull()
                        require(sortOrder != null && sortOrder in -10000..10000) { "Ange en giltig sorteringsordning" }
                        val fields = draft.fields.mapIndexed { index, field ->
                            require(field.label.isNotBlank()) { "Alla fält behöver ett namn" }
                            val validation = buildMap<String,Double> {
                                val keys = when(field.type) { "NUMBER" -> listOf("min","max"); "TEXT","MULTILINE" -> listOf("maxLength"); else -> emptyList() }
                                keys.forEach { name ->
                                    val raw = rules["${field.id}:$name"].orEmpty().trim()
                                    if (raw.isNotEmpty()) {
                                        val value = raw.replace(',','.').toDoubleOrNull()
                                        require(value != null && value.isFinite()) { "${field.label}: ogiltig gräns" }
                                        put(name,value)
                                    }
                                }
                            }
                            field.copy(sortOrder = index, options = if (field.type == "SELECT") field.options.map { it.trim() }.filter { it.isNotEmpty() } else emptyList(), validation = validation)
                        }
                        localError = null
                        onSave(draft.copy(sortOrder = sortOrder, fields = fields))
                    } catch (e: IllegalArgumentException) { localError = e.message }
                }) { Text(if (saving) "Sparar…" else "Spara kategori") }
            } }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                (localError ?: serverError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(100)) }, label = { Text("Namn") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft.description, { draft = draft.copy(description = it.take(500)) }, label = { Text("Beskrivning") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                ChoiceMenu("Ikon", draft.icon, icons, !saving) { draft = draft.copy(icon = it) }
                OutlinedTextField(order, { order = it }, label = { Text("Sorteringsordning") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = draft.isActive, onCheckedChange = { draft = draft.copy(isActive = it) }, enabled = !saving)
                    Text(if (draft.isActive) "Aktiv kategori" else "Arkiverad kategori")
                }
                Text("Formulärfält", style = MaterialTheme.typography.titleLarge)
                draft.fields.forEachIndexed { index, field ->
                    key(field.id) {
                        OutlinedCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Fält ${index+1}", fontWeight = FontWeight.Bold)
                                OutlinedTextField(field.label, { value -> updateField(field.id) { it.copy(label = value.take(100)) } }, label = { Text("Fältnamn") }, enabled = !saving, modifier = Modifier.fillMaxWidth())
                                ChoiceMenu("Typ", field.type, fieldTypes, !saving) { value -> updateField(field.id) { it.copy(type = value) } }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(field.required, { value -> updateField(field.id) { it.copy(required = value) } }, enabled = !saving)
                                    Text("Obligatoriskt")
                                }
                                if (field.type == "SELECT") {
                                    var options by remember { mutableStateOf(field.options.joinToString("\n")) }
                                    OutlinedTextField(options, { options = it; updateField(field.id) { f -> f.copy(options = it.lines()) } }, label = { Text("Ett val per rad") }, minLines = 3, enabled = !saving, modifier = Modifier.fillMaxWidth())
                                }
                                val names = when(field.type) { "NUMBER" -> listOf("min" to "Minsta värde", "max" to "Största värde"); "TEXT","MULTILINE" -> listOf("maxLength" to "Max antal tecken (1–4000)"); else -> emptyList() }
                                names.forEach { (name,label) -> OutlinedTextField(rules["${field.id}:$name"].orEmpty(), { rules["${field.id}:$name"] = it }, label = { Text(label) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) }
                                Row {
                                    TextButton(enabled = !saving && index > 0, onClick = {
                                        val list = draft.fields.toMutableList(); list.add(index-1,list.removeAt(index)); draft = draft.copy(fields=list)
                                    }) { Text("Upp") }
                                    TextButton(enabled = !saving && index < draft.fields.lastIndex, onClick = {
                                        val list = draft.fields.toMutableList(); list.add(index+1,list.removeAt(index)); draft = draft.copy(fields=list)
                                    }) { Text("Ned") }
                                    TextButton(enabled = !saving, onClick = { draft = draft.copy(fields=draft.fields.filterNot { it.id==field.id }) }) { Text("Ta bort") }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(enabled = !saving && draft.fields.size < 30, onClick = { draft = draft.copy(fields = draft.fields + CategoryFieldDto()) }) { Text("Lägg till fält") }
            }
        }
    }
}
