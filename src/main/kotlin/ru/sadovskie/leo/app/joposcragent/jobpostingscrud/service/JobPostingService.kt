package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import tools.jackson.databind.JsonNode
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
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import java.util.UUID

@Service
class JobPostingService(
	private val repository: PostingRepository,
) {

	fun get(jobPostingUuid: UUID): JobPostingsItem {
		val row = repository.findByUuid(jobPostingUuid)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
		return PostingMapper.toDto(row)
	}

	fun create(jobPostingUuid: UUID, item: JobPostingsItem) {
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
		repository.insert(jobPostingUuid, item)
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
		page: Int,
		size: Int,
	): JobPostingsList {
		val rows = repository.listFiltered(uuid, uid, title, company, page, size)
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
