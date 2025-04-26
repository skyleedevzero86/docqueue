package com.docqueue.domain.receipt.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class PdfSaverImpl(
    @Value("\${receipt.storage.path}") private val storagePath: String

) : PdfSaver {

    private val logger: Logger = LoggerFactory.getLogger(PdfSaverImpl::class.java)

    override suspend fun savePdf(content: ByteArray, filename: String): Path = withContext(Dispatchers.IO) {
        val storageDir = Paths.get(storagePath)
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir)
            logger.info("저장 디렉토리 생성: $storageDir")
        }

        val filePath = storageDir.resolve(filename)
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        logger.info("PDF 저장 완료: $filePath")
        filePath
    }
}