package com.divyang.studymateai.gemini


import com.divyang.studymateai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel


object GeminiClient {

    private const val MODEL_NAME = "gemini-3.5-flash"


    val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }


    suspend fun generateContent(prompt: String): String {
        val response = model.generateContent(prompt)
        return response.text ?: ""
    }


    fun cleanJson(raw: String): String =
        raw.replace("```json", "")
            .replace("```", "")
            .trim()
}