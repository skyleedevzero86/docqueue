package com.docqueue.global.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

// Mono<T>를 Flow<T>로 변환하는 확장 함수
fun <T : Any> Mono<T>.asKotlinFlow(): Flow<T> = this.asFlow()

// Flow<T>를 Flux<T>로 변환하는 확장 함수
fun <T : Any> Flow<T>.asFlux(): Flux<T> = Flux.from(this.asPublisher())

// Flow 변환을 위한 확장 함수
suspend inline fun <T : Any, R : Any> Mono<T>.mapAwait(crossinline transform: suspend (T) -> R): R {
    return transform(this.awaitSingle())
}

// Null 안전 Flow 변환 함수
fun <T : Any, R : Any> Flow<T>.mapNotNull(transform: suspend (T) -> R?): Flow<R> =
    this.map { transform(it) }.filterNotNull()