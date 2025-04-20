package com.docqueue.domain.queue.service

import com.docqueue.domain.home.dto.AllowedUserResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.springframework.stereotype.Service

@Service
class QueueService {
    // 여기서는 간단한 메모리 기반 구현
    private val allowedUsers = mutableMapOf<String, MutableSet<Long>>()
    private val userTokens = mutableMapOf<Pair<String, Long>, String>()

    fun addUserToQueue(queue: String, userId: Long, token: String) {
        userTokens[queue to userId] = token
    }

    fun allowUser(queue: String, userId: Long) {
        if (!allowedUsers.containsKey(queue)) {
            allowedUsers[queue] = mutableSetOf()
        }
        allowedUsers[queue]?.add(userId)
    }

    fun isUserAllowed(queue: String, userId: Long, token: String): Boolean {
        val storedToken = userTokens[queue to userId]
        val isTokenValid = storedToken == token

        return isTokenValid && (allowedUsers[queue]?.contains(userId) == true)
    }

    fun getAllowedUserResponse(queue: String, userId: Long, token: String): Flow<AllowedUserResponse> {
        val isAllowed = isUserAllowed(queue, userId, token)
        return flowOf(AllowedUserResponse(isAllowed = isAllowed))
    }
}