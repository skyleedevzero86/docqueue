package com.docqueue.domain.receipt.repository

import com.docqueue.domain.receipt.entity.ReceiptPrintLog
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface ReceiptPrintLogRepository : CoroutineCrudRepository<ReceiptPrintLog, String> {
    fun findByPrintedAtBetween(start: LocalDateTime, end: LocalDateTime): Flow<ReceiptPrintLog>
    fun findByUserId(userId: String): Flow<ReceiptPrintLog>
    suspend fun findByIdAndUserId(id: String, userId: String): ReceiptPrintLog?
}