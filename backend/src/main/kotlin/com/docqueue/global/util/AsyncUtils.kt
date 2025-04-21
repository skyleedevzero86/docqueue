package com.docqueue.global.util

// 타입 별칭 정의
typealias Transformer<T, R> = (T) -> R
typealias Predicate<T> = (T) -> Boolean
typealias AsyncOperation<T> = suspend () -> T

// 함수형 인터페이스 정의
fun interface AsyncProcessor<T, R> {
    suspend fun process(input: T): R
}

fun interface ErrorHandler {
    fun handle(error: Throwable)
}

// 확장 함수 정의
inline fun <T, R> T.applyIf(predicate: Predicate<T>, transform: Transformer<T, R>): R? {
    return if (predicate(this)) transform(this) else null
}

// 널 안전성을 위한 확장 함수
fun <T : Any> T?.requireNotNull(lazyMessage: () -> String): T {
    return this ?: throw IllegalArgumentException(lazyMessage())
}

// 비동기 작업 재시도 유틸리티
suspend fun <T> retry(
    times: Int,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    operation: AsyncOperation<T>
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return operation()
        } catch (e: Exception) {
            kotlinx.coroutines.delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return operation() // 마지막 시도
}