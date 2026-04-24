package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import tools.jackson.databind.JsonNode
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

@RestController
class JobPostingController(
	private val jobPostingService: JobPostingService,
) {

	@GetMapping("/job-postings/{jobPostingUuid}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun get(@PathVariable jobPostingUuid: UUID): JobPostingsItem =
		jobPostingService.get(jobPostingUuid)

	@PostMapping(
		"/job-postings/{jobPostingUuid}",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun create(
		@PathVariable jobPostingUuid: UUID,
		@RequestBody body: JobPostingsItem,
		@RequestHeader(name = "X-Joposcragent-correlationId", required = false) correlationIdHeader: String?,
	): ResponseEntity<Unit> {
		val correlationId = parseCorrelationId(correlationIdHeader)
		jobPostingService.create(jobPostingUuid, body, correlationId)
		return ResponseEntity.ok().build()
	}

	private fun parseCorrelationId(header: String?): UUID? {
		if (header.isNullOrBlank()) {
			return null
		}
		return try {
			UUID.fromString(header.trim())
		} catch (_: IllegalArgumentException) {
			throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Некорректный X-Joposcragent-correlationId: ожидается UUID",
			)
		}
	}

	@PatchMapping(
		"/job-postings/{jobPostingUuid}",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun patch(
		@PathVariable jobPostingUuid: UUID,
		@RequestBody body: JsonNode,
	): ResponseEntity<Unit> {
		jobPostingService.patch(jobPostingUuid, body)
		return ResponseEntity.ok().build()
	}
}
