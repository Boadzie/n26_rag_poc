package com.n26.rag.service

import com.n26.rag.config.RagConfig
import com.n26.rag.model.QueryResponse
import com.n26.rag.model.ResponseMetadata
import com.n26.rag.model.SourceDocument
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class RagService(
    private val chromaDbClient: ChromaDbClient,
    private val geminiService: GeminiService,
    private val config: RagConfig
) {

    /**
     * Process a RAG query: retrieve context and generate answer
     */
    fun query(question: String): QueryResponse {
        val totalStartTime = System.currentTimeMillis()

        logger.info { "Processing RAG query: $question" }

        try {
            // Step 1: Generate embedding for the question
            val retrievalStartTime = System.currentTimeMillis()
            val queryEmbedding = geminiService.generateEmbedding(question)

            // Step 2: Retrieve relevant documents from vector store
            val documents = chromaDbClient.query(queryEmbedding, config.retrieval.topK)

            // Filter by similarity threshold
            val filteredDocs = documents.filter {
                it.score >= config.retrieval.similarityThreshold
            }

            val retrievalTime = System.currentTimeMillis() - retrievalStartTime

            if (filteredDocs.isEmpty()) {
                logger.warn { "No relevant documents found for query" }
                return createNoResultsResponse(question, totalStartTime)
            }

            logger.info { "Retrieved ${filteredDocs.size} relevant documents" }

            // Step 3: Build context from retrieved documents
            val context = buildContext(filteredDocs)

            // Step 4: Generate answer using LLM
            val llmStartTime = System.currentTimeMillis()
            val answer = geminiService.generateAnswer(question, context)
            val llmTime = System.currentTimeMillis() - llmStartTime

            val totalTime = System.currentTimeMillis() - totalStartTime

            logger.info {
                "Query processed successfully - " +
                        "retrievalTime: ${retrievalTime}ms, " +
                        "llmTime: ${llmTime}ms, " +
                        "totalTime: ${totalTime}ms"
            }

            return QueryResponse(
                question = question,
                answer = answer,
                sources = filteredDocs,
                metadata = ResponseMetadata(
                    retrievalTimeMs = retrievalTime,
                    llmTimeMs = llmTime,
                    totalTimeMs = totalTime,
                    documentsRetrieved = filteredDocs.size,
                    model = config.llm.model,
                    timestamp = Instant.now().toString()
                )
            )

        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - totalStartTime
            logger.error(e) { "Query processing failed after ${totalTime}ms" }
            throw RuntimeException("Failed to process query", e)
        }
    }

    /**
     * Build context string from retrieved documents
     */
    private fun buildContext(documents: List<SourceDocument>): String {
        return documents.joinToString("\n\n---\n\n") { doc ->
            "Source: ${doc.source}\n${doc.content}"
        }
    }

    /**
     * Create response when no results found
     */
    private fun createNoResultsResponse(question: String, startTime: Long): QueryResponse {
        val totalTime = System.currentTimeMillis() - startTime

        return QueryResponse(
            question = question,
            answer = "I couldn't find any relevant information in the documentation to answer your question. " +
                    "Please try rephrasing your question or contact support for more help.",
            sources = emptyList(),
            metadata = ResponseMetadata(
                retrievalTimeMs = 0,
                llmTimeMs = 0,
                totalTimeMs = totalTime,
                documentsRetrieved = 0,
                model = config.llm.model,
                timestamp = Instant.now().toString()
            )
        )
    }
}
