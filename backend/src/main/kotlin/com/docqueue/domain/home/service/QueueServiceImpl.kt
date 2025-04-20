package com.docqueue.domain.home.service

import com.docqueue.domain.home.entity.UserQueue
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import kotlinx.coroutines.flow.flow
import com.docqueue.global.infrastructure.QueueClient

@Service
class QueueServiceImpl(
    private val queueClient: QueueClient
    //,
   // private val userQueueService: UserQueueService
) : QueueService {

    override fun checkAccess(queue: QueueName, userId: UserId, token: Token): Flow<UserQueue> = flow {
        val response = queueClient.verifyQueueAccess(queue, userId, token)
        emit(UserQueue(queue, userId, token, response.isAllowed))
    }
}