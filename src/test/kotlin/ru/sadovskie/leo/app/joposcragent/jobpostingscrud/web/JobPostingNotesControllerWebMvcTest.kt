package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingNotesResponse
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.exception.ApiExceptionHandler
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service.JobPostingService
import java.util.UUID

class JobPostingNotesControllerWebMvcTest {

	private lateinit var jobPostingService: JobPostingService
	private lateinit var mockMvc: org.springframework.test.web.servlet.MockMvc

	@BeforeEach
	fun setup() {
		jobPostingService = mockk(relaxed = true)
		mockMvc = MockMvcBuilders
			.standaloneSetup(JobPostingNotesController(jobPostingService))
			.setControllerAdvice(ApiExceptionHandler())
			.build()
	}

	@Test
	fun `get returns json text`() {
		val id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
		every { jobPostingService.getNotes(id) } returns JobPostingNotesResponse("hello")
		mockMvc.perform(get("/notes/$id"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""{"text":"hello"}"""))
		verify(exactly = 1) { jobPostingService.getNotes(id) }
	}

	@Test
	fun `post delegates to service`() {
		val id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
		every { jobPostingService.replaceNotes(id, any()) } just runs
		mockMvc.perform(
			post("/notes/$id")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"text":"new note"}"""),
		).andExpect(status().isOk)
		verify(exactly = 1) { jobPostingService.replaceNotes(id, match { it.text == "new note" }) }
	}
}
