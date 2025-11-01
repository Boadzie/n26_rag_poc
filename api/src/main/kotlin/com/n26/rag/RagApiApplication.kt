package com.n26.rag

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RagApiApplication

fun main(args: Array<String>) {
    runApplication<RagApiApplication>(*args)
}
