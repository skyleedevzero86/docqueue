package com.docqueue.domain.receipt.entity

import java.math.BigDecimal

data class ReceiptItem(
    val name: String,
    val price: BigDecimal
)