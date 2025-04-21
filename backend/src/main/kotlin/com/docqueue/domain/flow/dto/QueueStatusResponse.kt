package com.docqueue.domain.flow.dto

import com.docqueue.domain.flow.service.QueueStatus


data class QueueStatusResponse(
    val queueFront: Long,
    val queueBack: Long,
    val progress: Double
) {

    constructor(queueStatus: QueueStatus) : this(
        queueFront = queueStatus.first,
        queueBack = queueStatus.second,
        progress = queueStatus.third
    )
}