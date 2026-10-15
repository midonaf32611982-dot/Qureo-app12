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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.BothLogoMark
import com.example.ui.components.TechInputField
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TechSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SolveBothScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val username by viewModel.solveBothUsername.collectAsState()
    val password by viewModel.solveBothPassword.collectAsState()
    val isRunning by viewModel.isSolveBothRunning.collectAsState()
    val qureoJsProgress by viewModel.qureoJsProgress.collectAsState()
    val qureoPyProgress by viewModel.qureoPyProgress.collectAsState()
    val sprixProgress by viewModel.sprixSimProgress.collectAsState()
    val qureoLogs by viewModel.solveBothQureoLogs.collectAsState()
    val sprixLogs by viewModel.solveBothSprixLogs.collectAsState()
    var useLiveWebView by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041710),
                        Color(0xFF072118),
                        Color(0xFF03120E)
                    )
                )
            )
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Breadcrumb Top Link: ← الرئيسية & Browser Simulation Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← الرئيسية",
                    color = Color(0xFF34D399),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(AppScreen.HOME) }
                        .testTag("solve_both_back_breadcrumb")
                )

                // Toggle for Live Local Browser
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (useLiveWebView) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF0F231B))
                        .border(
                            1.dp,
                            if (useLiveWebView) Color(0xFF10B981) else Color(0xFF1B4332),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { useLiveWebView = !useLiveWebView }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (useLiveWebView) "🌐 إخفاء المتصفح الحي" else "🌐 محاكاة المتصفح الحي",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Browser View if enabled
        if (useLiveWebView) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                ) {
                    com.example.ui.components.PlatformBrowserView(
                        url = "https://me-tp.qureo.education/",
                        modifier = Modifier.fillMaxSize(),
                        username = username,
                        password = password,
                        onWebViewReady = { /* Ready for native local network bridge */ },
                        onErrorOccurred = { err ->
                            viewModel.solveBothQureoLogs.value = viewModel.solveBothQureoLogs.value + err
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Header Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BothLogoMark(size = 64)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "حل الاثنين معًا",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تشغيل Qureo (JS 54 + Python 25) والثقافة المالية (20 وحدة) بسجلين منفصلين",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Input Credentials Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF081813))
                    .border(1.dp, Color(0xFF133628), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TechInputField(
                        value = username,
                        onValueChange = { viewModel.solveBothUsername.value = it },
                        label = "اسم المستخدم",
                        placeholder = "أدخل كود الطالب (Login ID)",
                        testTag = "solve_both_username_input"
                    )

                    TechInputField(
                        value = password,
                        onValueChange = { viewModel.solveBothPassword.value = it },
                        label = "كلمة المرور",
                        placeholder = "أدخل كلمة المرور",
                        isPassword = true,
                        testTag = "solve_both_password_input"
                    )

                    // Action Buttons: Start (Green) & Stop (Red outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Button
                        Box(
                            modifier = Modifier
                                .weight(2.5f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isRunning) Color(0xFF059669) else Color(0xFF34D399)
                                )
                                .border(
                                    1.dp,
                                    if (isRunning) Color(0xFF22543D) else Color(0xFF6EE7B7),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !isRunning) {
                                    viewModel.runSolveBoth()
                                }
                                .testTag("start_solve_both_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF042017),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جاري الحل المزدوج...",
                                        color = Color(0xFF042017),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "🚀 ابدأ الحل الشامل للكل",
                                        color = Color(0xFF042017),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Stop Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1F1212))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
                                .clickable(enabled = isRunning) {
                                    viewModel.stopSolveBoth()
                                }
                                .testTag("stop_solve_both_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "إيقاف",
                                color = if (isRunning) Color(0xFFEF4444) else Color(0xFF6B7280),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Dual Progress Tracking Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF081813))
                    .border(1.dp, Color(0xFF133628), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "متابعة تقدم الحل الشامل للمسارات",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Qureo JavaScript Progress (54 chapters)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "1. Qureo JavaScript (54 شابتر)", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${(qureoJsProgress * 100).toInt()}%", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { qureoJsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF0F172A)
                        )
                    }

                    // 2. Qureo Python Progress (25 chapters)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "2. Qureo Python (25 شابتر)", color = Color(0xFFFACC15), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${(qureoPyProgress * 100).toInt()}%", color = Color(0xFFFACC15), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { qureoPyProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFACC15),
                            trackColor = Color(0xFF1E1E0F)
                        )
                    }

                    // 3. Sprix Financial Culture Progress
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "3. الثقافة المالية (20 وحدة)", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${(sprixProgress * 100).toInt()}%", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { sprixProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color(0xFF071F17)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Terminal Log 1: سجل كيريو (Qureo Log)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF030D12))
                    .border(1.dp, Color(0xFF0E3347), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💻 سجل كيريو (Qureo Log)",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "JS 54 + Py 25",
                                color = Color(0xFF0284C7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isRunning && (qureoJsProgress < 1f || qureoPyProgress < 1f)) Color(0xFF38BDF8) else Color(0xFF6B7280), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    com.example.ui.components.TechTerminalLogBox(
                        logs = qureoLogs,
                        height = 190.dp,
                        backgroundColor = Color(0xFF02090D),
                        borderColor = Color(0xFF0E3347)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Terminal Log 2: سجل الثقافة المالية (Financial Culture Log)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF04120C))
                    .border(1.dp, Color(0xFF133628), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📈 سجل الثقافة المالية (Sprix Log)",
                                color = Color(0xFF34D399),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "20 وحدة معتمدة",
                                color = Color(0xFF059669),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isRunning && sprixProgress < 1f) Color(0xFF34D399) else Color(0xFF6B7280), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    com.example.ui.components.TechTerminalLogBox(
                        logs = sprixLogs,
                        height = 190.dp,
                        backgroundColor = Color(0xFF020A06),
                        borderColor = Color(0xFF133628)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
