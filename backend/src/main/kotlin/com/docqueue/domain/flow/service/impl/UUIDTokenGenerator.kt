package com.docqueue.domain.flow.service.impl

import com.docqueue.domain.flow.service.TokenGenerator
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UUIDTokenGenerator : TokenGenerator {
    override fun generate(): String {
        return UUID.randomUUID().toString()
    }
}