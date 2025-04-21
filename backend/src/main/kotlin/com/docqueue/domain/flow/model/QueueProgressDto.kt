package com.docqueue.domain.flow.model

data class RegisterResponse(val rank: Long)
data class AllowResponse(val requestCount: Long, val allowedCount: Long)
data class AllowedStatus(val isAllowed: Boolean)
data class QueueProgress(
    val queueFront: Long,
    val queueBack: Long,
    val progress: Double
) {
    companion object {
        fun from(status: QueueStatus): QueueProgress = QueueProgress(
            queueFront = if (status.userRank > 0) status.userRank - 1 else 0,
            queueBack = status.totalQueueSize - status.userRank,
            progress = status.progress
        )
    }
}