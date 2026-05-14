package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.kafka")
data class JobPostingKafkaProperties(
	val enabled: Boolean = true,
	val jobPostingCreateConsumerGroup: String = "job-postings-crud-job-posting-create",
)
