package com.docqueue.domain.home.service

fun interface QueueService {
    fun checkAccess(queue: QueueName, userId: UserId, token: Token): Flow<UserQueue>
}