package com.docqueue.domain.receipt.controller

import com.docqueue.domain.receipt.dto.ReceiptForm
import com.docqueue.domain.receipt.entity.Receipt
import com.docqueue.domain.receipt.entity.ReceiptItem
import com.docqueue.domain.receipt.service.ReceiptService
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Controller
@RequestMapping("/receipts")
class ReceiptController(
    private val receiptService: ReceiptService
) {
    private val logger = LoggerFactory.getLogger(ReceiptController::class.java)

    @GetMapping
    suspend fun showReceiptPage(model: Model): String {
        logger.info("영수증 페이지 렌더링 중")
        val sampleReceipt = createSampleReceipt()
        model.addAttribute("receipt", sampleReceipt)
        return "domain/receipt/print"
    }

    @GetMapping("/generate")
    suspend fun handleInvalidGenerateRequest(): ResponseEntity<String> {
        logger.warn("잘못된 GET 요청 /receipts/generate")
        return ResponseEntity.status(405)
            .body("허용되지 않는 메서드: 영수증 생성을 위해 POST 요청을 사용하세요")
    }

    @PostMapping("/generate")
    suspend fun generateReceipt(
        @ModelAttribute receiptForm: ReceiptForm
    ): ResponseEntity<ByteArrayResource> {
        logger.info("POST /receipts/generate 요청을 받음, 폼 데이터: $receiptForm")
        try {
            val items = receiptForm.itemName.zip(receiptForm.itemPrice) { name, price ->
                ReceiptItem(name, BigDecimal(price))
            }

            val receipt = Receipt(
                address = receiptForm.address,
                phoneNumber = receiptForm.phoneNumber,
                date = LocalDateTime.now(),
                items = items,
                totalAmount = receiptForm.totalAmount,
                cashAmount = receiptForm.cashAmount,
                changeAmount = receiptForm.changeAmount
            )

            return receiptService.generateAndSaveReceipt(receipt)
                .map { (pdfContent, log) ->
                    logger.info("영수증 생성 성공, 파일명: ${log.fileName}")
                    val resource = ByteArrayResource(pdfContent)

                    val headers = HttpHeaders()
                    val encodedFileName = URLEncoder.encode(log.fileName, StandardCharsets.UTF_8.toString())
                        .replace("+", "%20")
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${log.fileName}\"; filename*=UTF-8''${encodedFileName}")

                    ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfContent.size.toLong())
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource)
                }
                .getOrElse { e ->
                    logger.error("PDF 생성 실패: ${e.message}", e)
                    ResponseEntity.status(500)
                        .body(ByteArrayResource("PDF 생성 중 오류 발생: ${e.message}".toByteArray()))
                }
        } catch (e: Exception) {
            logger.error("영수증 생성 요청 처리 중 오류 발생: ${e.message}", e)
            return ResponseEntity.status(500)
                .body(ByteArrayResource("서버 내부 오류: ${e.message}".toByteArray()))
        }
    }

    private fun createSampleReceipt(): Receipt {
        val items = listOf(
            ReceiptItem("Lorem", BigDecimal("6.50")),
            ReceiptItem("Ipsum", BigDecimal("7.50")),
            ReceiptItem("Dolor Sit", BigDecimal("48.00")),
            ReceiptItem("Amet", BigDecimal("9.30")),
            ReceiptItem("Consectetur", BigDecimal("11.90")),
            ReceiptItem("Adipiscing Elit", BigDecimal("1.20")),
            ReceiptItem("Sed Do", BigDecimal("0.40"))
        )

        val total = BigDecimal("84.80")
        val cash = BigDecimal("100.00")
        val change = BigDecimal("15.20")

        return Receipt(
            address = "1234 Lorem Ipsum, Dolor",
            phoneNumber = "123-456-7890",
            date = LocalDateTime.now(),
            items = items,
            totalAmount = total,
            cashAmount = cash,
            changeAmount = change
        )
    }
}