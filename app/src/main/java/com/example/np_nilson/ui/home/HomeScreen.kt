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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
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
import com.example.np_nilson.data.api.ReportCategory
import com.example.np_nilson.data.api.StoreDto
import com.example.np_nilson.data.api.UserDto
import com.example.np_nilson.data.session.SessionManager
import com.example.np_nilson.ui.branding.NpNilssonHeaderLogo
import com.example.np_nilson.ui.report.ReportDetailDialog
import com.example.np_nilson.ui.report.ReportListItem
import com.example.np_nilson.ui.theme.*

private const val RECENT_REPORTS_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionManager: SessionManager,
    onLoggedOut: () -> Unit,
    onCreateReport: (ReportCategory?) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initSession(sessionManager)
    }

    val user = viewModel.currentUser
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Hem, 1 = Rapporter, 2 = Profil, 3 = Admin
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
            val profileTabIndex = if (user?.role == "ADMIN") 3 else 2
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
                if (user?.role == "ADMIN") {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                        label = { Text("Admin", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NpBluePrimary,
                            selectedTextColor = NpBluePrimary,
                            indicatorColor = Color(0xFFE2E8F0)
                        )
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil", fontWeight = if (selectedTab == profileTabIndex) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == profileTabIndex,
                    onClick = { selectedTab = profileTabIndex },
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
                    token = sessionManager.getToken(),
                    onCreateReport = onCreateReport,
                    onShowAllReports = { selectedTab = 1 },
                    onActionClicked = { action -> showPlaceholderSnackbar = "$action (kommer i nästa fas)" }
                )
                1 -> ReportsTabContent(
                    user = user,
                    viewModel = viewModel,
                    sessionManager = sessionManager
                )
                2 -> if (user?.role == "ADMIN") {
                    AdminTabContent(
                        viewModel = viewModel,
                        sessionManager = sessionManager
                    )
                } else {
                    ProfileTabContent(
                        user = user,
                        viewModel = viewModel,
                        sessionManager = sessionManager,
                        onLoggedOut = onLoggedOut
                    )
                }
                3 -> ProfileTabContent(
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

    // Report Detail
    viewModel.selectedReport?.let { report ->
        ReportDetailDialog(
            report = report,
            token = sessionManager.getToken(),
            canChangeStatus = user?.role == "ADMIN",
            isUpdatingStatus = viewModel.isUpdatingReportStatus,
            statusError = viewModel.reportStatusError,
            onChangeStatus = { status -> viewModel.updateReportStatus(sessionManager, report, status) },
            onDismiss = { viewModel.closeReport() }
        )
    }

    // Create User Dialog (for Admin)
    if (viewModel.showCreateUserDialog) {
        CreateUserDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Edit User Dialog (for Admin)
    if (viewModel.showEditUserDialog) {
        EditUserDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Delete User Dialog (for Admin)
    if (viewModel.showDeleteUserDialog) {
        DeleteUserDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Reset Password Dialog (for Admin)
    if (viewModel.showResetPasswordDialog) {
        ResetPasswordDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Create Store Dialog (for Admin)
    if (viewModel.showCreateStoreDialog) {
        CreateStoreDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Edit Store Dialog (for Admin)
    if (viewModel.showEditStoreDialog) {
        EditStoreDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }

    // Delete Store Dialog (for Admin)
    if (viewModel.showDeleteStoreDialog) {
        DeleteStoreDialog(
            viewModel = viewModel,
            sessionManager = sessionManager
        )
    }
}

@Composable
fun HomeTabContent(
    user: UserDto?,
    viewModel: HomeViewModel,
    token: String?,
    onCreateReport: (ReportCategory?) -> Unit,
    onShowAllReports: () -> Unit,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NpBlueDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Välkommen, ${user?.firstname ?: "Användare"}! 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user?.store?.storeName ?: if (user?.role == "ADMIN") "Alla butiker (Admin)" else "NP Nilsson Byggmaterial",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onCreateReport(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Skapa ny rapport", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section C — Main Action Grid
        item {
            Text(
                text = "Snabbval & Rapportering",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NpTextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReportCategoryCard(
                        title = "Skadad vara",
                        subtitle = "Rapportera skadat gods",
                        icon = Icons.Outlined.Warning,
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateReport(ReportCategory.DAMAGED_GOODS) }
                    )
                    ReportCategoryCard(
                        title = "Säkerhet",
                        subtitle = "Tillbud & risksituationer",
                        icon = Icons.Default.Security,
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateReport(ReportCategory.SAFETY) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReportCategoryCard(
                        title = "Underhåll",
                        subtitle = "Fastighet & utrustning",
                        icon = Icons.Outlined.Build,
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateReport(ReportCategory.MAINTENANCE) }
                    )
                    ReportCategoryCard(
                        title = "Övrigt",
                        subtitle = "Allmänna avvikelser",
                        icon = Icons.Default.MoreHoriz,
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateReport(ReportCategory.OTHER) }
                    )
                }
            }
        }

        // Section D — Help Text
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = NpBluePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Här kan du enkelt rapportera problem och avvikelser i våra butiker och lager.",
                        fontSize = 13.sp,
                        color = NpTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section E — Recent Reports ("Senaste rapporter")
        item {
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
                TextButton(onClick = onShowAllReports) {
                    Text("Visa alla", color = NpBluePrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = NpBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (viewModel.isLoadingReports && viewModel.reports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NpBluePrimary, modifier = Modifier.size(32.dp))
                }
            }
        } else if (viewModel.reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inga rapporter ännu.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NpTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dina rapporter visas här när de har skapats.",
                            fontSize = 12.sp,
                            color = NpTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val recentReports = viewModel.reports.take(RECENT_REPORTS_COUNT)
            items(recentReports, key = { it.id }) { report ->
                ReportListItem(
                    report = report,
                    token = token,
                    showAuthor = user?.role == "ADMIN",
                    onClick = { viewModel.openReport(report) }
                )
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReportsTabContent(
    user: UserDto?,
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    val isAdmin = user?.role == "ADMIN"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAdmin) "Alla rapporter" else "Mina rapporter",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )
                    Text(
                        text = "Butik: ${user?.store?.storeName ?: "Alla butiker"}",
                        fontSize = 14.sp,
                        color = NpBluePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (viewModel.isLoadingReports) {
                    CircularProgressIndicator(
                        color = NpBluePrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                } else {
                    IconButton(onClick = { viewModel.loadReports(sessionManager) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Uppdatera", tint = NpBluePrimary)
                    }
                }
            }
        }

        viewModel.reportsError?.let { error ->
            item {
                Text(text = error, color = NpErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (viewModel.reports.isEmpty() && !viewModel.isLoadingReports && viewModel.reportsError == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = NpBluePrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Inga inskickade rapporter hittades.",
                        fontSize = 14.sp,
                        color = NpTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(viewModel.reports, key = { it.id }) { report ->
            ReportListItem(
                report = report,
                token = sessionManager.getToken(),
                showAuthor = isAdmin,
                onClick = { viewModel.openReport(report) }
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTabContent(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Användare & Butiker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NpTextPrimary
                )
                Text(
                    text = "Hantera användarkonton, roller, lösenord och butiker",
                    fontSize = 13.sp,
                    color = NpTextSecondary
                )
            }
        }

        // Search Bar for Users
        item {
            OutlinedTextField(
                value = viewModel.userSearchQuery,
                onValueChange = { viewModel.userSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Sök användare på namn, e-post, roll...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Sök", tint = NpBluePrimary) },
                trailingIcon = {
                    if (viewModel.userSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.userSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Rensa")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Status banner if present
        viewModel.statusMessage?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isErrorStatus) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (viewModel.isErrorStatus) NpErrorRed else Color(0xFF166534)
                    )
                }
            }
        }

        // User Management Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NpBluePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Användare (${viewModel.filteredUsersList.size})",
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
        }

        if (viewModel.filteredUsersList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = if (viewModel.userSearchQuery.isBlank()) "Inga användare finns ännu." else "Inga användare matchade sökningen.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 13.sp,
                        color = NpTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(viewModel.filteredUsersList, key = { it.id }) { u ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${u.firstname} ${u.lastname}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = u.email, fontSize = 13.sp, color = NpTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = if (u.role == "ADMIN") Color(0xFFE8F2FC) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = u.role,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (u.role == "ADMIN") NpBluePrimary else NpTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = u.store?.storeName ?: "Ingen butik",
                                        fontSize = 11.sp,
                                        color = NpTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.openEditUserDialog(u) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Redigera användare",
                                    tint = NpBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.openResetPasswordDialog(u) }) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Återställ lösenord",
                                    tint = NpBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.openDeleteUserDialog(u) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Ta bort användare",
                                    tint = NpErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Store Management Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = NpBluePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Butiker (${viewModel.storesList.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )
                }

                Button(
                    onClick = { viewModel.openCreateStoreDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skapa butik", fontSize = 12.sp)
                }
            }
        }

        if (viewModel.storesList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Inga butiker finns registrerade.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 13.sp,
                        color = NpTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(viewModel.storesList, key = { it.storeId }) { store ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store.storeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NpTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = store.address, fontSize = 13.sp, color = NpTextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.openEditStoreDialog(store) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Redigera butik",
                                    tint = NpBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.openDeleteStoreDialog(store) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Ta bort butik",
                                    tint = NpErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreDropdown(
    stores: List<StoreDto>,
    selectedStoreId: Long?,
    onStoreSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = stores.find { it.storeId == selectedStoreId }?.storeName ?: "Ingen specifikt tilldelad"

    Column {
        Text("Tilldelad butik:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NpTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedName, fontSize = 14.sp, color = NpTextPrimary)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NpTextSecondary)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Ingen tilldelad butik") },
                onClick = {
                    onStoreSelected(null)
                    expanded = false
                }
            )
            stores.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store.storeName) },
                    onClick = {
                        onStoreSelected(store.storeId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CreateUserDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissCreateUserDialog() },
        title = { Text("Skapa ny användare") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.newFirstname,
                    onValueChange = { viewModel.newFirstname = it },
                    label = { Text("Förnamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.newLastname,
                    onValueChange = { viewModel.newLastname = it },
                    label = { Text("Efternamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.newEmail,
                    onValueChange = { viewModel.newEmail = it },
                    label = { Text("E-postadress") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.newPassword,
                    onValueChange = { viewModel.newPassword = it },
                    label = { Text("Lösenord") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Roll: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    RadioButton(
                        selected = viewModel.newRole == "USER",
                        onClick = { viewModel.newRole = "USER" }
                    )
                    Text("USER", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(
                        selected = viewModel.newRole == "ADMIN",
                        onClick = { viewModel.newRole = "ADMIN" }
                    )
                    Text("ADMIN", fontSize = 14.sp)
                }

                StoreDropdown(
                    stores = viewModel.storesList,
                    selectedStoreId = viewModel.newStoreId,
                    onStoreSelected = { viewModel.newStoreId = it }
                )
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

@Composable
fun EditUserDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissEditUserDialog() },
        title = { Text("Redigera användare") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.editFirstname,
                    onValueChange = { viewModel.editFirstname = it },
                    label = { Text("Förnamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.editLastname,
                    onValueChange = { viewModel.editLastname = it },
                    label = { Text("Efternamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.editEmail,
                    onValueChange = { viewModel.editEmail = it },
                    label = { Text("E-postadress") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Roll: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    RadioButton(
                        selected = viewModel.editRole == "USER",
                        onClick = { viewModel.editRole = "USER" }
                    )
                    Text("USER", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(
                        selected = viewModel.editRole == "ADMIN",
                        onClick = { viewModel.editRole = "ADMIN" }
                    )
                    Text("ADMIN", fontSize = 14.sp)
                }

                StoreDropdown(
                    stores = viewModel.storesList,
                    selectedStoreId = viewModel.editStoreId,
                    onStoreSelected = { viewModel.editStoreId = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.updateUser(sessionManager) },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissEditUserDialog() }) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun DeleteUserDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    val u = viewModel.userToDelete
    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteUserDialog() },
        title = { Text("Ta bort användare") },
        text = { Text("Är du säker på att du vill ta bort användaren ${u?.firstname} ${u?.lastname} (${u?.email})?") },
        confirmButton = {
            Button(
                onClick = { viewModel.deleteUser(sessionManager) },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NpErrorRed)
            ) {
                Text("Ta bort")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissDeleteUserDialog() }) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun ResetPasswordDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
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
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
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

@Composable
fun CreateStoreDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissCreateStoreDialog() },
        title = { Text("Skapa ny butik") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.newStoreName,
                    onValueChange = { viewModel.newStoreName = it },
                    label = { Text("Butiksnamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.newStoreAddress,
                    onValueChange = { viewModel.newStoreAddress = it },
                    label = { Text("Adress") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.createStore(sessionManager) },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
            ) {
                Text("Skapa")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissCreateStoreDialog() }) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun EditStoreDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissEditStoreDialog() },
        title = { Text("Redigera butik") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.editStoreName,
                    onValueChange = { viewModel.editStoreName = it },
                    label = { Text("Butiksnamn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = viewModel.editStoreAddress,
                    onValueChange = { viewModel.editStoreAddress = it },
                    label = { Text("Adress") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.updateStore(sessionManager) },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary)
            ) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissEditStoreDialog() }) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun DeleteStoreDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    val s = viewModel.storeToDelete
    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteStoreDialog() },
        title = { Text("Ta bort butik") },
        text = { Text("Är du säker på att du vill ta bort butiken ${s?.storeName} (${s?.address})?") },
        confirmButton = {
            Button(
                onClick = { viewModel.deleteStore(sessionManager) },
                enabled = !viewModel.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NpErrorRed)
            ) {
                Text("Ta bort")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissDeleteStoreDialog() }) {
                Text("Avbryt")
            }
        }
    )
}
