package com.docqueue.domain.receipt.service

import java.nio.file.Path

fun interface PdfSaver {
    suspend fun savePdf(content: ByteArray, filename: String): Path
}
