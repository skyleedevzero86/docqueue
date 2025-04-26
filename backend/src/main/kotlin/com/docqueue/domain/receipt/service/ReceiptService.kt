package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt
import com.docqueue.domain.receipt.entity.ReceiptPrintLog
import com.docqueue.domain.receipt.repository.ReceiptPrintLogRepository
import com.docqueue.global.datas.receipt.ReceiptResult
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class ReceiptService(
    private val receiptPrintLogRepository: ReceiptPrintLogRepository
) {
    private val logger = LoggerFactory.getLogger(ReceiptService::class.java)

    @Value("\${receipt.storage.path:receipts}")
    private lateinit var storagePath: String

    // 함수형 스타일의 PDF 생성 함수
    val createReceiptPdf: PdfDocumentCreator = PdfDocumentCreator { receipt ->
        withContext(Dispatchers.IO) {
            try {
                val baos = ByteArrayOutputStream()
                val pdfWriter = PdfWriter(baos)
                val pdfDoc = PdfDocument(pdfWriter)
                val document = Document(pdfDoc)

                // 타이틀
                document.add(
                    Paragraph("CASH RECEIPT")
                        .setFontSize(16f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                )
                document.add(Paragraph(" "))

                // 주소 및 연락처
                document.add(Paragraph("Address: ${receipt.address}").setFontSize(10f))
                document.add(Paragraph("Tel: ${receipt.phoneNumber}").setFontSize(10f))

                // 구분선
                document.add(Paragraph("----------------------------------------").setFontSize(10f))

                // 날짜
                val dateTime = receipt.date
                val dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                val dateTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                dateTable.addCell(Paragraph("Date: $dateStr").setFontSize(10f))
                dateTable.addCell(Paragraph(timeStr).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))
                document.add(dateTable)

                // 구분선
                document.add(Paragraph("----------------------------------------").setFontSize(10f))

                // 아이템 목록
                val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                receipt.items.forEach { item ->
                    itemsTable.addCell(Paragraph(item.name).setFontSize(10f))
                    itemsTable.addCell(
                        Paragraph(item.price.toString()).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)
                    )
                }
                document.add(itemsTable)

                // 구분선
                document.add(Paragraph("----------------------------------------").setFontSize(10f))

                // 합계
                val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                totalTable.addCell(Paragraph("Total").setFontSize(12f).setBold())
                totalTable.addCell(
                    Paragraph(receipt.totalAmount.toString()).setFontSize(12f).setTextAlignment(TextAlignment.RIGHT)
                )
                document.add(totalTable)

                // 현금 및 거스름돈
                val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                paymentTable.addCell(Paragraph("Cash").setFontSize(10f))
                paymentTable.addCell(
                    Paragraph(receipt.cashAmount.toString()).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)
                )
                paymentTable.addCell(Paragraph("Change").setFontSize(10f))
                paymentTable.addCell(
                    Paragraph(receipt.changeAmount.toString()).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT)
                )
                document.add(paymentTable)

                // QR 코드 자리
                document.add(Paragraph(" "))
                document.add(
                    Paragraph("QR Code would be placed here")
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER)
                )

                document.close()
                baos.toByteArray()
            } catch (e: Exception) {
                logger.error("PDF 생성 중 오류 발생: ${e.message}", e)
                throw e
            }
        }
    }

    // 함수형 스타일의 PDF 저장 함수
    val savePdfToStorage: PdfSaver = PdfSaver { content, filename ->
        withContext(Dispatchers.IO) {
            try {
                val directory = Paths.get(storagePath).toFile()
                if (!directory.exists()) {
                    logger.info("Creating directory: $storagePath")
                    directory.mkdirs()
                }
                if (!directory.canWrite()) {
                    throw IllegalStateException("Directory is not writable: $storagePath")
                }

                val path = Paths.get(storagePath, filename)
                Files.write(path, content)
                logger.info("PDF saved to: $path")
                path
            } catch (e: Exception) {
                logger.error("PDF 저장 중 오류 발생: ${e.message}", e)
                throw e
            }
        }
    }

    // PDF 생성 및 저장
    suspend fun generateAndSaveReceipt(receipt: Receipt, userId: String? = null): ReceiptResult<Pair<ByteArray, ReceiptPrintLog>> = runCatching {
        val receiptId = UUID.randomUUID().toString()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "영수증_${timestamp}.pdf"

        val pdfContent = createReceiptPdf.createPdfDocument(receipt)
        val savedPath = savePdfToStorage.savePdf(pdfContent, filename)

        val printLog = ReceiptPrintLog(
            receiptId = receiptId,
            fileName = filename,
            filePath = savedPath.toString(),
            userId = userId
        )

        val savedLog = receiptPrintLogRepository.save(printLog)
        Pair(pdfContent, savedLog)
    }.onFailure { e ->
        logger.error("영수증 생성 및 저장 중 오류: ${e.message}", e)
    }
}