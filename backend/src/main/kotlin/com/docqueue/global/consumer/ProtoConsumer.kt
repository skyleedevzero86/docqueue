package com.docqueue.global.consumer

import com.docqueue.global.datas.proto.MyProtoMessages
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ProtoConsumer {
    @KafkaListener(topics = ["my-topic"], groupId = "my-group")
    fun listen(bytes: ByteArray) {
        val message = MyProtoMessages.MyProtoMessage.parseFrom(bytes)
        println("Received message: $message")
    }
}