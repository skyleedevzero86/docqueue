package com.docqueue.global.infrastructure

import com.docqueue.domain.home.dto.AllowedUserResponse
import com.docqueue.domain.home.service.QueueName
import com.docqueue.domain.home.service.Token
import com.docqueue.domain.home.service.UserId
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import kotlinx.coroutines.reactive.awaitFirstOrNull


@Component
class QueueClient(
    private val webClient: WebClient
) {
    /**
     * 대기열 접근 가능 여부를 외부 API로 확인
     * @param queue 대기열 이름
     * @param userId 사용자 ID
     * @param token 인증 토큰
     * @return AllowedUserResponse (입장 가능 여부)
     */
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

    suspend fun addUser(queue: QueueName, userId: UserId, token: Token) {
        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/queue/add")
                    .queryParam("queue", queue)
                    .queryParam("user-id", userId)
                    .queryParam("token", token)
                    .build()
            }
            .retrieve()
            .bodyToMono(Void::class.java)
            .awaitFirstOrNull()
    }

    suspend fun allow(queue: QueueName, userId: UserId) {
        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/queue/allow")
                    .queryParam("queue", queue)
                    .queryParam("user-id", userId)
                    .build()
            }
            .retrieve()
            .bodyToMono(Void::class.java)
            .awaitFirstOrNull()
    }

}