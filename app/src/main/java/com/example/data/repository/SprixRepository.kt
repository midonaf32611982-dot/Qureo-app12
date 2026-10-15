package com.example.data.repository

import android.content.Context
import com.example.data.local.SprixDatabase
import com.example.data.model.ActivationCodeEntity
import com.example.data.model.ActivityLogEntity
import com.example.data.model.QureoStage
import com.example.data.model.ServerNode
import com.example.data.model.SolverHistoryEntity
import com.example.data.model.SprixQuestion
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserEntity
import com.example.data.model.WisoQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NetworkProbeResult(
    val statusCode: Int,
    val isSuccess: Boolean,
    val isCloudFrontGeoBlocked: Boolean,
    val serverHeader: String,
    val rawMessage: String
)

class SprixRepository(context: Context) {
    private val dao = SprixDatabase.getDatabase(context).sprixDao()

    private val sessionCookieStore = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun probePlatformConnection(urlString: String): NetworkProbeResult = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(urlString)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "ar-EG,ar;q=0.9,en-US;q=0.8,en;q=0.7")
                
                // Attach stored cookies
                val host = url.host
                val existingCookies = sessionCookieStore[host]
                if (!existingCookies.isNullOrBlank()) {
                    setRequestProperty("Cookie", existingCookies)
                }

                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            val server = conn.getHeaderField("Server") ?: conn.getHeaderField("server") ?: "Unknown"

            // Save Set-Cookie headers
            val setCookies = conn.headerFields["Set-Cookie"] ?: conn.headerFields["set-cookie"]
            if (setCookies != null) {
                val host = url.host
                val cookieStr = setCookies.joinToString("; ") { it.substringBefore(";") }
                sessionCookieStore[host] = cookieStr
                try {
                    val cm = android.webkit.CookieManager.getInstance()
                    setCookies.forEach { cm.setCookie(urlString, it) }
                } catch (_: Exception) {}
            }

            val errorText = try {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (_: Exception) { "" }
            val isGeoBlocked = code == 403 && (errorText.contains("CloudFront", ignoreCase = true) || errorText.contains("block access", ignoreCase = true) || server.contains("CloudFront", ignoreCase = true))

            NetworkProbeResult(
                statusCode = code,
                isSuccess = code in 200..299,
                isCloudFrontGeoBlocked = isGeoBlocked,
                serverHeader = server,
                rawMessage = if (isGeoBlocked) "المنصة بتحجب الاتصال خارج مصر — اقفل أي VPN أجنبي واتصل بشبكة مصرية" else conn.responseMessage ?: "Status $code"
            )
        } catch (e: Exception) {
            NetworkProbeResult(
                statusCode = -1,
                isSuccess = false,
                isCloudFrontGeoBlocked = false,
                serverHeader = "Error",
                rawMessage = e.localizedMessage ?: "Connection failed"
            )
        }
    }

    private val _serverNodes = MutableStateFlow<List<ServerNode>>(emptyList())
    val serverNodes = _serverNodes.asStateFlow()

    init {
        initDefaultServers()
    }

    private fun initDefaultServers() {
        _serverNodes.value = listOf(
            ServerNode("srv-tokyo-1", "Tokyo Cluster 01", "Asia (Tokyo)", "172.67.142.11", 24, 42, true, 28, 100),
            ServerNode("srv-us-east", "US-East Lambda", "North America", "104.21.32.90", 86, 68, true, 45, 100),
            ServerNode("srv-eu-west", "Frankfurt Edge 02", "Europe (EU)", "185.199.108.153", 112, 31, true, 19, 80),
            ServerNode("srv-sg-sgp", "Singapore Micro", "Southeast Asia", "139.180.201.44", 48, 18, true, 12, 60)
        )
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val admin = dao.getUserByUsername("elmodmen00")
        if (admin == null) {
            val adminUser = UserEntity(
                username = "elmodmen00",
                passwordHash = hashPassword("amar00"),
                role = "ADMIN",
                isActive = true,
                isOnline = false,
                maxSessions = 10,
                currentSessions = 0,
                maxServers = 8,
                planName = "Super Admin (Unlimited)",
                planExpiryDate = "2099-12-31"
            )
            dao.insertUser(adminUser)
        } else {
            dao.updateUser(admin.copy(passwordHash = hashPassword("amar00"), role = "ADMIN", isActive = true))
        }

            val studentUser = UserEntity(
                username = "student1",
                passwordHash = hashPassword("pass123"),
                role = "USER",
                isActive = true,
                isOnline = false,
                maxSessions = 3,
                currentSessions = 0,
                maxServers = 2,
                planName = "Pro Solver",
                planExpiryDate = "2026-11-30"
            )
            dao.insertUser(studentUser)

            val testUser = UserEntity(
                username = "471772108",
                passwordHash = hashPassword("ut8fGCb!"),
                role = "USER",
                isActive = true,
                isOnline = false,
                maxSessions = 5,
                currentSessions = 0,
                maxServers = 4,
                planName = "Elite VIP",
                planExpiryDate = "2026-12-31"
            )
            dao.insertUser(testUser)

            // Seed activation codes
            val defaultCodes = listOf(
                ActivationCodeEntity("SPX-VIP-2026-A1B2", "Elite VIP", 365, 5, 0, false, "admin"),
                ActivationCodeEntity("SPX-PRO-3M-X9Y8", "Pro Solver", 90, 3, 0, false, "admin"),
                ActivationCodeEntity("SPX-LIFE-7777-K", "Lifetime VIP", 3650, 1, 0, false, "admin"),
                ActivationCodeEntity("SPX-BASIC-30D-M2", "Basic Starter", 30, 10, 0, false, "admin")
            )
            dao.insertActivationCodes(defaultCodes)

            // Seed initial log
            dao.insertLog(
                ActivityLogEntity(
                    action = "SYSTEM_INITIALIZED",
                    targetUser = "admin",
                    details = "Sprix Solver platform database initialized with default security credentials and activation codes.",
                    level = "SUCCESS"
                )
            )
        }

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Auth
    suspend fun authenticate(username: String, password: String):Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = dao.getUserByUsername(username.trim())
        if (user == null) {
            dao.insertLog(ActivityLogEntity(action = "LOGIN_FAILED", targetUser = username, details = "Account does not exist", level = "WARNING"))
            return@withContext Result.failure(Exception("Invalid username or account not found"))
        }
        if (!user.isActive) {
            dao.insertLog(ActivityLogEntity(action = "LOGIN_BLOCKED", targetUser = username, details = "Attempt to log in to suspended account", level = "ERROR"))
            return@withContext Result.failure(Exception("Account is currently suspended. Contact support."))
        }
        val hashed = hashPassword(password)
        if (user.passwordHash != hashed) {
            dao.insertLog(ActivityLogEntity(action = "LOGIN_FAILED", targetUser = username, details = "Incorrect password provided", level = "WARNING"))
            return@withContext Result.failure(Exception("Incorrect password. Please try again."))
        }

        val updatedUser = user.copy(isOnline = true, currentSessions = (user.currentSessions + 1).coerceAtMost(user.maxSessions))
        dao.updateUser(updatedUser)
        dao.insertLog(ActivityLogEntity(action = "LOGIN_SUCCESS", targetUser = username, details = "User logged in securely via mobile client", level = "SUCCESS"))
        Result.success(updatedUser)
    }

    suspend fun logout(user: UserEntity) = withContext(Dispatchers.IO) {
        val current = dao.getUserByUsername(user.username)
        if (current != null) {
            val updated = current.copy(isOnline = false, currentSessions = (current.currentSessions - 1).coerceAtLeast(0))
            dao.updateUser(updated)
            dao.insertLog(ActivityLogEntity(action = "LOGOUT", targetUser = user.username, details = "User logged out successfully", level = "INFO"))
        }
    }

    // User Operations
    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()
    fun getTotalUsersCountFlow(): Flow<Int> = dao.getTotalUsersCountFlow()
    fun getOnlineUsersCountFlow(): Flow<Int> = dao.getOnlineUsersCountFlow()
    fun getActiveUsersCountFlow(): Flow<Int> = dao.getActiveUsersCountFlow()
    fun getSuspendedUsersCountFlow(): Flow<Int> = dao.getSuspendedUsersCountFlow()

    suspend fun createUser(
        username: String,
        passwordPlain: String,
        role: String,
        planName: String,
        maxSessions: Int,
        maxServers: Int,
        actor: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (username.isBlank() || passwordPlain.isBlank()) {
            return@withContext Result.failure(Exception("Username and password cannot be empty"))
        }
        val existing = dao.getUserByUsername(username.trim())
        if (existing != null) {
            return@withContext Result.failure(Exception("Username already exists"))
        }

        val newUser = UserEntity(
            username = username.trim(),
            passwordHash = hashPassword(passwordPlain),
            role = role,
            isActive = true,
            isOnline = false,
            maxSessions = maxSessions,
            currentSessions = 0,
            maxServers = maxServers,
            planName = planName,
            planExpiryDate = "2026-12-31"
        )
        dao.insertUser(newUser)
        dao.insertLog(
            ActivityLogEntity(
                action = "USER_CREATED",
                targetUser = username.trim(),
                details = "User created by $actor with plan $planName",
                level = "SUCCESS"
            )
        )
        Result.success(Unit)
    }

    suspend fun toggleUserActive(user: UserEntity, actor: String): Result<Unit> = withContext(Dispatchers.IO) {
        val newStatus = !user.isActive
        val updated = user.copy(isActive = newStatus, isOnline = if (!newStatus) false else user.isOnline)
        dao.updateUser(updated)
        dao.insertLog(
            ActivityLogEntity(
                action = if (newStatus) "USER_REACTIVATED" else "USER_SUSPENDED",
                targetUser = user.username,
                details = "Account status changed to ${if (newStatus) "Active" else "Suspended"} by $actor",
                level = if (newStatus) "SUCCESS" else "WARNING"
            )
        )
        Result.success(Unit)
    }

    suspend fun updateUserCredentials(user: UserEntity, newPasswordPlain: String?, newPlan: String?, maxSessions: Int, maxServers: Int, actor: String): Result<Unit> = withContext(Dispatchers.IO) {
        val updated = user.copy(
            passwordHash = if (!newPasswordPlain.isNullOrBlank()) hashPassword(newPasswordPlain) else user.passwordHash,
            planName = newPlan ?: user.planName,
            maxSessions = maxSessions,
            maxServers = maxServers
        )
        dao.updateUser(updated)
        dao.insertLog(
            ActivityLogEntity(
                action = "USER_UPDATED",
                targetUser = user.username,
                details = "Account settings updated by $actor",
                level = "INFO"
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteUser(user: UserEntity, actor: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (user.username == "elmodmen00" || user.username == "admin") {
            return@withContext Result.failure(Exception("Cannot delete primary admin account"))
        }
        dao.deleteUser(user)
        dao.insertLog(
            ActivityLogEntity(
                action = "USER_DELETED",
                targetUser = user.username,
                details = "Account permanently removed by $actor",
                level = "WARNING"
            )
        )
        Result.success(Unit)
    }

    // Activation Codes
    fun getAllActivationCodesFlow(): Flow<List<ActivationCodeEntity>> = dao.getAllActivationCodesFlow()

    suspend fun generateActivationCode(
        planName: String,
        durationDays: Int,
        maxUses: Int,
        actor: String
    ): Result<ActivationCodeEntity> = withContext(Dispatchers.IO) {
        val randomPart = UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.ROOT)
        val codePrefix = when (planName) {
            "Elite VIP" -> "SPX-VIP"
            "Lifetime VIP" -> "SPX-LIFE"
            "Pro Solver" -> "SPX-PRO"
            else -> "SPX-BAS"
        }
        val generatedCode = "$codePrefix-$randomPart"
        val entity = ActivationCodeEntity(
            code = generatedCode,
            planName = planName,
            durationDays = durationDays,
            maxUses = maxUses,
            usedCount = 0,
            isRevoked = false,
            createdBy = actor
        )
        dao.insertActivationCode(entity)
        dao.insertLog(
            ActivityLogEntity(
                action = "CODE_GENERATED",
                targetUser = actor,
                details = "New activation code $generatedCode generated for $planName ($durationDays days, $maxUses uses)",
                level = "SUCCESS"
            )
        )
        Result.success(entity)
    }

    suspend fun generateBulkCodes(
        count: Int,
        planName: String,
        durationDays: Int,
        actor: String
    ): Result<List<ActivationCodeEntity>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ActivationCodeEntity>()
        val codePrefix = when (planName) {
            "Elite VIP" -> "SPX-VIP"
            "Lifetime VIP" -> "SPX-LIFE"
            "Pro Solver" -> "SPX-PRO"
            else -> "SPX-BAS"
        }
        repeat(count) {
            val randomPart = UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.ROOT)
            list.add(
                ActivationCodeEntity(
                    code = "$codePrefix-$randomPart",
                    planName = planName,
                    durationDays = durationDays,
                    maxUses = 1,
                    usedCount = 0,
                    isRevoked = false,
                    createdBy = actor
                )
            )
        }
        dao.insertActivationCodes(list)
        dao.insertLog(
            ActivityLogEntity(
                action = "BULK_CODES_GENERATED",
                targetUser = actor,
                details = "Generated $count bulk activation codes for plan $planName",
                level = "SUCCESS"
            )
        )
        Result.success(list)
    }

    suspend fun redeemCode(codeText: String, username: String): Result<String> = withContext(Dispatchers.IO) {
        val code = dao.getActivationCode(codeText.trim())
            ?: return@withContext Result.failure(Exception("Activation code does not exist or is invalid."))

        if (code.isRevoked) {
            return@withContext Result.failure(Exception("This activation code has been revoked by admin."))
        }
        if (code.usedCount >= code.maxUses) {
            return@withContext Result.failure(Exception("This activation code has already reached its maximum usage limit."))
        }

        val user = dao.getUserByUsername(username)
            ?: return@withContext Result.failure(Exception("User not found."))

        // Calculate new expiry
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, code.durationDays)
        val newExpiry = sdf.format(cal.time)

        val updatedCode = code.copy(
            usedCount = code.usedCount + 1,
            redeemedBy = username
        )
        dao.updateActivationCode(updatedCode)

        val updatedUser = user.copy(
            planName = code.planName,
            planExpiryDate = newExpiry,
            maxSessions = if (code.planName.contains("VIP")) 8 else 4,
            maxServers = if (code.planName.contains("VIP")) 8 else 4
        )
        dao.updateUser(updatedUser)

        dao.insertLog(
            ActivityLogEntity(
                action = "CODE_REDEEMED",
                targetUser = username,
                details = "Redeemed code ${code.code} for plan ${code.planName}. Valid until $newExpiry",
                level = "SUCCESS"
            )
        )
        Result.success("Success! Activated ${code.planName} plan (valid until $newExpiry)")
    }

    suspend fun revokeCode(code: String, actor: String): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = dao.getActivationCode(code) ?: return@withContext Result.failure(Exception("Code not found"))
        dao.updateActivationCode(entity.copy(isRevoked = true))
        dao.insertLog(ActivityLogEntity(action = "CODE_REVOKED", targetUser = actor, details = "Code $code was revoked", level = "WARNING"))
        Result.success(Unit)
    }

    // Activity Logs & Solver History
    fun getActivityLogsFlow(): Flow<List<ActivityLogEntity>> = dao.getActivityLogsFlow()
    fun getSolverHistoryFlow(): Flow<List<SolverHistoryEntity>> = dao.getSolverHistoryFlow()

    suspend fun recordSolverExecution(tool: String, title: String, subjectOrStage: String, answer: String) = withContext(Dispatchers.IO) {
        dao.insertSolverHistory(
            SolverHistoryEntity(
                toolType = tool,
                title = title,
                subjectOrStage = subjectOrStage,
                answer = answer
            )
        )
    }

    // Server Node Control
    fun toggleServerNode(serverId: String) {
        _serverNodes.value = _serverNodes.value.map { node ->
            if (node.id == serverId) {
                node.copy(isOnline = !node.isOnline)
            } else node
        }
    }

    // Plans
    fun getAvailablePlans(): List<SubscriptionPlan> = listOf(
        SubscriptionPlan("plan_basic", "Basic Starter", 30, 9.99, listOf("Single Device", "Sprix Bank Subject 1 & 2", "Standard Queue", "1 Server"), "Starter"),
        SubscriptionPlan("plan_pro", "Pro Solver", 90, 24.99, listOf("Sprix Subjects 1, 2, 3", "Qureo Auto Solver 1-50", "3 Concurrent Sessions", "Priority Server Pool"), "Popular"),
        SubscriptionPlan("plan_vip", "Elite VIP", 365, 79.99, listOf("Full Unlimited Solver Access", "WISO AI Generator", "Max 8 Sessions", "Zero Latency Tokyo & EU Nodes", "VIP Discord Support"), "Recommended"),
        SubscriptionPlan("plan_life", "Lifetime VIP", 3650, 149.99, listOf("Lifetime License", "All Future Solvers Included", "Dedicated Private Proxy", "VIP Badge"), "Best Value")
    )

    // Sprix Question Bank (Subjects 1, 2, 3 - Full 20 Lessons/Questions per Subject)
    fun getSprixQuestions(subject: Int): List<SprixQuestion> {
        val list = mutableListOf<SprixQuestion>()
        val topics = when (subject) {
            1 -> listOf(
                "أساسيات الدخل والإنفاق المنزلي", "الميزانية الشهرية وقاعدة 50/30/20", "صندوق الطوارئ والسيولة النقدية",
                "الفرق بين الأصول والخصوم", "إدارة الديون وبطاقات الائتمان", "الفائدة البسيطة والفائدة المركبة",
                "مفهوم التضخم والقوة الشرائية", "الحسابات المصرفية الجارية والتوفير", "شهادات الإيداع والودائع الآجلة",
                "الادخار التلقائي والاقتطاع الشهري", "التأمين ضد المخاطر المالية", "الاحتيال المالي والأمان الرقمي",
                "التخطيط المالي للأهداف قصيرة المدى", "تقليل النفقات التقديرية", "التصنيف الائتماني (I-Score)",
                "إدارة الأزمات المالية الطارئة", "الضرائب المباشرة وغير المباشرة", "الادخار التعليمي والعائلي",
                "القروض الشخصية وحساب الفوائد", "التقييم الدوري للموقف المالي"
            )
            2 -> listOf(
                "مفهوم الاستثمار وتنمية الثروة", "تنويع الأصول وإدارة المخاطر", "صناديق الاستثمار المشتركة",
                "صناديق المؤشرات المتداولة (ETFs)", "سوق الأسهم ومفاهيم الحصص", "عوائد التوزيعات النقدية (Dividends)",
                "السندات والصكوك الحكومية", "العائد الحقيقي مقابل العائد الاسمي", "قاعدة 72 لمضاعفة رأس المال",
                "استراتيجية الاستثمار الدوري (DCA)", "التحليل المالي الأساسي للشركات", "مكرر الربحية ومضاعف القيمة الدفترية",
                "الاستثمار في العقارات وصناديق الريت", "السلع الأساسية والمعادن الثمينة", "تقلبات السوق والتعامل مع الهبوط",
                "الأفق الزمني للاستثمار وتأثيره", "إعادة توازن المحفظة الاستثمارية", "تكاليف ورسوم إدارة الصناديق",
                "استراتيجيات الاستثمار طويل الأجل", "تقييم العائد المعدل بالمخاطر"
            )
            else -> listOf(
                "السياسة النقدية والبنك المركزي", "أسعار الفائدة والتيسير الكمي", "منحنى العائد وتوقعات الركود",
                "أسواق الصرف الأجنبي (فوركس)", "المشتقات المالية وعقود التحوط", "تسعير السندات والعائد حتى الاستحقاق",
                "نسبة شارب ونسبة سورتينو", "صناديق التحوط وإدارة الأصول البديلة", "التمويل المؤسسي وهيكل رأس المال",
                "السيولة النقدية والتدفقات الحرة", "إدارة مخاطر العملات الأجنبية", "سوق السندات الدولية واليوروبوندز",
                "صناديق الثروة السيادية وتأثيرها", "التضخم الجامح وأزمات الديون", "التكنولوجيا المالية (FinTech) والمدفوعات",
                "العملات الرقمية والبلوكشين المصرفي", "الحوكمة والامتثال المصرفي", "التخطيط للتقاعد والحرية المالية",
                "الوصايا وتوزيع التركات والثروات", "الخطة الشاملة للأمان المالي المستدام"
            )
        }

        for ((index, topic) in topics.withIndex()) {
            val qNum = index + 1
            list.add(
                SprixQuestion(
                    id = "SPX-S$subject-%02d".format(qNum),
                    subject = subject,
                    chapter = "الوحدة $qNum: $topic",
                    question = "ما هو المعيار المعتمد لتطبيق $topic في الإدارة المالية الاحترافية؟",
                    options = listOf(
                        "التطبيق المنهجي المعتمد وفق معايير السوق الدولية بنسبة 100%",
                        "التجاهل التام وتكبد خسائر تقلبات العملة",
                        "الاعتماد على التخمين الفردي العشوائي",
                        "تجميد السيولة بدون عائد استثماري"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "تطبيق معايير $topic يوفر أقصى كفاءة مالية وأمان تشغيلي وحماية شاملة لرأس المال."
                )
            )
        }
        return list
    }

    // Qureo Auto Solver Stages (JavaScript: 54 chapters, Python: 25 chapters)
    fun getQureoStages(isPython: Boolean = false): List<QureoStage> {
        val list = mutableListOf<QureoStage>()
        val totalStages = if (isPython) 25 else 54
        val trackName = if (isPython) "Python" else "JavaScript"
        for (i in 1..totalStages) {
            val title = when {
                i <= 10 -> "$trackName - الشابتر $i: أساسيات الكود والحركة والمدخلات"
                i <= 25 -> "$trackName - الشابتر $i: الحلقات التكرارية والتحكم بالبيانات"
                i <= 40 -> "$trackName - الشابتر $i: الشروط المنطقية والدوال المتقدمة"
                else -> "$trackName - الشابتر $i: الخوارزميات وهياكل البيانات وتحدي الزعيم"
            }
            val subtitle = "Qureo $trackName Level $i"
            val desc = "حل آلي معتمد لشابتر $i في مسار $trackName وحقن تسلسل البلوكات بدقة 100%."
            val blocks = listOf("init_engine()", "build_flow_graph(chapter=$i)", "inject_solution_payload()", "verify_score_100()")
            val code = if (isPython) {
                "# Qureo Python Chapter $i Solution\ndef solve_qureo_chapter_$i():\n    engine.initialize()\n    engine.solve_level(chapter_id=$i, full_stars=True)\n    return 'COMPLETED'\nsolve_qureo_chapter_$i()"
            } else {
                "// Qureo JavaScript Chapter $i Solution\nfunction solveQureoChapter$i() {\n    window.__qureoApp.executeChapter($i, { perfectScore: true });\n    return 100;\n}\nsolveQureoChapter$i();"
            }
            val diff = if (i <= 10) "Easy" else if (i <= 25) "Intermediate" else "Advanced"
            list.add(QureoStage(i, title, subtitle, desc, blocks, code, diff))
        }
        return list
    }

    // WISO AI Practice Bank
    fun getWisoQuestions(): List<WisoQuestion> = listOf(
        WisoQuestion(
            "WISO-01", "Arbeitsrecht & Vertrag",
            "Welche Frist gilt für die Einreichung einer Kündigungsschutzklage nach Erhalt der Kündigung?",
            listOf("3 Wochen", "1 Woche", "3 Monate", "6 Monate"),
            0, "Gemäß § 4 KSchG muss die Klage innerhalb von 3 Wochen nach Zugang erhoben werden."
        ),
        WisoQuestion(
            "WISO-02", "Wirtschaftsordnung & Markt",
            "Welche Funktion hat die Bundesbank bzw. die EZB im Rahmen des ESZB vorrangig?",
            listOf("Preisniveaustabilität gewährleisten", "Aktienkurse maximieren", "Löhne direkt festlegen", "Steuern eintreiben"),
            0, "Das oberste Ziel des ESZB ist die Gewährleistung der Preisstabilität (Inflationsziel ~2%)."
        ),
        WisoQuestion(
            "WISO-03", "Sozialversicherungssystem",
            "Welcher Zweig der deutschen gesetzlichen Sozialversicherung wird rein durch die Arbeitgeber finanziert?",
            listOf("Gesetzliche Unfallversicherung (Berufsgenossenschaft)", "Rentenversicherung", "Krankenversicherung", "Arbeitslosenversicherung"),
            0, "Die gesetzliche Unfallversicherung (DGUV) wird zu 100% von den Arbeitgebern getragen."
        ),
        WisoQuestion(
            "WISO-04", "Betriebsverfassungsgesetz",
            "Ab wie vielen ständigen wahlberechtigten Arbeitnehmern kann in einem Betrieb ein Betriebsrat gewählt werden?",
            listOf("Mindestens 5 wahlberechtigte Arbeitnehmer", "Mindestens 20", "Erst ab 100", "Nur bei Aktiengesellschaften"),
            0, "Nach § 1 BetrVG werden Betriebsräte in Betrieben mit mindestens 5 ständigen wahlberechtigten Arbeitnehmern gewählt."
        )
    )
}
