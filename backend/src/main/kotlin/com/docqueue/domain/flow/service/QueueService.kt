package com.docqueue.domain.flow.service

import com.docqueue.domain.flow.dto.QueueStatus
import com.docqueue.global.datas.queue.QueueName
import com.docqueue.global.datas.queue.Token
import com.docqueue.global.datas.queue.UserId
import kotlinx.coroutines.flow.Flow

interface QueueService {
    suspend fun registerUser(queue: QueueName, userId: UserId): Long
    suspend fun allowUsers(queue: QueueName, count: Long): Long
    suspend fun isAllowed(queue: QueueName, userId: UserId): Boolean
    suspend fun validateToken(queue: QueueName, userId: UserId, token: Token): Boolean
    suspend fun generateToken(queue: QueueName, userId: UserId): Token
    suspend fun getQueueStatus(queue: QueueName, userId: UserId): QueueStatus
    suspend fun registerOrGetStatus(queue: QueueName, userId: UserId): QueueStatus
    fun processAllQueues(maxCount: Long): Flow<Pair<QueueName, Long>>
}