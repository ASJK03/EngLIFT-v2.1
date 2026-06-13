/*
 * EngLIFT — EnhanceManager.kt
 *
 * Handles AI text enhancement via the Google Apps Script backend.
 * Enforces 20 free enhancements/day with a "Upgrade to Pro" paywall.
 *
 * Drop this file into:
 *   app/src/main/kotlin/dev/patrickgold/florisboard/ime/enhance/
 */

package dev.patrickgold.florisboard.ime.enhance

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Public data types
// ─────────────────────────────────────────────────────────────────────────────

sealed class EnhanceResult {
    data class Success(val enhancedText: String) : EnhanceResult()
    data class Error(val message: String)        : EnhanceResult()
    object LimitReached                          : EnhanceResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// EnhanceManager
// ─────────────────────────────────────────────────────────────────────────────

class EnhanceManager(context: Context) {

    companion object {
        private const val BACKEND_URL =
            "https://script.google.com/macros/s/AKfycbzzdd0b9O6_ZJ1QqEsuYm7qlIpf0OV7x4KC-RFiDB_qKes29TqL65TB6XzVvzAsB2V_jQ/exec"

        private const val PREFS_NAME        = "englift_enhance"
        private const val KEY_COUNT         = "daily_count"
        private const val KEY_DATE          = "count_date"
        private const val FREE_LIMIT        = 10
        private const val TIMEOUT_MS        = 15_000          // 15 s
        private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Quota helpers ─────────────────────────────────────────────────────────

    private fun todayString(): String = DATE_FMT.format(Date())

    /** How many free enhancements have been used today. */
    fun usedToday(): Int {
        val stored = prefs.getString(KEY_DATE, "") ?: ""
        return if (stored == todayString()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    /** Remaining free enhancements for today. */
    fun remainingToday(): Int = maxOf(0, FREE_LIMIT - usedToday())

    /** Whether the user has hit today's free limit. */
    fun isLimitReached(): Boolean = usedToday() >= FREE_LIMIT

    private fun incrementCount() {
        val today = todayString()
        val count = usedToday() + 1
        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_COUNT, count)
            .apply()
    }

    // ── Main enhance call ─────────────────────────────────────────────────────

    /**
     * Call the backend to enhance [text].
     *
     * Must be called from a coroutine scope (it is a suspend function).
     * The caller is responsible for showing UI feedback while this runs.
     *
     * @return [EnhanceResult.LimitReached] if the free limit is exhausted,
     *         [EnhanceResult.Success]      on a valid response,
     *         [EnhanceResult.Error]        on any network / parse failure.
     */
    suspend fun enhance(text: String): EnhanceResult = withContext(Dispatchers.IO) {
        if (isLimitReached()) return@withContext EnhanceResult.LimitReached

        if (text.isBlank()) {
            return@withContext EnhanceResult.Error("Nothing to enhance — type some text first.")
        }

        try {
            val payload = JSONObject().put("text", text).toString()

            val url  = URL(BACKEND_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod        = "POST"
                connectTimeout       = TIMEOUT_MS
                readTimeout          = TIMEOUT_MS
                doOutput             = true
                doInput              = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept",       "application/json")
            }

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val responseCode = conn.responseCode
            val body = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }
            conn.disconnect()

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext EnhanceResult.Error("Server error ($responseCode). Try again.")
            }

            val json    = JSONObject(body)
            val enhanced = json.optString("enhanced", "").trim()

            if (enhanced.isEmpty()) {
                return@withContext EnhanceResult.Error("Empty response from server.")
            }

            // Only count after a verified success
            incrementCount()
            EnhanceResult.Success(enhanced)

        } catch (e: java.net.SocketTimeoutException) {
            EnhanceResult.Error("Request timed out. Check your connection.")
        } catch (e: Exception) {
            EnhanceResult.Error("Enhancement failed: ${e.message}")
        }
    }
}
