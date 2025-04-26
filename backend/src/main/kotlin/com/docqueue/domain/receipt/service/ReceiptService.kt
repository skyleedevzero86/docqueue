package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class ReceiptService(
    private val pdfDocumentCreator: PdfDocumentCreator,
    private val pdfSaver: PdfSaver
) {
    private val logger: Logger = LoggerFactory.getLogger(ReceiptService::class.java)

    data class ReceiptMetadata(val fileName: String, val createdAt: Instant, val size: Long)
    data class ReceiptOutput(val content: ByteArray, val metadata: ReceiptMetadata)

    sealed interface ReceiptOutcome {
        data class Success(val output: ReceiptOutput) : ReceiptOutcome
        data class Failure(val error: Throwable) : ReceiptOutcome
    }

    suspend fun generateAndSaveReceipt(receipt: Receipt): ReceiptOutcome = withContext(Dispatchers.IO) {
        val fileName = "영수증_${receipt.date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
        val createdAt = Instant.now()

        runCatching {
            pdfDocumentCreator.createPdfDocument(receipt)
                .let { content ->
                    pdfSaver.savePdf(content, fileName)
                        .let { path ->
                            ReceiptOutput(
                                content,
                                ReceiptMetadata(fileName, createdAt, content.size.toLong())
                            )
                        }
                }
        }.fold(
            onSuccess = { ReceiptOutcome.Success(it).also { logger.info("영수증 생성 성공: $fileName") } },
            onFailure = { error: Throwable ->
                ReceiptOutcome.Failure(error).also {
                    logger.error("영수증 생성 실패: ${error.message ?: "알 수 없는 오류"}", error)
                }
            }
        )
    }
}