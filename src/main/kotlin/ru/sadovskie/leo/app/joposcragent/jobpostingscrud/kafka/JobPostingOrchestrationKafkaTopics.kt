package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

object JobPostingOrchestrationKafkaTopics {
	const val JOB_POSTING_CREATE = "async-job.job-posting-create"
}

object JobPostingOrchestrationMessageTypes {
	const val JOB_POSTING_CREATE_BEGIN = "async-job.job-posting-create-begin"
	const val JOB_POSTING_CREATE_RESULT = "async-job.job-posting-create-result"
}

object JobPostingOrchestrationKafkaConstants {
	const val SCHEMA_VERSION = "1.0"
}
