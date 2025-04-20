package com.docqueue.global.producer

import com.docqueue.global.datas.proto.MyProtoMessages
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ProtoProducer(@Autowired private val kafkaTemplate: KafkaTemplate<String, ByteArray>) {
    fun sendMessage(id: Int, name: String, message: String) {
        val protoMessage = MyProtoMessages.MyProtoMessage.newBuilder()
            .setId(id)
            .setName(name)
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis())
            .build()

        val bytes = protoMessage.toByteArray()
        kafkaTemplate.send("my-topic", bytes)
    }
}