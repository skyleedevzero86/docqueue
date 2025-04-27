package com.docqueue.domain.receipt.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class ReceiptQrService {
    // 함수형 인터페이스로 QR 코드 생성 기능 정의
    fun interface QrGenerator {
        suspend fun generate(content: String, width: Int, height: Int): ByteArray
    }

    // QR 코드 생성 구현
    val generateQrCode: QrGenerator = QrGenerator { content, width, height ->
        withContext(Dispatchers.IO) {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
            val image = MatrixToImageWriter.toBufferedImage(bitMatrix)

            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", baos)
            baos.toByteArray()
        }
    }

    // 영수증 정보로 QR 코드 컨텐츠 생성
    suspend fun generateReceiptQrCodeContent(receiptId: String, totalAmount: String, date: String): String {
        return "$receiptId|$totalAmount|$date"
    }
}
