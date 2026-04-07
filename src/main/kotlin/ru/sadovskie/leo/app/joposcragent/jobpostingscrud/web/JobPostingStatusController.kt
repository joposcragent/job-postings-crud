package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

@RestController
class JobPostingStatusController(
	private val jobPostingService: JobPostingService,
) {

	@PostMapping("/job-postings/{jobPostingUuid}/evaluation-status/{status}")
	fun evaluationStatus(
		@PathVariable jobPostingUuid: UUID,
		@PathVariable status: String,
	): ResponseEntity<Unit> {
		val parsed = EvaluationStatus.lookupLiteral(status)
			?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown evaluation status: $status")
		jobPostingService.updateEvaluationStatus(jobPostingUuid, parsed)
		return ResponseEntity.ok().build()
	}

	@PostMapping("/job-postings/{jobPostingUuid}/response-status/{status}")
	fun responseStatus(
		@PathVariable jobPostingUuid: UUID,
		@PathVariable status: String,
	): ResponseEntity<Unit> {
		val parsed = ResponseStatus.lookupLiteral(status)
			?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown response status: $status")
		jobPostingService.updateResponseStatus(jobPostingUuid, parsed)
		return ResponseEntity.ok().build()
	}
}
