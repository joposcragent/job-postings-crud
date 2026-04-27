package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import tools.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsUidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingMapper
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.UuidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration.OrchestratorEventsProducer
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class JobPostingService(
	private val repository: PostingRepository,
	private val orchestratorEventsProducer: OrchestratorEventsProducer,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun get(jobPostingUuid: UUID): JobPostingsItem {
		val row = repository.findByUuid(jobPostingUuid)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
		return PostingMapper.toDto(row)
	}

	fun create(jobPostingUuid: UUID, item: JobPostingsItem, correlationId: UUID? = null) {
		if (repository.existsByUuid(jobPostingUuid)) {
			throw ResponseStatusException(
				HttpStatus.CONFLICT,
				"Вакансия с uuid $jobPostingUuid уже есть в БД",
			)
		}
		if (repository.existsByUid(item.uid)) {
			throw ResponseStatusException(
				HttpStatus.CONFLICT,
				"Вакансия с uid ${item.uid} уже есть в БД",
			)
		}
		if (correlationId == null) {
			repository.insert(jobPostingUuid, item)
			return
		}
		try {
			repository.insert(jobPostingUuid, item)
			val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
			orchestratorEventsProducer.publishEvaluationQueued(correlationId, jobPostingUuid, createdAt)
		} catch (e: Exception) {
			log.error(
				"Сбой при создании вакансии jobPostingUuid={} с correlationId={}; попытка отправить progress в оркестратор",
				jobPostingUuid,
				correlationId,
				e,
			)
			try {
				orchestratorEventsProducer.publishSaveFailedProgress(
					correlationId = correlationId,
					jobPostingUuid = jobPostingUuid,
					vacancyUrl = item.url,
					executionLog = e.message ?: e.toString(),
					createdAt = OffsetDateTime.now(ZoneOffset.UTC),
				)
			} catch (secondary: Exception) {
				log.error("Не удалось отправить событие progress в оркестратор после основного сбоя", secondary)
			}
			throw e
		}
	}

	fun patch(jobPostingUuid: UUID, body: JsonNode) {
		val patch = JobPostingPatchParser.parse(body)
		if (!repository.existsByUuid(jobPostingUuid)) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.patch(jobPostingUuid, patch)
	}

	fun list(
		uuid: UUID?,
		uid: String?,
		title: String?,
		company: String?,
		evaluationStatuses: List<EvaluationStatus>?,
		page: Int,
		size: Int,
	): JobPostingsList {
		val statusFilter = evaluationStatuses?.takeIf { it.isNotEmpty() }
		val rows = repository.listFiltered(uuid, uid, title, company, statusFilter, page, size)
		if (rows.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдено ни одной вакансии")
		}
		return JobPostingsList(rows.map { PostingMapper.toDto(it) })
	}

	fun findByUuids(body: UuidsList): JobPostingsList {
		val rows = repository.findByUuids(body.list)
		if (rows.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдено ни одной вакансии")
		}
		return JobPostingsList(rows.map { PostingMapper.toDto(it) })
	}

	fun nonExistentUids(body: JobPostingsUidsList): JobPostingsUidsList {
		val requested = body.list
		val existing = repository.findExistingUids(requested)
		val remaining = requested.filter { it !in existing }
		if (remaining.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдено ни одной вакансии")
		}
		return JobPostingsUidsList(remaining)
	}

	fun updateEvaluationStatus(jobPostingUuid: UUID, status: EvaluationStatus) {
		if (!repository.existsByUuid(jobPostingUuid)) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.updateEvaluationStatus(jobPostingUuid, status)
	}

	fun updateResponseStatus(jobPostingUuid: UUID, status: ResponseStatus) {
		if (!repository.existsByUuid(jobPostingUuid)) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.updateResponseStatus(jobPostingUuid, status)
	}
}
