package com.docqueue.domain.flow.dto

data class QueueStatus(
    val userRank: Long,
    val totalQueueSize: Long,
    val progress: Double
) {
    companion object {
        fun calculate(userRank: Long, totalQueueSize: Long): QueueStatus {
            val progress = if (userRank <= 0) 100.0 else 100.0 / userRank.toDouble()
            return QueueStatus(userRank, totalQueueSize, progress)
        }
    }
}