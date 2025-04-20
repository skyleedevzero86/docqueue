package com.docqueue.domain.home.controller

import com.docqueue.domain.home.service.QueueName
import com.docqueue.domain.home.service.QueueService
import com.docqueue.domain.home.service.UserId
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.flow.first
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HomeController(
    private val queueService: QueueService
) {

    @GetMapping("/home")
    suspend fun getHome(
        @RequestParam(name = "queue", defaultValue = "default") queue: QueueName,
        @RequestParam(name = "user-id") userId: UserId,
        request: HttpServletRequest
    ): String {
        val token = extractToken(queue, request)
        val userQueue = queueService.checkAccess(queue, userId, token).first()

        return if (userQueue.isAllowed) {
            "home"
        } else {
            "redirect:/waiting-room?user-id=$userId&redirect-url=/home?user-id=$userId"
        }
    }

    private fun extractToken(queue: QueueName, request: HttpServletRequest): String {
        val cookieName = "user-queue-$queue-token"
        return request.cookies?.find { it.name.equals(cookieName, ignoreCase = true) }?.value ?: ""
    }
}