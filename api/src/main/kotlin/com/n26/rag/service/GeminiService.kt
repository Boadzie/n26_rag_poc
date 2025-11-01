package com.n26.rag.service

import com.google.gson.Gson
import com.n26.rag.config.RagConfig
import com.n26.rag.model.*
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@Service
class GeminiService(private val config: RagConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = System.getenv(config.llm.apiKeyEnv)
        ?: throw IllegalStateException("${config.llm.apiKeyEnv} environment variable not set")

    private val embeddingBaseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val generateBaseUrl = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Generate embedding for a query
     */
    fun generateEmbedding(text: String): List<Double> {
        val startTime = System.currentTimeMillis()

        try {
            val requestBody = GeminiEmbeddingRequest(
                model = config.embedding.model,
                content = Content(parts = listOf(Part(text = text))),
                taskType = "retrieval_query"
            )

            val url = "$embeddingBaseUrl/${config.embedding.model}:embedContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw RuntimeException("Gemini embedding failed: ${response.code} - $errorBody")
                }

                val responseBody = response.body?.string()
                    ?: throw RuntimeException("Empty response from Gemini")

                val result = gson.fromJson(responseBody, GeminiEmbeddingResponse::class.java)

                val latency = System.currentTimeMillis() - startTime
                logger.info { "Generated embedding in ${latency}ms" }

                return result.embedding.values
            }

        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logger.error(e) { "Embedding generation failed after ${latency}ms" }
            throw RuntimeException("Failed to generate embedding", e)
        }
    }

    /**
     * Generate answer using LLM
     */
    fun generateAnswer(question: String, context: String): String {
        val startTime = System.currentTimeMillis()

        try {
            val prompt = buildPrompt(question, context)

            val requestBody = GeminiGenerateRequest(
                contents = listOf(
                    ContentPart(parts = listOf(TextPart(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    temperature = config.llm.temperature,
                    topK = config.llm.topK,
                    topP = config.llm.topP,
                    maxOutputTokens = config.llm.maxTokens
                )
            )

            val url = "$generateBaseUrl/models/${config.llm.model}:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw RuntimeException("Gemini generation failed: ${response.code} - $errorBody")
                }

                val responseBody = response.body?.string()
                    ?: throw RuntimeException("Empty response from Gemini")

                val result = gson.fromJson(responseBody, GeminiGenerateResponse::class.java)

                if (result.candidates.isEmpty()) {
                    throw RuntimeException("No candidates returned from Gemini")
                }

                val answer = result.candidates[0].content.parts[0].text

                val latency = System.currentTimeMillis() - startTime
                logger.info { "Generated answer in ${latency}ms, length: ${answer.length} chars" }

                return answer
            }

        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logger.error(e) { "Answer generation failed after ${latency}ms" }
            throw RuntimeException("Failed to generate answer", e)
        }
    }

    /**
     * Build RAG prompt with context
     */
    private fun buildPrompt(question: String, context: String): String {
        return """
            You are a helpful assistant answering questions about N26's technical documentation.

            Use the following context to answer the question. If the context doesn't contain
            enough information to answer the question, say so and provide the best answer you can
            based on what's available.

            Context:
            $context

            Question: $question

            Answer:
        """.trimIndent()
    }

    /**
     * Health check for Gemini API
     */
    fun healthCheck(): Boolean {
        return try {
            // Simple check - try to generate a very short embedding
            generateEmbedding("test")
            true
        } catch (e: Exception) {
            logger.error(e) { "Gemini API health check failed" }
            false
        }
    }
}
