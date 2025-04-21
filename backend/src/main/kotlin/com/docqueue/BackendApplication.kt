package com.docqueue

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BackendApplication {

	@Bean
	fun commandLineRunner(ctx: ApplicationContext): CommandLineRunner {
		return CommandLineRunner {
			println("Spring Boot가 자동으로 등록:")
			ctx.beanDefinitionNames.sorted().forEach { println(it) }
		}
	}
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<BackendApplication>(*args)
		}
	}

}


/*
fun main(args: Array<String>) {
	runApplication<BackendApplication>(*args)
}
*/
