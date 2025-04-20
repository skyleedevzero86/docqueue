package com.docqueue.global.producer

import com.docqueue.global.datas.proto.MyProtoMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ProtoProducer(@Autowired private val kafkaTemplate: KafkaTemplate<String, ByteArray>) {
    fun sendMessage(message: MyProtoMessage) {
        val bytes = message.toByteArray()
        kafkaTemplate.send("my-topic", bytes)
    }
}