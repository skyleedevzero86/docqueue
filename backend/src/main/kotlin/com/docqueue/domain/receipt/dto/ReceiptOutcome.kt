package com.docqueue.domain.receipt.dto

sealed interface ReceiptOutcome {
    data class Success(val output: ReceiptOutput) : ReceiptOutcome
    data class Failure(val error: Throwable) : ReceiptOutcome
}