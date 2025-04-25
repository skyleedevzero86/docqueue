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
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode
import java.io.IOException

@Configuration
@EnableWebFlux
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

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        println("Adding resource handler: /images/** -> classpath:/static/gen/images/")
        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/gen/images/")

        println("Adding resource handler: /*.jpg, /*.png, /*.jpeg -> classpath:/static/gen/images/")
        registry.addResourceHandler("/*.jpg", "/*.png", "/*.jpeg")
            .addResourceLocations("classpath:/static/gen/images/")

        println("Adding resource handler: /static/** -> classpath:/static/")
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
    }

    @Bean
    fun templateResolver(): SpringResourceTemplateResolver {
        val resolver = SpringResourceTemplateResolver()
        resolver.prefix = "classpath:/templates/"
        resolver.suffix = ".html"
        resolver.templateMode = TemplateMode.HTML
        resolver.characterEncoding = "UTF-8"
        resolver.isCacheable = false
        return resolver
    }

    @Bean
    fun templateEngine(): SpringWebFluxTemplateEngine {
        val engine = SpringWebFluxTemplateEngine()
        engine.setTemplateResolver(templateResolver())
        return engine
    }

    @Bean
    fun encodingFilter(): WebFilter {
        return WebFilter { exchange: ServerWebExchange, chain: WebFilterChain ->
            val contentType = exchange.response.headers["Content-Type"]?.firstOrNull()
            if (contentType == null) {
                exchange.response.headers.add("Content-Type", "text/html;charset=UTF-8")
            } else if (contentType.contains("application/json")) {
                exchange.response.headers.set("Content-Type", "application/json;charset=UTF-8")
            }
            chain.filter(exchange)
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