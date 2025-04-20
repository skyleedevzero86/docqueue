package com.docqueue.domain.queue.controller

import com.docqueue.domain.home.dto.AllowedUserResponse
import com.docqueue.domain.home.service.QueueService
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/queue")
class QueueController(private val queueService: QueueService) {

    @GetMapping("/allowed")
    fun isUserAllowed(
        @RequestParam("queue") queue: String,
        @RequestParam("user-id") userId: Long,
        @RequestParam("token") token: String
    ): Flow<AllowedUserResponse> {
        return queueService.getAllowedUserResponse(queue, userId, token)
    }

    @PostMapping("/add")
    fun addUserToQueue(
        @RequestParam("queue") queue: String,
        @RequestParam("user-id") userId: Long,
        @RequestParam("token") token: String
    ) {
        queueService.addUserToQueue(queue, userId, token)
    }

    @PostMapping("/allow")
    fun allowUser(
        @RequestParam("queue") queue: String,
        @RequestParam("user-id") userId: Long
    ) {
        queueService.allowUser(queue, userId)
    }
}