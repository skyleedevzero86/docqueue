package com.docqueue.global.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange

@RestControllerAdvice
class ApplicationAdvice {

    @ExceptionHandler(ApplicationException::class)
    fun applicationExceptionHandler(ex: ApplicationException): Flow<ResponseEntity<ServerExceptionResponse>> {
        return flowOf(
            ResponseEntity
                .status(ex.httpStatus)
                .body(ServerExceptionResponse(ex.code, ex.reason))
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, exchange: ServerWebExchange): Flow<ResponseEntity<ServerExceptionResponse>> {
        println("ResponseStatusException이 발생했습니다: ${ex.message}, Status: ${ex.statusCode}")
        return flowOf(
            ResponseEntity
                .status(ex.statusCode)
                .body(ServerExceptionResponse("WEBFLUX-${ex.statusCode.value()}", ex.reason ?: "WebFlux 오류 발생"))
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception, exchange: ServerWebExchange): Flow<ResponseEntity<ServerExceptionResponse>> {
        println("예기치 않은 오류가 발생했습니다: ${ex.message}, Stacktrace: ${ex.stackTraceToString()}")
        return flowOf(
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ServerExceptionResponse("GEN-0001", "서버 내부 오류가 발생했습니다: ${ex.message}"))
        )
    }
}