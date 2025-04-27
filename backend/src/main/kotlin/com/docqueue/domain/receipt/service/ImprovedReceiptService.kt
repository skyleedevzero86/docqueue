package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt
import com.docqueue.domain.receipt.entity.ReceiptPrintLog
import com.docqueue.domain.receipt.repository.ReceiptPrintLogRepository
import com.docqueue.global.datas.receipt.ReceiptResult
import com.docqueue.global.exception.ReceiptError
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ImprovedReceiptService(
    private val receiptPrintLogRepository: ReceiptPrintLogRepository
) {
    private val logger = LoggerFactory.getLogger(ImprovedReceiptService::class.java)

    // 사용자별 영수증 출력 로그 조회
    suspend fun getReceiptLogsByUser(userId: String): Flow<ReceiptPrintLog> {
        return receiptPrintLogRepository.findByUserId(userId)
    }

    // 기간별 영수증 출력 로그 조회
    suspend fun getReceiptLogsByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<ReceiptPrintLog> {
        return receiptPrintLogRepository.findByPrintedAtBetween(start, end)
    }

    private fun createCell(text: String, font: PdfFont, alignment: TextAlignment): Cell {
        return Cell().add(
            Paragraph(text)
                .setFont(font)
                .setFontSize(10f)
                .setTextAlignment(alignment)
        ).setBorder(Border.NO_BORDER)
    }
}