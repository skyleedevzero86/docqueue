package com.docqueue.global.producer

import com.docqueue.global.datas.avro.MyAvroClass
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class AvroProducer(@Autowired private val kafkaTemplate: KafkaTemplate<String, MyAvroClass>) {
    fun sendMessage(message: MyAvroClass) {
        kafkaTemplate.send("my-topic", message)
    }
}