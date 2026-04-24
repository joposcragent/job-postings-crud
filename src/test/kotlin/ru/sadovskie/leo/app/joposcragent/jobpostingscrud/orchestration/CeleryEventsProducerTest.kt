package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CeleryEventsProducerTest {

	private lateinit var restClient: RestClient
	private lateinit var server: MockRestServiceServer
	private lateinit var producer: CeleryEventsProducer

	@BeforeEach
	fun setUp() {
		val builder = RestClient.builder().baseUrl("http://celery-stub")
		server = MockRestServiceServer.bindTo(builder).build()
		restClient = builder.build()
		producer = CeleryEventsProducer(restClient)
	}

	@AfterEach
	fun tearDown() {
		server.verify()
	}

	@Test
	fun `publishEvaluationQueued posts to evaluation queue and accepts 204`() {
		val cid = UUID.fromString("11111111-1111-1111-1111-111111111111")
		val jobUuid = UUID.fromString("22222222-2222-2222-2222-222222222222")
		val createdAt = OffsetDateTime.parse("2026-04-24T12:00:00Z")
		server.expect(ExpectedCount.once(), requestTo(endsWith("/events-queue/evaluation")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString(cid.toString())))
			.andRespond(withStatus(HttpStatus.NO_CONTENT))
		producer.publishEvaluationQueued(cid, jobUuid, createdAt)
	}

	@Test
	fun `publishSaveFailedProgress posts to progress queue and accepts 204`() {
		val cid = UUID.fromString("33333333-3333-3333-3333-333333333333")
		val jobUuid = UUID.fromString("44444444-4444-4444-4444-444444444444")
		val createdAt = OffsetDateTime.now(ZoneOffset.UTC)
		server.expect(ExpectedCount.once(), requestTo(endsWith("/events-queue/progress")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(containsString("FAILED")))
			.andExpect(content().string(containsString("https://jobs.example/1")))
			.andRespond(withStatus(HttpStatus.NO_CONTENT))
		producer.publishSaveFailedProgress(
			correlationId = cid,
			jobPostingUuid = jobUuid,
			vacancyUrl = "https://jobs.example/1",
			executionLog = "boom",
			createdAt = createdAt,
		)
	}

	@Test
	fun `publishEvaluationQueued wraps non success as IllegalStateException`() {
		val cid = UUID.randomUUID()
		val jobUuid = UUID.randomUUID()
		server.expect(ExpectedCount.once(), requestTo(endsWith("/events-queue/evaluation")))
			.andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("upstream"))
		val ex = assertThrows<IllegalStateException> {
			producer.publishEvaluationQueued(cid, jobUuid, OffsetDateTime.now(ZoneOffset.UTC))
		}
		assertTrue(ex.message!!.contains("502"))
	}
}
