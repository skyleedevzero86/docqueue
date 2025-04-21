package com.docqueue.domain.flow.service

import com.docqueue.domain.flow.repository.UserQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class FunctionalUserQueueService(
    private val repository: UserQueueRepository
) {
    private val tokenGenerator: TokenGenerator = TokenGenerator { UUID.randomUUID().toString() }

    fun registerUser(queue: String, userId: Long): Mono<Long> =
        repository.findWaitingOrder(queue)
            .flatMap { currentOrder ->
                val nextOrder = currentOrder + 1
                repository.addWaitQueue(queue, userId, nextOrder)
                    .map { nextOrder }
            }

    fun allowUsers(queue: String, count: Long): Mono<Long> =
        repository.findAllowedOrder(queue)
            .flatMap { currentAllowed ->
                val newAllowed = currentAllowed + count
                repository.setAllowedOrder(queue, newAllowed)
                    .map { count }
            }

    fun createToken(queue: String, userId: Long): Mono<String> {
        val token = tokenGenerator.generate()
        return repository.addToken(queue, userId, token)
            .map { token }
    }

    fun verifyUserAccess(queue: String, userId: Long, token: String): Mono<Boolean> =
        if (token.isBlank()) Mono.just(false)
        else repository.isTokenValid(queue, userId, token)
            .flatMap { isValid ->
                if (isValid) checkUserAllowed(queue, userId)
                else Mono.just(false)
            }

    private fun checkUserAllowed(queue: String, userId: Long): Mono<Boolean> =
        Mono.zip(
            repository.findAllowedOrder(queue),
            repository.findUserWaitOrder(queue, userId)
        ).map { tuple ->
            val (allowedOrder, userOrder) = tuple.t1 to tuple.t2
            userOrder > 0 && userOrder <= allowedOrder
        }

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

    private fun calculateProgress(allowedOrder: Long, waitingOrder: Long): Double =
        if (allowedOrder > 0 && waitingOrder > 0) {
            (allowedOrder.toDouble() / waitingOrder.toDouble()) * 100.0
        } else {
            0.0
        }

    fun streamQueueStatus(queue: String, userId: Long, intervalMs: Long = 1000): Flow<QueueStatus> = flow {
        while (true) {
            emit(getUserQueueStatus(queue, userId).awaitSingle())
            kotlinx.coroutines.delay(intervalMs)
        }
    }

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