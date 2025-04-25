package com.docqueue.global.config

import jakarta.annotation.PostConstruct
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException

@Configuration
class AppConfig {

    @Value("\${custom.site.name}")
    lateinit var siteName: String

    @Value("\${queue.api.base-url}")
    private lateinit var baseUrl: String

    @PostConstruct
    fun init() {
        instance = this
    }

    @Bean
    fun webClient(): WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    // UTF-8 인코딩 필터 추가
    @Bean
    fun characterEncodingFilter(): Filter = object : Filter {
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            request.characterEncoding = "UTF-8"
            val httpResponse = response as HttpServletResponse
            httpResponse.characterEncoding = "UTF-8" // 응답 인코딩 설정
            httpResponse.setContentType("application/json;charset=UTF-8")
            chain.doFilter(request, response)
        }
    }

    companion object {
        private lateinit var instance: AppConfig

        fun getResourcesStaticDirPath(): String {
            val resource = ClassPathResource("static/")
            return try {
                resource.file.absolutePath
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}