package dev.patrickgold.florisboard.ime.enhance

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiEnhanceService(private val context: Context) {
    
    // Step 1: Create secure storage for API key (like a safe)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gemini_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Step 2: Create internet connection tool (like a phone)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Step 3: Tool to read/write JSON data (like reading a message)
    private val json = Json { ignoreUnknownKeys = true }

    // Save API key securely (keep it in the safe)
    fun saveApiKey(apiKey: String) {
        encryptedSharedPreferences.edit().putString("gemini_api_key", apiKey).apply()
    }

    // Get API key from secure storage (take it out of the safe)
    fun getApiKey(): String? {
        return encryptedSharedPreferences.getString("gemini_api_key", null)
    }

    // The main function that enhances text (the brain of everything)
    suspend fun enhanceText(text: String): Result<String> = withContext(Dispatchers.IO) {
        // Step 1: Get the API key from the safe
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("API key not set"))
        }

        // Step 2: Check if text is empty
        if (text.isBlank()) {
            return@withContext Result.failure(Exception("Text is empty"))
        }

        try {
            // Step 3: Create the instruction for AI
            val prompt = """
                Enhance the following text to professional English. 
                Only return the enhanced text, no explanations.
                Text: "$text"
            """.trimIndent()

            // Step 4: Create the request message (like writing a letter)
            val requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$prompt"
                        }]
                    }]
                }
            """.trimIndent().toRequestBody()

            // Step 5: Create the internet request (like preparing to send the letter)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            // Step 6: Send request to Google AI (send the letter)
            val response = httpClient.newCall(request).execute()
            
            // Step 7: Check if request was successful (did the letter arrive?)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API request failed: ${response.code}"))
            }

            // Step 8: Read the response (read the reply)
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            
            // Step 9: Extract the enhanced text from response (take out the important part)
            val enhancedText = jsonResponse["candidates"]
                ?.jsonObject?.get("0")
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonObject?.get("0")
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("Invalid API response"))

            // Step 10: Return the enhanced text (give back the improved text)
            Result.success(enhancedText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
