package com.docqueue.domain.dbinfo.controller

import com.docqueue.domain.dbinfo.dto.DbSizeDto
import com.docqueue.domain.dbinfo.dto.HealthResponseDto
import com.docqueue.domain.dbinfo.service.DbStatusService
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.client.RestTemplate

@Controller
class DbStatusController(
    private val dbStatusService: DbStatusService,
    private val restTemplate: RestTemplate

) {
    private val logger = LoggerFactory.getLogger(DbStatusController::class.java)

    @GetMapping("/db/status")
    suspend fun dbStatus(model: Model): String {
        logger.info("DB 상태 정보 요청받음")

        val statusList: List<DbSizeDto> = dbStatusService.fetchDbStatus()
            .fold(
                { error -> throw error },
                { flow -> flow.toList() }
            )

        val actuatorResponse = restTemplate.getForObject(
            "http://localhost:8080/actuator/health", // 필요시 포트 변경
            HealthResponseDto::class.java
        )

        val components = actuatorResponse?.components ?: emptyMap()

        logger.debug("조회된 DB 개수: {}", statusList.size)
        model.addAttribute("dbList", statusList)
        model.addAttribute("healthComponents", components)
        model.addAttribute("overallStatus", actuatorResponse?.status ?: "UNKNOWN")

        return "domain/dbinfo/db_status"
    }

}