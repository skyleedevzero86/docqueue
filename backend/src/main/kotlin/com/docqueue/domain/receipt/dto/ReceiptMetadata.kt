package com.docqueue.domain.receipt.dto

import java.time.Instant


data class ReceiptMetadata(val fileName: String, val createdAt: Instant, val size: Long)