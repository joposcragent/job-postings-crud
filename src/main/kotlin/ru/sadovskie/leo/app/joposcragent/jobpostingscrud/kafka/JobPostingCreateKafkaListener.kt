package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.logging.LogTruncate
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
		val valuePreview = LogTruncate.forLog(record.value())
		val headersPreview = formatHeaders(record)
		log.debug(
			"kafka consume: topic={} partition={} offset={} key={} type={} headers={} value={}",
			record.topic(),
			record.partition(),
			record.offset(),
			record.key(),
			type,
			headersPreview,
			valuePreview,
		)
		if (type == null) {
			log.info("kafka consume: skip job-posting-create message without type key={}", record.key())
			return
		}
		if (type != JobPostingOrchestrationMessageTypes.JOB_POSTING_CREATE_BEGIN) {
			log.info(
				"kafka consume: skip unsupported type={} key={} (expected {})",
				type,
				record.key(),
				JobPostingOrchestrationMessageTypes.JOB_POSTING_CREATE_BEGIN,
			)
			return
		}
		log.info("kafka consume: dispatch job-posting-create-begin key={}", record.key())
		processor.handle(record)
	}

	private fun parseTypeFromJson(json: String): String? =
		runCatching {
			jsonMapper.readTree(json).path("headers").path("type").asText(null)
		}.getOrNull()

	private fun formatHeaders(record: ConsumerRecord<String, String>): String {
		val parts = mutableListOf<String>()
		for (h in record.headers()) {
			val v = h.value()
			val s = if (v == null) {
				""
			} else {
				LogTruncate.forLog(String(v, Charsets.UTF_8), max = 256)
			}
			parts.add("${h.key()}=$s")
		}
		return parts.joinToString(";")
	}
}
