package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.dto.ReceiptOutcome
import com.docqueue.domain.receipt.dto.ReceiptOutput
import com.docqueue.domain.receipt.dto.ReceiptMetadata
import com.docqueue.domain.receipt.entity.Receipt
import com.docqueue.domain.receipt.entity.ReceiptPrintLog
import com.docqueue.domain.receipt.repository.ReceiptPrintLogRepository
import com.docqueue.global.exception.ReceiptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ReceiptService(
    private val pdfDocumentCreator: PdfDocumentCreator,
    private val pdfSaver: PdfSaver,
    private val receiptPrintLogRepository: ReceiptPrintLogRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun generateAndSaveReceipt(receipt: Receipt, userId: String? = null): ReceiptOutcome = withContext(Dispatchers.IO) {
        val receiptId = UUID.randomUUID().toString()
        val fileName = "영수증_${receipt.date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
        val createdAt = Instant.now()

        runCatching {
            val content = pdfDocumentCreator.createPdfDocument(receipt, receiptId).getOrThrow()
            val filePath = pdfSaver.savePdf(content, fileName)
            val effectiveUserId = userId ?: "Anonymous"
            val printLog = ReceiptPrintLog(
                id = null, // 데이터베이스에서 자동 생성
                receiptId = receiptId,
                fileName = fileName,
                filePath = filePath.toString(),
                userId = effectiveUserId,
                printedAt = LocalDateTime.now()
            )
            val savedLog = receiptPrintLogRepository.save(printLog)
            ReceiptOutput(
                content = content,
                metadata = ReceiptMetadata(fileName, createdAt, content.size.toLong()),
                printLog = savedLog
            )
        }.fold(
            onSuccess = { output ->
                logger.info("영수증 생성 및 로그 저장 성공: $fileName, receiptId=$receiptId")
                ReceiptOutcome.Success(output)
            },
            onFailure = { error ->
                when (error) {
                    is ReceiptError.PdfGenerationError -> logger.error("PDF 생성 실패: ${error.message}", error)
                    is ReceiptError.StorageError -> logger.error("PDF 저장 실패: ${error.message}", error)
                    is ReceiptError.DatabaseError -> logger.error("로그 저장 실패: ${error.message}", error)
                    else -> logger.error("영수증 처리 중 오류 발생: ${error.message}", error)
                }
                ReceiptOutcome.Failure(error)
            }
        )
    }

    suspend fun getReceiptLogsByUser(userId: String): Flow<ReceiptPrintLog> {
        return receiptPrintLogRepository.findByUserId(userId)
    }

    suspend fun getReceiptLogsByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<ReceiptPrintLog> {
        return receiptPrintLogRepository.findByPrintedAtBetween(start, end)
    }

    suspend fun getReceiptLogByIdAndUser(id: Long, userId: String): ReceiptPrintLog? {
        return receiptPrintLogRepository.findByIdAndUserId(id, userId)
    }
}