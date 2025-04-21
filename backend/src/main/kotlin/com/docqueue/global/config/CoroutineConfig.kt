package com.docqueue.global.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class CoroutineConfig {

    // IO 작업을 위한 코루틴 디스패처
    @Bean
    fun ioDispatcher(): CoroutineDispatcher =
        Dispatchers.IO

    // Redis 연산을 위한 별도의 코루틴 디스패처
    @Bean
    fun redisCoroutineDispatcher(): CoroutineDispatcher =
        Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    // 계산 작업을 위한 코루틴 디스패처
    @Bean
    fun computationDispatcher(): CoroutineDispatcher =
        Dispatchers.Default
}