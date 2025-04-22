package com.docqueue.domain.home.dto

data class UserQueue(
    val queue: String,
    val userId: Long,
    val token: String,
    val isAllowed: Boolean
)