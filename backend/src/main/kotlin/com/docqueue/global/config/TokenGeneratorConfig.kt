package com.docqueue.global.config

import com.docqueue.domain.flow.service.TokenGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class TokenGeneratorConfig {
    @Bean
    fun tokenGenerator(): TokenGenerator {
        return TokenGenerator { UUID.randomUUID().toString() }
    }
}