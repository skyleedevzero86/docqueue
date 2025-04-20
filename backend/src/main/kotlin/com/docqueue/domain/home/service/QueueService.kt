package com.docqueue.domain.home.service

import com.docqueue.domain.home.entity.UserQueue
import kotlinx.coroutines.flow.Flow
import com.docqueue.domain.home.dto.AllowedUserResponse

interface QueueService {
    fun checkAccess(queue: QueueName, userId: UserId, token: Token): Flow<UserQueue>
    fun getAllowedUserResponse(queue: QueueName, userId: UserId, token: Token): Flow<AllowedUserResponse>
    fun addUserToQueue(queue: QueueName, userId: UserId, token: Token)
    fun allowUser(queue: QueueName, userId: UserId)
}