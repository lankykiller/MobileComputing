package com.example.mobilecomputing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatGPT {


    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = ""
    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val defaultPrompt = "short message talking about the future of mobile technology"

    suspend fun getMessage(): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "you are beast in mobile developing")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", defaultPrompt)
                        })
                    })
                }

                val body =
                    requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseCode = response.code

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    return@withContext "Error: Code $responseCode\n$errorBody"
                }

                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody ?: "")
                val message = jsonObject.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                return@withContext message
            } catch (e: Exception) {
                return@withContext "Error: ${e.localizedMessage}"
            }
        }
    }
}