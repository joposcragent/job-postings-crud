package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class JobPostingCreateResultPublisher(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val jsonMapper: JsonMapper,
) {

	fun publishSucceeded(jobUuid: UUID, messageKey: String, jobPostingUuid: UUID) {
		publish(
			jobUuid = jobUuid,
			messageKey = messageKey,
			status = "SUCCEEDED",
			result = mapOf("message" to "Вакансия сохранена"),
			jobPostingUuid = jobPostingUuid,
		)
	}

	fun publishFailed(jobUuid: UUID, messageKey: String, errorMessage: String) {
		publish(
			jobUuid = jobUuid,
			messageKey = messageKey,
			status = "FAILED",
			result = mapOf("message" to errorMessage),
			jobPostingUuid = null,
		)
	}

	private fun publish(
		jobUuid: UUID,
		messageKey: String,
		status: String,
		result: Any?,
		jobPostingUuid: UUID?,
	) {
		val createdAt = OffsetDateTime.now(ZoneOffset.UTC).toString()
		val payload = LinkedHashMap<String, Any?>()
		payload["jobUuid"] = jobUuid.toString()
		payload["status"] = status
		payload["result"] = result
		if (jobPostingUuid != null && status == "SUCCEEDED") {
			payload["jobPostingUuid"] = jobPostingUuid.toString()
		}
		val json = jsonMapper.writeValueAsString(payload)
		val headers = listOf(
			RecordHeader("key", messageKey.toByteArray(StandardCharsets.UTF_8)),
			RecordHeader("type", JobPostingOrchestrationMessageTypes.JOB_POSTING_CREATE_RESULT.toByteArray(StandardCharsets.UTF_8)),
			RecordHeader("createdAt", createdAt.toByteArray(StandardCharsets.UTF_8)),
			RecordHeader(
				"schemaVersion",
				JobPostingOrchestrationKafkaConstants.SCHEMA_VERSION.toByteArray(StandardCharsets.UTF_8),
			),
		)
		val record = ProducerRecord(
			JobPostingOrchestrationKafkaTopics.JOB_POSTING_CREATE,
			null,
			messageKey,
			json,
			headers,
		)
		kafkaTemplate.send(record).get()
	}
}
