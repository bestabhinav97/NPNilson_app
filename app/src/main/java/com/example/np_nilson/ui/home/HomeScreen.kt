package com.example.np_nilson.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Hem, 1 = Rapporter, 2 = Profil
    var showPlaceholderSnackbar by remember { mutableStateOf<String?>(null) }
    var showProfileMenuDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showPlaceholderSnackbar) {
        showPlaceholderSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            showPlaceholderSnackbar = null
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        NpNilssonHeaderLogo()
                        Text(
                            text = "NP Nilsson Rapport",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NpTextSecondary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                            .clickable { showProfileMenuDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profil",
                            tint = NpTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Hem") },
                    label = { Text("Hem", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NpBluePrimary,
                        selectedTextColor = NpBluePrimary,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Description, contentDescription = "Rapporter") },
                    label = { Text("Rapporter", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NpBluePrimary,
                        selectedTextColor = NpBluePrimary,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NpBluePrimary,
                        selectedTextColor = NpBluePrimary,
                        indicatorColor = Color(0xFFE2E8F0)
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NpBackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeTabContent(
                    user = user,
                    viewModel = viewModel,
                    onActionClicked = { action -> showPlaceholderSnackbar = "$action (kommer i nästa fas)" }
                )
                1 -> ReportsTabContent(
                    user = user
                )
                2 -> ProfileTabContent(
                    user = user,
                    viewModel = viewModel,
                    sessionManager = sessionManager,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }

    // Profile Menu Dialog
    if (showProfileMenuDialog) {
        AlertDialog(
            onDismissRequest = { showProfileMenuDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NpBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${user?.firstname ?: "Användare"} ${user?.lastname ?: ""}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("E-post: ${user?.email ?: ""}", fontSize = 14.sp)
                    Text("Roll: ${user?.role ?: "USER"}", fontSize = 14.sp)
                    Text("Butik: ${user?.store?.storeName ?: "Ingen tilldelad butik"}", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileMenuDialog = false
                        viewModel.logout(sessionManager, onLoggedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NpErrorRed)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logga ut")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileMenuDialog = false }) {
                    Text("Stäng")
                }
            }
        )
    }

    // Create User Dialog (for Admin)
    if (viewModel.showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateUserDialog() },
            title = { Text("Skapa ny användare (Admin)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.newFirstname,
                        onValueChange = { viewModel.newFirstname = it },
                        label = { Text("Förnamn") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newLastname,
                        onValueChange = { viewModel.newLastname = it },
                        label = { Text("Efternamn") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newEmail,
                        onValueChange = { viewModel.newEmail = it },
                        label = { Text("E-postadress") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.newPassword,
                        onValueChange = { viewModel.newPassword = it },
                        label = { Text("Lösenord") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Roll: ", fontWeight = FontWeight.Bold)
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
                    Text("Skapa")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateUserDialog() }) {
                    Text("Avbryt")
                }
            }
        )
    }

    // Reset Password Dialog (for Admin)
    if (viewModel.showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetPasswordDialog() },
            title = { Text("Återställ lösenord") },
            text = {
                Column {
                    Text("Återställer lösenord för ${viewModel.selectedUserForReset?.email}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.resetNewPassword,
                        onValueChange = { viewModel.resetNewPassword = it },
                        label = { Text("Nytt lösenord") },
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
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetPasswordDialog() }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

@Composable
fun HomeTabContent(
    user: UserDto?,
    viewModel: HomeViewModel,
    onActionClicked: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section B — Personalized Greeting & Store Info
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Hej, ${user?.firstname ?: "Test"}!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NpTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Här kan du enkelt rapportera problem och avvikelser i våra butiker och lager.",
                    fontSize = 14.sp,
                    color = NpTextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Assigned Store Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F2FC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Din butik: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NpBlueDark
                        )
                        Text(
                            text = user?.store?.storeName ?: if (user?.role == "ADMIN") "Alla butiker (Admin)" else "NP Nilsson Testbutik Båstad",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NpBluePrimary
                        )
                    }
                }
            }
        }

        // Section C — Create Report Card ("Skapa ny rapport")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickable { onActionClicked("Skapa ny rapport") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NpBluePrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Camera icon inside circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Kamera",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Skapa ny rapport",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ta ett foto, lägg till anteckningar och skicka in en rapport.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Gå vidare",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Section D — Common Report Types ("Vanliga rapporttyper")
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vanliga rapporttyper",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NpTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid of Category Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCategoryCard(
                            title = "Skadat gods",
                            subtitle = "Rapportera trasiga eller skadade varor.",
                            icon = Icons.Outlined.Inventory2,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionClicked("Skadat gods") }
                        )
                        ReportCategoryCard(
                            title = "Säkerhet",
                            subtitle = "Risker, tillbud eller säkerhetsproblem.",
                            icon = Icons.Outlined.Warning,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionClicked("Säkerhet") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCategoryCard(
                            title = "Underhåll",
                            subtitle = "Fel, reparationer eller underhållsbehov.",
                            icon = Icons.Outlined.Build,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionClicked("Underhåll") }
                        )
                        ReportCategoryCard(
                            title = "Övrigt",
                            subtitle = "Andra avvikelser eller förbättringsförslag.",
                            icon = Icons.Default.MoreHoriz,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionClicked("Övrigt") }
                        )
                    }
                }
            }
        }

        // Section E — Recent Reports ("Senaste rapporter")
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Senaste rapporter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )

                    Text(
                        text = "Visa alla >",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NpBluePrimary,
                        modifier = Modifier.clickable { onActionClicked("Visa alla rapporter") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Empty State Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = NpTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Inga rapporter ännu.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NpTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Dina rapporter visas här när de har skapats.",
                            fontSize = 13.sp,
                            color = NpTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Admin Provisioning Section (Visible only for ADMIN role)
        if (user?.role == "ADMIN") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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
                                    text = "Användaradministration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NpTextPrimary
                                )
                            }

                            Button(
                                onClick = { viewModel.openCreateUserDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Skapa användare", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Befintliga användare i systemet:",
                            fontSize = 13.sp,
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${u.firstname} ${u.lastname}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NpTextPrimary
                            )
                            Text(text = u.email, fontSize = 12.sp, color = NpTextSecondary)
                            Text(
                                text = "Roll: ${u.role} | Butik: ${u.store?.storeName ?: "Ingen"}",
                                fontSize = 11.sp,
                                color = NpBluePrimary
                            )
                        }

                        IconButton(onClick = { viewModel.openResetPasswordDialog(u) }) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = "Återställ lösenord",
                                tint = NpBluePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F2FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = NpBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = NpTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NpTextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = NpTextSecondary,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun ReportsTabContent(
    user: UserDto?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = NpBluePrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mina rapporter",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NpTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Butik: ${user?.store?.storeName ?: "Alla butiker"}",
            fontSize = 14.sp,
            color = NpBluePrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Inga inskickade rapporter hittades. Fullständig rapporthantering aktiveras i nästa fas.",
            fontSize = 13.sp,
            color = NpTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun ProfileTabContent(
    user: UserDto?,
    viewModel: HomeViewModel,
    sessionManager: SessionManager,
    onLoggedOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F2FC))
                .border(2.dp, NpBluePrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = NpBluePrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${user?.firstname ?: ""} ${user?.lastname ?: ""}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NpTextPrimary
        )

        Text(
            text = user?.email ?: "",
            fontSize = 14.sp,
            color = NpTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Roll", fontWeight = FontWeight.Medium, color = NpTextSecondary)
                    Text(user?.role ?: "USER", fontWeight = FontWeight.Bold, color = NpBluePrimary)
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tilldelad butik", fontWeight = FontWeight.Medium, color = NpTextSecondary)
                    Text(
                        user?.store?.storeName ?: if (user?.role == "ADMIN") "Alla (Admin)" else "Ej angiven",
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )
                }
                if (user?.store?.address != null) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Adress", fontWeight = FontWeight.Medium, color = NpTextSecondary)
                        Text(user.store.address, fontWeight = FontWeight.Normal, color = NpTextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.logout(sessionManager, onLoggedOut) },
            colors = ButtonDefaults.buttonColors(containerColor = NpErrorRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logga ut", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
