package com.docqueue.global.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

// Mono<T>를 Flow<T>로 변환하는 확장 함수
fun <T> Mono<T>.asKotlinFlow(): Flow<T> = this.asFlow()

// Flow<T>를 Flux<T>로 변환하는 확장 함수
fun <T> Flow<T>.asFlux(): Flux<T> = Flux.from(this)

// Flow 변환을 위한 확장 함수
suspend inline fun <T, R> Mono<T>.mapAwait(crossinline transform: suspend (T) -> R): R {
    return this.awaitSingle().let { transform(it) }
}

// Null 안전 Flow 변환 함수
fun <T, R> Flow<T>.mapNotNull(transform: suspend (T) -> R?): Flow<R> =
    this.map { value -> transform(value) ?: throw IllegalStateException("데이터 변환 중 null이 발생했습니다.") }