package com.example.np_nilson.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.np_nilson.data.api.UserDto
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.branding.NpNilssonHeaderLogo
import com.example.np_nilson.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionManager: SessionManager,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initSession(sessionManager)
    }

    val user = viewModel.currentUser

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
                    IconButton(onClick = { viewModel.logout(sessionManager, onLoggedOut) }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = NpErrorRed
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        },
        containerColor = NpBackgroundLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Welcome,",
                                    fontSize = 14.sp,
                                    color = NpTextSecondary
                                )
                                Text(
                                    text = "${user?.firstname ?: ""} ${user?.lastname ?: ""}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NpTextPrimary
                                )
                            }

                            // Role Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (user?.role == "ADMIN") NpBluePrimary else NpBlueLight,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = user?.role ?: "USER",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Email: ${user?.email ?: ""}",
                            fontSize = 15.sp,
                            color = NpTextPrimary
                        )
                    }
                }
            }

            // Status message
            viewModel.statusMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.isErrorStatus) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (viewModel.isErrorStatus) NpErrorRed else Color(0xFF2E7D32),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Administrator Section
            if (user?.role == "ADMIN") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = NpBluePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "User Provisioning (Admin)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NpTextPrimary
                                    )
                                }

                                Button(
                                    onClick = { viewModel.openCreateUserDialog() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add User")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Existing Users in System:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NpTextSecondary
                            )
                        }
                    }
                }

                items(viewModel.usersList) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${u.firstname} ${u.lastname}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NpTextPrimary
                                )
                                Text(text = u.email, fontSize = 13.sp, color = NpTextSecondary)
                                Text(text = "Role: ${u.role}", fontSize = 12.sp, color = NpBluePrimary)
                            }

                            IconButton(onClick = { viewModel.openResetPasswordDialog(u) }) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Reset Password",
                                    tint = NpBluePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create User Dialog
    if (viewModel.showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateUserDialog() },
            title = { Text("Provision New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.newFirstname,
                        onValueChange = { viewModel.newFirstname = it },
                        label = { Text("First Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newLastname,
                        onValueChange = { viewModel.newLastname = it },
                        label = { Text("Last Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newEmail,
                        onValueChange = { viewModel.newEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newPassword,
                        onValueChange = { viewModel.newPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Role: ", fontWeight = FontWeight.Bold)
                        RadioButton(
                            selected = viewModel.newRole == "USER",
                            onClick = { viewModel.newRole = "USER" }
                        )
                        Text("USER")
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(
                            selected = viewModel.newRole == "ADMIN",
                            onClick = { viewModel.newRole = "ADMIN" }
                        )
                        Text("ADMIN")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createUser(sessionManager) },
                    enabled = !viewModel.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
                ) {
                    Text("Create User")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateUserDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Password Dialog
    if (viewModel.showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetPasswordDialog() },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Resetting password for ${viewModel.selectedUserForReset?.email}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.resetNewPassword,
                        onValueChange = { viewModel.resetNewPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetUserPassword(sessionManager) },
                    enabled = !viewModel.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
                ) {
                    Text("Reset Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetPasswordDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
