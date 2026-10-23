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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                        sessionManager = sessionManager,
                        onShowReports = { selectedTab = 1 }
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("ALL") }

    val reports = viewModel.reports

    val openCount = reports.count { it.status == "OPEN" }
    val inProgressCount = reports.count { it.status == "IN_PROGRESS" }
    val resolvedCount = reports.count { it.status == "RESOLVED" }

    val filteredReports = reports.filter { report ->
        val matchesStatus =
            selectedStatus == "ALL" || report.status == selectedStatus

        val searchableText = buildString {
            append(report.title)
            append(" ")
            append(report.description)
            append(" ")
            append(report.storeName.orEmpty())
            append(" ")
            append(report.author.firstname)
            append(" ")
            append(report.author.lastname)
        }

        val matchesSearch =
            searchQuery.isBlank() ||
                    searchableText.contains(searchQuery, ignoreCase = true)

        matchesStatus && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isAdmin) "Inkomna rapporter" else "Mina rapporter",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NpTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isAdmin)
                        "Hantera och följ upp inkomna rapporter."
                    else
                        "Se status och information för dina rapporter.",
                    fontSize = 14.sp,
                    color = NpTextSecondary
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Sök rapporter") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportFilterChip(
                    label = "Alla",
                    count = reports.size,
                    selected = selectedStatus == "ALL",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "ALL" }
                )
                ReportFilterChip(
                    label = "Ny",
                    count = openCount,
                    selected = selectedStatus == "OPEN",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "OPEN" }
                )
                ReportFilterChip(
                    label = "Pågår",
                    count = inProgressCount,
                    selected = selectedStatus == "IN_PROGRESS",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "IN_PROGRESS" }
                )
                ReportFilterChip(
                    label = "Klar",
                    count = resolvedCount,
                    selected = selectedStatus == "RESOLVED",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedStatus = "RESOLVED" }
                )
            }
        }

        if (viewModel.isLoadingReports) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NpBluePrimary)
                }
            }
        }

        viewModel.reportsError?.let { error ->
            item {
                Text(
                    text = error,
                    color = NpErrorRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (
            filteredReports.isEmpty() &&
            !viewModel.isLoadingReports &&
            viewModel.reportsError == null
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = NpTextSecondary,
                            modifier = Modifier.size(52.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Inga rapporter hittades",
                            fontWeight = FontWeight.Bold,
                            color = NpTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Prova att ändra sökning eller filter.",
                            fontSize = 13.sp,
                            color = NpTextSecondary
                        )
                    }
                }
            }
        }

        items(filteredReports, key = { it.id }) { report ->
            ReportListItem(
                report = report,
                token = sessionManager.getToken(),
                showAuthor = isAdmin,
                onClick = { viewModel.openReport(report) }
            )
        }

        item {
            OutlinedButton(
                onClick = { viewModel.loadReports(sessionManager) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uppdatera rapporter")
            }
        }
    }
}

@Composable
private fun ReportFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) NpBluePrimary else Color.White,
        border = if (selected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFD9E0E7)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else NpTextPrimary
            )
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                color = if (selected)
                    Color.White.copy(alpha = 0.85f)
                else
                    NpTextSecondary
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
    sessionManager: SessionManager,
    onShowReports: () -> Unit
) {
    var section by remember { mutableIntStateOf(0) }
    var roleFilter by remember { mutableStateOf("ALL") }
    var activeFilter by remember { mutableStateOf("ALL") }
    var storeFilter by remember { mutableStateOf<Long?>(null) }
    var storeMenuOpen by remember { mutableStateOf(false) }
    var userMenuId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers(sessionManager)
        viewModel.loadStores(sessionManager)
        viewModel.loadAdminDashboard(sessionManager)
    }

    val dashboard = viewModel.adminDashboard
    val currentUserId = viewModel.currentUser?.id

    val users = viewModel.usersList.filter { user ->
        val query = viewModel.userSearchQuery.trim()

        val searchMatches =
            query.isBlank() ||
                    "${user.firstname} ${user.lastname}".contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.role.contains(query, ignoreCase = true) ||
                    (user.store?.storeName?.contains(query, ignoreCase = true) == true)

        val roleMatches =
            roleFilter == "ALL" || user.role == roleFilter

        val activeMatches =
            when (activeFilter) {
                "ACTIVE" -> user.isActive
                "INACTIVE" -> !user.isActive
                else -> true
            }

        val storeMatches =
            storeFilter == null || user.store?.storeId == storeFilter

        searchMatches && roleMatches && activeMatches && storeMatches
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Administration",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NpTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hantera personal, butiker och rapportflöde.",
                    fontSize = 14.sp,
                    color = NpTextSecondary
                )
            }
        }

        viewModel.statusMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isErrorStatus) {
                            Color(0xFFFEE2E2)
                        } else {
                            Color(0xFFDCFCE7)
                        }
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = if (viewModel.isErrorStatus) NpErrorRed else Color(0xFF166534),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        value = dashboard?.totalUsers?.toString() ?: "–",
                        label = "Användare",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        value = dashboard?.adminCount?.toString() ?: "–",
                        label = "Admins",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminMetricCard(
                        value = dashboard?.storeCount?.toString() ?: "–",
                        label = "Butiker",
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        value = dashboard?.openReports?.toString() ?: "–",
                        label = "Nya rapporter",
                        modifier = Modifier.weight(1f),
                        onClick = onShowReports
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rapportstatus",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NpTextPrimary
                        )

                        if (viewModel.isLoadingAdminDashboard) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = NpBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AdminReportStat("Ny", dashboard?.openReports ?: 0)
                        AdminReportStat("Pågår", dashboard?.inProgressReports ?: 0)
                        AdminReportStat("Klar", dashboard?.resolvedReports ?: 0)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onShowReports,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Visa alla rapporter")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Användare", "Butiker", "Aktivitet").forEachIndexed { index, label ->
                    Surface(
                        onClick = { section = index },
                        modifier = Modifier.weight(1f),
                        color = if (section == index) NpBluePrimary else Color.Transparent,
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            color = if (section == index) Color.White else NpTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (section == 0) {
            item {
                OutlinedTextField(
                    value = viewModel.userSearchQuery,
                    onValueChange = { viewModel.userSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Sök namn, e-post, roll eller butik") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Sök")
                    },
                    trailingIcon = {
                        if (viewModel.userSearchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.userSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Rensa")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminChoiceChip(
                            label = "Alla",
                            selected = roleFilter == "ALL",
                            onClick = { roleFilter = "ALL" }
                        )
                        AdminChoiceChip(
                            label = "Admin",
                            selected = roleFilter == "ADMIN",
                            onClick = { roleFilter = "ADMIN" }
                        )
                        AdminChoiceChip(
                            label = "Personal",
                            selected = roleFilter == "USER",
                            onClick = { roleFilter = "USER" }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminChoiceChip(
                            label = "Alla status",
                            selected = activeFilter == "ALL",
                            onClick = { activeFilter = "ALL" }
                        )
                        AdminChoiceChip(
                            label = "Aktiva",
                            selected = activeFilter == "ACTIVE",
                            onClick = { activeFilter = "ACTIVE" }
                        )
                        AdminChoiceChip(
                            label = "Inaktiva",
                            selected = activeFilter == "INACTIVE",
                            onClick = { activeFilter = "INACTIVE" }
                        )
                    }

                    Box {
                        OutlinedButton(onClick = { storeMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                viewModel.storesList
                                    .firstOrNull { it.storeId == storeFilter }
                                    ?.storeName
                                    ?: "Alla butiker"
                            )
                        }

                        DropdownMenu(
                            expanded = storeMenuOpen,
                            onDismissRequest = { storeMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alla butiker") },
                                onClick = {
                                    storeFilter = null
                                    storeMenuOpen = false
                                }
                            )

                            viewModel.storesList.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.storeName) },
                                    onClick = {
                                        storeFilter = store.storeId
                                        storeMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Användare (${users.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )

                    Button(
                        onClick = { viewModel.openCreateUserDialog() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Ny användare")
                    }
                }
            }

            if (users.isEmpty()) {
                item {
                    AdminEmptyState("Inga användare matchar de valda filtren.")
                }
            } else {
                items(users, key = { it.id }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F2FC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${user.firstname.firstOrNull() ?: '?'}${user.lastname.firstOrNull() ?: '?'}",
                                    color = NpBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${user.firstname} ${user.lastname}",
                                        fontWeight = FontWeight.Bold,
                                        color = NpTextPrimary
                                    )

                                    if (user.id == currentUserId) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        AdminSmallPill("Du")
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = NpTextSecondary
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AdminSmallPill(
                                        if (user.role == "ADMIN") "Admin" else "Personal"
                                    )
                                    AdminSmallPill(user.store?.storeName ?: "Ingen butik")

                                    if (!user.isActive) {
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Inaktiv",
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                ),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NpErrorRed
                                            )
                                        }
                                    }
                                }
                            }

                            Box {
                                IconButton(onClick = { userMenuId = user.id }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Åtgärder"
                                    )
                                }

                                DropdownMenu(
                                    expanded = userMenuId == user.id,
                                    onDismissRequest = { userMenuId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Redigera") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            userMenuId = null
                                            viewModel.openEditUserDialog(user)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Byt lösenord") },
                                        leadingIcon = {
                                            Icon(Icons.Default.LockReset, contentDescription = null)
                                        },
                                        onClick = {
                                            userMenuId = null
                                            viewModel.openResetPasswordDialog(user)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (user.isActive) "Inaktivera" else "Aktivera"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (user.isActive) {
                                                    Icons.Default.Block
                                                } else {
                                                    Icons.Default.CheckCircle
                                                },
                                                contentDescription = null
                                            )
                                        },
                                        enabled = user.id != currentUserId,
                                        onClick = {
                                            userMenuId = null
                                            viewModel.setUserActive(
                                                sessionManager = sessionManager,
                                                user = user,
                                                active = !user.isActive
                                            )
                                        }
                                    )

                                    HorizontalDivider()

                                    DropdownMenuItem(
                                        text = { Text("Ta bort", color = NpErrorRed) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = NpErrorRed
                                            )
                                        },
                                        enabled = user.id != currentUserId,
                                        onClick = {
                                            userMenuId = null
                                            viewModel.openDeleteUserDialog(user)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (section == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Butiker (${viewModel.storesList.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NpTextPrimary
                    )

                    Button(
                        onClick = { viewModel.openCreateStoreDialog() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Ny butik")
                    }
                }
            }

            if (viewModel.storesList.isEmpty()) {
                item {
                    AdminEmptyState("Inga butiker finns registrerade.")
                }
            } else {
                items(viewModel.storesList, key = { it.storeId }) { store ->
                    val stats = dashboard?.stores
                        ?.firstOrNull { it.storeId == store.storeId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = store.storeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NpTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = store.address,
                                        color = NpTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.openEditStoreDialog(store) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Redigera butik",
                                            tint = NpBluePrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.openDeleteStoreDialog(store) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Ta bort butik",
                                            tint = NpErrorRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminSmallPill("${stats?.userCount ?: 0} användare")
                                AdminSmallPill("${stats?.openReports ?: 0} nya rapporter")
                            }
                        }
                    }
                }
            }
        }

        if (section == 2) {
            item {
                Text(
                    text = "Senaste adminaktivitet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NpTextPrimary
                )
            }

            val activity = dashboard?.recentActivity.orEmpty()

            if (activity.isEmpty()) {
                item {
                    AdminEmptyState("Ingen adminaktivitet har registrerats ännu.")
                }
            } else {
                items(activity, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = adminActivityLabel(entry.action),
                                fontWeight = FontWeight.SemiBold,
                                color = NpTextPrimary
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "${entry.actorName} • ${entry.details ?: entry.targetType}",
                                fontSize = 12.sp,
                                color = NpTextSecondary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = entry.createdAt
                                    .replace("T", " ")
                                    .take(16),
                                fontSize = 11.sp,
                                color = NpTextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    viewModel.loadUsers(sessionManager)
                    viewModel.loadStores(sessionManager)
                    viewModel.loadReports(sessionManager)
                    viewModel.loadAdminDashboard(sessionManager)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uppdatera administration")
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NpBluePrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = NpTextSecondary
            )
        }
    }
}

@Composable
private fun AdminReportStat(
    label: String,
    count: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NpTextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = NpTextSecondary
        )
    }
}

@Composable
private fun AdminChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) NpBluePrimary else Color.White,
        shape = RoundedCornerShape(50),
        border = if (selected) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFFD9E0E7)
            )
        }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (selected) Color.White else NpTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AdminSmallPill(
    text: String
) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color = NpTextSecondary
        )
    }
}

@Composable
private fun AdminEmptyState(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = TextAlign.Center,
            color = NpTextSecondary
        )
    }
}

private fun adminActivityLabel(
    action: String
): String {
    return when (action) {
        "USER_CREATED" -> "Användare skapad"
        "USER_UPDATED" -> "Användare uppdaterad"
        "USER_ACTIVATED" -> "Användare aktiverad"
        "USER_DEACTIVATED" -> "Användare inaktiverad"
        "USER_DELETED" -> "Användare borttagen"
        "PASSWORD_RESET" -> "Lösenord återställt"
        "STORE_CREATED" -> "Butik skapad"
        "STORE_UPDATED" -> "Butik uppdaterad"
        "STORE_DELETED" -> "Butik borttagen"
        else -> action
    }
}

@Composable
private fun AdminFullScreenForm(
    title: String,
    primaryLabel: String,
    isLoading: Boolean,
    onClose: () -> Unit,
    onPrimary: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NpBackgroundLight
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Avbryt")
                    }

                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NpTextPrimary
                    )

                    Spacer(modifier = Modifier.width(64.dp))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content
                )

                Button(
                    onClick = onPrimary,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NpBluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = primaryLabel,
                            fontWeight = FontWeight.Bold
                        )
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
    AdminFullScreenForm(
        title = "Ny användare",
        primaryLabel = "Skapa användare",
        isLoading = viewModel.isLoading,
        onClose = { viewModel.dismissCreateUserDialog() },
        onPrimary = { viewModel.createUser(sessionManager) }
    ) {
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
            supportingText = { Text("Minst 8 tecken rekommenderas") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Roll",
            fontWeight = FontWeight.Bold,
            color = NpTextPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = viewModel.newRole == "USER",
                onClick = { viewModel.newRole = "USER" }
            )
            Text("Personal")

            Spacer(modifier = Modifier.width(20.dp))

            RadioButton(
                selected = viewModel.newRole == "ADMIN",
                onClick = { viewModel.newRole = "ADMIN" }
            )
            Text("Admin")
        }

        StoreDropdown(
            stores = viewModel.storesList,
            selectedStoreId = viewModel.newStoreId,
            onStoreSelected = { viewModel.newStoreId = it }
        )
    }
}

@Composable
fun EditUserDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AdminFullScreenForm(
        title = "Redigera användare",
        primaryLabel = "Spara ändringar",
        isLoading = viewModel.isLoading,
        onClose = { viewModel.dismissEditUserDialog() },
        onPrimary = { viewModel.updateUser(sessionManager) }
    ) {
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

        Text(
            text = "Roll",
            fontWeight = FontWeight.Bold,
            color = NpTextPrimary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = viewModel.editRole == "USER",
                onClick = { viewModel.editRole = "USER" }
            )
            Text("Personal")

            Spacer(modifier = Modifier.width(20.dp))

            RadioButton(
                selected = viewModel.editRole == "ADMIN",
                onClick = { viewModel.editRole = "ADMIN" }
            )
            Text("Admin")
        }

        StoreDropdown(
            stores = viewModel.storesList,
            selectedStoreId = viewModel.editStoreId,
            onStoreSelected = { viewModel.editStoreId = it }
        )
    }
}

@Composable
fun DeleteUserDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    val user = viewModel.userToDelete

    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteUserDialog() },
        title = { Text("Ta bort användare?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${user?.firstname.orEmpty()} ${user?.lastname.orEmpty()}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email.orEmpty(),
                    color = NpTextSecondary
                )
                Text(
                    text = "Användare med rapporthistorik kan inte tas bort. Inaktivera kontot i stället. Den här åtgärden kan inte ångras.",
                    fontSize = 13.sp,
                    color = NpTextSecondary
                )
            }
        },
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
    AdminFullScreenForm(
        title = "Ny butik",
        primaryLabel = "Skapa butik",
        isLoading = viewModel.isLoading,
        onClose = { viewModel.dismissCreateStoreDialog() },
        onPrimary = { viewModel.createStore(sessionManager) }
    ) {
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
}

@Composable
fun EditStoreDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    AdminFullScreenForm(
        title = "Redigera butik",
        primaryLabel = "Spara ändringar",
        isLoading = viewModel.isLoading,
        onClose = { viewModel.dismissEditStoreDialog() },
        onPrimary = { viewModel.updateStore(sessionManager) }
    ) {
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
}

@Composable
fun DeleteStoreDialog(
    viewModel: HomeViewModel,
    sessionManager: SessionManager
) {
    val store = viewModel.storeToDelete

    AlertDialog(
        onDismissRequest = { viewModel.dismissDeleteStoreDialog() },
        title = { Text("Ta bort butik?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = store?.storeName.orEmpty(),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = store?.address.orEmpty(),
                    color = NpTextSecondary
                )
                Text(
                    text = "Butiken kan inte tas bort om den har tilldelade användare eller rapporthistorik. Den här åtgärden kan inte ångras.",
                    fontSize = 13.sp,
                    color = NpTextSecondary
                )
            }
        },
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
