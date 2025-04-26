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
                    .pathMatchers("/api/v1/queue/**","/waiting-room", "/login","/","/index","/home","/logout",
                        "/images/**","/gen/images/**","/*.jpg", "/*.png", "/*.jpeg",
                        "/receipts/**","/*.css","/*.js", "/*.gif","/*.svg","/*.woff","/*.woff2","/*.ttf", "/*.eot"
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login")
            }
            .build()
    }
}