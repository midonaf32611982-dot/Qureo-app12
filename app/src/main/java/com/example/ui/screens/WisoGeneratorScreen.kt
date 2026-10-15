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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TechError
import com.example.ui.theme.TechSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WisoGeneratorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val currentQIndex by viewModel.wisoCurrentQuestionIndex.collectAsState()
    val selectedAnswerIndex by viewModel.wisoSelectedAnswerIndex.collectAsState()
    val showRationale by viewModel.wisoShowRationale.collectAsState()
    val score by viewModel.wisoScore.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    val questions = viewModel.getWisoQuestions()
    val currentQ = questions.getOrNull(currentQIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(bottom = 24.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    modifier = Modifier.testTag("wiso_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "محاكي ومولد اختبارات WISO" else "WISO Exam & Quiz Generator",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "التدريب الاقتصادي والاجتماعي" else "Wirtschafts- und Sozialkunde Practice",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Reset Button
                IconButton(onClick = { viewModel.resetWiso() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = NeonCyan
                    )
                }
            }
        }

        // Score & Progress Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131D2D))
                    .border(1.dp, Color(0xFF23354E), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "السؤال ${currentQIndex + 1} من ${questions.size}" else "Question ${currentQIndex + 1} of ${questions.size}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentQ?.topic ?: "",
                            color = NeonCyan,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F2B20))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الدرجة: $score" else "Score: $score",
                            color = TechSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Question Details Card
        if (currentQ != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = currentQ.question,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Options
                        currentQ.options.forEachIndexed { idx, opt ->
                            val isChosen = selectedAnswerIndex == idx
                            val isCorrect = idx == currentQ.answerIndex

                            val optBg = when {
                                showRationale && isCorrect -> Color(0xFF0F3826)
                                showRationale && isChosen && !isCorrect -> Color(0xFF381414)
                                isChosen -> Color(0xFF192A40)
                                else -> Color(0xFF0F1522)
                            }

                            val optBorder = when {
                                showRationale && isCorrect -> TechSuccess
                                showRationale && isChosen && !isCorrect -> TechError
                                isChosen -> NeonCyan
                                else -> Color(0xFF1F2A3D)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(optBg)
                                    .border(1.dp, optBorder, RoundedCornerShape(12.dp))
                                    .clickable(enabled = selectedAnswerIndex == null) {
                                        viewModel.selectWisoAnswer(idx)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (showRationale && isCorrect) TechSuccess else Color(0xFF1B2538),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A' + idx).toString(),
                                            color = if (showRationale && isCorrect) Color(0xFF062316) else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = opt,
                                        color = if (showRationale && isCorrect) Color(0xFFA7F3D0) else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (showRationale && isCorrect) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Explanation
                        AnimatedVisibility(visible = showRationale) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .background(Color(0xFF081C15), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "الشرح القانوني والتبرير:" else "Rationale & Legal Reference:",
                                    color = TechSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentQ.rationale,
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Next Question Button
                        if (selectedAnswerIndex != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(GoldGradientStart, GoldGradientEnd))
                                    )
                                    .clickable { viewModel.nextWisoQuestion() }
                                    .testTag("wiso_next_question_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (currentQIndex < questions.size - 1) {
                                            if (isArabic) "السؤال التالي" else "Next Question"
                                        } else {
                                            if (isArabic) "إنهاء الاختبار" else "Finish Practice"
                                        },
                                        color = Color(0xFF171003),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF171003),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
