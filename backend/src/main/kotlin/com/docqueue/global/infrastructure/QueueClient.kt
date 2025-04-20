package com.docqueue.global.infrastructure

import com.docqueue.domain.home.dto.AllowedUserResponse
import com.docqueue.domain.home.service.QueueName
import com.docqueue.domain.home.service.Token
import com.docqueue.domain.home.service.UserId
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class QueueClient(
    private val webClient: WebClient
) {
    suspend fun verifyQueueAccess(queue: QueueName, userId: UserId, token: Token): AllowedUserResponse {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/queue/allowed")
                    .queryParam("queue", queue)
                    .queryParam("user-id", userId)
                    .queryParam("token", token)
                    .build()
            }
            .retrieve()
            .bodyToMono(AllowedUserResponse::class.java)
            .awaitSingle()
    }
}