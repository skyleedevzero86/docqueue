package com.docqueue.global.config


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.beans.factory.annotation.Value

@Configuration
class WebClientConfig {

    @Value("\${queue.api.base-url}")
    private lateinit var baseUrl: String

    @Bean
    fun webClient(): WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()
}