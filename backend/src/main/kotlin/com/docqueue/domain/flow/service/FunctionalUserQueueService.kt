package com.docqueue.domain.flow.service

import com.docqueue.domain.flow.repository.UserQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

// 함수형 스타일의 서비스 구현
@Service
class FunctionalUserQueueService(
    private val repository: UserQueueRepository,
    private val tokenGenerator: TokenGenerator = TokenGenerator { UUID.randomUUID().toString() }
) {
    // 순수 함수로 구성된 메서드들

    // 대기열 등록
    fun registerUser(queue: String, userId: Long): Mono<Long> =
        repository.findWaitingOrder(queue)
            .flatMap { currentOrder ->
                val nextOrder = currentOrder + 1
                repository.addWaitQueue(queue, userId, nextOrder)
                    .map { nextOrder }
            }

    // 사용자 입장 허용
    fun allowUsers(queue: String, count: Long): Mono<Long> =
        repository.findAllowedOrder(queue)
            .flatMap { currentAllowed ->
                val newAllowed = currentAllowed + count
                repository.setAllowedOrder(queue, newAllowed)
                    .map { count }
            }

    // 토큰 생성
    fun createToken(queue: String, userId: Long): Mono<String> {
        val token = tokenGenerator.generate()
        return repository.addToken(queue, userId, token)
            .map { token }
    }

    // 토큰 검증 및 사용자 입장 가능 여부 확인
    fun verifyUserAccess(queue: String, userId: Long, token: String): Mono<Boolean> =
        if (token.isBlank()) Mono.just(false)
        else repository.isTokenValid(queue, userId, token)
            .flatMap { isValid ->
                if (isValid) checkUserAllowed(queue, userId)
                else Mono.just(false)
            }

    // 사용자 입장 가능 여부 확인
    private fun checkUserAllowed(queue: String, userId: Long): Mono<Boolean> =
        Mono.zip(
            repository.findAllowedOrder(queue),
            repository.findUserWaitOrder(queue, userId)
        ).map { tuple ->
            val (allowedOrder, userOrder) = tuple.t1 to tuple.t2
            userOrder > 0 && userOrder <= allowedOrder
        }

    // 대기열 상태 조회
    fun getUserQueueStatus(queue: String, userId: Long): Mono<QueueStatus> =
        Mono.zip(
            repository.findUserWaitOrder(queue, userId),
            repository.findWaitingOrder(queue),
            repository.findAllowedOrder(queue)
        ).map { tuple ->
            val (userOrder, waitingOrder, allowedOrder) = Triple(tuple.t1, tuple.t2, tuple.t3)

            val queueFront = if (userOrder > 0) userOrder - 1 else 0
            val queueBack = if (waitingOrder >= userOrder) waitingOrder - userOrder else 0
            val progress = calculateProgress(allowedOrder, waitingOrder)

            Triple(queueFront, queueBack, progress)
        }

    // 진행률 계산 (순수 함수)
    private fun calculateProgress(allowedOrder: Long, waitingOrder: Long): Double =
        if (allowedOrder > 0 && waitingOrder > 0) {
            (allowedOrder.toDouble() / waitingOrder.toDouble()) * 100.0
        } else {
            0.0
        }

    // 코루틴 Flow를 활용한 대기열 상태 스트리밍
    fun streamQueueStatus(queue: String, userId: Long, intervalMs: Long = 1000): Flow<QueueStatus> = flow {
        while (true) {
            emit(getUserQueueStatus(queue, userId).awaitSingle())
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    // 대기열 등록 또는 상태 조회 (함수 합성)
    suspend fun registerOrGetStatus(queue: String, userId: Long): QueueStatus =
        repository.findUserWaitOrder(queue, userId)
            .flatMap { userOrder ->
                if (userOrder > 0) {
                    getUserQueueStatus(queue, userId)
                } else {
                    registerUser(queue, userId)
                        .flatMap { _ -> getUserQueueStatus(queue, userId) }
                }
            }
            .awaitSingle()
}