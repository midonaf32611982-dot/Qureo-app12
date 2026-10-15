package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivationCodeEntity
import com.example.data.model.UserEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberMetricCard
import com.example.ui.components.TechInputField
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.SprixCardBg
import com.example.ui.theme.SprixCardBorder
import com.example.ui.theme.SprixDarkBg
import com.example.ui.theme.SprixDarkBgEnd
import com.example.ui.theme.SprixDarkBgMid
import com.example.ui.theme.SprixEmerald
import com.example.ui.theme.SprixEmeraldBorder
import com.example.ui.theme.SprixEmeraldContainer
import com.example.ui.theme.SprixEmeraldDark
import com.example.ui.theme.SprixSurfaceDark
import com.example.ui.theme.TechError
import com.example.ui.theme.TechSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Users, 2: Activation Codes, 3: Servers, 4: Logs
    val isArabic by viewModel.isArabic.collectAsState()

    val totalUsers by viewModel.totalUsersCount.collectAsState()
    val onlineUsers by viewModel.onlineUsersCount.collectAsState()
    val activeUsers by viewModel.activeUsersCount.collectAsState()
    val suspendedUsers by viewModel.suspendedUsersCount.collectAsState()
    val usersList by viewModel.allUsers.collectAsState()
    val codesList by viewModel.activationCodes.collectAsState()
    val logsList by viewModel.activityLogs.collectAsState()
    val serverNodes by viewModel.serverNodes.collectAsState()

    val searchQuery by viewModel.userSearchQuery.collectAsState()
    val statusFilter by viewModel.userStatusFilter.collectAsState()

    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    val showAddUserDialog by viewModel.showAddUserDialog.collectAsState()
    val showGenerateCodeDialog by viewModel.showGenerateCodeDialog.collectAsState()

    val tabs = if (isArabic) {
        listOf("نظرة عامة", "المستخدمين", "أكواد التفعيل", "السيرفرات", "سجل العمليات")
    } else {
        listOf("Overview", "Users", "Codes", "Servers", "Audit Logs")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SprixDarkBg,
                        SprixDarkBgMid,
                        SprixDarkBgEnd
                    )
                )
            )
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                modifier = Modifier.testTag("admin_panel_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SprixEmerald
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "لوحة تحكم الإدارة الاحترافية" else "Sprix Admin Control Center",
                    color = SprixEmerald,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "إدارة النظام والخوادم والاشتراكات" else "Unified Administrative Infrastructure",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Navigation Tabs styled in Sprix Theme
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SprixSurfaceDark,
            contentColor = SprixEmerald,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SprixEmerald,
                    height = 2.5.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tabTitle,
                            color = if (selectedTab == index) SprixEmerald else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> AdminOverviewTab(
                totalUsers = totalUsers,
                onlineUsers = onlineUsers,
                activeUsers = activeUsers,
                suspendedUsers = suspendedUsers,
                serversOnline = serverNodes.count { it.isOnline },
                totalServers = serverNodes.size,
                recentLogs = logsList.take(5),
                isArabic = isArabic,
                onAddUserClick = { viewModel.showAddUserDialog.value = true },
                onGenerateCodeClick = { viewModel.showGenerateCodeDialog.value = true },
                onNavigateToScreen = { screen -> viewModel.navigateTo(screen) }
            )
            1 -> AdminUsersTab(
                users = usersList,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.userSearchQuery.value = it },
                statusFilter = statusFilter,
                onStatusFilterChange = { viewModel.userStatusFilter.value = it },
                onToggleStatus = { viewModel.toggleUserStatus(it) },
                onDeleteUser = { userToDelete = it },
                onAddUserClick = { viewModel.showAddUserDialog.value = true },
                isArabic = isArabic
            )
            2 -> AdminCodesTab(
                codes = codesList,
                onGenerateCodeClick = { viewModel.showGenerateCodeDialog.value = true },
                onRevokeCode = { viewModel.toggleServerNode("") },
                onCodeCopied = { code -> viewModel.snackbarMessage.value = if (isArabic) "تم نسخ الكود: $code" else "Copied code: $code" },
                isArabic = isArabic
            )
            3 -> AdminServersTab(
                serverNodes = serverNodes,
                onToggleServer = { viewModel.toggleServerNode(it) },
                isArabic = isArabic
            )
            4 -> AdminLogsTab(
                logs = logsList,
                isArabic = isArabic
            )
        }
    }

    // Confirmation Delete Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = SprixCardBg,
            title = {
                Text(
                    text = if (isArabic) "تأكيد حذف الحساب" else "Confirm Delete User",
                    color = TechError,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic) {
                        "هل أنت متأكد من حذف حساب ${userToDelete?.username} نهائيًا؟ هذه العملية لا يمكن التراجع عنها."
                    } else {
                        "Are you sure you want to permanently delete user '${userToDelete?.username}'? This action cannot be undone."
                    },
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete?.let { viewModel.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TechError)
                ) {
                    Text(text = if (isArabic) "حذف نهائي" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Add User Modal
    if (showAddUserDialog) {
        AddUserModal(
            onDismiss = { viewModel.showAddUserDialog.value = false },
            onSubmit = { user, pass, role, plan, sessions, servers ->
                viewModel.createUser(user, pass, role, plan, sessions, servers)
            },
            isArabic = isArabic
        )
    }

    // Generate Codes Modal
    if (showGenerateCodeDialog) {
        GenerateCodeModal(
            onDismiss = { viewModel.showGenerateCodeDialog.value = false },
            onSubmitSingle = { plan, duration, maxUses ->
                viewModel.generateCode(plan, duration, maxUses)
            },
            onSubmitBulk = { count, plan, duration ->
                viewModel.generateBulkCodes(count, plan, duration)
            },
            isArabic = isArabic
        )
    }
}

@Composable
fun AdminOverviewTab(
    totalUsers: Int,
    onlineUsers: Int,
    activeUsers: Int,
    suspendedUsers: Int,
    serversOnline: Int,
    totalServers: Int,
    recentLogs: List<com.example.data.model.ActivityLogEntity>,
    isArabic: Boolean,
    onAddUserClick: () -> Unit,
    onGenerateCodeClick: () -> Unit,
    onNavigateToScreen: (AppScreen) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SprixEmeraldContainer)
                        .border(1.dp, SprixEmeraldBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onAddUserClick)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = SprixEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isArabic) "إضافة مستخدم" else "Add User", color = SprixEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF261D0C))
                        .border(1.dp, Color(0xFF855D15), RoundedCornerShape(14.dp))
                        .clickable(onClick = onGenerateCodeClick)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isArabic) "توليد كود تفعيل" else "Generate Code", color = Color(0xFFFDE68A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Tools Launchers for Admin
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SprixCardBg)
                    .border(1.dp, SprixCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (isArabic) "تشغيل سريع لأدوات المنصة" else "Quick Platform Tool Shortcuts",
                    color = SprixEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F261D))
                            .border(1.dp, Color(0xFF1E4D3B), RoundedCornerShape(10.dp))
                            .clickable { onNavigateToScreen(AppScreen.QUREO_SOLVER) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Qureo Solver", color = SprixEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F261D))
                            .border(1.dp, Color(0xFF1E4D3B), RoundedCornerShape(10.dp))
                            .clickable { onNavigateToScreen(AppScreen.SPRIX_SOLVER) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isArabic) "الثقافة المالية" else "Sprix Financial", color = SprixEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Metrics Grid (Row 1)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberMetricCard(
                    title = if (isArabic) "إجمالي المستخدمين" else "Total Accounts",
                    value = "$totalUsers",
                    icon = Icons.Default.People,
                    accentColor = SprixEmerald,
                    subtitle = if (isArabic) "$activeUsers نشط" else "$activeUsers Active",
                    modifier = Modifier.weight(1f)
                )
                CyberMetricCard(
                    title = if (isArabic) "المتصلون الآن" else "Online Sessions",
                    value = "$onlineUsers",
                    icon = Icons.Default.Wifi,
                    accentColor = TechSuccess,
                    subtitle = if (isArabic) "مستخدمين نشطين" else "Real-time active",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid (Row 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberMetricCard(
                    title = if (isArabic) "الحسابات الموقوفة" else "Suspended",
                    value = "$suspendedUsers",
                    icon = Icons.Default.Block,
                    accentColor = if (suspendedUsers > 0) TechError else TextMuted,
                    subtitle = if (isArabic) "بحاجة مراجعة" else "Restricted users",
                    modifier = Modifier.weight(1f)
                )
                CyberMetricCard(
                    title = if (isArabic) "عقد الخوادم" else "Cluster Nodes",
                    value = "$serversOnline / $totalServers",
                    icon = Icons.Default.Dns,
                    accentColor = Color(0xFFFBBF24),
                    subtitle = if (isArabic) "Tokyo, US, EU, SG" else "Multi-region online",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Administrative Activity Feed
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SprixCardBg)
                    .border(1.dp, SprixCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (isArabic) "آخر العمليات الإدارية الحقيقية" else "Recent Administrative Audit Log",
                    color = SprixEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (recentLogs.isEmpty()) {
                    Text(text = "No logs yet.", color = TextMuted, fontSize = 12.sp)
                } else {
                    recentLogs.forEach { log ->
                        val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${log.action} - ${log.targetUser}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = log.details, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                            }
                            Text(text = dateStr, color = SprixEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(
    users: List<UserEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    onToggleStatus: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onAddUserClick: () -> Unit,
    isArabic: Boolean
) {
    val filtered = users.filter { u ->
        val matchSearch = searchQuery.isBlank() || u.username.contains(searchQuery.trim(), ignoreCase = true)
        val matchFilter = when (statusFilter) {
            "ACTIVE" -> u.isActive
            "SUSPENDED" -> !u.isActive
            "ONLINE" -> u.isOnline
            else -> true
        }
        matchSearch && matchFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(text = if (isArabic) "بحث بالاسم..." else "Search username...", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SprixEmerald, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF091F18),
                    unfocusedContainerColor = Color(0xFF091F18),
                    focusedBorderColor = SprixEmerald,
                    unfocusedBorderColor = SprixCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SprixEmeraldContainer)
                    .border(1.dp, SprixEmeraldBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onAddUserClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SprixEmerald)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filterOptions = listOf("ALL", "ONLINE", "ACTIVE", "SUSPENDED")
            filterOptions.forEach { opt ->
                val isSel = statusFilter == opt
                val label = when (opt) {
                    "ONLINE" -> if (isArabic) "متصل" else "Online"
                    "ACTIVE" -> if (isArabic) "نشط" else "Active"
                    "SUSPENDED" -> if (isArabic) "موقوف" else "Suspended"
                    else -> if (isArabic) "الكل" else "All"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) SprixEmerald else Color(0xFF091F18))
                        .border(1.dp, if (isSel) SprixEmerald else SprixCardBorder, RoundedCornerShape(10.dp))
                        .clickable { onStatusFilterChange(opt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSel) SprixEmeraldDark else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Users List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.username }) { user ->
                UserItemCard(
                    user = user,
                    onToggleStatus = { onToggleStatus(user) },
                    onDelete = { onDeleteUser(user) },
                    isArabic = isArabic
                )
            }
        }
    }
}

@Composable
fun UserItemCard(
    user: UserEntity,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    isArabic: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SprixCardBg)
            .border(1.dp, if (!user.isActive) Color(0xFF7F1D1D) else SprixCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = user.username, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (user.isOnline) TechSuccess else Color(0xFF64748B), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (user.role == "ADMIN") Color(0xFF261D0C) else SprixEmeraldContainer)
                            .border(0.5.dp, if (user.role == "ADMIN") Color(0xFF855D15) else SprixEmeraldBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = user.role,
                            color = if (user.role == "ADMIN") Color(0xFFFBBF24) else SprixEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${user.planName} • Sessions: ${user.currentSessions}/${user.maxSessions} • Exp: ${user.planExpiryDate}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Toggle status button
                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = "Toggle status",
                        tint = if (user.isActive) TechError else TechSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                if (user.username != "elmodmen00" && user.username != "admin") {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCodesTab(
    codes: List<ActivationCodeEntity>,
    onGenerateCodeClick: () -> Unit,
    onRevokeCode: (String) -> Unit,
    onCodeCopied: (String) -> Unit = {},
    isArabic: Boolean
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "أكواد التفعيل والاشتراكات" else "License Activation Codes",
                color = SprixEmerald,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(GoldGradientStart, GoldGradientEnd)))
                    .clickable(onClick = onGenerateCodeClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF171003), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isArabic) "توليد كود" else "Generate", color = Color(0xFF171003), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(codes, key = { it.code }) { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SprixCardBg)
                        .border(1.dp, if (code.isRevoked) Color(0xFF5A1D1D) else SprixCardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = code.code, color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${code.planName} (${code.durationDays} days) • Uses: ${code.usedCount}/${code.maxUses}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code.code))
                                    onCodeCopied(code.code)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = SprixEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            code.isRevoked -> Color(0xFF451A1A)
                                            code.usedCount >= code.maxUses -> Color(0xFF15221C)
                                            else -> SprixEmeraldContainer
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        code.isRevoked -> "Revoked"
                                        code.usedCount >= code.maxUses -> "Exhausted"
                                        else -> "Active"
                                    },
                                    color = when {
                                        code.isRevoked -> TechError
                                        code.usedCount >= code.maxUses -> TextMuted
                                        else -> SprixEmerald
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
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
fun AdminServersTab(
    serverNodes: List<com.example.data.model.ServerNode>,
    onToggleServer: (String) -> Unit,
    isArabic: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(serverNodes, key = { it.id }) { node ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SprixCardBg)
                    .border(1.dp, if (node.isOnline) SprixCardBorder else Color(0xFF591C1C), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = node.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (node.isOnline) TechSuccess else TechError, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${node.region} • IP: ${node.ip}", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ping: ${node.pingMs}ms • Load: ${node.loadPercentage}% • Conns: ${node.activeConnections}/${node.maxConnections}",
                            color = SprixEmerald,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { onToggleServer(node.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle",
                            tint = if (node.isOnline) TechSuccess else TechError
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLogsTab(
    logs: List<com.example.data.model.ActivityLogEntity>,
    isArabic: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SprixCardBg)
                    .border(1.dp, SprixCardBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = log.action, color = when(log.level) {
                            "ERROR" -> TechError
                            "WARNING" -> Color(0xFFFBBF24)
                            "SUCCESS" -> TechSuccess
                            else -> SprixEmerald
                        }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = dateStr, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Target: ${log.targetUser} • ${log.details}", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// Dialog: Add User
@Composable
fun AddUserModal(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, Int, Int) -> Unit,
    isArabic: Boolean
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var plan by remember { mutableStateOf("Pro Solver") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SprixCardBg,
        title = {
            Text(
                text = if (isArabic) "إضافة مستخدم جديد" else "Create New Account",
                color = SprixEmerald,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TechInputField(
                    value = username,
                    onValueChange = { username = it },
                    label = if (isArabic) "اسم المستخدم" else "Username",
                    placeholder = "e.g. john_doe"
                )
                TechInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = if (isArabic) "كلمة المرور" else "Password",
                    placeholder = "Secure password"
                )
                // Role Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("USER", "ADMIN").forEach { r ->
                        val isSel = role == r
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) SprixEmerald else Color(0xFF0F261D))
                                .border(1.dp, if (isSel) SprixEmerald else SprixCardBorder, RoundedCornerShape(10.dp))
                                .clickable { role = r }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = r, color = if (isSel) SprixEmeraldDark else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onSubmit(username, password, role, plan, if (role == "ADMIN") 10 else 3, if (role == "ADMIN") 8 else 4)
                    }
                }
            ) {
                Text(text = if (isArabic) "إنشاء الحساب" else "Create", color = SprixEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
            }
        }
    )
}

// Dialog: Generate Activation Code
@Composable
fun GenerateCodeModal(
    onDismiss: () -> Unit,
    onSubmitSingle: (String, Int, Int) -> Unit,
    onSubmitBulk: (Int, String, Int) -> Unit,
    isArabic: Boolean
) {
    var isBulk by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("Pro Solver") }
    var durationDays by remember { mutableIntStateOf(90) }
    var maxUses by remember { mutableIntStateOf(1) }
    var bulkCount by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SprixCardBg,
        title = {
            Text(
                text = if (isArabic) "توليد أكواد التفعيل" else "Generate Activation Codes",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mode Toggle: Single vs Bulk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isBulk) Color(0xFFD97706) else Color(0xFF0F261D))
                            .border(1.dp, if (!isBulk) Color(0xFFFBBF24) else SprixCardBorder, RoundedCornerShape(8.dp))
                            .clickable { isBulk = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isArabic) "كود فردي" else "Single Code", color = if (!isBulk) Color(0xFF171003) else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBulk) Color(0xFFD97706) else Color(0xFF0F261D))
                            .border(1.dp, if (isBulk) Color(0xFFFBBF24) else SprixCardBorder, RoundedCornerShape(8.dp))
                            .clickable { isBulk = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isArabic) "مجموعة (Bulk)" else "Bulk Batch", color = if (isBulk) Color(0xFF171003) else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Plan selection
                Text(text = if (isArabic) "اختر الخطة:" else "Select Plan:", color = TextSecondary, fontSize = 12.sp)
                listOf("Basic Starter", "Pro Solver", "Elite VIP", "Lifetime VIP").forEach { p ->
                    val isSel = selectedPlan == p
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFF261D0C) else Color(0xFF071E17))
                            .border(1.dp, if (isSel) Color(0xFFFBBF24) else SprixCardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedPlan = p
                                durationDays = when (p) {
                                    "Basic Starter" -> 30
                                    "Pro Solver" -> 90
                                    "Elite VIP" -> 365
                                    else -> 3650
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(text = p, color = if (isSel) Color(0xFFFDE68A) else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isBulk) {
                        onSubmitBulk(bulkCount, selectedPlan, durationDays)
                    } else {
                        onSubmitSingle(selectedPlan, durationDays, maxUses)
                    }
                }
            ) {
                Text(text = if (isArabic) "توليد الآن" else "Generate Now", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextSecondary)
            }
        }
    )
}
