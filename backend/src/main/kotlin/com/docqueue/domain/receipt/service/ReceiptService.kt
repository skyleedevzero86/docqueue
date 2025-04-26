package com.docqueue.domain.receipt.service

import com.docqueue.domain.receipt.entity.Receipt
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.format.DateTimeFormatter

@Service
class ReceiptService(
    @Value("\${receipt.storage.path}") private val storagePath: String
) {
    private val logger = LoggerFactory.getLogger(ReceiptService::class.java)

    data class ReceiptLog(val fileName: String)
    data class ReceiptResult(val pdfContent: ByteArray, val log: ReceiptLog)

    suspend fun generateAndSaveReceipt(receipt: Receipt): Result<ReceiptResult> = withContext(Dispatchers.IO) {
        try {
            logger.info("현재 작업 디렉토리: ${System.getProperty("user.dir")}")

            val outputStream = ByteArrayOutputStream()
            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Monospace 폰트 설정 (Courier 사용, iText 기본 제공)
            val font: PdfFont = PdfFontFactory.createFont("Courier", PdfEncodings.UTF8)

            // 헤더 (CASH RECEIPT)
            document.add(
                Paragraph(Text("CASH RECEIPT").setBold())
                    .setFont(font)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
            )

            // 주소 및 전화번호
            document.add(
                Paragraph("Address: ${receipt.address}")
                    .setFont(font)
                    .setFontSize(10f)
                    .setMarginTop(5f)
            )
            document.add(
                Paragraph("Tel: ${receipt.phoneNumber}")
                    .setFont(font)
                    .setFontSize(10f)
            )

            // 점선 구분선
            document.add(
                LineSeparator(DottedLine())
                    .setMarginTop(5f)
                    .setMarginBottom(5f)
            )

            // 날짜
            document.add(
                Paragraph("Date: ${receipt.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                    .setFont(font)
                    .setFontSize(10f)
            )

            // 점선 구분선
            document.add(
                LineSeparator(DottedLine())
                    .setMarginTop(5f)
                    .setMarginBottom(5f)
            )

            // 항목 목록 (고정 폭으로 좌우 정렬)
            receipt.items.forEach { item ->
                val itemDiv = Div()
                    .setFont(font)
                    .setFontSize(10f)
                    .setMarginBottom(2f)

                val itemText = Paragraph()
                val itemName = "${item.name.padEnd(30, ' ')}"
                itemText.add(Text(itemName))
                itemText.add(Text("${item.price}").setTextAlignment(TextAlignment.RIGHT))
                itemDiv.add(itemText)

                document.add(itemDiv)
            }

            // 점선 구분선
            document.add(
                LineSeparator(DottedLine())
                    .setMarginTop(5f)
                    .setMarginBottom(5f)
            )

            // 총액 (고정 폭으로 좌우 정렬)
            val totalDiv = Div()
                .setFont(font)
                .setFontSize(12f)
                .setMarginTop(5f)
            totalDiv.add(
                Paragraph()
                    .add(Text("Total".padEnd(30, ' ')))
                    .add(Text("${receipt.totalAmount}").setTextAlignment(TextAlignment.RIGHT).setBold())
                    .setFont(font)
            )
            document.add(totalDiv)

            // 현금 및 거스름돈 (고정 폭으로 좌우 정렬)
            val cashDiv = Div()
                .setFont(font)
                .setFontSize(10f)
            cashDiv.add(
                Paragraph()
                    .add(Text("Cash".padEnd(30, ' ')))
                    .add(Text("${receipt.cashAmount}").setTextAlignment(TextAlignment.RIGHT))
                    .setFont(font)
            )
            document.add(cashDiv)

            val changeDiv = Div()
                .setFont(font)
                .setFontSize(10f)
            changeDiv.add(
                Paragraph()
                    .add(Text("Change".padEnd(30, ' ')))
                    .add(Text("${receipt.changeAmount}").setTextAlignment(TextAlignment.RIGHT))
                    .setFont(font)
            )
            document.add(changeDiv)

            // QR 코드 추가
            document.add(
                Paragraph("QR CODE")
                    .setFont(font)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f)
                    .setMarginTop(10f)
            )

            // HTML에 있는 QR 코드 이미지 데이터를 사용 (Base64 디코딩 후 삽입)
            val qrCodeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAABlBMVEX///8AAABVwtN+AAAA8ElEQVR4nO3VsQ2EAAwAQd7L4PT/r8VmDXQYKVOschlj5lrH7LXWnPPsexwze39/f39/f39/f39//3f9b/KfJ/n7+/v7+/v7+/v7+/v7v++/kv/7if/+/v7+/v7+/v7+/v7+/m/+/v7+/v7+/v7+/v7+7/v/7e/v7+/v7+/v7+/v7/++/+3/3+Tv7+/v7+/v7+/v7+/v//7/r7+/v7+/v7+/v7+/v//7/if5+/v7+/v7+/v7+/v7+7/vv5O/v7+/v7+/v7+/v7//+/4r+c/f39/f39/f39/f39//ff+d/I/f39/f39/f39/f39//Myf1AVGUDYpwRaLaAAAAAElFTkSuQmCC"
            val imageData = java.util.Base64.getDecoder().decode(qrCodeBase64)
            val image = Image(ImageDataFactory.create(imageData))
                .setWidth(100f)
                .setHeight(100f)
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
            document.add(image)

            document.close()

            // 파일 저장
            val fileName = "영수증_${receipt.date.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
            val storageDir = Paths.get(storagePath)
            if (!Files.exists(storageDir)) {
                withContext(Dispatchers.IO) {
                    try {
                        Files.createDirectories(storageDir)
                        logger.info("저장 디렉토리가 생성되었습니다: $storageDir")
                    } catch (e: IOException) {
                        logger.error("저장 디렉토리 생성에 실패했습니다: $storageDir", e)
                        throw RuntimeException("저장 디렉토리를 생성할 수 없습니다: $storageDir", e)
                    }
                }
            }

            val filePath = storageDir.resolve(fileName)
            Files.write(filePath, outputStream.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)

            logger.info("PDF저장 확인: $filePath")

            Result.success(ReceiptResult(outputStream.toByteArray(), ReceiptLog(fileName)))
        } catch (e: Exception) {
            logger.error("PDF 생성 또는 저장 중 오류가 발생했습니다: ${e.message}", e)
            Result.failure(e)
        }
    }
}