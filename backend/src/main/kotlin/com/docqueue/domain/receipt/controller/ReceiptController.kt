package com.docqueue.domain.receipt.controller

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

@Controller
@RequestMapping("/receipts")
class ReceiptController(
    private val receiptService: ReceiptService
) {
    private val logger = LoggerFactory.getLogger(ReceiptController::class.java)

    // 영수증 페이지 표시
    @GetMapping
    suspend fun showReceiptPage(model: Model): String {
        val sampleReceipt = createSampleReceipt()
        model.addAttribute("receipt", sampleReceipt)
        return "domain/receipt/print"
    }

    // PDF 다운로드
    @PostMapping("/generate")
    suspend fun generateReceipt(
        @RequestParam address: String,
        @RequestParam phoneNumber: String,
        @RequestParam("itemName") itemNames: List<String>,
        @RequestParam("itemPrice") itemPrices: List<String>,
        @RequestParam totalAmount: BigDecimal,
        @RequestParam cashAmount: BigDecimal,
        @RequestParam changeAmount: BigDecimal
    ): ResponseEntity<ByteArrayResource> {
        try {
            val items = itemNames.zip(itemPrices) { name, price ->
                ReceiptItem(name, BigDecimal(price))
            }

            val receipt = Receipt(
                address = address,
                phoneNumber = phoneNumber,
                date = LocalDateTime.now(),
                items = items,
                totalAmount = totalAmount,
                cashAmount = cashAmount,
                changeAmount = changeAmount
            )

            return receiptService.generateAndSaveReceipt(receipt)
                .map { (pdfContent, log) ->
                    val resource = ByteArrayResource(pdfContent)

                    val headers = HttpHeaders()
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${log.fileName}")

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
            logger.error("영수증 생성 요청 처리 중 오류: ${e.message}", e)
            return ResponseEntity.status(500)
                .body(ByteArrayResource("서버 내부 오류: ${e.message}".toByteArray()))
        }
    }

    // 샘플 영수증 생성 함수
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