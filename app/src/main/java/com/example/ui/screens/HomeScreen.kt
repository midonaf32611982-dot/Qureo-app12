package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.BothLogoMark
import com.example.ui.components.FloatingTerminalButton
import com.example.ui.components.QureoLogoMark
import com.example.ui.components.SprixLogoMark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == "ADMIN"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041710),
                        Color(0xFF072118),
                        Color(0xFF04140F)
                    )
                )
            )
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Action Bar with Terminal & Admin Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logout Icon
                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF261D0C))
                                .border(1.dp, Color(0xFF855D15), RoundedCornerShape(12.dp))
                                .clickable { viewModel.navigateTo(AppScreen.ADMIN_PANEL) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("admin_panel_top_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "لوحة الإدارة",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    FloatingTerminalButton(
                        onClick = {
                            if (isAdmin) {
                                viewModel.navigateTo(AppScreen.ADMIN_PANEL)
                            } else {
                                viewModel.navigateTo(AppScreen.SPRIX_SOLVER)
                            }
                        }
                    )
                }
            }
        }

        // Header Section (Exact match to Screenshot 2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SprixLogoMark(size = 72)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Sprix",
                    color = Color(0xFF34D399),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أدوات تعليمية ذكية — اختر الأداة اللي عايز تشغّلها.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Two Tool Cards Grid (Qureo & الثقافة المالية)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Qureo
                ToolBoxCard(
                    title = "Qureo",
                    description = "حل المسارات تلقائيًا من المحاولة الأولى.",
                    logo = { QureoLogoMark(size = 38) },
                    buttonText = "ابدأ ←",
                    onClick = { viewModel.navigateTo(AppScreen.QUREO_SOLVER) },
                    modifier = Modifier.weight(1f),
                    testTag = "home_qureo_card"
                )

                // Card 2: الثقافة المالية
                ToolBoxCard(
                    title = "الثقافة المالية",
                    description = "حل الكورس تلقائيًا من المحاولة الأولى.",
                    logo = { SprixLogoMark(size = 38) },
                    buttonText = "ابدأ ←",
                    onClick = { viewModel.navigateTo(AppScreen.SPRIX_SOLVER) },
                    modifier = Modifier.weight(1f),
                    testTag = "home_sprix_card"
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Card 3: حل الاثنين معًا (Solve Both)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A1C16))
                    .border(1.dp, Color(0xFF163C2E), RoundedCornerShape(20.dp))
                    .clickable { viewModel.navigateTo(AppScreen.SOLVE_BOTH) }
                    .padding(18.dp)
                    .testTag("solve_both_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حل الاثنين معًا",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ادخل البيانات مرة واحدة، وشغّل Qureo + الثقافة المالية بسجلين منفصلين.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillTag(text = "Qureo")
                            PillTag(text = "الثقافة المالية")
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF34D399))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ابدأ الآن ←",
                                color = Color(0xFF042017),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Merged Icon illustration (The user's official combined logo)
                    Box(
                        modifier = Modifier.padding(start = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BothLogoMark(size = 58)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Section: المميزات (Features) - matching Screenshot 2 & 3
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "المميزات",
                    color = Color(0xFF34D399),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "كل اللي محتاجه علشان توفّر وقتك",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Feature Cards Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureItemCard(
                    title = "حسابات متعددة في نفس الوقت",
                    description = "شغّل لحد 8 حسابات مع بعض، وكل حساب بيتنفيذ بشكل منفصل.",
                    icon = Icons.Default.People
                )
                FeatureItemCard(
                    title = "متابعة مباشرة للتقدّم",
                    description = "شوف سجل التنفيذ ونسبة التقدم لحظة بلحظة من نفس الصفحة.",
                    icon = Icons.Default.ShowChart
                )
                FeatureItemCard(
                    title = "إيقاف في أي وقت",
                    description = "زر إيقاف يوقف المهمة فورًا لو حبيت تغيّر أي حاجة.",
                    icon = Icons.Default.Stop
                )
                FeatureItemCard(
                    title = "خصوصية بياناتك",
                    description = "بيانات الدخول بتستخدم وقت التنفيذ بس، ومش بتتخزّن في السيرفر.",
                    icon = Icons.Default.Lock
                )
                FeatureItemCard(
                    title = "بيشتغل من الموبايل",
                    description = "نسخة أندرويد جاهزة تشتغل على Termux بدون كمبيوتر.",
                    icon = Icons.Default.PhoneAndroid
                )
                FeatureItemCard(
                    title = "أدوات متكاملة",
                    description = "Qureo والثقافة المالية في مكان واحد، وكمان حل الاثنين معًا بخطوة واحدة.",
                    icon = Icons.Default.Layers
                )
            }
            Spacer(modifier = Modifier.height(34.dp))
        }

        // Section: إزاي تستخدمها؟ (How to use) - matching Screenshot 3
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إزاي تستخدمها؟",
                    color = Color(0xFF34D399),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ثلاث خطوات بس",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Steps Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepItemCard(
                    number = "1",
                    title = "اختار الأداة",
                    description = "Qureo أو الثقافة المالية أو الاثنين معًا."
                )
                StepItemCard(
                    number = "2",
                    title = "ادخل بياناتك",
                    description = "كود الطالب وكلمة المرور، وحدد المسار أو المادة."
                )
                StepItemCard(
                    number = "3",
                    title = "ابدأ وتابع",
                    description = "اضغط ابدأ وراقب التقدم، وتقدر توقف في أي لحظة."
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Section: للتواصل والاستفسارات
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "للتواصل والاستفسارات",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0C241B))
                        .border(1.dp, Color(0xFF1E4D3B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الدعم الفني متاح 24/7 عبر تيليجرام وديسكورد",
                            color = Color(0xFFA7F3D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolBoxCard(
    title: String,
    description: String,
    logo: @Composable () -> Unit,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "tool_box_card"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF091A14))
            .border(1.dp, Color(0xFF143327), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Column {
            logo()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = buttonText,
                color = Color(0xFF34D399),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PillTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF082B1F))
            .border(1.dp, Color(0xFF14533D), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF6EE7B7),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FeatureItemCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF081813))
            .border(1.dp, Color(0xFF133629), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF0F2E23), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun StepItemCard(
    number: String,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF081813))
            .border(1.dp, Color(0xFF133629), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF34D399), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color(0xFF032219),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
