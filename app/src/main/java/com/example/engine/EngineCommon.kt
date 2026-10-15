package com.example.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class StoppedException(message: String = "Stopped by user") : RuntimeException(message)

class GeoBlockedException(message: String = "Geo-blocked: Connection from outside Egypt is blocked") : RuntimeException(message)

interface BankStore {
    fun load(key: String): String?
    fun save(key: String, value: String)
}

interface AssetSource {
    fun read(fileName: String): String?
}

class AppAssetSource(private val context: Context) : AssetSource {
    override fun read(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}

class SharedPreferencesBankStore(context: Context) : BankStore {
    private val prefs = context.getSharedPreferences("solver_banks", Context.MODE_PRIVATE)

    override fun load(key: String): String? {
        return prefs.getString(key, null)
    }

    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

object Speed {
    fun delayMs(speed: Int): Long {
        return when (speed) {
            1 -> 1500L
            2 -> 800L
            3 -> 400L
            4 -> 150L
            5 -> 0L
            else -> maxOf(0L, (2000L - speed * 300L))
        }
    }
}

fun pause(ms: Long, isStopped: () -> Boolean) {
    if (ms <= 0) {
        if (isStopped()) throw StoppedException()
        return
    }
    val step = 100L
    var elapsed = 0L
    while (elapsed < ms) {
        if (isStopped()) throw StoppedException()
        val waitMs = minOf(step, ms - elapsed)
        Thread.sleep(waitMs)
        elapsed += waitMs
    }
    if (isStopped()) throw StoppedException()
}

fun parseJson(text: String): Any? {
    val t = text.trim()
    return try {
        if (t.startsWith("{")) JSONObject(t)
        else if (t.startsWith("[")) JSONArray(t)
        else null
    } catch (_: Exception) {
        null
    }
}

fun normVal(v: Any?): Any? {
    if (v == null || v == JSONObject.NULL) return null
    if (v is Long) return v
    if (v is Int) return v.toLong()
    if (v is Number) {
        val l = v.toLong()
        if (v.toDouble() == l.toDouble()) return l
        return v.toDouble()
    }
    if (v is String) {
        val l = v.toLongOrNull()
        if (l != null) return l
        val d = v.toDoubleOrNull()
        if (d != null) return d
        return v
    }
    return v
}

fun JSONObject.str(key: String): String {
    if (!has(key) || isNull(key)) return ""
    return optString(key, "")
}

fun JSONObject.anyId(key: String = "id"): Any? {
    if (!has(key) || isNull(key)) return null
    return normVal(opt(key))
}

fun JSONObject.truthy(key: String): Boolean {
    if (!has(key) || isNull(key)) return false
    val v = opt(key) ?: return false
    if (v is Boolean) return v
    if (v is Number) return v.toLong() != 0L
    if (v is String) {
        val s = v.trim().lowercase()
        return s.isNotEmpty() && s != "false" && s != "0" && s != "null"
    }
    return true
}

class SessionResponse(
    val status: Int,
    val text: String,
    val json: Any?,
    val geoBlocked: Boolean
) {
    fun obj(): JSONObject? = json as? JSONObject
}

class Session {
    val defaultHeaders = HashMap<String, String>()
    private val cookies = HashMap<String, String>()

    fun setCookie(name: String, value: String) {
        cookies[name] = value
    }

    fun hasCookie(name: String): Boolean {
        return cookies.containsKey(name) && !cookies[name].isNullOrEmpty()
    }

    fun request(
        method: String,
        urlString: String,
        headers: Map<String, String>? = null,
        body: String? = null
    ): SessionResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.instanceFollowRedirects = false

        for ((k, v) in defaultHeaders) {
            conn.setRequestProperty(k, v)
        }
        if (headers != null) {
            for ((k, v) in headers) {
                conn.setRequestProperty(k, v)
            }
        }

        if (cookies.isNotEmpty()) {
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            conn.setRequestProperty("Cookie", cookieHeader)
        }

        if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
            conn.doOutput = true
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
                os.flush()
            }
        }

        val status = conn.responseCode

        val headerFields = conn.headerFields
        val setCookies = headerFields["Set-Cookie"] ?: headerFields["set-cookie"]
        setCookies?.forEach { cookieStr ->
            val cookiePart = cookieStr.split(";").firstOrNull()?.trim()
            if (!cookiePart.isNullOrEmpty()) {
                val eqIdx = cookiePart.indexOf('=')
                if (eqIdx > 0) {
                    val cName = cookiePart.substring(0, eqIdx).trim()
                    val cVal = cookiePart.substring(eqIdx + 1).trim()
                    cookies[cName] = cVal
                }
            }
        }

        val stream = if (status in 200..399) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        conn.disconnect()

        val parsed = parseJson(text)
        val isGeo = status == 403 && (text.contains("Egypt") || text.contains("geo") || text.contains("blocked") || text.contains("حجب"))

        return SessionResponse(status, text, parsed, isGeo)
    }
}
