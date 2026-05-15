package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers(disabledWithoutDocker = true)
@EmbeddedKafka(partitions = 1, topics = [JobPostingOrchestrationKafkaTopics.JOB_POSTING_CREATE])
@ActiveProfiles("test-kafka")
class JobPostingCreateKafkaIntegrationTest @Autowired constructor(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val jdbcTemplate: JdbcTemplate,
) {

	private val json = JsonMapper.builder().findAndAddModules().build()

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

	@BeforeEach
	fun cleanPostings() {
		jdbcTemplate.update("DELETE FROM job_postings.postings")
	}

	@Test
	fun `begin message creates row and publishes result`() {
		val jobUuid = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
		val postingUuid = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
		val sq = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
		val uid = "kafka-it-${UUID.randomUUID().toString().take(8)}"
		val payload = mapOf(
			"jobUuid" to jobUuid.toString(),
			"entityUuid" to postingUuid.toString(),
			"jobPostingUuid" to postingUuid.toString(),
			"searchQueryUuid" to sq.toString(),
			"uid" to uid,
			"title" to "Kafka IT",
			"url" to "https://hh.ru/v/$uid",
			"company" to "Co",
			"content" to "text",
			"publicationDate" to "2026-05-14T12:00:00Z",
		)
		val createdAt = "2026-05-14T12:00:01Z"
		val jsonStr = json.writeValueAsString(payload)
		val headers = listOf(
			RecordHeader("key", jobUuid.toString().toByteArray(StandardCharsets.UTF_8)),
			RecordHeader("type", JobPostingOrchestrationMessageTypes.JOB_POSTING_CREATE_BEGIN.toByteArray(StandardCharsets.UTF_8)),
			RecordHeader("createdAt", createdAt.toByteArray(StandardCharsets.UTF_8)),
			RecordHeader(
				"schemaVersion",
				JobPostingOrchestrationKafkaConstants.SCHEMA_VERSION.toByteArray(StandardCharsets.UTF_8),
			),
		)
		kafkaTemplate.send(
			ProducerRecord(
				JobPostingOrchestrationKafkaTopics.JOB_POSTING_CREATE,
				null,
				jobUuid.toString(),
				jsonStr,
				headers,
			),
		).get()

		awaitUidVisible(uid, Duration.ofSeconds(10))
		val title = jdbcTemplate.queryForObject(
			"SELECT title FROM job_postings.postings WHERE uid = ?",
			String::class.java,
			uid,
		)
		assertEquals("Kafka IT", title)

		val resultCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM job_postings.postings WHERE uuid = ?",
			Int::class.java,
			postingUuid,
		) ?: 0
		assertEquals(1, resultCount)
	}

	private fun awaitUidVisible(uid: String, timeout: Duration) {
		val deadline = System.nanoTime() + timeout.toNanos()
		while (System.nanoTime() < deadline) {
			val n = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM job_postings.postings WHERE uid = ?",
				Int::class.java,
				uid,
			) ?: 0
			if (n >= 1) {
				return
			}
			Thread.sleep(50)
		}
		fail("Posting with uid=$uid did not appear within $timeout")
	}
}
