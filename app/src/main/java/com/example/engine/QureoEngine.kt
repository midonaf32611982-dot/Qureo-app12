package com.example.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class QureoChapter(
    val id: Long,
    val name: String,
    val seq: Long,
    val q: Int,
    val best: Int,
    val done: Boolean
)

data class QureoSummary(val course: String, val total: Int, val solved: Int, val locked: Int)

class QureoLoginException(message: String) : RuntimeException(message)

/**
 * محرك حل Qureo الحقيقي (HTTP مباشر من الجهاز) — ترجمة لمنطق solver.py:
 * تسجيل دخول، اكتشاف الفصول من الـ API، تخطي المنتهي، حل كل فصل، وتعلّم الإجابات الصحيحة من المنصة.
 */
class QureoEngine(
    private val course: String,
    private val log: (String) -> Unit,
    private val isStopped: () -> Boolean,
    speed: Int,
    private val store: BankStore,
    assets: AssetSource
) {
    companion object {
        const val BASE = "https://me-tp.qureo.education"
        const val API = "$BASE/api/study"
        const val PORTAL = "https://me-portal.qureo.education"
        const val JAVASCRIPT = "javascript_ar"
        const val PYTHON = "python_short_ar"
        private const val BANK_KEY = "qureo_learned"
    }

    private val pace = Speed.delayMs(speed)
    private val http = Session()
    private var sectionId = 0L
    private var credsId = ""
    private var credsPass = ""
    private var lastLogin = 0L
    private var lastReason = ""
    private val bank = HashMap<String, Any>()
    private val learned = HashMap<String, Any>()

    init {
        http.defaultHeaders.putAll(
            mapOf(
                "accept" to "application/json, text/plain, */*",
                "accept-language" to "ar-EG,ar;q=0.9,en;q=0.7",
                "content-type" to "application/json",
                "origin" to BASE,
                "referer" to "$BASE/",
                "sec-fetch-dest" to "empty",
                "sec-fetch-mode" to "cors",
                "sec-fetch-site" to "same-origin",
                "cache-control" to "no-cache",
                "pragma" to "no-cache",
                "user-agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36"
            )
        )
        setCourseCookie()
        loadBank(assets)
    }

    // ───────── البنك: إجابات جاهزة + ما تعلّمه التطبيق من المنصة ─────────
    private fun loadBank(assets: AssetSource) {
        try {
            val seed = assets.read("qureo_answers.json")
            val root = seed?.let { parseJson(it) } as? JSONObject
            if (root != null) {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val chap = root.optJSONObject(keys.next()) ?: continue
                    val qk = chap.keys()
                    while (qk.hasNext()) {
                        val qid = qk.next()
                        val tv = chap.optJSONArray(qid) ?: continue
                        if (tv.length() == 2) normVal(tv.get(1))?.let { bank[qid] = it }
                    }
                }
            }
        } catch (_: Exception) {
        }
        try {
            val l = store.load(BANK_KEY)?.let { parseJson(it) } as? JSONObject
            if (l != null) {
                val keys = l.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    normVal(l.get(k))?.let { bank[k] = it; learned[k] = it }
                }
            }
        } catch (_: Exception) {
        }
        log("Answer bank: ${bank.size} saved answers")
    }

    private fun learn(qid: Any?, value: Any?): Boolean {
        if (qid == null || value == null) return false
        val key = qid.toString()
        val v = normVal(value) ?: return false
        if (bank[key] == v) return false
        bank[key] = v
        learned[key] = v
        try {
            val o = JSONObject()
            for ((k, x) in learned) o.put(k, x)
            store.save(BANK_KEY, o.toString())
        } catch (_: Exception) {
        }
        return true
    }

    private fun check() {
        if (isStopped()) throw StoppedException()
    }

    private fun snip(d: Any?): String = (d?.toString() ?: "").replace(Regex("\\s+"), " ").take(160)

    private fun setCourseCookie() {
        http.setCookie("student_selected_content_id", course)
    }

    private fun call(method: String, path: String, body: JSONObject? = null): Pair<Int, Any?> {
        for (i in 1..5) {
            check()
            if (pace > 0) pause(pace, isStopped)
            try {
                var r = http.request(method, API + path, body = body?.toString())
                if (r.geoBlocked) throw GeoBlockedException()
                if (r.status == 401 && credsId.isNotEmpty() && System.currentTimeMillis() - lastLogin > 20000) {
                    log("401 - renewing session")
                    login(credsId, credsPass)
                    r = http.request(method, API + path, body = body?.toString())
                }
                return r.status to (r.json ?: r.text)
            } catch (e: IOException) {
                pause(minOf(2000L * i, 10000L), isStopped)
            }
        }
        return 0 to null
    }

    /** true = نجح الدخول. يرمي QureoLoginException لو الشبكة/الحجب. */
    fun login(sid: String, pwd: String): Boolean {
        credsId = sid
        credsPass = pwd
        lastLogin = System.currentTimeMillis()
        http.defaultHeaders.remove("authorization")
        try {
            val body = JSONObject().put("student_id", sid).put("password", pwd)
            val r = http.request(
                "POST", "$PORTAL/oauth2/password-login",
                mapOf("origin" to PORTAL, "referer" to "$PORTAL/"), body.toString()
            )
            if (r.geoBlocked) throw GeoBlockedException()
            val got = http.hasCookie("egy_student_access_token") || http.hasCookie("egy_student_id_token")
            log("Login POST -> HTTP ${r.status} | auth cookie: ${if (got) "yes" else "no"}")
            if ((r.status == 200 || r.status == 302) && got) {
                val d = r.obj()
                val tok = d?.str("access_token")?.ifEmpty { null }
                    ?: d?.str("token")?.ifEmpty { null }
                    ?: d?.str("jwt")?.ifEmpty { null }
                    ?: d?.optJSONObject("data")?.str("access_token")?.ifEmpty { null }
                if (tok != null) http.defaultHeaders["authorization"] = "Bearer $tok"
                setCourseCookie()
                call("GET", "/students/me")
                return true
            }
            log("Login rejected (check ID / password).")
            return false
        } catch (e: IOException) {
            throw QureoLoginException("تعذّر الاتصال بمنصة Qureo — تحقق من الإنترنت وحاول مرة أخرى.")
        }
    }

    private fun resolveSection() {
        var (st, d) = call("GET", "/courses/$course")
        if (st == 403) {
            setCourseCookie()
            val again = call("GET", "/courses/$course")
            st = again.first
            d = again.second
        }
        if (st == 200 && d is JSONObject) {
            val sid = d.optJSONObject("basic_section")?.optLong("id", 0L) ?: 0L
            if (sid != 0L) {
                sectionId = sid
                return
            }
        }
        throw IllegalStateException("المسار '$course' غير متاح لهذا الحساب (HTTP $st).")
    }

    /** اكتشاف الفصول تلقائيًا من الـ API مع حالة التقدم. */
    private fun chapters(): List<QureoChapter> {
        val (_, static) = call("GET", "/sections/$sectionId")
        val base: JSONArray = when (static) {
            is JSONObject -> static.optJSONArray("chapters") ?: JSONArray()
            is JSONArray -> static
            else -> JSONArray()
        }
        val (st, prog) = call("GET", "/students/sections/$sectionId/chapters")
        val pm = HashMap<Long, JSONObject>()
        if (st == 200 && prog is JSONArray) {
            for (i in 0 until prog.length()) {
                val p = prog.optJSONObject(i) ?: continue
                val k = (normVal(p.anyId("chapter_id")) ?: normVal(p.anyId("id"))) as? Long ?: continue
                pm[k] = p
            }
        }
        val out = ArrayList<QureoChapter>()
        for (i in 0 until base.length()) {
            val c = base.optJSONObject(i) ?: continue
            val cid = (c.anyId("id") ?: c.anyId("chapter_id")) as? Long ?: continue
            val p = pm[cid] ?: JSONObject()
            out.add(
                QureoChapter(
                    id = cid,
                    name = c.str("name").ifEmpty { c.str("title") },
                    seq = c.optLong("seq", 0L),
                    q = c.optInt("question_count", 0),
                    best = p.optInt("best_correct_count", 0),
                    done = p.truthy("completed_at") || c.truthy("completed_at")
                )
            )
        }
        return out.sortedWith(compareBy({ it.seq }, { it.id }))
    }

    private fun chapterInfo(cid: Long): Pair<JSONObject, JSONArray> {
        val (st, d) = call("GET", "/chapters/$cid")
        if (st != 200 || d !is JSONObject) throw IllegalStateException("تعذّر قراءة الفصل $cid (HTTP $st).")
        return (d.optJSONObject("chapter") ?: JSONObject()) to (d.optJSONArray("lectures") ?: JSONArray())
    }

    /** true = المحاضرات غير مكتملة الشروط (الفصل مقفول). */
    private fun lectures(cid: Long): Boolean {
        val (_, lects) = chapterInfo(cid)
        for (i in 0 until lects.length()) {
            val lid = lects.optJSONObject(i)?.anyId("id") ?: continue
            call("PUT", "/students/lectures/$lid")
            val (st, d) = call("PUT", "/students/lectures/$lid/complete")
            if (st == 403 && d is JSONObject && d.truthy("not_fulfill_preconditions")) {
                lastReason = "lecture $lid not completable (HTTP 403): ${snip(d)}"
                return true
            }
        }
        return false
    }

    private fun startTest(cid: Long) = call("PUT", "/students/chapters/$cid/test")

    private fun answer(qtype: String, qid: Any, value: Any, review: Boolean): Pair<Int, Any?> {
        val description = qtype == "description"
        val kind = if (description) "description" else "choice"
        val body = JSONObject().put(if (description) "answer_code" else "choice_id", value)
        val base = if (review) "review-questions" else "test-questions"
        var res = call("PUT", "/students/$base/$qid/answer/$kind", body)
        if (res.first == 404 && !review) {
            res = call("PUT", "/students/review-questions/$qid/answer/$kind", body)
        }
        return res
    }

    private fun dummy(q: JSONObject, item: JSONObject): Any? {
        if (q.str("type") == "description") {
            val lang = item.optJSONArray("descriptions")?.optJSONObject(0)?.str("lang")?.lowercase() ?: "python"
            return if (lang.contains("javascript") || lang == "js" || lang == "ts") "console.log(0)" else "print(0)"
        }
        val ch = item.optJSONArray("choices") ?: return null
        for (i in 0 until ch.length()) ch.optJSONObject(i)?.anyId("id")?.let { return it }
        return null
    }

    private fun reveal(qtype: String, d: Any?): Any? {
        val res = (d as? JSONObject)?.optJSONObject("result") ?: return null
        if (qtype == "description") return if (res.isNull("model_answer")) null else normVal(res.opt("model_answer"))
        val ch = res.optJSONArray("choices") ?: return null
        for (i in 0 until ch.length()) {
            val c = ch.optJSONObject(i) ?: continue
            if (c.optBoolean("correct", false)) return c.optJSONObject("choice")?.anyId("id")
        }
        return null
    }

    private fun correct(d: Any?): Boolean {
        val o = d as? JSONObject ?: return false
        val r = o.optJSONObject("result")
        return if (r != null && r.has("correct")) r.optBoolean("correct", false) else o.truthy("complete")
    }

    private fun completeScore(d: Any?): Int? {
        val v = (d as? JSONObject)?.optJSONObject("complete")?.optJSONObject("chapter")
        return if (v != null && v.has("best_correct_count") && !v.isNull("best_correct_count")) v.optInt("best_correct_count") else null
    }

    /** الإجابات الصحيحة التي تكشفها المنصة بعد محاولة الاختبار. */
    private fun resultAnswers(cid: Long): Map<Any, Any> {
        val (st, data) = call("GET", "/students/chapters/$cid/test/result")
        val out = HashMap<Any, Any>()
        if (st == 200 && data is JSONObject) {
            val res = data.optJSONObject("result") ?: return out
            val cr = res.optJSONArray("choice_results") ?: JSONArray()
            for (i in 0 until cr.length()) {
                val item = cr.optJSONObject(i) ?: continue
                val chs = item.optJSONArray("choices") ?: continue
                for (j in 0 until chs.length()) {
                    val c = chs.optJSONObject(j) ?: continue
                    if (c.optBoolean("correct", false)) {
                        val id = c.optJSONObject("choice")?.anyId("id")
                        val q = item.anyId("question_id")
                        if (id != null && q != null) out[q] = id
                        break
                    }
                }
            }
            val dr = res.optJSONArray("description_results") ?: JSONArray()
            for (i in 0 until dr.length()) {
                val item = dr.optJSONObject(i) ?: continue
                val q = item.anyId("question_id")
                if (q != null && !item.isNull("model_answer")) out[q] = normVal(item.get("model_answer"))!!
            }
        }
        return out
    }

    private fun bestSection(pool: List<SiteSection>, item: JSONObject): SiteSection? {
        val q = item.optJSONObject("question") ?: return null
        val qn = SiteAnswers.norm(q.str("content").replace(Regex("<[^>]+>"), " "))
        if (qn.isEmpty()) return null
        val qHead = qn.take(300)
        val q60 = qn.take(60)
        var top: SiteSection? = null
        var score = 0.0
        for (s in pool) {
            val sn = SiteAnswers.norm((if (s.q.size > 1) s.q.drop(1) else s.q).joinToString(" "))
            var r = SiteAnswers.ratio(qHead, sn.take(300))
            if (q60.isNotEmpty() && sn.contains(q60)) r = maxOf(r, 0.9)
            if (r > score) {
                top = s
                score = r
            }
        }
        return if (score >= 0.55) top else null
    }

    private fun bestChoice(sec: SiteSection, item: JSONObject): Any? {
        val atxt = SiteAnswers.norm(sec.a.joinToString(" "))
        val choices = item.optJSONArray("choices") ?: return null
        var top: JSONObject? = null
        var score = 0.0
        for (i in 0 until choices.length()) {
            val c = choices.optJSONObject(i) ?: continue
            val cn = SiteAnswers.norm(c.str("content"))
            if (cn.isEmpty() || atxt.isEmpty()) continue
            var r = SiteAnswers.ratio(atxt, cn)
            if (atxt.contains(cn) || cn.contains(atxt)) r = maxOf(r, 0.92)
            if (r > score) {
                top = c
                score = r
            }
        }
        return if (top != null && score >= 0.6) top.anyId("id") else null
    }

    /** إجابات مراجعات المستوى واختبارات الفهم من صفحات efrah7 (مطابقة بنص السؤال). */
    private fun siteAnswers(chapter: JSONObject, items: JSONArray): Map<Any, Any> {
        val pages = SiteAnswers.PAGES[course] ?: return emptyMap()
        // لا حاجة لجلب الصفحات لو البنك يغطي كل الأسئلة
        var needed = false
        for (i in 0 until items.length()) {
            val qid = items.optJSONObject(i)?.optJSONObject("question")?.anyId("id") ?: continue
            if (!bank.containsKey(qid.toString())) {
                needed = true
                break
            }
        }
        if (!needed) return emptyMap()

        val seq = chapter.optInt("seq", -1)
        val order = ArrayList<Triple<Int, String, Int>>()
        for ((rel, nums) in pages) for (n in nums) order.add(Triple(if (n == seq) 0 else 1, rel, n))
        val ordered = order.sortedBy { it.first }   // صفحة الفصل نفسه أولًا ثم الباقي

        val out = HashMap<Any, Any>()
        val pool = ArrayList<SiteSection>()
        var failed = 0
        var todo: List<JSONObject> = (0 until items.length()).mapNotNull { items.optJSONObject(it) }
        for ((_, rel, n) in ordered) {
            if (todo.isEmpty()) break
            check()
            val secs = SiteAnswers.load(course, rel, n)
            if (secs == null) {
                failed++
                continue
            }
            pool.addAll(secs)
            val rest = ArrayList<JSONObject>()
            for (item in todo) {
                val sec = bestSection(pool, item)
                if (sec == null) {
                    rest.add(item)
                    continue
                }
                val q = item.optJSONObject("question") ?: JSONObject()
                val qid = q.anyId("id")
                val v: Any? = if (q.str("type") == "description") SiteAnswers.answerCode(sec.a) else bestChoice(sec, item)
                if (qid != null && v != null && v != "") out[qid] = v else rest.add(item)
            }
            todo = rest
            if (out.size >= items.length()) break
        }
        if (failed > 0 && out.isEmpty()) {
            log("Answer pages unreachable - continuing without them")
        } else if (out.isNotEmpty()) {
            log("Answer pages: matched ${out.size}/${items.length()} questions")
        }
        return out
    }

    /** يرجع Triple(الحالة، الصحيح، الإجمالي): الحالة "ok" أو "locked". */
    private fun solveChapter(cid: Long, maxTry: Int = 5): Triple<String, Int, Int> {
        lastReason = ""
        val (chapter, lects) = chapterInfo(cid)
        val review = chapter.str("test_type") == "review"
        log("  type=${chapter.str("test_type").ifEmpty { "?" }}, lectures=${lects.length()}")
        if (lectures(cid)) return Triple("locked", 0, 0)
        val (st, stBody) = startTest(cid)
        if (st == 403) {
            lastReason = "start test HTTP 403: ${snip(stBody)}"
            return Triple("locked", 0, 0)
        }
        val (qs, items) = call("GET", "/chapters/$cid/test")
        if (qs != 200 || items !is JSONArray || items.length() == 0) {
            lastReason = "questions HTTP $qs: ${snip(items)}"
            return Triple("locked", 0, 0)
        }
        val site = siteAnswers(chapter, items)
        return if (review) solveReview(cid, items, maxTry, site) else solveTest(cid, items, maxTry, site)
    }

    private fun pickAnswer(q: JSONObject, item: JSONObject, local: Map<Any, Any>, site: Map<Any, Any>): Any? {
        val qid = q.anyId("id")
        return (if (qid != null) local[qid] else null)
            ?: bank[qid.toString()]
            ?: (if (qid != null) site[qid] else null)
            ?: dummy(q, item)
    }

    private fun solveReview(cid: Long, items: JSONArray, maxTry: Int, site: Map<Any, Any>): Triple<String, Int, Int> {
        val local = HashMap<Any, Any>()
        var correct = 0
        for (attempt in 0 until maxTry) {
            check()
            startTest(cid)
            correct = 0
            var saved = 0
            for (i in 0 until items.length()) {
                val it = items.optJSONObject(i) ?: continue
                val q = it.optJSONObject("question") ?: continue
                val qid = q.anyId("id") ?: continue
                val qt = q.str("type")
                val v = pickAnswer(q, it, local, site) ?: continue
                val (_, res) = answer(qt, qid, v, true)
                val rv = reveal(qt, res)
                if (rv != null) {
                    local[qid] = rv
                    if (learn(qid, rv) && !correct(res)) saved++
                }
                if (correct(res)) correct++
            }
            if (saved > 0) log("  saved $saved correction(s) from the platform")
            if (correct >= items.length()) break
        }
        return Triple("ok", correct, items.length())
    }

    private fun solveTest(cid: Long, items: JSONArray, maxTry: Int, site: Map<Any, Any>): Triple<String, Int, Int> {
        var answers: Map<Any, Any> = emptyMap()
        var best = 0
        for (attempt in 0 until maxTry) {
            check()
            startTest(cid)
            var last: Any? = null
            for (i in 0 until items.length()) {
                val it = items.optJSONObject(i) ?: continue
                val q = it.optJSONObject("question") ?: continue
                val qid = q.anyId("id") ?: continue
                val v = pickAnswer(q, it, answers, site) ?: continue
                last = answer(q.str("type"), qid, v, false).second
            }
            completeScore(last)?.let { if (it > best) best = it }
            answers = resultAnswers(cid)
            var n = 0
            for ((k, v) in answers) if (learn(k, v)) n++
            if (n > 0) log("  saved $n correction(s) from the platform")
            if (best >= items.length() || answers.isEmpty()) break
        }
        return Triple("ok", best, items.length())
    }

    /** يحل المسار كاملًا. onProgress(done, total, label). */
    fun runCourse(
        studentId: String,
        password: String,
        onProgress: (Int, Int, String) -> Unit
    ): QureoSummary {
        log("Logging in...")
        if (!login(studentId, password)) throw QureoLoginException("فشل تسجيل الدخول — تحقق من الكود وكلمة المرور.")
        log("Logged in")
        resolveSection()
        val rows = chapters()
        if (rows.isEmpty()) throw IllegalStateException("لم يتم العثور على فصول للمسار.")
        log("$course: ${rows.size} chapters")
        var solved = 0
        var locked = 0
        onProgress(0, rows.size, "")
        for ((idx, r) in rows.withIndex()) {
            check()
            val n = idx + 1
            if (r.done && r.q > 0 && r.best >= r.q) {
                solved++
                log("Chapter $n/${rows.size}: ${r.name} — skipped (done)")
            } else {
                val (state, c, t) = solveChapter(r.id)
                if (state == "locked") {
                    locked++
                    log("Chapter $n/${rows.size}: ${r.name} — locked (${lastReason.ifEmpty { "unknown" }})")
                } else {
                    solved++
                    log("Chapter $n/${rows.size}: ${r.name} — $c/$t")
                }
                if (n < rows.size && pace > 0) pause(pace * 4, isStopped)
            }
            onProgress(n, rows.size, r.name)
        }
        return QureoSummary(course, rows.size, solved, locked)
    }
}
