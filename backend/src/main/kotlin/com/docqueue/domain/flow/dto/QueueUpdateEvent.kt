package com.docqueue.domain.flow.dto

data class QueueUpdateEvent(
    val userRank: Long,
    val totalQueueSize: Long,
    val progress: Double
)