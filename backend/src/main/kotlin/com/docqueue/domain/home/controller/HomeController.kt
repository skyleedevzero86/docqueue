package com.docqueue.domain.home.controller

import com.docqueue.domain.home.service.QueueService
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.flow.first
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.util.UriComponentsBuilder

@Controller
class HomeController(
    private val queueService: QueueService
) {

    @GetMapping("/home")
    suspend fun getHome(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user-id") userId: Long,
        request: HttpServletRequest
    ): String {
        val token = extractToken(queue, request)
        val userQueue = queueService.checkAccess(queue, userId, token).first()

        return if (userQueue.isAllowed) {
            "home"
        } else {
            val redirectUrl = UriComponentsBuilder
                .fromPath("/home")
                .queryParam("user-id", userId)
                .build()
                .toUriString()
            "redirect:/waiting-room?user-id=$userId&redirect-url=$redirectUrl"
        }
    }

    private fun extractToken(queue: String, request: HttpServletRequest): String {
        val cookieName = "user-queue-$queue-token"
        return request.cookies?.find { it.name.equals(cookieName, ignoreCase = true) }?.value ?: ""
    }
}