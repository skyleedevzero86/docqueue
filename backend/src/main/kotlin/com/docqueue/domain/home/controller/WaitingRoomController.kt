package com.docqueue.domain.home.controller

import com.docqueue.domain.flow.dto.QueueProgress
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import com.docqueue.domain.flow.service.UserQueueService
import com.docqueue.global.datas.queue.QueueName
import com.docqueue.global.datas.queue.UserId
import lombok.RequiredArgsConstructor
import kotlinx.coroutines.reactive.awaitSingle

@Controller
@RequiredArgsConstructor
class WaitingRoomController(
    private val userQueueService: UserQueueService
) {

    @GetMapping("/waiting-room")
    suspend fun getWaitingRoomPage(
        @RequestParam(name = "queue", defaultValue = "default") queue: QueueName,
        @RequestParam(name = "user-id") userId: UserId,
        @RequestParam(name = "redirect-url") redirectUrl: String,
        exchange: ServerWebExchange
    ): Rendering {
        val key = "user-queue-$queue-token"
        val cookieValue = exchange.request.cookies.getFirst(key)
        val token = cookieValue?.value ?: ""

        val isAllowed = userQueueService.isAllowedByToken(queue, userId, token).awaitSingle()

        return if (isAllowed) {
            Rendering.redirectTo(redirectUrl).build()
        } else {
            val queueStatus = userQueueService.registerWaitingQueueOrGetQueueStatus(queue, userId)
            val res = QueueProgress.from(queueStatus)
            Rendering.view("waiting-room")
                .modelAttribute("queue", queue)
                .modelAttribute("userId", userId)
                .modelAttribute("queueFront", res.queueFront)
                .modelAttribute("queueBack", res.queueBack)
                .modelAttribute("progress", res.progress)
                .build()
        }
    }
}