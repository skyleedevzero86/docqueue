package com.docqueue.global.exception

import org.springframework.http.HttpStatus

data class ApplicationException(
    val httpStatus: HttpStatus,
    val code: String,
    val reason: String
) : RuntimeException(reason)