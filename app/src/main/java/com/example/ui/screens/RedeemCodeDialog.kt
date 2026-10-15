package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.TechInputField
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RedeemCodeDialog(
    viewModel: MainViewModel
) {
    val showDialog by viewModel.showRedeemCodeDialog.collectAsState()
    val codeInput by viewModel.redeemCodeInput.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showRedeemCodeDialog.value = false },
            containerColor = Color(0xFF131826),
            title = {
                Text(
                    text = if (isArabic) "تفعيل كود الاشتراك" else "Redeem License Key",
                    color = Color(0xFFFBBF24),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) {
                            "أدخل كود التفعيل الممنوح لك (مثال: SPX-PRO-3M-X9Y8 أو SPX-VIP-2026-A1B2) لترقية حسابك فورًا."
                        } else {
                            "Enter your license code (e.g. SPX-PRO-3M-X9Y8 or SPX-VIP-2026-A1B2) to upgrade your account quota instantly."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TechInputField(
                        value = codeInput,
                        onValueChange = { viewModel.redeemCodeInput.value = it },
                        label = if (isArabic) "كود التفعيل" else "Activation Code",
                        placeholder = "SPX-XXXX-XXXX-XXXX"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.redeemCode() }
                ) {
                    Text(
                        text = if (isArabic) "تفعيل الآن" else "Redeem Now",
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showRedeemCodeDialog.value = false }
                ) {
                    Text(
                        text = if (isArabic) "إلغاء" else "Cancel",
                        color = TextMuted
                    )
                }
            }
        )
    }
}
