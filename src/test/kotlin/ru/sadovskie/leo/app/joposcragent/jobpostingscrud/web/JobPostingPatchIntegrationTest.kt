package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class JobPostingPatchIntegrationTest @Autowired constructor(
	private val webApplicationContext: WebApplicationContext,
	private val jdbcTemplate: JdbcTemplate,
) {

	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setupMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	companion object {
		@JvmStatic
		@Container
		val postgres = PostgreSQLContainer("postgres:16-alpine")

		@JvmStatic
		@DynamicPropertySource
		fun registerDataSource(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
		}
	}

	@Test
	fun `patch clears contentVector with empty array and relevance with zero`() {
		val uuid = UUID.randomUUID()
		val uid = "patch-it-${uuid.toString().take(8)}"
		val sq = UUID.fromString("22222222-2222-2222-2222-222222222222")
		val createJson = """
			{
				"uuid": "$uuid",
				"searchQueryUuid": "$sq",
				"uid": "$uid",
				"publicationDate": "2026-04-15",
				"title": "Integration title",
				"url": "https://example.com/job",
				"contentVector": [0.5, 0.25],
				"relevance": 0.91
			}
		""".trimIndent()

		mockMvc.perform(
			post("/job-postings/$uuid")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createJson),
		).andExpect(status().isOk)

		mockMvc.perform(
			patch("/job-postings/$uuid")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"contentVector":[],"relevance":0}"""),
		).andExpect(status().isOk)

		val row = jdbcTemplate.queryForMap(
			"select content_vector, relevance from job_postings.postings where uuid = ?",
			uuid,
		)
		assertNull(row["content_vector"])
		assertEquals(0.0f, (row["relevance"] as Number).toFloat(), 1e-6f)

		mockMvc.perform(get("/job-postings/$uuid"))
			.andExpect(status().isOk)
	}
}
