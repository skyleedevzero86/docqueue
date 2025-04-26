package com.docqueue.domain.receipt.dto

import java.math.BigDecimal

data class ReceiptForm(
    var address: String = "",
    var phoneNumber: String = "",
    var itemName: MutableList<String> = mutableListOf(),
    var itemPrice: MutableList<String> = mutableListOf(),
    var totalAmount: BigDecimal = BigDecimal.ZERO,
    var cashAmount: BigDecimal = BigDecimal.ZERO,
    var changeAmount: BigDecimal = BigDecimal.ZERO
)