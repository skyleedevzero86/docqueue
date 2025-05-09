package com.docqueue.domain.receipt.dto

import com.docqueue.domain.receipt.entity.ReceiptPrintLog

data class ReceiptOutput(
    val content: ByteArray,
    val metadata: ReceiptMetadata,
    val printLog: ReceiptPrintLog
)