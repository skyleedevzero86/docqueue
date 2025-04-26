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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Controller
@RequestMapping("/receipts")
class ReceiptController(
    private val receiptService: ReceiptService
) {
    private val logger = LoggerFactory.getLogger(ReceiptController::class.java)

    @GetMapping
    fun showReceiptForm(model: Model): String {
        logger.info("GET /receipts 요청을 받음, 영수증 생성 폼 렌더링")
        // 기본 ReceiptForm 객체 추가
        val receiptForm = ReceiptForm(
            address = "1234 Lorem Ipsum, Dolor",
            phoneNumber = "123-456-7890",
            itemName = mutableListOf("Item Name"),
            itemPrice = mutableListOf("0.00"),
            totalAmount = "0.00".toBigDecimal(),
            cashAmount = "0.00".toBigDecimal(),
            changeAmount = "0.00".toBigDecimal()
        )
        model.addAttribute("receiptForm", receiptForm)

        val receipt = Receipt(
            address = receiptForm.address,
            phoneNumber = receiptForm.phoneNumber,
            date = LocalDateTime.now(),
            items = receiptForm.itemName.zip(receiptForm.itemPrice) { name, price ->
                ReceiptItem(name, price.toBigDecimal())
            },
            totalAmount = receiptForm.totalAmount,
            cashAmount = receiptForm.cashAmount,
            changeAmount = receiptForm.changeAmount
        )
        model.addAttribute("receipt", receipt)
        return "domain/receipt/print"
    }

    @PostMapping("/generate")
    suspend fun generateReceipt(
        @ModelAttribute receiptForm: ReceiptForm
    ): ResponseEntity<Any> {
        logger.info("POST /receipts/generate 요청을 받음, 폼 데이터: $receiptForm")
        try {
            val items = receiptForm.itemName.zip(receiptForm.itemPrice) { name, price ->
                ReceiptItem(name, price.toBigDecimal())
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
            val outcome = receiptService.generateAndSaveReceipt(receipt)
            return when (outcome) {
                is ReceiptService.ReceiptOutcome.Success -> {
                    val (pdfContent, metadata) = outcome.output
                    logger.info("영수증 생성 성공, 파일명: ${metadata.fileName}")
                    val resource = ByteArrayResource(pdfContent)
                    val headers = HttpHeaders()
                    val encodedFileName = URLEncoder.encode(metadata.fileName, StandardCharsets.UTF_8.toString())
                        .replace("+", "%20")
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.fileName}\"; filename*=UTF-8''${encodedFileName}")
                    ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfContent.size.toLong())
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource)
                }
                is ReceiptService.ReceiptOutcome.Failure -> {
                    logger.error("PDF 생성 실패: ${outcome.error.message ?: "알 수 없는 오류"}", outcome.error)
                    ResponseEntity.status(500)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("error" to "PDF 생성 중 오류 발생: ${outcome.error.message ?: "알 수 없는 오류"}"))
                }
            }
        } catch (e: Exception) {
            logger.error("영수증 생성 요청 처리 중 오류 발생: ${e.message ?: "알 수 없는 오류"}", e)
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("error" to "서버 내부 오류: ${e.message ?: "알 수 없는 오류"}"))
        }
    }
}