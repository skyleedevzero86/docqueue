package com.docqueue.domain.receipt.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("receipt_print_logs")
data class ReceiptPrintLog(
    @Id
    val id: String? = null,
    val receiptId: String,
    val fileName: String,
    val filePath: String,
    val printedAt: LocalDateTime = LocalDateTime.now(),
    val userId: String? = null
)