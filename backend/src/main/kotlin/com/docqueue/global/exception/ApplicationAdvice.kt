package com.docqueue.global.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

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
}