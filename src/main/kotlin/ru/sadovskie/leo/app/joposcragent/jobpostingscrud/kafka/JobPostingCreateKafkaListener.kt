package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class JobPostingCreateKafkaListener(
	private val jsonMapper: JsonMapper,
	private val processor: JobPostingCreateKafkaProcessor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = [JobPostingOrchestrationKafkaTopics.JOB_POSTING_CREATE],
		groupId = "\${app.kafka.job-posting-create-consumer-group}",
	)
	fun onMessage(record: ConsumerRecord<String, String>) {
		val type = record.headers().lastHeader("type")?.value()?.toString(Charsets.UTF_8)
			?: record.value()?.let { parseTypeFromJson(it) }
			?: return
		if (type != JobPostingOrchestrationMessageTypes.JOB_POSTING_CREATE_BEGIN) {
			return
		}
		processor.handle(record)
	}

	private fun parseTypeFromJson(json: String): String? =
		runCatching {
			jsonMapper.readTree(json).path("headers").path("type").asText(null)
		}.getOrNull()
}
