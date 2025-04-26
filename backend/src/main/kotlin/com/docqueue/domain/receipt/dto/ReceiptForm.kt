package com.docqueue.domain.receipt.dto

import java.math.BigDecimal

data class ReceiptForm(
    val address: String,
    val phoneNumber: String,
    val itemName: List<String>,
    val itemPrice: List<String>,
    val totalAmount: BigDecimal,
    val cashAmount: BigDecimal,
    val changeAmount: BigDecimal
)