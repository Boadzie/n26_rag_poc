package com.n26.rag.controller

import com.n26.rag.model.ErrorResponse
import com.n26.rag.model.QueryRequest
import com.n26.rag.model.QueryResponse
import com.n26.rag.service.RagService
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1")
class RagController(private val ragService: RagService) {

    /**
     * Query endpoint - main RAG functionality
     */
    @PostMapping("/query")
    fun query(@Valid @RequestBody request: QueryRequest): ResponseEntity<QueryResponse> {
        logger.info { "Received query request: ${request.question}" }

        return try {
            val response = ragService.query(request.question)
            ResponseEntity.ok(response)

        } catch (e: Exception) {
            logger.error(e) { "Query processing failed" }
            throw e
        }
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ErrorResponse {
        val errors = ex.bindingResult.allErrors.joinToString(", ") { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            "$fieldName: ${error.defaultMessage}"
        }

        logger.warn { "Validation error: $errors" }

        return ErrorResponse(
            error = "Validation Error",
            message = errors,
            timestamp = Instant.now().toString(),
            path = "/api/v1/query"
        )
    }

    /**
     * General exception handler
     */
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneralExceptions(ex: Exception): ErrorResponse {
        logger.error(ex) { "Unexpected error occurred" }

        return ErrorResponse(
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            timestamp = Instant.now().toString()
        )
    }
}
