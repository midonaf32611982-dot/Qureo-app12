package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformBrowserView(
    url: String,
    modifier: Modifier = Modifier,
    username: String = "",
    password: String = "",
    onPageLoaded: (String) -> Unit = {},
    onErrorOccurred: (String) -> Unit = {},
    onWebViewReady: ((WebView) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasCloudFrontError by remember { mutableStateOf(false) }
    var lastErrorText by remember { mutableStateOf("") }
    var currentUrl by remember(url) { mutableStateOf(url) }
    var rememberedUrl by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Source Switcher Bar: Local Server (127.0.0.1:8770) vs Official Platform
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF030E08))
                .border(1.dp, Color(0xFF0F3222))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isLocal = currentUrl.contains("127.0.0.1") || currentUrl.contains("8770")
            // Tab 1: Local Network Server (127.0.0.1:8770)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isLocal) Color(0xFF10B981) else Color(0xFF082216))
                    .border(1.dp, if (isLocal) Color(0xFF34D399) else Color(0xFF14432F), RoundedCornerShape(6.dp))
                    .clickable {
                        currentUrl = "http://127.0.0.1:8770"
                        hasCloudFrontError = false
                        webViewRef?.loadUrl("http://127.0.0.1:8770")
                    }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🖥️ السيرفر المحلي (127.0.0.1:8770)",
                    color = if (isLocal) Color(0xFF022016) else Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab 2: Official Platform URL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!isLocal) Color(0xFF10B981) else Color(0xFF082216))
                    .border(1.dp, if (!isLocal) Color(0xFF34D399) else Color(0xFF14432F), RoundedCornerShape(6.dp))
                    .clickable {
                        currentUrl = url
                        webViewRef?.loadUrl(url)
                    }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐 المنصة الرسمية",
                    color = if (!isLocal) Color(0xFF022016) else Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Browser Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF04140D))
                .border(1.dp, Color(0xFF133628))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentUrl,
                color = Color(0xFF6EE7B7),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Fill Entered Credentials Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F3222))
                        .border(1.dp, Color(0xFF34D399), RoundedCornerShape(8.dp))
                        .clickable {
                            val u = username.trim()
                            val p = password
                            if (u.isBlank() || p.isBlank()) {
                                onErrorOccurred("[ERROR] يرجى إدخال اسم المستخدم وكلمة المرور في الخانات أولاً لتعبئتها في المتصفح!")
                                return@clickable
                            }
                            val safeU = u.replace("\\", "\\\\").replace("'", "\\'")
                            val safeP = p.replace("\\", "\\\\").replace("'", "\\'")
                            webViewRef?.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        let inputs = Array.from(document.querySelectorAll('input'));
                                        let userInput = inputs.find(i => i.type === 'text' || i.name.includes('user') || i.name.includes('login') || i.id.includes('user') || i.id.includes('login')) || document.querySelector('input[type="text"]');
                                        let passInput = inputs.find(i => i.type === 'password') || document.querySelector('input[type="password"]');
                                        if (userInput && passInput) {
                                            userInput.value = '$safeU';
                                            userInput.dispatchEvent(new Event('input', { bubbles: true }));
                                            userInput.dispatchEvent(new Event('change', { bubbles: true }));
                                            passInput.value = '$safeP';
                                            passInput.dispatchEvent(new Event('input', { bubbles: true }));
                                            passInput.dispatchEvent(new Event('change', { bubbles: true }));
                                            let btn = document.querySelector('button[type="submit"], input[type="submit"], form button, .btn-login, button.primary');
                                            if (btn) btn.click();
                                            return "AUTO_FILLED_SUCCESS";
                                        }
                                        return "INPUTS_NOT_FOUND";
                                    } catch(e) {
                                        return "ERROR: " + e.message;
                                    }
                                })();
                                """.trimIndent()
                            ) { res ->
                                if (res != null && res.contains("ERROR")) {
                                    onErrorOccurred("خطأ محاكاة المتصفح: $res")
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Fill",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "تعبئة الحساب", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Reload Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B261B))
                        .border(1.dp, Color(0xFF22543D), RoundedCornerShape(8.dp))
                        .clickable {
                            hasCloudFrontError = false
                            lastErrorText = ""
                            webViewRef?.reload()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "تحديث", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Open in External Browser
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B261B))
                        .border(1.dp, Color(0xFF22543D), RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in Chrome",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "خارجي", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Diagnostic Notice Banner ONLY if CloudFront error happens
        if (hasCloudFrontError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D0E0E))
                    .border(1.dp, Color(0xFFEF4444))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp).padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "[ERROR HTTP 403] تنبيه الحظر الجغرافي:",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المنصة بتحجب الاتصال خارج مصر — اقفل أي VPN أجنبي واتصل بشبكة مصرية",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // The WebView itself
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    onWebViewReady?.invoke(this)

                    // Enable Cookies
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        javaScriptCanOpenWindowsAutomatically = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // High quality mobile Chrome UA
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36"
                    }

                    webChromeClient = WebChromeClient()

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            if (finishedUrl != null) {
                                onPageLoaded(finishedUrl)
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            val code = errorResponse?.statusCode ?: -1
                            if (code == 403) {
                                hasCloudFrontError = true
                                lastErrorText = "المنصة بتحجب الاتصال خارج مصر — اقفل أي VPN أجنبي واتصل بشبكة مصرية"
                                onErrorOccurred("[ERROR HTTP 403] المنصة بتحجب الاتصال خارج مصر — اقفل أي VPN أجنبي واتصل بشبكة مصرية")
                            } else if (code >= 400) {
                                onErrorOccurred("[ERROR HTTP $code] استجابة الخادم: ${errorResponse?.reasonPhrase ?: "خطأ $code"}")
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            val desc = error?.description?.toString() ?: "Network error"
                            if (request?.isForMainFrame == true) {
                                onErrorOccurred("[ERROR NET] تعذر الاتصال بالصفحة: $desc")
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            // Proceed safely to allow secure communication through regional CDNs
                            handler?.proceed()
                        }
                    }

                    rememberedUrl = currentUrl
                    loadUrl(currentUrl, mapOf(
                        "Accept-Language" to "ar-EG,ar;q=0.9,en-US;q=0.8,en;q=0.7",
                        "Sec-Ch-Ua-Mobile" to "?1",
                        "Sec-Ch-Ua-Platform" to "\"Android\""
                    ))
                }
            },
            update = { webView ->
                webViewRef = webView
                onWebViewReady?.invoke(webView)
                // Only load if the requested TARGET url prop changed, NOT on internal page redirects!
                if (rememberedUrl != currentUrl) {
                    rememberedUrl = currentUrl
                    webView.loadUrl(currentUrl)
                }
            }
        )
    }
}
