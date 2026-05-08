package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.exception.ApiExceptionHandler
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

class JobPostingControllerWebMvcTest {

	private lateinit var jobPostingService: JobPostingService
	private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc

	@BeforeEach
	fun setup() {
		jobPostingService = mockk(relaxed = true)
		mockMvc = MockMvcBuilders
			.standaloneSetup(JobPostingController(jobPostingService))
			.setControllerAdvice(ApiExceptionHandler())
			.build()
	}

	@Test
	fun `create without correlation header passes null`() {
		val jobPostingUuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
		every { jobPostingService.create(jobPostingUuid, any(), null) } just runs
		mockMvc.perform(
			post("/job-postings/$jobPostingUuid")
				.contentType(MediaType.APPLICATION_JSON)
				.content(minimalCreateJson(jobPostingUuid)),
		).andExpect(status().isOk)
		verify(exactly = 1) { jobPostingService.create(jobPostingUuid, any(), null) }
	}

	@Test
	fun `create with valid correlation header passes uuid`() {
		val jobPostingUuid = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
		val cid = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
		every { jobPostingService.create(jobPostingUuid, any(), cid) } just runs
		mockMvc.perform(
			post("/job-postings/$jobPostingUuid")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Joposcragent-correlationId", cid.toString())
				.content(minimalCreateJson(jobPostingUuid)),
		).andExpect(status().isOk)
		verify(exactly = 1) { jobPostingService.create(jobPostingUuid, any(), cid) }
	}

	@Test
	fun `create with invalid correlation header returns 400`() {
		val jobPostingUuid = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
		mockMvc.perform(
			post("/job-postings/$jobPostingUuid")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Joposcragent-correlationId", "not-a-uuid")
				.content(minimalCreateJson(jobPostingUuid)),
		).andExpect(status().isBadRequest)
		verify(exactly = 0) { jobPostingService.create(any(), any(), any()) }
	}

	private fun minimalCreateJson(jobPostingUuid: UUID): String {
		val uid = "webmvc-${jobPostingUuid.toString().take(8)}"
		val sq = UUID.fromString("33333333-3333-3333-3333-333333333333")
		return """
			{
				"uuid": "$jobPostingUuid",
				"searchQueryUuid": "$sq",
				"uid": "$uid",
				"publicationDate": "2026-04-15",
				"title": "Title",
				"url": "https://example.com/job"
			}
		""".trimIndent()
	}
}
