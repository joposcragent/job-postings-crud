package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingNotesResponse
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingNotesWrite
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

@RestController
class JobPostingNotesController(
	private val jobPostingService: JobPostingService,
) {

	@GetMapping("/notes/{jobPostingUuid}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun get(@PathVariable jobPostingUuid: UUID): JobPostingNotesResponse =
		jobPostingService.getNotes(jobPostingUuid)

	@PostMapping(
		"/notes/{jobPostingUuid}",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun post(
		@PathVariable jobPostingUuid: UUID,
		@RequestBody body: JobPostingNotesWrite,
	): ResponseEntity<Unit> {
		jobPostingService.replaceNotes(jobPostingUuid, body)
		return ResponseEntity.ok().build()
	}
}
