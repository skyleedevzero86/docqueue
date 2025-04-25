package com.docqueue.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/v1/queue/**").permitAll()
                    .pathMatchers("/waiting-room").permitAll()
                    .pathMatchers("/index").permitAll()
                    .pathMatchers("/home").permitAll()
                    .pathMatchers("/login").permitAll() // 로그인 페이지 접근 허용
                    .anyExchange().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login") // 로그인 페이지 경로 설정
            }
            .build()
    }
}