package com.docqueue.global.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.slf4j.LoggerFactory


@RestControllerAdvice
class ApplicationAdvice {

    private val logger = LoggerFactory.getLogger(ApplicationAdvice::class.java)

    @ExceptionHandler(ApplicationException::class)
    fun applicationExceptionHandler(ex: ApplicationException): Flow<ResponseEntity<ServerExceptionResponse>> {
        return flowOf(
            ResponseEntity
                .status(ex.httpStatus)
                .body(ServerExceptionResponse(ex.code, ex.reason))
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, exchange: ServerWebExchange): ResponseEntity<Map<String, String>> {
        logger.error("ResponseStatusException이 발생했습니다: ${ex.message}, Status: ${ex.statusCode}")
        return ResponseEntity.status(ex.statusCode).body(mapOf("error" to (ex.reason ?: "Unknown error")))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, exchange: ServerWebExchange): ResponseEntity<Map<String, String>> {
        logger.error("예상치 못한 오류 발생: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "서버 내부 오류: ${ex.message ?: "Unknown error"}"))
    }
}