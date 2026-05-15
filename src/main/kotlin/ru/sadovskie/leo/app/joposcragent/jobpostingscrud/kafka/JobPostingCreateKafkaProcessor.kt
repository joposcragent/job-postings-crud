package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.logging.LogTruncate
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@Service
@ConditionalOnProperty(prefix = "app.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class JobPostingCreateKafkaProcessor(
	private val repository: PostingRepository,
	private val resultPublisher: JobPostingCreateResultPublisher,
	private val jsonMapper: JsonMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun handle(record: ConsumerRecord<String, String>) {
		val root = runCatching { jsonMapper.readTree(record.value()) }.getOrElse {
			log.warn("job-posting-create-begin: invalid json: {}", it.message)
			return
		}
		val payload = root.kafkaMessagePayloadOrNull() ?: run {
			log.warn("job-posting-create-begin: missing or invalid body")
			return
		}
		if (log.isDebugEnabled) {
			val payloadPreview = LogTruncate.forLog(jsonMapper.writeValueAsString(payload))
			log.debug("job-posting-create-begin: parsed envelope key={} payload={}", record.key(), payloadPreview)
		}
		try {
			val parsed = JobPostingCreateBeginPayloadMapper.parse(record.key(), payload)
			parsed.softWarning?.let { warn ->
				log.warn("job-posting-create-begin: {}", warn)
			}
			persistAndPublish(parsed)
		} catch (e: JobPostingCreateBeginPayloadException) {
			log.error("job-posting-create-begin: {}", e.reason, e)
			val jobUuid = e.jobUuid
			if (jobUuid != null) {
				resultPublisher.publishFailed(jobUuid, e.messageKey, e.reason)
			}
		}
	}

	private fun persistAndPublish(parsed: BeginPayloadParseResult) {
		val jobPostingUuid = parsed.jobPostingUuid
		val item = parsed.item
		val jobUuid = parsed.jobUuid
		val messageKey = parsed.messageKey

		try {
			repository.insert(jobPostingUuid, item)
			resultPublisher.publishSucceeded(jobUuid, messageKey, jobPostingUuid)
			log.info(
				"job-posting-create-begin: inserted and published succeeded jobUuid={} jobPostingUuid={} messageKey={}",
				jobUuid,
				jobPostingUuid,
				messageKey,
			)
		} catch (e: Exception) {
			log.error("job-posting-create-begin: insert failed jobUuid={}", jobUuid, e)
			resultPublisher.publishFailed(
				jobUuid,
				messageKey,
				e.message ?: e.toString(),
			)
		}
	}
}

private fun JsonNode.kafkaMessagePayloadOrNull(): JsonNode? {
	val headers = get("headers")
	val payload = get("payload")
	return when {
		headers != null && headers.isObject && payload != null && payload.isObject -> payload
		isObject -> this
		else -> null
	}
}
