package com.docqueue.domain.flow.service

import com.docqueue.domain.flow.model.QueueStatus
import com.docqueue.domain.flow.repository.UserQueueRepository
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Service
class UserQueueService(
    private val userQueueRepository: UserQueueRepository,
    private val tokenGenerator: TokenGenerator
) {

    // 대기열에 사용자 등록 - suspend 키워드 제거하고 mono 블록으로 감싸기
    fun registerWaitQueue(queue: QueueName, userId: UserId): Mono<WaitingNumber> {
        return userQueueRepository.findWaitingOrder(queue)
            .flatMap { waitingOrder ->
                userQueueRepository.addWaitQueue(queue, userId, waitingOrder + 1)
                    .map { waitingOrder + 1 }
            }
    }

    // 사용자 입장 허용
    fun allowUser(queue: QueueName, count: Long): Mono<Long> {
        return userQueueRepository.findWaitingOrder(queue)
            .flatMap { currentOrder ->
                val targetOrder = currentOrder + count
                userQueueRepository.setAllowedOrder(queue, targetOrder)
                    .map { targetOrder - currentOrder }
            }
    }

    // 토큰 생성
    fun generateToken(queue: QueueName, userId: UserId): Mono<String> {
        val token = tokenGenerator.generate()
        return userQueueRepository.addToken(queue, userId, token)
            .map { token }
    }

    // 토큰을 통한 사용자 허용 확인
    fun isAllowedByToken(queue: QueueName, userId: UserId, token: String): Mono<Boolean> {
        if (token.isBlank()) {
            return Mono.just(false)
        }

        return userQueueRepository.isTokenValid(queue, userId, token)
            .flatMap { isValid ->
                if (isValid) {
                    isUserAllowed(queue, userId)
                } else {
                    Mono.just(false)
                }
            }
    }

    // 사용자 허용 상태 확인
    private fun isUserAllowed(queue: QueueName, userId: UserId): Mono<Boolean> {
        return userQueueRepository.findAllowedOrder(queue)
            .zipWith(userQueueRepository.findUserWaitOrder(queue, userId))
            .map { tuple ->
                val allowedOrder = tuple.t1
                val userOrder = tuple.t2
                userOrder > 0 && userOrder <= allowedOrder
            }
    }

    // 대기열 상태 조회
    fun getQueueStatus(queue: QueueName, userId: UserId): Mono<QueueStatus> {
        return userQueueRepository.findUserWaitOrder(queue, userId)
            .zipWith(userQueueRepository.findWaitingOrder(queue))
            .zipWith(userQueueRepository.findAllowedOrder(queue))
            .map { tuple ->
                val userOrder = tuple.t1.t1
                val waitingOrder = tuple.t1.t2
                val allowedOrder = tuple.t2

                val userRank = if (userOrder > 0) userOrder else 0
                val totalQueueSize = waitingOrder
                val progress = if (allowedOrder > 0 && waitingOrder > 0) {
                    (allowedOrder.toDouble() / waitingOrder.toDouble()) * 100.0
                } else {
                    0.0
                }

                QueueStatus(userRank, totalQueueSize, progress)
            }
            .onErrorReturn(QueueStatus(0L, 0L, 0.0))
    }

    // Flow 활용 메서드
    fun getQueueStatusAsFlow(queue: QueueName, userId: UserId): Flow<QueueStatus> {
        return getQueueStatus(queue, userId).asFlow()
    }

    // 대기 큐 등록 또는 상태 조회 (코루틴 방식으로 수정)
    suspend fun registerWaitingQueueOrGetQueueStatus(queue: QueueName, userId: UserId): QueueStatus {
        return withContext(Dispatchers.IO) {
            val userOrder = userQueueRepository.findUserWaitOrder(queue, userId).awaitSingle()

            if (userOrder > 0) {
                getQueueStatus(queue, userId).awaitSingle()
            } else {
                // 먼저 대기열에 등록한 후
                registerWaitQueue(queue, userId).awaitSingle()
                // 상태 조회
                getQueueStatus(queue, userId).awaitSingle()
            }
        }
    }
}