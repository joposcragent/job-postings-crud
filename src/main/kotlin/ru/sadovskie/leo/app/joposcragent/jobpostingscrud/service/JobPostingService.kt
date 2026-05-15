package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import org.slf4j.LoggerFactory
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
import tools.jackson.databind.JsonNode
import java.util.UUID

@Service
class JobPostingService(
	private val repository: PostingRepository,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun get(jobPostingUuid: UUID): JobPostingsItem {
		val row = repository.findByUuid(jobPostingUuid)
			?: run {
				log.info("get job posting: not found jobPostingUuid={}", jobPostingUuid)
				throw ResponseStatusException(HttpStatus.NOT_FOUND)
			}
		log.info("get job posting: found jobPostingUuid={} uid={}", jobPostingUuid, row.uid)
		return PostingMapper.toDto(row)
	}

	fun create(jobPostingUuid: UUID, item: JobPostingsItem, @Suppress("UNUSED_PARAMETER") correlationId: UUID? = null) {
		if (repository.existsByUuid(jobPostingUuid)) {
			log.info("create job posting: conflict by uuid jobPostingUuid={}", jobPostingUuid)
			throw ResponseStatusException(
				HttpStatus.CONFLICT,
				"Вакансия с uuid $jobPostingUuid уже есть в БД",
			)
		}
		if (repository.existsByUid(item.uid)) {
			log.info("create job posting: conflict by uid jobPostingUuid={} uid={}", jobPostingUuid, item.uid)
			throw ResponseStatusException(
				HttpStatus.CONFLICT,
				"Вакансия с uid ${item.uid} уже есть в БД",
			)
		}
		repository.insert(jobPostingUuid, item)
		log.info("create job posting: inserted jobPostingUuid={} uid={}", jobPostingUuid, item.uid)
	}

	fun patch(jobPostingUuid: UUID, body: JsonNode) {
		val patch = JobPostingPatchParser.parse(body)
		if (!repository.existsByUuid(jobPostingUuid)) {
			log.info("patch job posting: not found jobPostingUuid={}", jobPostingUuid)
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.patch(jobPostingUuid, patch)
		val fieldNames = patch.keys.joinToString(",") { it.jsonName }
		log.info("patch job posting: updated jobPostingUuid={} fields={}", jobPostingUuid, fieldNames)
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
		log.info(
			"list job postings: uuid={} uid={} title={} company={} evaluationStatuses={} evaluationIncludeNull={} " +
				"responseStatuses={} responseIncludeNull={} sort={} page={} size={} totalCount={} totalPages={} rowCount={}",
			uuid,
			uid,
			title,
			company,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
			sortRaw,
			page,
			size,
			totalCount,
			totalPages,
			rows.size,
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
			log.info("list job postings by substring: rejected blank substring")
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
		log.info(
			"list job postings by substring: substringLen={} evaluationStatuses={} evaluationIncludeNull={} " +
				"responseStatuses={} responseIncludeNull={} sort={} page={} size={} totalCount={} totalPages={} rowCount={}",
			trimmed.length,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
			sortRaw,
			page,
			size,
			totalCount,
			totalPages,
			rows.size,
		)
		return JobPostingsList(rows.map { PostingMapper.toDto(it) }, totalPages)
	}

	fun findByUuids(body: UuidsList): JobPostingsList {
		val rows = repository.findByUuids(body.list)
		if (rows.isEmpty()) {
			log.info("find by uuids: none found requestedCount={}", body.list.size)
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдено ни одной вакансии")
		}
		log.info("find by uuids: found requestedCount={} resultCount={}", body.list.size, rows.size)
		return JobPostingsList(rows.map { PostingMapper.toDto(it) })
	}

	fun nonExistentUids(body: JobPostingsUidsList): JobPostingsUidsList {
		val requested = body.list
		val existing = repository.findExistingUids(requested)
		val remaining = requested.filter { it !in existing }
		if (remaining.isEmpty()) {
			log.info("non-existent uids: all exist requestedCount={}", requested.size)
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Не найдено ни одной вакансии")
		}
		log.info(
			"non-existent uids: requestedCount={} existingCount={} remainingCount={}",
			requested.size,
			existing.size,
			remaining.size,
		)
		return JobPostingsUidsList(remaining)
	}

	fun updateEvaluationStatus(jobPostingUuid: UUID, status: EvaluationStatus) {
		if (!repository.existsByUuid(jobPostingUuid)) {
			log.info("update evaluation status: not found jobPostingUuid={}", jobPostingUuid)
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.updateEvaluationStatus(jobPostingUuid, status)
		log.info("update evaluation status: updated jobPostingUuid={} status={}", jobPostingUuid, status)
	}

	fun updateResponseStatus(jobPostingUuid: UUID, status: ResponseStatus) {
		if (!repository.existsByUuid(jobPostingUuid)) {
			log.info("update response status: not found jobPostingUuid={}", jobPostingUuid)
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		repository.updateResponseStatus(jobPostingUuid, status)
		log.info("update response status: updated jobPostingUuid={} status={}", jobPostingUuid, status)
	}

	fun getNotes(jobPostingUuid: UUID): JobPostingNotesResponse {
		val text = repository.getNotesText(jobPostingUuid)
			?: run {
				log.info("get notes: not found jobPostingUuid={}", jobPostingUuid)
				throw ResponseStatusException(HttpStatus.NOT_FOUND)
			}
		log.info("get notes: found jobPostingUuid={} textLength={}", jobPostingUuid, text.length)
		return JobPostingNotesResponse(text)
	}

	fun replaceNotes(jobPostingUuid: UUID, body: JobPostingNotesWrite) {
		if (repository.replaceNotes(jobPostingUuid, body.text) == 0) {
			log.info("replace notes: not found jobPostingUuid={}", jobPostingUuid)
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		log.info("replace notes: updated jobPostingUuid={} textLength={}", jobPostingUuid, body.text.length)
	}
}
