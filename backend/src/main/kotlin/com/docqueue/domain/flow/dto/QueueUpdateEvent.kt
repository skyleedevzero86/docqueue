package com.docqueue.domain.flow.dto

data class QueueUpdateEvent(
    val queueFront: Long,
    val queueBack: Long,
    val progress: Double,
    val timestamp: Long = System.currentTimeMillis()
)