package com.docqueue.domain.dbinfo.dto

data class HealthResponseDto(
    val status: String,
    val components: Map<String, ComponentHealth>
)