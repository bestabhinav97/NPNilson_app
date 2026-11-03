package com.example.np_nilson.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.np_nilson.data.api.CategoryFieldDto
import java.time.LocalDate

object DynamicFieldValidation {
    fun values(fields: List<CategoryFieldDto>, inputs: Map<String, String>): Map<String, Any> = buildMap {
        fields.forEach { field ->
            val raw = inputs[field.id]?.trim().orEmpty()
            if (raw.isEmpty()) {
                require(!field.required) { "${field.label} krävs" }
            } else {
                val value: Any = when (field.type) {
                    "NUMBER" -> {
                        val number = raw.replace(',', '.').toDoubleOrNull()
                        require(number != null && number.isFinite()) { "${field.label}: ange ett giltigt tal" }
                        require(field.validation["min"]?.let { number >= it } != false &&
                            field.validation["max"]?.let { number <= it } != false) { "${field.label}: talet ligger utanför tillåtet intervall" }
                        number
                    }
                    "CHECKBOX" -> { require(raw in listOf("true", "false")) { "${field.label}: välj ja eller nej" }; raw.toBoolean() }
                    "DATE" -> {
                        require(runCatching { LocalDate.parse(raw).toString() == raw }.getOrDefault(false)) { "${field.label}: använd ÅÅÅÅ-MM-DD" }
                        raw
                    }
                    "SELECT" -> { require(raw in field.options) { "${field.label}: välj ett alternativ" }; raw }
                    else -> {
                        require(raw.length <= (field.validation["maxLength"]?.toInt() ?: 4000)) { "${field.label}: texten är för lång" }
                        raw
                    }
                }
                put(field.id, value)
            }
        }
    }
}

@Composable
fun ChoiceMenu(label: String, value: String, choices: List<Pair<String, String>>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${choices.find { it.first == value }?.second ?: "Välj"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onChange(key); expanded = false })
            }
        }
    }
}

@Composable
fun DynamicFields(fields: List<CategoryFieldDto>, values: Map<String, String>, enabled: Boolean, onChange: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fields.forEach { field ->
            key(field.id) {
                val value = values[field.id].orEmpty()
                val label = field.label + if (field.required) " *" else ""
                when (field.type) {
                    "CHECKBOX" -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = value == "true", onCheckedChange = { onChange(field.id, it.toString()) }, enabled = enabled)
                        Text(label, modifier = Modifier.weight(1f))
                    }
                    "SELECT" -> ChoiceMenu(label, value, listOf("" to "Välj") + field.options.map { it to it }, enabled) { onChange(field.id,it) }
                    else -> OutlinedTextField(value = value, onValueChange = { onChange(field.id,it) },
                        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), enabled = enabled,
                        singleLine = field.type != "MULTILINE", minLines = if (field.type == "MULTILINE") 3 else 1,
                        placeholder = if (field.type == "DATE") ({ Text("ÅÅÅÅ-MM-DD") }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = if (field.type == "NUMBER") KeyboardType.Decimal else KeyboardType.Text))
                }
            }
        }
    }
}
