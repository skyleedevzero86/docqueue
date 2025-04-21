package com.docqueue.domain.flow.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class UserQueueRepository(
    @Qualifier("docQueueReactiveRedisTemplate") private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    // Redis 키 생성을 위한 순수 함수
    private fun generateKey(queue: String, keyType: String): String =
        "queue:$queue:$keyType"

    // 대기 순서 조회 - 순수 함수
    fun findWaitingOrder(queue: String): Mono<Long> {
        val key = generateKey(queue, "waiting-order")
        return redisTemplate.opsForValue().get(key)
            .defaultIfEmpty("0")
            .map { it.toLong() }
    }

    // 허용된 순서 조회 - 순수 함수
    fun findAllowedOrder(queue: String): Mono<Long> {
        val key = generateKey(queue, "allowed-order")
        return redisTemplate.opsForValue().get(key)
            .defaultIfEmpty("0")
            .map { it.toLong() }
    }

    // 사용자의 대기 순서 조회 - 순수 함수
    fun findUserWaitOrder(queue: String, userId: Long): Mono<Long> {
        val key = generateKey(queue, "user-order")
        return redisTemplate.opsForHash<String, String>().get(key, userId.toString())
            .defaultIfEmpty("0")
            .map { it.toLong() }
    }

    // 대기열에 사용자 추가 - 순수 함수 스타일로 구현
    fun addWaitQueue(queue: String, userId: Long, waitingOrder: Long): Mono<Boolean> {
        val orderKey = generateKey(queue, "user-order")
        val waitingKey = generateKey(queue, "waiting-order")

        return redisTemplate.opsForHash<String, String>().put(orderKey, userId.toString(), waitingOrder.toString())
            .then(redisTemplate.opsForValue().set(waitingKey, waitingOrder.toString()))
    }

    // 허용된 순서 설정 - 순수 함수 스타일로 구현
    fun setAllowedOrder(queue: String, allowedOrder: Long): Mono<Boolean> {
        val key = generateKey(queue, "allowed-order")
        return redisTemplate.opsForValue().set(key, allowedOrder.toString())
    }

    // 토큰 추가 - 순수 함수 스타일로 구현
    fun addToken(queue: String, userId: Long, token: String): Mono<Boolean> {
        val key = generateKey(queue, "token")
        return redisTemplate.opsForHash<String, String>().put(key, userId.toString(), token)
            .then(redisTemplate.expire(key, Duration.ofMinutes(5)))
    }

    // 토큰 유효성 검사 - 순수 함수 스타일로 구현
    fun isTokenValid(queue: String, userId: Long, token: String): Mono<Boolean> {
        val key = generateKey(queue, "token")
        return redisTemplate.opsForHash<String, String>().get(key, userId.toString())
            .defaultIfEmpty("")
            .map { storedToken -> storedToken == token && token.isNotBlank() }
    }
}