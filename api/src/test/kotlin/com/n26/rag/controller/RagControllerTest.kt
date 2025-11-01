package com.n26.rag.controller

import com.n26.rag.model.QueryResponse
import com.n26.rag.model.ResponseMetadata
import com.n26.rag.service.RagService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(RagController::class)
class RagControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun ragService(): RagService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var ragService: RagService

    @Test
    fun `should return query response for valid request`() {
        // Given
        val question = "What is the backend architecture?"
        val mockResponse = QueryResponse(
            question = question,
            answer = "The backend consists of microservices written in Kotlin and Java.",
            sources = emptyList(),
            metadata = ResponseMetadata(
                retrievalTimeMs = 100,
                llmTimeMs = 500,
                totalTimeMs = 600,
                documentsRetrieved = 3,
                model = "gemini-2.0-flash-exp",
                timestamp = Instant.now().toString()
            )
        )

        every { ragService.query(question) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question": "$question"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.question").value(question))
            .andExpect(jsonPath("$.answer").exists())
            .andExpect(jsonPath("$.metadata.totalTimeMs").exists())
    }

    @Test
    fun `should return 400 for blank question`() {
        mockMvc.perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `should return 400 for question exceeding max length`() {
        val longQuestion = "a".repeat(501)

        mockMvc.perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question": "$longQuestion"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `should return 500 when service throws exception`() {
        val question = "What is N26?"

        every { ragService.query(question) } throws RuntimeException("Service unavailable")

        mockMvc.perform(
            post("/api/v1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question": "$question"}""")
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
    }
}
