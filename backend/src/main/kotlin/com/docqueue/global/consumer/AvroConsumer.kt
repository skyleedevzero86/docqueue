package com.docqueue.global.consumer

import com.docqueue.global.datas.avro.MyAvroClass
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class AvroConsumer {
    @KafkaListener(topics = ["my-topic"], groupId = "my-group")
    fun listen(message: MyAvroClass) {
        println("Received message: $message")
    }
}