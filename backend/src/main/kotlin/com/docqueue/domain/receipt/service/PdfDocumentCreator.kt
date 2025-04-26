package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt

fun interface PdfDocumentCreator {
    suspend fun createPdfDocument(receipt: Receipt): ByteArray
}