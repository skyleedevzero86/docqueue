package com.docqueue.domain.home.entity

data class UserQueue(
    val queue: String,
    val userId: Long,
    val token: String,
    val isAllowed: Boolean
)