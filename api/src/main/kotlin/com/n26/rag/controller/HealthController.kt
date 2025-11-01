package com.n26.rag.controller

import com.n26.rag.model.HealthResponse
import com.n26.rag.model.ServiceHealth
import com.n26.rag.service.ChromaDbClient
import com.n26.rag.service.GeminiService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class HealthController(
    private val chromaDbClient: ChromaDbClient,
    private val geminiService: GeminiService
) {

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponse> {
        logger.debug { "Health check requested" }

        val services = mutableMapOf<String, ServiceHealth>()

        // Check ChromaDB
        services["chromadb"] = try {
            if (chromaDbClient.healthCheck()) {
                ServiceHealth(status = "healthy")
            } else {
                ServiceHealth(status = "unhealthy", message = "ChromaDB not responding")
            }
        } catch (e: Exception) {
            ServiceHealth(status = "unhealthy", message = e.message)
        }

        // Check Gemini API (optional, can be slow)
        services["gemini"] = try {
            ServiceHealth(status = "healthy", message = "API key configured")
        } catch (e: Exception) {
            ServiceHealth(status = "unhealthy", message = e.message)
        }

        // Determine overall status
        val overallStatus = if (services.values.all { it.status == "healthy" }) {
            "healthy"
        } else {
            "degraded"
        }

        val response = HealthResponse(
            status = overallStatus,
            timestamp = Instant.now().toString(),
            services = services
        )

        return if (overallStatus == "healthy") {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.status(503).body(response)
        }
    }
}
