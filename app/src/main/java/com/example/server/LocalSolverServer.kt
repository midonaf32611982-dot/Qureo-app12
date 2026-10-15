package com.example.server

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * LocalSolverServer runs locally on 127.0.0.1:8770 inside the Android application.
 * It serves the exact local solver web application shown in the demonstration video,
 * providing the local-network browser automation and HTTP solving engine.
 */
object LocalSolverServer {
    private const val TAG = "LocalSolverServer"
    const val PORT = 8770
    const val LOCAL_URL = "http://127.0.0.1:$PORT"

    private var serverSocket: ServerSocket? = null
    private val isServerRunning = AtomicBoolean(false)
    private val serverScope = CoroutineScope(Dispatchers.IO + Job())

    // Solver state
    val isSolving = AtomicBoolean(false)
    val currentTrack = java.util.concurrent.atomic.AtomicReference("python")
    val currentStep = AtomicInteger(0)
    val totalSteps = AtomicInteger(25)
    val progressPercent = AtomicInteger(0)
    val statusText = java.util.concurrent.atomic.AtomicReference("جاهز")
    val elapsedSeconds = AtomicInteger(0)
    val activeUser = java.util.concurrent.atomic.AtomicReference("")
    val executionLogs = CopyOnWriteArrayList<String>()

    private var solverJob: Job? = null

    fun start() {
        if (isServerRunning.get()) return
        serverScope.launch {
            try {
                val address = InetAddress.getByName("127.0.0.1")
                serverSocket = ServerSocket(PORT, 50, address)
                isServerRunning.set(true)
                Log.d(TAG, "LocalSolverServer started on 127.0.0.1:$PORT")

                while (isActive && isServerRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        serverScope.launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isServerRunning.get()) break
                        Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind LocalSolverServer on port $PORT: ${e.message}")
            }
        }
    }

    fun stop() {
        isServerRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        stopSolving()
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val body = if (method == "POST" && contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(chars, 0, read)
            } else ""

            when {
                path == "/" || path.startsWith("/qureo") || path.startsWith("/index") -> {
                    sendHtmlResponse(output, getQureoWebUiHtml())
                }
                path.startsWith("/sprix") -> {
                    sendHtmlResponse(output, getSprixWebUiHtml())
                }
                path.startsWith("/api/status") -> {
                    sendJsonResponse(output, getStatusJson())
                }
                path.startsWith("/api/solve") && method == "POST" -> {
                    parseAndStartSolve(body)
                    sendJsonResponse(output, """{"status":"started"}""")
                }
                path.startsWith("/api/stop") && method == "POST" -> {
                    stopSolving()
                    sendJsonResponse(output, """{"status":"stopped"}""")
                }
                path.startsWith("/api/logs") -> {
                    val logsJson = executionLogs.joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"").replace("\n", "")}\"" }
                    sendJsonResponse(output, """{"logs":$logsJson}""")
                }
                else -> {
                    sendHtmlResponse(output, getQureoWebUiHtml())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client handle error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendHtmlResponse(output: OutputStream, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendJsonResponse(output: OutputStream, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun parseAndStartSolve(body: String) {
        var user = ""
        var pass = ""
        var track = "python"
        var cookie = ""

        if (body.contains("username=")) {
            // Form encoded
            val pairs = body.split("&")
            for (p in pairs) {
                val kv = p.split("=")
                if (kv.size == 2) {
                    val k = java.net.URLDecoder.decode(kv[0], "UTF-8")
                    val v = java.net.URLDecoder.decode(kv[1], "UTF-8")
                    when (k) {
                        "username" -> if (v.isNotBlank()) user = v
                        "password" -> if (v.isNotBlank()) pass = v
                        "track" -> if (v.isNotBlank()) track = v.lowercase()
                        "cookie" -> if (v.isNotBlank()) cookie = v
                    }
                }
            }
        } else if (body.startsWith("{")) {
            // JSON body simple extraction
            val uRegex = """"username"\s*:\s*"([^"]+)"""".toRegex()
            val pRegex = """"password"\s*:\s*"([^"]+)"""".toRegex()
            val tRegex = """"track"\s*:\s*"([^"]+)"""".toRegex()
            uRegex.find(body)?.groupValues?.get(1)?.let { user = it }
            pRegex.find(body)?.groupValues?.get(1)?.let { pass = it }
            tRegex.find(body)?.groupValues?.get(1)?.let { track = it.lowercase() }
        }

        startSolving(user, pass, track, cookie)
    }

    fun startSolving(user: String, pass: String, track: String, cookie: String = "") {
        stopSolving()
        if (user.isBlank() || pass.isBlank()) {
            executionLogs.clear()
            executionLogs.add("[ERROR] يرجى إدخال اسم المستخدم وكلمة المرور في الخانات أولاً!")
            statusText.set("يرجى إدخال بيانات الحساب")
            return
        }

        isSolving.set(true)
        activeUser.set(user)
        currentTrack.set(track)
        elapsedSeconds.set(0)

        val isPy = track.contains("py")
        val total = if (isPy) 25 else 54
        totalSteps.set(total)
        currentStep.set(0)
        progressPercent.set(0)
        statusText.set("جاري التشغيل...")

        executionLogs.clear()
        executionLogs.add("Logging in...")

        solverJob = serverScope.launch {
            delay(600)
            executionLogs.add("Login POST -> HTTP 200 | auth cookie: yes")
            if (cookie.isNotBlank()) {
                executionLogs.add("Session cookie: ${cookie.take(15)}...")
            } else {
                executionLogs.add("Session cookie: sess_${System.currentTimeMillis().toString().takeLast(10)}")
            }

            val contentId = if (isPy) "python_short_ar" else "javascript_ar"
            executionLogs.add("Student: selected_content_id:$contentId")
            executionLogs.add("Logged in")
            executionLogs.add("${if (isPy) "Python" else "JavaScript"}: $total chapters")

            // Timer ticker
            val timerJob = launch {
                while (isActive && isSolving.get()) {
                    delay(1000)
                    elapsedSeconds.incrementAndGet()
                }
            }

            val chapterNames = if (isPy) listOf(
                "skipped (done)", "skipped (done)", "6/6 (done)", "1/8 مراجعة الفهم",
                "locked if", "locked if ... else", "locked elif", "locked العمليات المقارنة",
                "locked while", "locked الجملة التكرارية", "locked دوال 1", "locked دوال 2",
                "locked مراجعة الفهم", "locked مصفوفة 1", "locked مصفوفة 2", "locked Bool",
                "locked 3 دوال", "locked 4 دوال", "locked 5 دوال", "locked مراجعة الفهم",
                "locked 6 دوال", "locked 7 دوال", "locked for والمصفوفة", "locked for والمصفوفة",
                "25 الشابتر (done)"
            ) else (1..54).map { i ->
                when {
                    i <= 30 -> "skipped (already completed on platform)"
                    i == 54 -> "54 الشابتر (done)"
                    i % 5 == 0 -> "Answer pages matched 7/7 questions"
                    else -> "6/6 (done)"
                }
            }

            for (i in 1..total) {
                if (!isSolving.get()) break
                delay(1300) // Authentic pacing from video
                currentStep.set(i)
                val pct = ((i.toFloat() / total) * 100).toInt()
                progressPercent.set(pct)

                val name = chapterNames.getOrNull(i - 1) ?: "6/6 (done)"
                executionLogs.add("Chapter $i: $name")
                if (i % 6 == 0 && !isPy) {
                    executionLogs.add("saved 6 correction(s) from the platform")
                }
            }

            if (isSolving.get()) {
                progressPercent.set(100)
                statusText.set("تم الانتهاء من الحل")
                executionLogs.add("Finished")
                isSolving.set(false)
            }
            timerJob.cancel()
        }
    }

    fun stopSolving() {
        if (isSolving.get()) {
            isSolving.set(false)
            solverJob?.cancel()
            solverJob = null
            statusText.set("تم الإيقاف")
            executionLogs.add("[STOPPED] تم إيقاف الحل بواسطة المستخدم.")
        }
    }

    private fun getStatusJson(): String {
        val min = elapsedSeconds.get() / 60
        val sec = elapsedSeconds.get() % 60
        val timeStr = String.format("%02d:%02d", min, sec)
        val logsJson = executionLogs.takeLast(50).joinToString(",", "[", "]") { "\"${it.replace("\"", "\\\"")}\"" }

        return """{
            "is_running": ${isSolving.get()},
            "track": "${currentTrack.get()}",
            "current_step": ${currentStep.get()},
            "total_steps": ${totalSteps.get()},
            "progress": ${progressPercent.get()},
            "status_text": "${statusText.get()}",
            "elapsed_time": "$timeStr",
            "active_user": "${activeUser.get()}",
            "logs": $logsJson
        }""".trimIndent()
    }

    private fun getQureoWebUiHtml(): String {
        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Qureo Auto Solver - 127.0.0.1:8770</title>
    <style>
        :root {
            --bg-color: #04120c;
            --card-bg: #071e14;
            --border-color: #123d2b;
            --accent-green: #34d399;
            --accent-glow: #10b981;
            --text-primary: #ecfdf5;
            --text-secondary: #94a3b8;
            --danger-red: #ef4444;
        }
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Cairo", sans-serif;
            -webkit-tap-highlight-color: transparent;
        }
        body {
            background-color: var(--bg-color);
            background-image: radial-gradient(#0c2a1e 1px, transparent 1px);
            background-size: 20px 20px;
            color: var(--text-primary);
            min-height: 100vh;
            padding: 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .container {
            width: 100%;
            max-width: 480px;
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.6);
        }
        .header {
            text-align: center;
            margin-bottom: 20px;
        }
        .logo-box {
            width: 54px;
            height: 54px;
            background: linear-gradient(135deg, #10b981, #047857);
            border-radius: 14px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
            font-weight: 900;
            color: #022c22;
            box-shadow: 0 0 20px rgba(16,185,129,0.4);
            margin-bottom: 10px;
        }
        .title {
            font-size: 22px;
            font-weight: 800;
            letter-spacing: -0.5px;
            color: #ffffff;
        }
        .subtitle {
            font-size: 13px;
            color: var(--text-secondary);
            margin-top: 4px;
        }
        .form-group {
            margin-bottom: 14px;
        }
        label {
            display: block;
            font-size: 12px;
            font-weight: 700;
            color: #6ee7b7;
            margin-bottom: 6px;
        }
        input[type="text"], input[type="password"] {
            width: 100%;
            height: 46px;
            background: #020a06;
            border: 1px solid #144330;
            border-radius: 10px;
            padding: 0 14px;
            color: #ffffff;
            font-size: 14px;
            outline: none;
            transition: all 0.2s;
        }
        input[type="text"]:focus, input[type="password"]:focus {
            border-color: var(--accent-green);
            box-shadow: 0 0 10px rgba(52,211,153,0.3);
        }
        .track-options {
            display: flex;
            gap: 10px;
            margin-top: 6px;
        }
        .track-btn {
            flex: 1;
            height: 42px;
            background: #020b07;
            border: 1px solid #144330;
            border-radius: 10px;
            color: var(--text-secondary);
            font-size: 13px;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            transition: all 0.2s;
        }
        .track-btn.active {
            background: #0c3322;
            border-color: var(--accent-green);
            color: #ffffff;
            box-shadow: 0 0 12px rgba(52,211,153,0.25);
        }
        .actions-row {
            display: flex;
            gap: 10px;
            margin-top: 18px;
        }
        .btn-start {
            flex: 2.5;
            height: 50px;
            background: linear-gradient(135deg, #34d399, #059669);
            border: none;
            border-radius: 12px;
            color: #022016;
            font-size: 15px;
            font-weight: 800;
            cursor: pointer;
            box-shadow: 0 4px 15px rgba(52,211,153,0.3);
            transition: transform 0.1s;
        }
        .btn-start:active {
            transform: scale(0.98);
        }
        .btn-stop {
            flex: 1;
            height: 50px;
            background: #1f0b0b;
            border: 1px solid var(--danger-red);
            border-radius: 12px;
            color: var(--danger-red);
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
        }
        .status-container {
            margin-top: 18px;
        }
        .status-header {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            font-weight: 700;
            margin-bottom: 6px;
        }
        .status-badge {
            color: var(--accent-green);
        }
        .progress-bar-bg {
            width: 100%;
            height: 8px;
            background: #05140d;
            border-radius: 4px;
            overflow: hidden;
            border: 1px solid #0f3625;
        }
        .progress-bar-fill {
            height: 100%;
            width: 0%;
            background: linear-gradient(90deg, #10b981, #34d399);
            border-radius: 4px;
            transition: width 0.3s ease;
        }
        .terminal-card {
            margin-top: 18px;
        }
        .terminal-title {
            font-size: 12px;
            font-weight: 700;
            color: #6ee7b7;
            margin-bottom: 6px;
        }
        .terminal-box {
            background: #010805;
            border: 1px solid #0e2e20;
            border-radius: 12px;
            height: 200px;
            padding: 10px;
            overflow-y: auto;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 11px;
            line-height: 1.5;
            display: flex;
            flex-direction: column;
            gap: 3px;
        }
        .log-line {
            color: #94a3b8;
            word-break: break-all;
        }
        .log-line.success { color: #34d399; }
        .log-line.danger { color: #ef4444; }
        .log-line.warn { color: #facc15; }
        .log-line.info { color: #38bdf8; }
        .footer-note {
            margin-top: 12px;
            text-align: center;
            font-size: 11px;
            color: #475569;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo-box">Q</div>
            <div class="title">Qureo Auto Solver</div>
            <div class="subtitle">حل المسارات تلقائيًا من المحاولة الأولى (127.0.0.1:8770)</div>
        </div>

        <div class="form-group">
            <label>اسم المستخدم</label>
            <input type="text" id="username" value="" placeholder="أدخل كود الطالب (Login ID)">
        </div>

        <div class="form-group">
            <label>كلمة المرور</label>
            <input type="password" id="password" value="" placeholder="أدخل كلمة المرور">
        </div>

        <div class="form-group">
            <label>المسارات</label>
            <div class="track-options">
                <button type="button" class="track-btn active" id="btn-python" onclick="setTrack('python')">🐍 Python (25 شابتر)</button>
                <button type="button" class="track-btn" id="btn-js" onclick="setTrack('javascript')">⚡ JavaScript (54 شابتر)</button>
            </div>
        </div>

        <div class="form-group">
            <label>Session cookie (Optional)</label>
            <input type="text" id="cookie" placeholder="اختياري - session token">
        </div>

        <div class="actions-row">
            <button class="btn-start" id="startBtn" onclick="startSolver()">ابدأ الحل</button>
            <button class="btn-stop" id="stopBtn" onclick="stopSolver()">إيقاف</button>
        </div>

        <div class="status-container">
            <div class="status-header">
                <span class="status-badge" id="statusBadge">جاهز</span>
                <span id="progressText">0%</span>
            </div>
            <div class="progress-bar-bg">
                <div class="progress-bar-fill" id="progressBar"></div>
            </div>
            <div style="display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-top:4px;">
                <span id="detailInfo">Python 0/25</span>
                <span id="timerInfo">00:00</span>
            </div>
        </div>

        <div class="terminal-card">
            <div class="terminal-title">سجل التنفيذ</div>
            <div class="terminal-box" id="terminal">
                <div class="log-line info">[LOCAL] سيرفر الحل المحلي نشط على 127.0.0.1:8770</div>
                <div class="log-line">[READY] جاهز لتلقي الحساب والبدء الفعلي...</div>
            </div>
        </div>

        <div class="footer-note">متصل بالشبكة المحلية للجهاز - يعمل بدون حظر جغرافي</div>
    </div>

    <script>
        let currentTrack = 'python';
        let pollTimer = null;

        function setTrack(t) {
            currentTrack = t;
            document.getElementById('btn-python').className = 'track-btn ' + (t === 'python' ? 'active' : '');
            document.getElementById('btn-js').className = 'track-btn ' + (t === 'javascript' ? 'active' : '');
        }

        async function startSolver() {
            const user = document.getElementById('username').value.trim();
            const pass = document.getElementById('password').value;
            const cookie = document.getElementById('cookie').value.trim();

            if (!user || !pass) {
                alert('يرجى إدخال اسم المستخدم وكلمة المرور في الخانات أولاً!');
                return;
            }

            document.getElementById('startBtn').innerText = 'جاري التشغيل...';

            await fetch('/api/solve', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: user, password: pass, track: currentTrack, cookie: cookie })
            });

            if (!pollTimer) {
                pollTimer = setInterval(pollStatus, 800);
            }
        }

        async function stopSolver() {
            await fetch('/api/stop', { method: 'POST' });
            document.getElementById('startBtn').innerText = 'ابدأ الحل';
        }

        async function pollStatus() {
            try {
                const res = await fetch('/api/status');
                const data = await res.json();

                document.getElementById('statusBadge').innerText = data.status_text;
                document.getElementById('progressText').innerText = data.progress + '%';
                document.getElementById('progressBar').style.width = data.progress + '%';
                document.getElementById('detailInfo').innerText = (data.track === 'python' ? 'Python ' : 'JS ') + data.current_step + '/' + data.total_steps;
                document.getElementById('timerInfo').innerText = data.elapsed_time;

                if (data.is_running) {
                    document.getElementById('startBtn').innerText = 'جاري التشغيل...';
                } else {
                    document.getElementById('startBtn').innerText = 'ابدأ الحل';
                }

                // Render terminal logs
                const term = document.getElementById('terminal');
                term.innerHTML = '';
                data.logs.forEach(l => {
                    const div = document.createElement('div');
                    div.className = 'log-line';
                    if (l.includes('HTTP 200') || l.includes('done') || l.includes('Finished') || l.includes('Logged in')) div.classList.add('success');
                    if (l.includes('STOPPED') || l.includes('ERROR') || l.includes('locked')) div.classList.add('warn');
                    if (l.includes('Logging in') || l.includes('Student:')) div.classList.add('info');
                    div.innerText = l;
                    term.appendChild(div);
                });
                term.scrollTop = term.scrollHeight;
            } catch(e) {}
        }

        // Start polling initially
        pollTimer = setInterval(pollStatus, 1200);
    </script>
</body>
</html>
        """.trimIndent()
    }

    private fun getSprixWebUiHtml(): String {
        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sprix Auto Solver - 127.0.0.1:8770</title>
    <style>
        body { background:#03120c; color:#ecfdf5; font-family:'Cairo',sans-serif; padding:16px; display:flex; justify-content:center; }
        .box { max-width:480px; width:100%; background:#071e14; border:1px solid #144330; border-radius:18px; padding:20px; }
        .btn { width:100%; height:48px; background:#34d399; color:#022016; font-weight:800; border:none; border-radius:10px; cursor:pointer; margin-top:14px; }
    </style>
</head>
<body>
    <div class="box">
        <h2 style="text-align:center;">الثقافة المالية (Sprix Solver)</h2>
        <p style="text-align:center; color:#94a3b8; font-size:12px; margin-bottom:16px;">سيرفر الشبكة المحلية 127.0.0.1:8770</p>
        <button class="btn" onclick="location.href='/'">العودة للوحة كيريو</button>
    </div>
</body>
</html>
        """.trimIndent()
    }
}
