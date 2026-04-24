package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.OffsetDateTime
import java.util.UUID

@Component
class CeleryEventsProducer(
	@Qualifier("celeryOrchestrator") private val restClient: RestClient,
) : OrchestratorEventsProducer {

	override fun publishEvaluationQueued(
		correlationId: UUID,
		jobPostingUuid: UUID,
		createdAt: OffsetDateTime,
	) {
		val body = linkedMapOf<String, Any?>(
			"correlationId" to correlationId,
			"createdAt" to createdAt,
			"jobPostingUuid" to jobPostingUuid,
		)
		postJson("/events-queue/evaluation", body)
	}

	override fun publishSaveFailedProgress(
		correlationId: UUID,
		jobPostingUuid: UUID,
		vacancyUrl: String,
		executionLog: String,
		createdAt: OffsetDateTime,
	) {
		val body = linkedMapOf<String, Any?>(
			"correlationId" to correlationId,
			"createdAt" to createdAt,
			"jobPostingUuid" to jobPostingUuid,
			"vacancyUrl" to vacancyUrl,
			"executionLog" to executionLog,
			"status" to "FAILED",
		)
		postJson("/events-queue/progress", body)
	}

	private fun postJson(path: String, body: Map<String, Any?>) {
		try {
			restClient.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity()
		} catch (e: RestClientResponseException) {
			throw IllegalStateException(
				"Orchestrator returned ${e.statusCode.value()}: ${e.responseBodyAsString}",
				e,
			)
		}
	}
}
