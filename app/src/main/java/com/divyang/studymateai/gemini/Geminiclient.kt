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

    // Upper bound on user/OCR content embedded in a prompt. Caps Gemini cost and
    // blast radius; scanned chapters longer than this are truncated.
    private const val MAX_PROMPT_CHARS = 12_000

    /**
     * Sanitize untrusted content (OCR output, typed chapter text) before it is
     * interpolated into a Gemini prompt. Neutralizes markdown code-fence markers
     * used to break out of the delimited block, and enforces a length cap so an
     * oversized document cannot inflate cost or be used for prompt-injection.
     */
    fun sanitizeForPrompt(raw: String): String =
        raw.replace("```", "'''")
            .trim()
            .take(MAX_PROMPT_CHARS)
}