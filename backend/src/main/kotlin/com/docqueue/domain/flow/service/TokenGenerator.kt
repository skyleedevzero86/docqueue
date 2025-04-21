package com.docqueue.domain.flow.service

fun interface TokenGenerator {
    fun generate(): String
}