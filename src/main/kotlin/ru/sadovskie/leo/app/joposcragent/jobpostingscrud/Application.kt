package ru.sadovskie.leo.app.joposcragent.jobpostingscrud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class JobPostingsCrudApplication

fun main(args: Array<String>) {
	runApplication<JobPostingsCrudApplication>(*args)
}
