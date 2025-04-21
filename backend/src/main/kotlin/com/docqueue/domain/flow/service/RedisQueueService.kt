package com.docqueue.domain.flow.service

import com.docqueue.domain.flow.model.QueueStatus
import com.docqueue.global.datas.queue.QueueName
import com.docqueue.global.datas.queue.Token
import com.docqueue.global.datas.queue.UserId
import com.docqueue.global.exception.QueueError
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

@Service
class RedisQueueService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>, // @Qualifier 제거
    @Value("\${scheduler.enabled:false}") private val scheduling: Boolean
) : QueueService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // 상수 정의
    private companion object {
        const val USER_QUEUE_WAIT_KEY = "users:queue:%s:wait"
        const val USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait"
        const val USER_QUEUE_ALLOW_KEY = "users:queue:%s:allow"
    }

    // 해시 함수 - 순수 함수
    private object HashUtil {
        fun hashString(input: String, algorithm: String = "SHA-256"): String {
            val digest = MessageDigest.getInstance(algorithm)
            val encodedHash = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            return encodedHash.joinToString("") { "%02x".format(it) }
        }
    }

    // 진행률 계산 - 순수 함수
    private fun calculateProgress(userRank: Long): Double =
        if (userRank <= 0) 100.0 else 100.0 / userRank.toDouble()

    override suspend fun registerUser(queue: QueueName, userId: UserId): Long {
        val timestamp = Instant.now().epochSecond
        val isAdded = redisTemplate.opsForZSet()
            .add(USER_QUEUE_WAIT_KEY.format(queue), userId.toString(), timestamp.toDouble())
            .awaitFirst()

        if (!isAdded) throw QueueError.UserAlreadyRegistered

        return redisTemplate.opsForZSet()
            .rank(USER_QUEUE_WAIT_KEY.format(queue), userId.toString())
            .awaitFirst()
            .let { if (it >= 0) it + 1 else it }
    }

    override suspend fun allowUsers(queue: QueueName, count: Long): Long {
        val timestamp = Instant.now().epochSecond

        return redisTemplate.opsForZSet()
            .popMin(USER_QUEUE_WAIT_KEY.format(queue), count)
            .asFlow()
            .map { entry ->
                entry.value?.let { userId ->
                    redisTemplate.opsForZSet()
                        .add(USER_QUEUE_ALLOW_KEY.format(queue), userId, timestamp.toDouble())
                        .awaitFirstOrNull()
                }
            }
            .filterNotNull()
            .count()
            .toLong()
    }

    override suspend fun isAllowed(queue: QueueName, userId: UserId): Boolean {
        return redisTemplate.opsForZSet()
            .rank(USER_QUEUE_ALLOW_KEY.format(queue), userId.toString())
            .awaitFirstOrNull()?.let { it >= 0 } ?: false
    }

    override suspend fun validateToken(queue: QueueName, userId: UserId, token: Token): Boolean {
        logger.info("검증 토큰: $token")
        return generateToken(queue, userId) == token
    }

    override suspend fun generateToken(queue: QueueName, userId: UserId): Token {
        val input = "user-queue-$queue-$userId"
        return HashUtil.hashString(input)
    }

    private suspend fun getUserRank(queue: QueueName, userId: UserId): Long {
        return redisTemplate.opsForZSet()
            .rank(USER_QUEUE_WAIT_KEY.format(queue), userId.toString())
            .awaitFirstOrNull() ?: -1L
            .let { if (it >= 0) it + 1 else it }
    }

    private suspend fun getTotalSize(queue: QueueName): Long {
        val waitSize = redisTemplate.opsForZSet()
            .size(USER_QUEUE_WAIT_KEY.format(queue))
            .awaitFirstOrNull() ?: 0L

        val allowSize = redisTemplate.opsForZSet()
            .size(USER_QUEUE_ALLOW_KEY.format(queue))
            .awaitFirstOrNull() ?: 0L

        return waitSize + allowSize
    }

    override suspend fun getQueueStatus(queue: QueueName, userId: UserId): QueueStatus {
        val userRank = getUserRank(queue, userId)
        val totalSize = getTotalSize(queue)
        val progress = calculateProgress(userRank)

        logger.info("상태 조회 - 순위: $userRank, 전체: $totalSize, 진행률: $progress")
        return QueueStatus(userRank, totalSize, progress)
    }

    override suspend fun registerOrGetStatus(queue: QueueName, userId: UserId): QueueStatus {
        return try {
            val rank = registerUser(queue, userId)
            val total = getTotalSize(queue)
            QueueStatus.calculate(rank, total)
        } catch (e: QueueError.UserAlreadyRegistered) {
            getQueueStatus(queue, userId)
        }
    }

    override fun processAllQueues(maxCount: Long): Flow<Pair<QueueName, Long>> {
        if (!scheduling) {
            logger.info("스케줄링 비활성화 상태")
            return emptyFlow()
        }

        logger.info("스케줄링 실행 중...")

        return redisTemplate.scan(
            ScanOptions.scanOptions()
                .match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .build()
        )
            .asFlow()
            .map { key -> key.split(":")[2] } // 큐 이름 추출
            .map { queue ->
                val allowed = allowUsers(queue, maxCount)
                queue to allowed
            }
            .onEach { (queue, allowed) ->
                logger.info("$queue 큐에서 $maxCount 명 처리 시도, $allowed 명 처리 완료")
            }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    suspend fun scheduleAllowUsers() {
        processAllQueues(3L).collect()
    }
}