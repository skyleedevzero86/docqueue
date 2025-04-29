package com.docqueue.domain.dbinfo.controller

import com.docqueue.domain.dbinfo.dto.DbSizeDto
import com.docqueue.domain.dbinfo.service.DbStatusService
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DbStatusController(
    private val dbStatusService: DbStatusService
) {
    private val logger = LoggerFactory.getLogger(DbStatusController::class.java)

    @GetMapping("/db/status")
    suspend fun dbStatus(model: Model): String {
        logger.info("DB 상태 정보 요청받음")
        val statusList: List<DbSizeDto> = dbStatusService.fetchDbStatus()
            .fold(
                { error -> throw error }, // 실패 시 예외 던짐
                { flow -> flow.toList() } // 성공 시 Flow를 List로 변환
            )

        logger.debug("조회된 DB 개수: {}", statusList.size)
        model.addAttribute("dbList", statusList)
        return "domain/dbinfo/db_status"
    }
}