package com.docqueue.domain.dbinfo.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.docqueue.domain.dbinfo.dto.DbSizeDto
import com.docqueue.domain.dbinfo.repository.DbStatusRepository
import com.docqueue.global.exception.ApplicationException
import com.docqueue.global.util.mapNotNull
import com.docqueue.global.util.retry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class DbStatusService(
    private val dbStatusRepository: DbStatusRepository
) {
    // Arrow Either를 사용한 FP 스타일 fetchDbStatus
    suspend fun fetchDbStatus(): Either<ApplicationException, Flow<DbSizeDto>> =
        try {
            retry(times = 3) {
                dbStatusRepository.getDbSizeInfo()
                    .map { it } // 필요 시 변환 로직 추가
                    .mapNotNull { it }
            }.right()
        } catch (e: Exception) {
            ApplicationException(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                code = "DB-ERROR",
                reason = "DB 조회 실패: ${e.message}"
            ).left()
        }

    // 기존 fetchDbStatus (선택적으로 유지)
    suspend fun fetchDbStatusLegacy(transform: (DbSizeDto) -> DbSizeDto = { it }): Flow<DbSizeDto> =
        retry(times = 3) {
            dbStatusRepository.getDbSizeInfo()
                .map { transform(it) }
                .mapNotNull { it }
        }
}