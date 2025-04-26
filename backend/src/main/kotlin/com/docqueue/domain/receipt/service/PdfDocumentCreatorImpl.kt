package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Base64

@Component
class PdfDocumentCreatorImpl(
    @Value("\${receipt.font:Courier}") private val fontName: String
) : PdfDocumentCreator {
    private val logger = LoggerFactory.getLogger(PdfDocumentCreatorImpl::class.java)

    private fun generateQRCodeBase64(content: String, width: Int = 100, height: Int = 100): String {
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
            val outputStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
            return Base64.getEncoder().encodeToString(outputStream.toByteArray())
        } catch (e: Exception) {
            logger.error("QR 코드 생성 실패: ${e.message}", e)
            return ""
        }
    }

    override suspend fun createPdfDocument(receipt: Receipt): ByteArray = withContext(Dispatchers.IO) {
        logger.info("PDF 생성 시작: receipt=$receipt")
        ByteArrayOutputStream().use { outputStream ->
            PdfWriter(outputStream).use { pdfWriter ->
                PdfDocument(pdfWriter).use { pdfDocument ->
                    Document(pdfDocument).use { document ->
                        val font = PdfFontFactory.createFont(fontName, PdfEncodings.UTF8)

                        logger.info("Header 추가 시작")
                        val headerElements = listOf(
                            Paragraph(Text("CASH RECEIPT").setBold())
                                .setFont(font)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(14f),
                            Paragraph("Address: ${receipt.address}")
                                .setFont(font)
                                .setFontSize(10f)
                                .setMarginTop(5f),
                            Paragraph("Tel: ${receipt.phoneNumber}")
                                .setFont(font)
                                .setFontSize(10f),
                            LineSeparator(DottedLine()).setMarginTop(5f).setMarginBottom(5f),
                            Paragraph("Date: ${receipt.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                                .setFont(font)
                                .setFontSize(10f),
                            LineSeparator(DottedLine()).setMarginTop(5f).setMarginBottom(5f)
                        )
                        headerElements.forEach { document.add(it) }
                        logger.info("Header 추가 완료")

                        // Item list
                        logger.info("Item 목록 추가 시작")
                        receipt.items.map { item ->
                            Div().setFont(font).setFontSize(10f).setMarginBottom(2f).apply {
                                add(Paragraph().apply {
                                    add(Text("${item.name.padEnd(30, ' ')}"))
                                    add(Text("${item.price}").setTextAlignment(TextAlignment.RIGHT))
                                })
                            }
                        }.forEach { document.add(it) }
                        logger.info("Item 목록 추가 완료")

                        // Total, cash, change
                        logger.info("Footer 추가 시작")
                        val footerElements = mutableListOf<IBlockElement>(
                            LineSeparator(DottedLine()).setMarginTop(5f).setMarginBottom(5f),
                            Div().setFont(font).setFontSize(12f).setMarginTop(5f).apply {
                                add(Paragraph().apply {
                                    add(Text("Total".padEnd(30, ' ')))
                                    add(Text("${receipt.totalAmount}").setTextAlignment(TextAlignment.RIGHT).setBold())
                                })
                            },
                            Div().setFont(font).setFontSize(10f).apply {
                                add(Paragraph().apply {
                                    add(Text("Cash".padEnd(30, ' ')))
                                    add(Text("${receipt.cashAmount}").setTextAlignment(TextAlignment.RIGHT))
                                })
                            },
                            Div().setFont(font).setFontSize(10f).apply {
                                add(Paragraph().apply {
                                    add(Text("Change".padEnd(30, ' ')))
                                    add(Text("${receipt.changeAmount}").setTextAlignment(TextAlignment.RIGHT))
                                })
                            }
                        )

                        // Footer 요소 추가
                        footerElements.forEach { document.add(it) }
                        logger.info("Footer 추가 완료")

                        // 동적 QR 코드 생성
                        logger.info("QR 코드 생성 시작")
                        val qrContent = "http://localhost:8080/receipts/${receipt.phoneNumber}/${receipt.date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}"
                        val dynamicQrCode = generateQRCodeBase64(qrContent)
                        if (dynamicQrCode.isNotBlank()) {
                            try {
                                val qrImageBytes = Base64.getDecoder().decode(dynamicQrCode)
                                val qrImage = Image(ImageDataFactory.create(qrImageBytes))
                                    .setWidth(100f)
                                    .setHeight(100f)
                                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                document.add(Paragraph("QR CODE")
                                    .setFont(font)
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(10f)
                                    .setMarginTop(10f))
                                document.add(qrImage)
                                logger.info("QR 코드 추가 완료")
                            } catch (e: Exception) {
                                logger.error("QR 코드 이미지 생성 실패: ${e.message}", e)
                            }
                        } else {
                            logger.warn("QR 코드 생성 실패, PDF에 QR 코드 추가 안 함")
                        }

                        // 모든 요소가 추가된 후 Document를 명시적으로 닫음
                        document.close()
                        logger.info("Document 닫힘")

                        // ByteArray 반환
                        outputStream.toByteArray().also {
                            logger.info("PDF 생성 완료: size=${it.size} bytes")
                        }
                    }
                }
            }
        }
    }
}