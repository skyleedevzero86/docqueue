package com.docqueue.global.config

import com.docqueue.global.datas.avro.MyAvroClass
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import io.confluent.kafka.serializers.KafkaAvroSerializer

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaByteArrayTemplate(): KafkaTemplate<String, ByteArray> {
        return KafkaTemplate(byteArrayProducerFactory())
    }

    @Bean
    fun byteArrayProducerFactory(): ProducerFactory<String, ByteArray> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java
        )
        return DefaultKafkaProducerFactory(config)
    }

    // Avro 시리얼라이저를 사용하는 기존 구성도 유지

    @Bean
    fun avroKafkaTemplate(): KafkaTemplate<String, MyAvroClass> {
        return KafkaTemplate(avroProducerFactory())
    }

    @Bean
    fun avroProducerFactory(): ProducerFactory<String, MyAvroClass> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            "schema.registry.url" to "http://localhost:8081"
        )
        return DefaultKafkaProducerFactory(config)
    }

}