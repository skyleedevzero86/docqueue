package com.docqueue.domain.home.service

import com.docqueue.domain.home.dto.AllowedUserResponse
import com.docqueue.domain.queue.service.QueueService
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class HomeService(private val queueService: QueueService) {

    suspend fun getAllowedUserResponse(queue: String, userId: Long, token: String): Flow<AllowedUserResponse> {
        // 내부 서비스 직접 호출
        return queueService.getAllowedUserResponse(queue, userId, token)
    }
}