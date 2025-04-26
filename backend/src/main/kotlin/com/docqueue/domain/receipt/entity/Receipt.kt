package com.docqueue.domain.receipt.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class Receipt(
    val address: String,
    val phoneNumber: String,
    val date: LocalDateTime = LocalDateTime.now(),
    val items: List<ReceiptItem>,
    val totalAmount: BigDecimal,
    val cashAmount: BigDecimal,
    val changeAmount: BigDecimal
)