package dev.patrickgold.florisboard.ime.enhance

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class EnhanceService(private val context: Context) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "enhance_service_prefs",
        Context.MODE_PRIVATE
    )

    private val backendUrl = "https://script.google.com/macros/s/AKfycbzcvj6abkrgIezLbIhiyNb5h5qYI0q3W7NeilEoieOBN8uq9Ik4g7Hhxdninq4reT8/exec"
    private val dailyLimitKey = "daily_usage"
    private val lastDateKey = "last_date"
    private val dailyLimit = 20

    /**
     * Get today's date as a string (YYYY-MM-DD format)
     */
    private fun getTodayDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return dateFormat.format(Date())
    }

    /**
     * Check and update the daily usage counter
     * Returns true if usage is under the limit, false if limit reached
     */
    private fun checkDailyUsage(): Boolean {
        val lastDate = sharedPreferences.getString(lastDateKey, "")
        val today = getTodayDate()

        // If it's a new day, reset the counter
        if (lastDate != today) {
            sharedPreferences.edit().apply {
                putString(lastDateKey, today)
                putInt(dailyLimitKey, 0)
            }.apply()
        }

        // Get current usage
        val currentUsage = sharedPreferences.getInt(dailyLimitKey, 0)

        // Check if we're at the limit
        if (currentUsage >= dailyLimit) {
            return false
        }

        // Increment the usage counter
        sharedPreferences.edit().putInt(dailyLimitKey, currentUsage + 1).apply()
        return true
    }

    /**
     * Main function that enhances text using the backend API
     * @param text The text to enhance
     * @param tone The tone for enhancement (default: "Professional")
     * @return Result with enhanced text or error message
     */
    suspend fun enhanceText(text: String, tone: String = "Professional"): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if text is empty
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Text is empty"))
            }

            // Check daily usage limit
            if (!checkDailyUsage()) {
                return@withContext Result.failure(Exception("Daily limit reached. Subscribe for unlimited."))
            }

            // Create the JSON request body
            val requestJson = """
                {"text": "$text", "tone": "$tone"}
            """.trimIndent()

            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            // Create the HTTP request
            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            // Send the request
            val response = httpClient.newCall(request).execute()

            // Check if request was successful
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API request failed: ${response.code}"))
            }

            // Parse the response
            val responseBody = response.body?.string() ?: ""
            if (responseBody.isEmpty()) {
                return@withContext Result.failure(Exception("Empty response from server"))
            }

            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            
            // Extract the enhanced text from response
            val enhancedText = jsonResponse["enhanced"]?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("Invalid API response: 'enhanced' field not found"))

            // Return the enhanced text
            Result.success(enhancedText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
