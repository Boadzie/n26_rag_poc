package com.n26.rag.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream

data class RagConfig(
    val vectorDb: VectorDbConfig,
    val embedding: EmbeddingConfig,
    val llm: LlmConfig,
    val retrieval: RetrievalConfig,
    val api: ApiConfig,
    val logging: LoggingConfig
)

data class VectorDbConfig(
    val type: String,
    val host: String,
    val port: Int,
    val collectionName: String,
    val persistDirectory: String?
)

data class EmbeddingConfig(
    val provider: String,
    val model: String,
    val apiKeyEnv: String,
    val batchSize: Int,
    val dimensions: Int
)

data class LlmConfig(
    val provider: String,
    val model: String,
    val apiKeyEnv: String,
    val temperature: Double,
    val maxTokens: Int,
    val topP: Double,
    val topK: Int
)

data class RetrievalConfig(
    val topK: Int,
    val similarityThreshold: Double,
    val searchType: String
)

data class ApiConfig(
    val port: Int,
    val host: String,
    val maxQueryLength: Int,
    val timeoutSeconds: Int
)

data class LoggingConfig(
    val level: String,
    val format: String,
    val output: String
)

@Configuration
class ConfigLoader {

    fun loadConfig(configPath: String): RagConfig {
        val yaml = Yaml()
        val config: Map<String, Any> = FileInputStream(configPath).use { inputStream ->
            yaml.load(inputStream)
        }

        return RagConfig(
            vectorDb = parseVectorDbConfig(config["vector_db"] as Map<String, Any>),
            embedding = parseEmbeddingConfig(config["embedding"] as Map<String, Any>),
            llm = parseLlmConfig(config["llm"] as Map<String, Any>),
            retrieval = parseRetrievalConfig(config["retrieval"] as Map<String, Any>),
            api = parseApiConfig(config["api"] as Map<String, Any>),
            logging = parseLoggingConfig(config["logging"] as Map<String, Any>)
        )
    }

    private fun parseVectorDbConfig(map: Map<String, Any>) = VectorDbConfig(
        type = map["type"] as String,
        host = map["host"] as String,
        port = map["port"] as Int,
        collectionName = map["collection_name"] as String,
        persistDirectory = map["persist_directory"] as String?
    )

    private fun parseEmbeddingConfig(map: Map<String, Any>) = EmbeddingConfig(
        provider = map["provider"] as String,
        model = map["model"] as String,
        apiKeyEnv = map["api_key_env"] as String,
        batchSize = map["batch_size"] as Int,
        dimensions = map["dimensions"] as Int
    )

    private fun parseLlmConfig(map: Map<String, Any>) = LlmConfig(
        provider = map["provider"] as String,
        model = map["model"] as String,
        apiKeyEnv = map["api_key_env"] as String,
        temperature = (map["temperature"] as Number).toDouble(),
        maxTokens = map["max_tokens"] as Int,
        topP = (map["top_p"] as Number).toDouble(),
        topK = map["top_k"] as Int
    )

    private fun parseRetrievalConfig(map: Map<String, Any>) = RetrievalConfig(
        topK = map["top_k"] as Int,
        similarityThreshold = (map["similarity_threshold"] as Number).toDouble(),
        searchType = map["search_type"] as String
    )

    private fun parseApiConfig(map: Map<String, Any>) = ApiConfig(
        port = map["port"] as Int,
        host = map["host"] as String,
        maxQueryLength = map["max_query_length"] as Int,
        timeoutSeconds = map["timeout_seconds"] as Int
    )

    private fun parseLoggingConfig(map: Map<String, Any>) = LoggingConfig(
        level = map["level"] as String,
        format = map["format"] as String,
        output = map["output"] as String
    )
}
