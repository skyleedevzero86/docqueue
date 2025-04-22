package com.docqueue.domain.home.service

import com.docqueue.global.datas.queue.QueueName
import com.docqueue.global.datas.queue.Token
import com.docqueue.global.datas.queue.UserId
import com.docqueue.domain.home.dto.UserQueue
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import kotlinx.coroutines.flow.flow
import com.docqueue.global.infrastructure.QueueClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.docqueue.domain.flow.dto.AllowedUserResponse

@Service
class QueueServiceImpl(
    private val queueClient: QueueClient
) : QueueService {

    override fun checkAccess(queue: QueueName, userId: UserId, token: Token): Flow<UserQueue> = flow {
        val response = queueClient.verifyQueueAccess(queue, userId, token)
        emit(UserQueue(queue, userId, token, response.isAllowed))
    }

    override fun getAllowedUserResponse(queue: QueueName, userId: UserId, token: Token): Flow<AllowedUserResponse> = flow {
        val result = queueClient.verifyQueueAccess(queue, userId, token)
        emit(AllowedUserResponse(result.isAllowed))
    }

    override fun addUserToQueue(queue: QueueName, userId: UserId, token: Token) {
        CoroutineScope(Dispatchers.IO).launch {
            queueClient.addUser(queue, userId, token)
        }
    }

    override fun allowUser(queue: QueueName, userId: UserId) {
        CoroutineScope(Dispatchers.IO).launch {
            queueClient.allow(queue, userId)
        }
    }
}