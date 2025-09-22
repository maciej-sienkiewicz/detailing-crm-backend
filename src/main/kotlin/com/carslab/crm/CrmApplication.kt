package com.carslab.crm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
class CrmApplication

fun main(args: Array<String>) {
	runApplication<CrmApplication>(*args)
}
