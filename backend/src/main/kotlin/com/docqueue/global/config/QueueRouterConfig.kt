package com.docqueue.global.config

import com.docqueue.domain.flow.dto.AllowResponse
import com.docqueue.domain.flow.dto.AllowedStatus
import com.docqueue.domain.flow.dto.QueueProgress
import com.docqueue.domain.flow.dto.RegisterResponse
import com.docqueue.domain.flow.service.QueueService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseCookie
import org.springframework.web.reactive.function.server.*
import java.time.Duration

@Configuration
class QueueRouterConfig(private val queueService: QueueService) {

    @Bean
    fun queueRoutes() = coRouter {
        "/api/v1/queue".nest {
            // 사용자 등록
            POST("") { request ->
                val queue = request.queryParamOrNull("queue") ?: "default"
                val userId = request.queryParamOrNull("user-id")?.toLong()
                    ?: return@POST ServerResponse.badRequest().buildAndAwait()

                val rank = queueService.registerUser(queue, userId)
                ServerResponse.ok().bodyValueAndAwait(RegisterResponse(rank))
            }

            // 사용자 허용
            POST("/allow") { request ->
                val queue = request.queryParamOrNull("queue") ?: "default"
                val count = request.queryParamOrNull("count")?.toLong()
                    ?: return@POST ServerResponse.badRequest().buildAndAwait()

                val allowedCount = queueService.allowUsers(queue, count)
                ServerResponse.ok().bodyValueAndAwait(AllowResponse(count, allowedCount))
            }

            // 허용 여부 확인
            GET("/allowed") { request ->
                val queue = request.queryParamOrNull("queue") ?: "default"
                val userId = request.queryParamOrNull("user-id")?.toLong()
                    ?: return@GET ServerResponse.badRequest().buildAndAwait()
                val token = request.queryParamOrNull("token") ?: ""

                val isAllowed = queueService.validateToken(queue, userId, token)
                ServerResponse.ok().bodyValueAndAwait(AllowedStatus(isAllowed))
            }

            // 진행상황 조회
            GET("/progress") { request ->
                val queue = request.queryParamOrNull("queue") ?: "default"
                val userId = request.queryParamOrNull("user-id")?.toLong()
                    ?: return@GET ServerResponse.badRequest().buildAndAwait()

                val status = queueService.getQueueStatus(queue, userId)
                ServerResponse.ok().bodyValueAndAwait(QueueProgress.from(status))
            }

            // 토큰 발급
            GET("/touch") { request ->
                val queue = request.queryParamOrNull("queue") ?: "default"
                val userId = request.queryParamOrNull("user-id")?.toLong()
                    ?: return@GET ServerResponse.badRequest().buildAndAwait()
                val exchange = request.exchange()

                val token = queueService.generateToken(queue, userId)
                exchange.response.addCookie(
                    ResponseCookie.from("user-queue-$queue-token", token)
                        .maxAge(Duration.ofSeconds(300))
                        .path("/")
                        .build()
                )

                ServerResponse.ok().bodyValueAndAwait(token)
            }
        }

        // 대기실 페이지
        GET("/waiting-room") { request ->
            val queue = request.queryParamOrNull("queue") ?: "default"
            val userId = request.queryParamOrNull("user-id")?.toLong()
                ?: return@GET ServerResponse.badRequest().buildAndAwait()
            val redirectUrl = request.queryParamOrNull("redirect-url")
                ?: return@GET ServerResponse.badRequest().buildAndAwait()

            // 쿠키에서 토큰 가져오기
            val cookieToken = request.cookies().get("user-queue-$queue-token")
                ?.firstOrNull()?.value ?: ""

            if (queueService.validateToken(queue, userId, cookieToken)) {
                ServerResponse.temporaryRedirect(java.net.URI(redirectUrl)).buildAndAwait()
            } else {
                val status = queueService.registerOrGetStatus(queue, userId)
                val progress = QueueProgress.from(status)

                ServerResponse.ok()
                    .renderAndAwait("waiting-room", mapOf(
                        "queue" to queue,
                        "userId" to userId,
                        "queueFront" to progress.queueFront,
                        "queueBack" to progress.queueBack,
                        "progress" to progress.progress
                    ))
            }
        }
    }
}