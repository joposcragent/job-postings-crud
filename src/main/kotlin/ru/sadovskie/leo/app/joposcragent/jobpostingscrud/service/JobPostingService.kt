package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import tools.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingNotesResponse
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingNotesWrite
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsUidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingMapper
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.UuidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web.ListQueryParamParser
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

	fun create(jobPostingUuid: UUID, item: JobPostingsItem, @Suppress("UNUSED_PARAMETER") correlationId: UUID? = null) {
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
		evaluationStatusRaw: List<String>?,
		responseStatusRaw: List<String>?,
		sortRaw: String?,
		page: Int,
		size: Int,
	): JobPostingsList {
		val sort = ListQueryParamParser.parseListSort(sortRaw)
		val (evaluationStatuses, evaluationIncludeNull) =
			ListQueryParamParser.parseEvaluationStatus(evaluationStatusRaw)
		val (responseStatuses, responseIncludeNull) =
			ListQueryParamParser.parseResponseStatus(responseStatusRaw)
		val (_, safeSize) = PostingRepository.normalizePageSize(page, size)
		val totalCount = repository.countFiltered(
			uuid,
			uid,
			title,
			company,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
		)
		val totalPages = if (totalCount == 0L) {
			0
		} else {
			((totalCount + safeSize - 1L) / safeSize).toInt()
		}
		val rows = repository.listFiltered(
			uuid,
			uid,
			title,
			company,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
			page,
			size,
			sort,
		)
		return JobPostingsList(rows.map { PostingMapper.toDto(it) }, totalPages)
	}

	fun listBySubstring(
		substring: String?,
		evaluationStatusRaw: List<String>?,
		responseStatusRaw: List<String>?,
		sortRaw: String?,
		page: Int,
		size: Int,
	): JobPostingsList {
		if (substring.isNullOrBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "substring must not be blank")
		}
		val trimmed = substring.trim()
		val sort = ListQueryParamParser.parseListSort(sortRaw)
		val (evaluationStatuses, evaluationIncludeNull) =
			ListQueryParamParser.parseEvaluationStatus(evaluationStatusRaw)
		val (responseStatuses, responseIncludeNull) =
			ListQueryParamParser.parseResponseStatus(responseStatusRaw)
		val (_, safeSize) = PostingRepository.normalizePageSize(page, size)
		val totalCount = repository.countFilteredBySubstringUnion(
			trimmed,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
		)
		val totalPages = if (totalCount == 0L) {
			0
		} else {
			((totalCount + safeSize - 1L) / safeSize).toInt()
		}
		val rows = repository.listFilteredBySubstringUnion(
			trimmed,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
			page,
			size,
			sort,
		)
		return JobPostingsList(rows.map { PostingMapper.toDto(it) }, totalPages)
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

	fun getNotes(jobPostingUuid: UUID): JobPostingNotesResponse {
		val text = repository.getNotesText(jobPostingUuid)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
		return JobPostingNotesResponse(text)
	}

	fun replaceNotes(jobPostingUuid: UUID, body: JobPostingNotesWrite) {
		if (repository.replaceNotes(jobPostingUuid, body.text) == 0) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
	}
}
