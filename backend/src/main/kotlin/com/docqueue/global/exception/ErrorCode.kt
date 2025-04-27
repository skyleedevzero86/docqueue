package com.docqueue.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: String,
    val reason: String
) {
    QUEUE_ALREADY_REGISTERED_USER(
        HttpStatus.CONFLICT,
        "UQ-0001",
        "이미 대기열에 등록된 사용자입니다"
    ),
    RECEIPT_PDF_GENERATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "RECEIPT-0001",
        "PDF 생성에 실패했습니다"
    );

    fun build(): ApplicationException = ApplicationException(httpStatus, code, reason)
}