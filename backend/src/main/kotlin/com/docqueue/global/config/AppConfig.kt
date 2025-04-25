package com.docqueue.global.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.io.IOException

@Configuration
//@EnableWebFlux
class AppConfig : WebFluxConfigurer {

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

    @Bean
    fun encodingFilter(): WebFilter {
        return WebFilter { exchange: ServerWebExchange, chain: WebFilterChain ->
            exchange.response.beforeCommit {
                Mono.fromRunnable {
                    val contentType = exchange.response.headers["Content-Type"]?.firstOrNull()
                    if (contentType == null) {
                        exchange.response.headers.add("Content-Type", "application/json;charset=UTF-8")
                    } else if (contentType.contains("application/json") && !contentType.contains("charset")) {
                        exchange.response.headers.set("Content-Type", "application/json;charset=UTF-8")
                    } else if (contentType.contains("text/html") && !contentType.contains("charset")) {
                        exchange.response.headers.set("Content-Type", "text/html;charset=UTF-8")
                    }
                }
            }
            chain.filter(exchange)
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/gen/images/")

        registry.addResourceHandler("/*.jpg", "/*.png", "/*.jpeg")
            .addResourceLocations("classpath:/static/gen/images/")

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
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