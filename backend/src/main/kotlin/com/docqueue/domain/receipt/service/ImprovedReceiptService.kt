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
    private val receiptPrintLogRepository: ReceiptPrintLogRepository,
    private val receiptQrService: ReceiptQrService
) {
    private val logger = LoggerFactory.getLogger(ImprovedReceiptService::class.java)

    @Value("\${receipt.storage.path:receipts}")
    private lateinit var storagePath: String

    // 함수형 스타일의 PDF 생성 함수
    val createReceiptPdf: PdfDocumentCreator = PdfDocumentCreator { receipt, receiptId ->
        runCatching {
            withContext(Dispatchers.IO) {
                logger.info("영수증 PDF 생성 시작: $receiptId")
                ByteArrayOutputStream().use { baos ->
                    PdfWriter(baos).use { writer ->
                        PdfDocument(writer).use { pdfDoc ->
                            Document(pdfDoc).use { document ->
                                val font: PdfFont = PdfFontFactory.createFont("Courier")
                                val boldFont: PdfFont = PdfFontFactory.createFont("Courier-Bold")

                                // 타이틀
                                document.add(
                                    Paragraph("CASH RECEIPT")
                                        .setFont(boldFont)
                                        .setFontSize(16f)
                                        .setTextAlignment(TextAlignment.CENTER)
                                )
                                document.add(Paragraph(" "))

                                // 주소 및 연락처
                                document.add(
                                    Paragraph("Address: ${receipt.address}")
                                        .setFont(font)
                                        .setFontSize(10f)
                                )
                                document.add(
                                    Paragraph("Tel: ${receipt.phoneNumber}")
                                        .setFont(font)
                                        .setFontSize(10f)
                                )

                                // 구분선
                                document.add(LineSeparator(DottedLine()))

                                // 날짜
                                val dateTime = receipt.date
                                val dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                val timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                                val dateTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                                dateTable.addCell(
                                    Cell().add(
                                        Paragraph("Date: $dateStr")
                                            .setFont(font)
                                            .setFontSize(10f)
                                    )
                                )
                                dateTable.addCell(
                                    Cell().add(
                                        Paragraph(timeStr)
                                            .setFont(font)
                                            .setFontSize(10f)
                                            .setTextAlignment(TextAlignment.RIGHT)
                                    )
                                )
                                document.add(dateTable)

                                // 구분선
                                document.add(LineSeparator(DottedLine()))

                                // 아이템 목록
                                val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                                receipt.items.forEach { item ->
                                    itemsTable.addCell(
                                        Cell().add(
                                            Paragraph(item.name)
                                                .setFont(font)
                                                .setFontSize(10f)
                                        )
                                    )
                                    itemsTable.addCell(
                                        Cell().add(
                                            Paragraph(item.price.toString())
                                                .setFont(font)
                                                .setFontSize(10f)
                                                .setTextAlignment(TextAlignment.RIGHT)
                                        )
                                    )
                                }
                                document.add(itemsTable)

                                // 구분선
                                document.add(LineSeparator(DottedLine()))

                                // 합계
                                val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                                totalTable.addCell(
                                    Cell().add(
                                        Paragraph("Total")
                                            .setFont(boldFont)
                                            .setFontSize(12f)
                                    )
                                )
                                totalTable.addCell(
                                    Cell().add(
                                        Paragraph(receipt.totalAmount.toString())
                                            .setFont(boldFont)
                                            .setFontSize(12f)
                                            .setTextAlignment(TextAlignment.RIGHT)
                                    )
                                )
                                document.add(totalTable)

                                // 현금 및 거스름돈
                                val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(75f, 25f)))
                                paymentTable.addCell(
                                    Cell().add(
                                        Paragraph("Cash")
                                            .setFont(font)
                                            .setFontSize(10f)
                                    )
                                )
                                paymentTable.addCell(
                                    Cell().add(
                                        Paragraph(receipt.cashAmount.toString())
                                            .setFont(font)
                                            .setFontSize(10f)
                                            .setTextAlignment(TextAlignment.RIGHT)
                                    )
                                )
                                paymentTable.addCell(
                                    Cell().add(
                                        Paragraph("Change")
                                            .setFont(font)
                                            .setFontSize(10f)
                                    )
                                )
                                paymentTable.addCell(
                                    Cell().add(
                                        Paragraph(receipt.changeAmount.toString())
                                            .setFont(font)
                                            .setFontSize(10f)
                                            .setTextAlignment(TextAlignment.RIGHT)
                                    )
                                )
                                document.add(paymentTable)

                                // QR 코드 추가
                                try {
                                    val qrContent = receiptQrService.generateReceiptQrCodeContent(
                                        receiptId,
                                        receipt.totalAmount.toString(),
                                        dateStr
                                    )
                                    val qrCode = receiptQrService.generateQrCode.generate(qrContent, 100, 100)
                                    val qrImage = Image(com.itextpdf.io.image.ImageDataFactory.create(qrCode))
                                        .setTextAlignment(TextAlignment.CENTER)
                                    document.add(qrImage)
                                } catch (e: Exception) {
                                    logger.error("QR 코드 생성 실패", e)
                                    document.add(
                                        Paragraph("QR Code Generation Failed")
                                            .setFont(font)
                                            .setFontSize(10f)
                                    )
                                }

                                logger.info("영수증 PDF 생성 완료: $receiptId")
                                baos.toByteArray()
                            }
                        }
                    }
                }
            }
        }.onFailure { e ->
            logger.error("PDF 생성 실패: ${e.message}", e)
            throw ReceiptError.PdfGenerationError("PDF 생성 중 오류 발생", e)
        }
    }

    // 함수형 스타일의 PDF 저장 함수
    val savePdfToStorage: PdfSaver = PdfSaver { content, filename ->
        withContext(Dispatchers.IO) {
            logger.info("영수증 PDF 저장 시작: $filename")
            val directory = Paths.get(storagePath)
            if (!Files.exists(directory)) {
                Files.createDirectories(directory)
                logger.info("저장 디렉토리 생성: $directory")
            }

            val path = directory.resolve(filename)
            try {
                Files.write(path, content)
                logger.info("영수증 PDF 저장 완료: ${path.toAbsolutePath()}")
                path
            } catch (e: Exception) {
                logger.error("PDF 저장 실패: ${e.message}", e)
                throw ReceiptError.StorageError("PDF 저장 중 오류 발생", e)
            }
        }
    }

    // PDF 생성 및 저장
    suspend fun generateAndSaveReceipt(receipt: Receipt, userId: String? = null): ReceiptResult<Pair<ByteArray, ReceiptPrintLog>> {
        val receiptId = UUID.randomUUID().toString()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "영수증_${timestamp}.pdf"

        return try {
            // PDF 생성
            val pdfContent = createReceiptPdf.createPdfDocument(receipt, receiptId).getOrThrow()

            // PDF 저장
            val savedPath = savePdfToStorage.savePdf(pdfContent, filename)

            // 로그 저장
            val printLog = ReceiptPrintLog(
                receiptId = receiptId,
                fileName = filename,
                filePath = savedPath.toString(),
                userId = userId
            )

            val savedLog = receiptPrintLogRepository.save(printLog)
            Result.success(Pair(pdfContent, savedLog))
        } catch (e: Exception) {
            when (e) {
                is ReceiptError.PdfGenerationError -> logger.error("PDF 생성 오류: ${e.message}", e)
                is ReceiptError.StorageError -> logger.error("PDF 저장 오류: ${e.message}", e)
                is ReceiptError.DatabaseError -> logger.error("로그 저장 오류: ${e.message}", e)
                else -> logger.error("영수증 처리 중 오류 발생: ${e.message}", e)
            }
            Result.failure(e)
        }
    }

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