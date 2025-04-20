package com.docqueue

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@SpringBootApplication
class BackendApplication {

	@Bean
	fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
		return CommandLineRunner {
			println("Spring Boot가 자동으로 등록:")
			ctx.beanDefinitionNames.sorted().forEach { println(it) }
		}
	}
}

fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}