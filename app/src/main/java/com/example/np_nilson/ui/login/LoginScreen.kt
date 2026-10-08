package com.example.np_nilson.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.branding.NpNilssonHeaderLogo
import com.example.np_nilson.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NpNilssonHeaderLogo()
                    IconButton(onClick = { viewModel.onSettingsClicked() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Serverinställningar",
                            tint = NpTextSecondary
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        },
        containerColor = NpBackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Logga in",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Banner
                    viewModel.errorMessage?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                .border(1.dp, NpErrorRed, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = error,
                                color = NpErrorRed,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("E-postadress") },
                        placeholder = { Text("test@test.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = NpBluePrimary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NpTextSecondary
                            )
                        },
                        isError = viewModel.emailError != null,
                        supportingText = {
                            viewModel.emailError?.let {
                                Text(text = it, color = NpErrorRed)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NpBluePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = NpBluePrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Lösenord") },
                        placeholder = { Text("Lösenord") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = NpBluePrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (viewModel.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (viewModel.isPasswordVisible) "Dölj lösenord" else "Visa lösenord",
                                    tint = NpTextSecondary
                                )
                            }
                        },
                        isError = viewModel.passwordError != null,
                        supportingText = {
                            viewModel.passwordError?.let {
                                Text(text = it, color = NpErrorRed)
                            }
                        },
                        visualTransformation = if (viewModel.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login(sessionManager, onLoginSuccess)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NpBluePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = NpBluePrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // LOG IN Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login(sessionManager, onLoginSuccess)
                        },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NpBluePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "LOGGA IN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Contact Administrator link
                    TextButton(onClick = { viewModel.onContactAdminClicked() }) {
                        Text(
                            text = "Glömt lösenord? Kontakta din administratör",
                            color = NpBluePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Contact Admin Dialog
    if (viewModel.showContactAdminDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissContactAdminDialog() },
            title = { Text("Kontakta administratör") },
            text = {
                Text("Kontoanläggning och återställning av lösenord hanteras av din systemadministratör på NP Nilsson. Vänligen kontakta IT-avdelningen för hjälp.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissContactAdminDialog() }) {
                    Text("OK", color = NpBluePrimary)
                }
            }
        )
    }

    // Backend Settings Dialog
    if (viewModel.showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(viewModel.backendUrl) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissSettingsDialog() },
            title = { Text("Serveradress för backend") },
            text = {
                Column {
                    Text(
                        text = "Konfigurera backend-serverns adress (t.ex. http://10.0.2.2:8080/ för Android Emulator eller din dators IP-adress):",
                        fontSize = 13.sp,
                        color = NpTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Serveradress") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateBackendUrl(tempUrl) }) {
                    Text("Spara", color = NpBluePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSettingsDialog() }) {
                    Text("Avbryt", color = NpTextSecondary)
                }
            }
        )
    }
}
