package com.docqueue.domain.flow.dto

data class AllowUserResponse(
    val requestCount: Long,
    val allowedCount: Long
)