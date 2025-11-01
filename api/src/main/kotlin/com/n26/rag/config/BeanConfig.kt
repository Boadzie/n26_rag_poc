package com.n26.rag.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfig {

    @Value("\${app.config-path:/app/config.yaml}")
    private lateinit var configPath: String

    @Bean
    fun ragConfig(configLoader: ConfigLoader): RagConfig {
        return configLoader.loadConfig(configPath)
    }

    @Bean
    fun configLoader(): ConfigLoader {
        return ConfigLoader()
    }
}
