package com.docqueue.domain.home.service

import com.docqueue.domain.home.entity.UserQueue
import kotlinx.coroutines.flow.Flow

fun interface QueueService {
    fun checkAccess(queue: QueueName, userId: UserId, token: Token): Flow<UserQueue>
}