package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsUidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.UuidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

@RestController
class JobPostingSearchController(
	private val jobPostingService: JobPostingService,
) {

	@GetMapping("/job-postings/list", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun list(
		@RequestParam(required = false) uuid: UUID?,
		@RequestParam(required = false) uid: String?,
		@RequestParam(required = false) title: String?,
		@RequestParam(required = false) company: String?,
		@RequestParam(name = "evaluationStatus", required = false) evaluationStatusRaw: List<String>?,
		@RequestParam(name = "responseStatus", required = false) responseStatusRaw: List<String>?,
		@RequestParam(defaultValue = "1") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	): JobPostingsList = jobPostingService.list(
		uuid = uuid,
		uid = uid,
		title = title,
		company = company,
		evaluationStatusRaw = evaluationStatusRaw,
		responseStatusRaw = responseStatusRaw,
		page = page,
		size = size,
	)

	@PostMapping(
		"/job-postings/search-query/by-uuids",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
		produces = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun byUuids(@RequestBody body: UuidsList): JobPostingsList =
		jobPostingService.findByUuids(body)

	@PostMapping(
		"/job-postings/search-query/non-existent",
		consumes = [MediaType.APPLICATION_JSON_VALUE],
		produces = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun nonExistent(@RequestBody body: JobPostingsUidsList): JobPostingsUidsList =
		jobPostingService.nonExistentUids(body)
}
