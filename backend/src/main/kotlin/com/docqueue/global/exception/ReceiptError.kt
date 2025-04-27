package com.docqueue.global.exception


sealed class ReceiptError : Exception() {
    data class PdfGenerationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ReceiptError()

    data class StorageError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ReceiptError()

    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ReceiptError()
}