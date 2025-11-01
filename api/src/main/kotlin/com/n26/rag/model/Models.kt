package com.n26.rag.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request model for RAG queries
 */
data class QueryRequest(
    @field:NotBlank(message = "Question cannot be blank")
    @field:Size(max = 500, message = "Question must not exceed 500 characters")
    val question: String
)

/**
 * Response model for RAG queries
 */
data class QueryResponse(
    val question: String,
    val answer: String,
    val sources: List<SourceDocument>,
    val metadata: ResponseMetadata
)

/**
 * Source document retrieved from vector store
 */
data class SourceDocument(
    val id: String,
    val content: String,
    val source: String,
    val score: Double,
    val metadata: Map<String, Any>?
)

/**
 * Response metadata for observability
 */
data class ResponseMetadata(
    val retrievalTimeMs: Long,
    val llmTimeMs: Long,
    val totalTimeMs: Long,
    val documentsRetrieved: Int,
    val model: String,
    val timestamp: String
)

/**
 * Health check response
 */
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val services: Map<String, ServiceHealth>
)

data class ServiceHealth(
    val status: String,
    val message: String? = null
)

/**
 * Error response model
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String,
    val path: String? = null
)

/**
 * ChromaDB query result
 */
data class ChromaQueryResult(
    val ids: List<List<String>>,
    val documents: List<List<String>>,
    val metadatas: List<List<Map<String, Any>>>,
    val distances: List<List<Double>>
)

/**
 * Gemini API request for embeddings
 */
data class GeminiEmbeddingRequest(
    val model: String,
    val content: Content,
    val taskType: String = "retrieval_query"
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

/**
 * Gemini API response for embeddings
 */
data class GeminiEmbeddingResponse(
    val embedding: Embedding
)

data class Embedding(
    val values: List<Double>
)

/**
 * Gemini API request for text generation
 */
data class GeminiGenerateRequest(
    val contents: List<ContentPart>,
    val generationConfig: GenerationConfig? = null
)

data class ContentPart(
    val parts: List<TextPart>
)

data class TextPart(
    val text: String
)

data class GenerationConfig(
    val temperature: Double?,
    val topK: Int?,
    val topP: Double?,
    val maxOutputTokens: Int?
)

/**
 * Gemini API response for text generation
 */
data class GeminiGenerateResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: GeneratedContent,
    val finishReason: String? = null
)

data class GeneratedContent(
    val parts: List<GeneratedPart>
)

data class GeneratedPart(
    val text: String
)
