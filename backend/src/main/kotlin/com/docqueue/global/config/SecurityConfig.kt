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
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/v1/queue/**","/waiting-room", "/login","/","/index","/home","/logout",
                        "/images/**","/gen/images/**","/*.jpg", "/*.png", "/*.jpeg","/db/status",
                        "/receipts/**","/*.css","/*.js", "/*.gif","/*.svg","/*.woff","/*.woff2","/*.ttf", "/*.eot"
                    ).permitAll()
                    //.pathMatchers("/login","/","/index","/home","/logout", "/receipts", "/images/**", "/gen/images/**", "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.woff", "/*.woff2", "/*.ttf", "/*.eot").permitAll()
                    //.pathMatchers("/receipts/**","/api/v1/queue/**","/db/status", "/waiting-room").authenticated()
                    .anyExchange().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login")
            }
            .logout { logout ->
                logout.logoutUrl("/logout")
            }
            .csrf { csrf ->
                csrf.disable()
            }
            .build()
    }
}


