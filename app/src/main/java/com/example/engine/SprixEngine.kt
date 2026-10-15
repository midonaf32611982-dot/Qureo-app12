package com.example.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.Normalizer

data class SprixSubject(
    val id: String,
    val name: String,
    val platformName: String,
    val supported: Boolean,
    val totalItems: Int
)

class SprixLoginException(message: String) : RuntimeException(message)

/**
 * محرك حل الثقافة المالية (Sprix) الحقيقي — ترجمة لمنطق solver.py:
 * يكتشف البنود من المنهج (curriculum) ويحل المفتوح تباعًا: درس ← مراجعة ← اختبار، ويتعلّم من نتائج الاختبارات.
 */
class SprixEngine(
    private val subjectId: String,
    private val log: (String) -> Unit,
    private val isStopped: () -> Boolean,
    speed: Int,
    private val store: BankStore,
    private val assets: AssetSource
) {
    companion object {
        const val BASE = "https://egy.app.spx-learning-square.com"
        const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
        private const val TEST_ATTEMPTS = 6

        /** المواد المعروفة (7 = عربي، 3 = لغات) — أي مادة أخرى تعمل بدون بنك جاهز. */
        val KNOWN = mapOf(
            "7" to "الثقافة المالية (عربي)",
            "3" to "Financial Literacy (لغات)"
        )

        fun normKey(text: String?): String {
            if (text.isNullOrEmpty()) return ""
            val sb = StringBuilder()
            for (c in text) {
                if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) continue
                sb.append(
                    when (c) {
                        '\u0623', '\u0625', '\u0622' -> '\u0627'
                        '\u0649' -> '\u064a'
                        else -> c
                    }
                )
            }
            return sb.toString().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
        }

        /** تسجيل الدخول وجلب مواد الحساب لقائمة المادة. يرجع (القائمة، رسالة خطأ). */
        fun listSubjects(code: String, password: String): Pair<List<SprixSubject>, String?> {
            val s = Session()
            s.defaultHeaders.putAll(mapOf("Content-Type" to "application/json", "Accept" to "application/json", "User-Agent" to UA))
            return try {
                val body = JSONObject().put("login_id", code).put("password", password).toString()
                val r = s.request("POST", "$BASE/api/v1/auth/login", body = body)
                if (r.geoBlocked) return emptyList<SprixSubject>() to "المنصة بتحجب الاتصال خارج مصر — اقفل أي VPN أجنبي واتصل بشبكة مصرية"
                val d = r.obj()
                if (d == null || !d.optBoolean("success", false)) {
                    val e = d?.opt("error")
                    val msg = if (e is JSONObject) e.str("message") else (e?.toString() ?: "")
                    return emptyList<SprixSubject>() to msg.ifEmpty { "بيانات الدخول غير صحيحة" }
                }
                val list = s.request("GET", "$BASE/api/v1/subjects").obj()?.optJSONArray("data") ?: JSONArray()
                val out = ArrayList<SprixSubject>()
                for (i in 0 until list.length()) {
                    val o = list.optJSONObject(i) ?: continue
                    val sid = o.anyId("id")?.toString() ?: continue
                    out.add(
                        SprixSubject(
                            id = sid,
                            name = KNOWN[sid] ?: o.str("name").ifEmpty { sid },
                            platformName = o.str("name").ifEmpty { sid },
                            supported = KNOWN.containsKey(sid),
                            totalItems = o.optInt("total_items", 0)
                        )
                    )
                }
                out to null
            } catch (e: IOException) {
                emptyList<SprixSubject>() to "تعذّر الاتصال بالمنصة — تحقق من الإنترنت."
            }
        }
    }

    private val pace = Speed.delayMs(speed)
    private val http = Session()
    private val subjectName = KNOWN[subjectId] ?: "مادة $subjectId"
    private val bankKey = "sprix_bank_$subjectId"
    private val testBank = HashMap<Long, Long>()
    private val blockBank = HashMap<String, Long>()
    private val textBank = HashMap<String, Pair<String, String>>()
    private var solvedCount = 0
    private var quizCount = 0

    init {
        http.defaultHeaders.putAll(mapOf("Content-Type" to "application/json", "Accept" to "application/json", "User-Agent" to UA))
    }

    // ───────── البنوك ─────────
    private fun readBank(text: String?) {
        val root = text?.let { parseJson(it) } as? JSONObject ?: return
        root.optJSONObject("test")?.let { t ->
            val ks = t.keys()
            while (ks.hasNext()) {
                val k = ks.next()
                val v = normVal(t.opt(k)) as? Long ?: continue
                k.toLongOrNull()?.let { testBank[it] = v }
            }
        }
        root.optJSONObject("block")?.let { b ->
            val ks = b.keys()
            while (ks.hasNext()) {
                val k = ks.next()
                (normVal(b.opt(k)) as? Long)?.let { blockBank[k] = it }
            }
        }
    }

    private fun loadBanks() {
        readBank(assets.read("sprix_bank_$subjectId.json"))
        readBank(store.load(bankKey))
        try {
            val arch = assets.read("sprix_text_$subjectId.json")?.let { parseJson(it) } as? JSONObject
            val lessons = arch?.optJSONArray("lessons") ?: JSONArray()
            for (i in 0 until lessons.length()) {
                val qs = lessons.optJSONObject(i)?.optJSONArray("questions") ?: continue
                for (j in 0 until qs.length()) {
                    val q = qs.optJSONObject(j) ?: continue
                    val at = q.optJSONObject("answer")?.str("text")?.trim() ?: ""
                    val nq = normKey(q.str("question"))
                    if (nq.isNotEmpty() && at.isNotEmpty()) textBank[nq] = normKey(at) to at
                }
            }
        } catch (_: Exception) {
        }
        log("المادة $subjectId ($subjectName): ${testBank.size} إجابة اختبار + ${blockBank.size} إجابة درس + ${textBank.size} سؤال نصي.")
    }

    private fun saveBank() {
        try {
            val t = JSONObject()
            for ((k, v) in testBank) t.put(k.toString(), v)
            val b = JSONObject()
            for ((k, v) in blockBank) b.put(k, v)
            store.save(bankKey, JSONObject().put("test", t).put("block", b).toString())
        } catch (_: Exception) {
        }
    }

    private fun check() {
        if (isStopped()) throw StoppedException()
    }

    private fun req(method: String, path: String, body: String? = null): Pair<Int, JSONObject?> {
        var lastErr: IOException? = null
        for (i in 1..3) {
            check()
            if (pace > 0) pause(pace, isStopped)
            try {
                val r = http.request(method, BASE + path, body = body)
                if (r.geoBlocked) throw GeoBlockedException()
                return r.status to r.obj()
            } catch (e: IOException) {
                lastErr = e
                pause(1500L * i, isStopped)
            }
        }
        throw IllegalStateException("تعذّر الاتصال بالمنصة: ${lastErr?.javaClass?.simpleName}")
    }

    private fun errMsg(d: JSONObject?): String {
        val e = d?.opt("error")
        return if (e is JSONObject) e.str("message") else (e?.toString() ?: "")
    }

    private fun optsFor(payload: JSONObject, qid: Long): JSONArray? {
        val opts = payload.optJSONObject("options") ?: return null
        opts.optJSONArray(qid.toString())?.let { return it }
        val ks = opts.keys()
        while (ks.hasNext()) {
            val a = opts.optJSONArray(ks.next())
            if (a != null && a.length() > 0 && a.optJSONObject(0)?.optLong("question_id", -1L) == qid) return a
        }
        return null
    }

    private fun matchTextAnswer(payload: JSONObject, qid: Long, opts: JSONArray): Long? {
        val text = payload.optJSONObject("questions")?.str(qid.toString()) ?: ""
        val nq = normKey(text)
        val hit = textBank[nq] ?: return null
        for (i in 0 until opts.length()) {
            val o = opts.optJSONObject(i) ?: continue
            if (normKey(o.str("text")) == hit.first) return o.optLong("id")
        }
        return null
    }

    /** يحل المادة كاملة. onProgress(done, total, title). يرجع (نجاح، رسالة). */
    fun run(code: String, password: String, onProgress: (Int, Int, String) -> Unit): Pair<Boolean, String> {
        val started = System.currentTimeMillis()
        try {
            log("المادة المطلوبة: $subjectName (رقم $subjectId).")
            loadBanks()
            log("Logging in...")
            val body = JSONObject().put("login_id", code).put("password", password).toString()
            val (_, d) = req("POST", "/api/v1/auth/login", body)
            if (d == null || !d.optBoolean("success", false)) {
                log("فشل تسجيل الدخول: ${errMsg(d).ifEmpty { "بيانات غير صحيحة" }}")
                throw SprixLoginException("فشل تسجيل الدخول — تحقق من الكود وكلمة المرور.")
            }
            log("Logged in")

            val (cs, cd) = req("GET", "/api/v1/subjects/$subjectId/curriculum")
            if (cs == 403 || cd == null || !cd.optBoolean("success", false)) {
                return false to "لا تملك صلاحية على مادة «$subjectName» — جرّب مادة أخرى."
            }

            val failed = HashSet<String>()
            var first = true
            while (true) {
                check()
                val (_, cur) = req("GET", "/api/v1/subjects/$subjectId/curriculum")
                val data = cur?.opt("data")
                val units: JSONArray = when (data) {
                    is JSONArray -> data
                    is JSONObject -> data.optJSONArray("units") ?: JSONArray()
                    else -> JSONArray()
                }
                var total = 0
                var done = 0
                var next: JSONObject? = null
                for (i in 0 until units.length()) {
                    val items = units.optJSONObject(i)?.optJSONArray("items") ?: continue
                    total += items.length()
                    for (j in 0 until items.length()) {
                        if (items.optJSONObject(j)?.optBoolean("is_completed", false) == true) done++
                    }
                    if (next == null) {
                        for (j in 0 until items.length()) {
                            val it = items.optJSONObject(j) ?: continue
                            val key = it.str("item_type") + ":" + it.anyId("id")
                            if (it.optBoolean("is_unlocked", false) && !it.optBoolean("is_completed", false) && key !in failed) {
                                next = it
                                break
                            }
                        }
                    }
                }
                onProgress(done, total, next?.str("title") ?: "")

                if (next == null) {
                    log(if (first) "لا بنود مفتوحة الآن — لم يظهر شيء." else "تم الانتهاء من كل البنود المفتوحة.")
                    val secs = (System.currentTimeMillis() - started) / 1000.0
                    log("الملخص [$subjectName]: حل $solvedCount بندًا، أجريت $quizCount اختبار، في ${"%.1f".format(secs)} ثانية.")
                    return true to "اكتمل حل كل البنود في $subjectName."
                }
                first = false

                val itype = next.str("item_type")
                val iid = next.anyId("id") as? Long
                if (iid == null) {
                    // معرّف غير رقمي: نتجاوزه حتى لا تتكرر الحلقة على نفس البند
                    failed.add(next.str("item_type") + ":" + next.anyId("id"))
                    continue
                }
                val title = next.str("title").ifEmpty { "$itype $iid" }
                log("جاري حل: $title")
                val key = "$itype:$iid"
                try {
                    val ok = when (itype) {
                        "chapter" -> solveChapter(iid)
                        "review" -> solveReview(iid)
                        "test" -> solveTest(iid)
                        else -> {
                            log("نوع بند غير مدعوم: $itype — تجاوز.")
                            false
                        }
                    }
                    if (ok) solvedCount++ else failed.add(key)
                } catch (e: StoppedException) {
                    throw e
                } catch (e: GeoBlockedException) {
                    throw e
                } catch (e: Exception) {
                    log("تعذر حل $itype #$iid: ${(e.message ?: "").take(120)}")
                    failed.add(key)
                }
                saveBank()
            }
        } catch (e: StoppedException) {
            saveBank()
            log("تم إيقاف العملية بطلب منك — جرى حفظ ما اكتمل.")
            return false to "أوقفت العملية — حفظ ما اكتمل."
        }
    }

    private fun solveChapter(cid: Long): Boolean {
        val (_, d) = req("GET", "/api/v1/chapters/$cid/blocks")
        val blocks = d?.optJSONObject("data")?.optJSONArray("blocks") ?: JSONArray()
        for (i in 0 until blocks.length()) {
            val b = blocks.optJSONObject(i) ?: continue
            if (!b.truthy("question_id")) continue
            val opts = b.optJSONArray("options") ?: continue
            if (opts.length() == 0) continue
            val bid = b.anyId("id") ?: continue
            val key = bid.toString()
            val known = blockBank[key]
            val trials = ArrayList<Long>()
            if (known != null) trials.add(known)
            for (k in 0 until opts.length()) opts.optJSONObject(k)?.optLong("id")?.let { trials.add(it) }
            val seen = HashSet<Long>()
            var okQ = false
            for (oid in trials) {
                if (!seen.add(oid)) continue
                check()
                val payload = JSONObject().put("block_id", bid).put("selected_option_id", oid).put("hint_used", 0).toString()
                val (_, da) = req("POST", "/api/v1/chapters/$cid/answer", payload)
                if (da?.optJSONObject("data")?.optBoolean("is_correct", false) == true) {
                    if (known != null && known != oid) log("تصحيح إجابة محفوظة لكتلة $key.")
                    blockBank[key] = oid
                    okQ = true
                    break
                }
            }
            if (!okQ) log("لم تُجب كتل الدرس #$cid كلها — المتابعة على أي حال.")
        }
        val (_, dc) = req("POST", "/api/v1/chapters/$cid/complete", "{}")
        val ok = dc?.optJSONObject("data")?.optBoolean("completed", false) == true
        if (ok) log("درس اكتمل: #$cid.") else log("تعذر إكمال الدرس #$cid: ${errMsg(dc)}")
        return ok
    }

    private fun solveReview(rid: Long): Boolean {
        val (_, d) = req("GET", "/api/v1/reviews/$rid/items")
        val n = d?.optJSONObject("data")?.optJSONArray("items")?.length() ?: 0
        val (_, d2) = req("POST", "/api/v1/reviews/$rid/complete", "{}")
        val ok = d2?.optJSONObject("data")?.optBoolean("completed", false) == true
        if (ok) log("مراجعة اكتملت: #$rid ($n بطاقة).") else log("تعذر إكمال المراجعة #$rid: ${errMsg(d2)}")
        return ok
    }

    private fun solveTest(tid: Long): Boolean {
        var attempts = 0
        while (attempts < TEST_ATTEMPTS) {
            check()
            attempts++
            val (_, d) = req("POST", "/api/v1/tests/$tid/start", "{}")
            val data = d?.optJSONObject("data")
            if (data == null) {
                log("تعذر بدء الاختبار #$tid: ${errMsg(d)}")
                break
            }
            val attemptId = data.optJSONObject("response")?.anyId("id")
            val tqs = data.optJSONArray("test_questions") ?: JSONArray()
            var unknown = 0
            for (qi in 0 until tqs.length()) {
                val tq = tqs.optJSONObject(qi) ?: continue
                val tqId = tq.optLong("id", -1L)
                var opt: Long? = testBank[tqId]
                if (opt == null) {
                    val qid = tq.optLong("question_id", -1L)
                    val opts = optsFor(data, qid) ?: continue
                    if (opts.length() == 0) continue
                    unknown++
                    opt = matchTextAnswer(data, qid, opts)
                    if (opt == null) {
                        // سؤال جديد: نبدّل الخيار بين المحاولات حتى يتسع التعلّم مع كل محاولة.
                        opt = opts.optJSONObject((attempts - 1 + qi) % opts.length())?.optLong("id")
                    }
                }
                if (opt == null) continue
                req("PUT", "/api/v1/tests/$tid/questions/$tqId/answer", JSONObject().put("selected_option_id", opt).toString())
            }
            val (_, ds) = req("POST", "/api/v1/tests/$tid/submit", "{}")
            val resp = ds?.optJSONObject("data")?.optJSONObject("response")
            val passed = resp?.optBoolean("passed", false) == true
            val score = resp?.opt("total_score")

            var learnedN = 0
            if (attemptId != null) {
                try {
                    val (_, dr) = req("GET", "/api/v1/tests/$tid/result?response_id=$attemptId")
                    val rdata = dr?.optJSONObject("data")
                    val rtq = rdata?.optJSONArray("test_questions") ?: JSONArray()
                    for (k in 0 until rtq.length()) {
                        val tq = rtq.optJSONObject(k) ?: continue
                        val opts = optsFor(rdata!!, tq.optLong("question_id", -1L)) ?: continue
                        for (m in 0 until opts.length()) {
                            val o = opts.optJSONObject(m) ?: continue
                            if (o.optBoolean("is_correct", false)) {
                                val tqId = tq.optLong("id", -1L)
                                if (!testBank.containsKey(tqId)) learnedN++
                                testBank[tqId] = o.optLong("id")
                                break
                            }
                        }
                    }
                } catch (e: StoppedException) {
                    throw e
                } catch (e: GeoBlockedException) {
                    throw e
                } catch (_: Exception) {
                }
            }
            if (learnedN > 0) {
                log("تعلّمت $learnedN إجابة جديدة للاختبار #$tid — حُفظت في بنك المادة.")
                saveBank()
            }
            if (passed) {
                quizCount++
                log("اختبار اجتاز بنجاح بدرجة $score في محاولة $attempts.")
                return true
            }
            log("الاختبار #$tid لم يجتز (درجة $score) — محاولة $attempts/$TEST_ATTEMPTS" + (if (unknown > 0) " — $unknown سؤال بلا إجابة محفوظة." else "."))
        }
        log("فشل اجتياز الاختبار #$tid بعد $attempts محاولات — أعد التشغيل فسيكمل من البنك.")
        return false
    }
}
