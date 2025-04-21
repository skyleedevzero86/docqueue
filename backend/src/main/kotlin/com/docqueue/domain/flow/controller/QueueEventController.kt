package com.docqueue.domain.flow.controller

import com.docqueue.domain.flow.dto.QueueUpdateEvent
import com.docqueue.domain.flow.model.QueueStatus
import com.docqueue.domain.flow.service.UserQueueService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/v1/queue")
class QueueEventController(
    private val userQueueService: UserQueueService
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
                    status.userRank,            // first 대신 userRank 속성 사용
                    status.totalQueueSize,      // second 대신 totalQueueSize 속성 사용
                    status.progress             // third 대신 progress 속성 사용
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
                    status.userRank,            // first 대신 userRank 속성 사용
                    status.totalQueueSize,      // second 대신 totalQueueSize 속성 사용
                    status.progress             // third 대신 progress 속성 사용
                )
            }

        return statusFlow.asFlux()
    }
}