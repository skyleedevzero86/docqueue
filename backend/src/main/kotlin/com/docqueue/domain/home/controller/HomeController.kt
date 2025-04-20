package com.docqueue.domain.home.controller

import com.docqueue.domain.home.service.HomeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import com.docqueue.domain.home.dto.AllowedUserResponse
import jakarta.servlet.http.HttpServletRequest

@Controller
class HomeController(private val homeService: HomeService) {

    @GetMapping("/home")
    suspend fun getHome(
        @RequestParam(name = "queue", defaultValue = "default") queue: String,
        @RequestParam(name = "user-id") userId: Long,
        request: HttpServletRequest
    ): String {
        // 토큰 추출
        val token = getTokenFromRequest(queue, request)

        // 외부 API 호출 및 사용자 허용 여부 확인
        val allowedUserResponse: AllowedUserResponse = withContext(Dispatchers.IO) {
            homeService.getAllowedUserResponse(queue, userId, token).single()
        }

        return if (allowedUserResponse.isAllowed) {
            "home"
        } else {
            "redirect:/waiting-room?user-id=$userId"
        }
    }

    private fun getTokenFromRequest(queue: String, request: HttpServletRequest): String {
        val cookies = request.cookies ?: emptyArray()
        val cookieName = "user-queue-$queue-token"
        return cookies.firstOrNull { it.name.equals(cookieName, ignoreCase = true) }?.value ?: ""
    }
}