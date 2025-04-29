package com.docqueue.domain.receipt.controller

import com.docqueue.domain.receipt.dto.ReceiptForm
import com.docqueue.domain.receipt.dto.ReceiptOutcome
import com.docqueue.domain.receipt.entity.Receipt
import com.docqueue.domain.receipt.entity.ReceiptItem
import com.docqueue.domain.receipt.entity.ReceiptPrintLog
import com.docqueue.domain.receipt.service.ReceiptService
import com.docqueue.global.exception.ApplicationException
import com.docqueue.global.exception.ErrorCode
import java.util.UUID
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource

@Controller
@RequestMapping("/receipts")
class ReceiptController(
    private val receiptService: ReceiptService
) {
    private val logger = LoggerFactory.getLogger(ReceiptController::class.java)
    @GetMapping
    fun showReceiptForm(): String {
        return "domain/receipt/main"
    }
    @GetMapping("/print")
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
        @ModelAttribute receiptForm: ReceiptForm,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<Any> {
        logger.info("POST /receipts/generate 요청을 받음, 폼 데이터: $receiptForm")
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

        val userId = userDetails?.username
        val outcome = receiptService.generateAndSaveReceipt(receipt, userId)
        return when (outcome) {
            is ReceiptOutcome.Success -> {
                val output = outcome.output
                logger.info("영수증 생성 성공, 파일명: ${output.metadata.fileName}, 로그 저장됨: ${output.printLog.receiptId}")
                val resource = ByteArrayResource(output.content)
                val headers = HttpHeaders()
                val encodedFileName = URLEncoder.encode(output.metadata.fileName, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${output.metadata.fileName}\"; filename*=UTF-8''${encodedFileName}")
                ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(output.content.size.toLong())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource)
            }
            is ReceiptOutcome.Failure -> {
                logger.error("PDF 생성 실패: ${outcome.error.message ?: "알 수 없는 오류"}", outcome.error)
                throw ErrorCode.RECEIPT_PDF_GENERATION_FAILED.build(outcome.error.message)
            }
        }
    }

    // 영수증 로그 페이지
    @GetMapping("/logs")
    suspend fun showLogsPage(
        model: Model,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): String {
        logger.info("GET /receipts/logs 요청, userId={}", userDetails?.username ?: "Anonymous")
        /*val userId = userDetails?.username ?: throw ApplicationException(
            httpStatus = HttpStatus.UNAUTHORIZED,
            code = "AUTH-0001",
            reason = "User must be authenticated to view receipt logs"
        )*/

        val userId = userDetails?.username ?: "Anonymous"
        logger.info("GET /receipts/logs 요청, userId={}", userId)

        val logs = receiptService.getReceiptLogsByUser(userId)
        model.addAttribute("logs", logs)
        return "domain/receipt/receipt_log"
    }

    // 날짜별 로그 조회 API
    @GetMapping("/by-date", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    suspend fun getLogsByDate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<ReceiptPrintLog> {
        val start = LocalDateTime.of(date, LocalTime.MIN)
        val end = LocalDateTime.of(date, LocalTime.MAX)
        return receiptService.getReceiptLogsByDateRange(start, end).toList()
    }

    // 기간별 로그 조회 API
    @GetMapping("/by-date-range", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    suspend fun getLogsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<ReceiptPrintLog> {
        val start = LocalDateTime.of(startDate, LocalTime.MIN)
        val end = LocalDateTime.of(endDate, LocalTime.MAX)
        return receiptService.getReceiptLogsByDateRange(start, end).toList()
    }


    @GetMapping("/download/{id}")
    suspend fun downloadReceipt(
        @PathVariable id: String,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<Resource> {
        logger.info("GET /receipts/download/{} 요청, userId={}", id, userDetails?.username ?: "Anonymous")
        val userId = userDetails?.username ?: "Anonymous"

        val log = receiptService.getReceiptLogByIdAndUser(id, userId)
            ?: throw ApplicationException(
                httpStatus = HttpStatus.NOT_FOUND,
                code = "RECEIPT-0002",
                reason = "Receipt log with id $id not found for user $userId"
            )

        val filePath = Paths.get(log.filePath)
        if (!Files.exists(filePath)) {
            logger.error("파일이 존재하지 않습니다: {}", log.filePath)
            throw ApplicationException(
                httpStatus = HttpStatus.NOT_FOUND,
                code = "RECEIPT-0003",
                reason = "File not found at ${log.filePath}"
            )
        }

        val resource = UrlResource(filePath.toUri())
        val encodedFileName = URLEncoder.encode(log.fileName, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${log.fileName}\"; filename*=UTF-8''${encodedFileName}")
        headers.contentType = MediaType.APPLICATION_PDF

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(Files.size(filePath))
            .body(resource)
    }

}