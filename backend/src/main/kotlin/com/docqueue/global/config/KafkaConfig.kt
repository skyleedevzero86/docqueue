package com.docqueue.global.config

import com.docqueue.global.datas.avro.MyAvroClass
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, MyAvroClass> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, MyAvroClass> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
            "schema.registry.url" to "http://localhost:8081"
        )
        return DefaultKafkaProducerFactory(config)
    }
}