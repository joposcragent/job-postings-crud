package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
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
	): ResponseEntity<Unit> {
		jobPostingService.create(jobPostingUuid, body)
		return ResponseEntity.ok().build()
	}

	@PutMapping(
		"/job-postings/{jobPostingUuid}",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun update(
		@PathVariable jobPostingUuid: UUID,
		@RequestBody body: JobPostingsItem,
	): ResponseEntity<Unit> {
		jobPostingService.update(jobPostingUuid, body)
		return ResponseEntity.ok().build()
	}
}
