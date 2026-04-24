package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Публикация доменных событий в оркестратор пайплайна (реализация — HTTP к celery-orchestrator).
 */
interface OrchestratorEventsProducer {

	fun publishEvaluationQueued(
		correlationId: UUID,
		jobPostingUuid: UUID,
		createdAt: OffsetDateTime,
	)

	fun publishSaveFailedProgress(
		correlationId: UUID,
		jobPostingUuid: UUID,
		vacancyUrl: String,
		executionLog: String,
		createdAt: OffsetDateTime,
	)
}
