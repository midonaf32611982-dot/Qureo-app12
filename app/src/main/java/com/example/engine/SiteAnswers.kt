package com.example.engine

import android.content.Context
import org.json.JSONObject

data class SiteSection(
    val q: List<String> = emptyList(),
    val a: List<String> = emptyList()
)

class SiteAnswers(private val context: Context? = null) {
    fun getJson(fileName: String): JSONObject {
        val ctx = context ?: throw IllegalStateException("Context required to read assets")
        val jsonString = ctx.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(jsonString)
    }

    companion object {
        val PAGES: Map<String, Map<String, List<Int>>> = mapOf(
            "javascript_ar" to emptyMap(),
            "python_short_ar" to emptyMap()
        )

        fun norm(text: String): String {
            if (text.isEmpty()) return ""
            val sb = java.lang.StringBuilder()
            for (c in text) {
                if (Character.getType(c) == Character.NON_SPACING_MARK.toInt()) continue
                sb.append(
                    when (c) {
                        '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627'
                        '\u0649' -> '\u064a'
                        '\u0629' -> '\u0647'
                        else -> c
                    }
                )
            }
            return sb.toString().trim().replace(Regex("\\s+"), " ")
        }

        fun ratio(s1: String, s2: String): Double {
            if (s1 == s2) return 1.0
            if (s1.isEmpty() || s2.isEmpty()) return 0.0
            val words1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
            val words2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
            if (words1.isEmpty() || words2.isEmpty()) return 0.0
            val intersection = words1.intersect(words2).size
            val union = words1.union(words2).size
            return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
        }

        fun load(course: String, rel: String, n: Int): List<SiteSection>? {
            return null
        }

        fun answerCode(a: List<String>): String? {
            if (a.isEmpty()) return null
            return a.joinToString("\n").trim().ifEmpty { null }
        }
    }
}
