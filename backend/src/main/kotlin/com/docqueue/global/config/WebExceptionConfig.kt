package com.docqueue.global.config

import com.docqueue.global.exception.QueueError
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

@Configuration
class WebExceptionConfig {

    @Bean
    fun errorHandler(): ErrorWebExceptionHandler {
        return ErrorWebExceptionHandler { exchange, ex ->
            when (ex) {
                is QueueError.UserAlreadyRegistered -> {
                    exchange.response.statusCode = HttpStatus.CONFLICT
                    exchange.response.writeWith(
                        Mono.just(exchange.response.bufferFactory().wrap(
                            """{"error": "already_registered", "message": "${ex.message}"}""".toByteArray()
                        ))
                    )
                }
                is QueueError.InvalidToken -> {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    exchange.response.writeWith(
                        Mono.just(exchange.response.bufferFactory().wrap(
                            """{"error": "invalid_token", "message": "${ex.message}"}""".toByteArray()
                        ))
                    )
                }
                else -> {
                    exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                    exchange.response.writeWith(
                        Mono.just(exchange.response.bufferFactory().wrap(
                            """{"error": "server_error", "message": "서버 내부 오류가 발생했습니다"}""".toByteArray()
                        ))
                    )
                }
            }
        }
    }
}