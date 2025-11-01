package com.n26.rag.service

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.n26.rag.config.RagConfig
import com.n26.rag.model.ChromaQueryResult
import com.n26.rag.model.SourceDocument
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

data class ChromaCollection(
    val id: String,
    val name: String,
    val dimension: Int?
)

@Service
class ChromaDbClient(private val config: RagConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "http://${config.vectorDb.host}:${config.vectorDb.port}"
    private val collectionName = config.vectorDb.collectionName

    /**
     * Get collection ID by name
     */
    private fun getCollectionId(): String {
        val request = Request.Builder()
            .url("$baseUrl/api/v2/tenants/default_tenant/databases/default_database/collections")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to fetch collections: ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw RuntimeException("Empty response from ChromaDB")

            // Parse the array of collections and find our collection by name
            val collections = gson.fromJson(responseBody, Array<ChromaCollection>::class.java)
            val collection = collections.find { it.name == collectionName }
                ?: throw RuntimeException("Collection '$collectionName' not found")

            return collection.id
        }
    }

    /**
     * Query the vector database with an embedding
     */
    fun query(embedding: List<Double>, topK: Int): List<SourceDocument> {
        val startTime = System.currentTimeMillis()

        try {
            // Get collection ID first
            val collectionId = getCollectionId()

            val requestBody = mapOf(
                "query_embeddings" to listOf(embedding),
                "n_results" to topK,
                "include" to listOf("documents", "metadatas", "distances")
            )

            val request = Request.Builder()
                .url("$baseUrl/api/v2/tenants/default_tenant/databases/default_database/collections/$collectionId/query")
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("ChromaDB query failed: ${response.code} - ${response.message}")
                }

                val responseBody = response.body?.string()
                    ?: throw RuntimeException("Empty response from ChromaDB")

                val result = gson.fromJson(responseBody, ChromaQueryResult::class.java)

                val documents = result.ids[0].mapIndexed { index, id ->
                    SourceDocument(
                        id = id,
                        content = result.documents[0][index],
                        source = result.metadatas[0][index]["source_file"] as? String ?: "unknown",
                        score = 1.0 - result.distances[0][index], // Convert distance to similarity
                        metadata = result.metadatas[0][index]
                    )
                }

                val latency = System.currentTimeMillis() - startTime
                logger.info { "ChromaDB query completed in ${latency}ms, retrieved ${documents.size} documents" }

                return documents
            }

        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            logger.error(e) { "ChromaDB query failed after ${latency}ms" }
            throw RuntimeException("Failed to query vector database", e)
        }
    }

    /**
     * Health check for ChromaDB
     */
    fun healthCheck(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v2/pre-flight-checks")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            logger.error(e) { "ChromaDB health check failed" }
            false
        }
    }
}
