package com.docqueue.domain.flow.controller

import com.docqueue.domain.flow.dto.AllowedUserResponse
import com.docqueue.domain.flow.dto.QueueUpdateEvent
import com.docqueue.domain.home.service.QueueService
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.*
import com.docqueue.domain.flow.service.UserQueueService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/v1/queue")
class QueueEventController(
    private val userQueueService: UserQueueService,
    private val queueService: QueueService
) {
    /**
     * Server-Sent Events를 통한 대기열 상태 업데이트 스트리밍
     */
    @GetMapping(path = ["/events"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamQueueEvents(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user-id") userId: Long
    ): Flux<QueueUpdateEvent> {
        return Flux.interval(Duration.ofSeconds(1))
            .flatMap { userQueueService.getQueueStatus(queue, userId) }
            .map { status ->
                QueueUpdateEvent(
                    status.userRank,
                    status.totalQueueSize,
                    status.progress
                )
            }
            .distinctUntilChanged()
    }

    /**
     * Flow를 활용한 대기열 상태 업데이트 스트리밍
     */
    @GetMapping(path = ["/flow-events"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamQueueEventsFlow(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user-id") userId: Long
    ): Flux<QueueUpdateEvent> {
        val statusFlow: Flow<QueueUpdateEvent> = userQueueService
            .getQueueStatusAsFlow(queue, userId)
            .map { status ->
                QueueUpdateEvent(
                    status.userRank,
                    status.totalQueueSize,
                    status.progress
                )
            }
        return statusFlow.asFlux()
    }

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