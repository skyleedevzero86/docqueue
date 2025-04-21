package com.docqueue.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun docQueueReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        return ReactiveRedisTemplate(factory, RedisSerializationContext.string())
    }

    @Bean
    @Primary
    fun reactiveRedisTemplate(connectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val serializer = StringRedisSerializer()
        val serializationContext = RedisSerializationContext.newSerializationContext<String, String>()
            .key(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .value(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .hashKey(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}