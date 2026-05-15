package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class JobPostingCreateKafkaProcessorTest {

	private val json = JsonMapper.builder().findAndAddModules().build()

	@Test
	fun `handle skips non-json`() {
		val repo = mockk<PostingRepository>(relaxed = true)
		val publisher = mockk<JobPostingCreateResultPublisher>(relaxed = true)
		val processor = JobPostingCreateKafkaProcessor(repo, publisher, json)
		processor.handle(ConsumerRecord("t", 0, 0L, "k", "not json {{{"))
		verify(exactly = 0) { repo.insert(any(), any()) }
		verify(exactly = 0) { publisher.publishSucceeded(any(), any(), any()) }
		verify(exactly = 0) { publisher.publishFailed(any(), any(), any()) }
	}

	@Test
	fun `handle inserts and publishes succeeded`() {
		val jobUuid = UUID.fromString("11111111-1111-4111-8111-111111111111")
		val postingUuid = UUID.fromString("22222222-2222-4222-8222-222222222222")
		val sq = UUID.fromString("33333333-3333-4333-8333-333333333333")
		val payload = """
			{
				"jobUuid": "$jobUuid",
				"entityUuid": "$postingUuid",
				"jobPostingUuid": "$postingUuid",
				"searchQueryUuid": "$sq",
				"uid": "u-proc-1",
				"title": "T",
				"url": "https://x",
				"company": "C",
				"content": "body",
				"publicationDate": "2026-06-01"
			}
		""".trimIndent()
		val repo = mockk<PostingRepository>()
		every { repo.insert(postingUuid, any()) } returns Unit
		val publisher = mockk<JobPostingCreateResultPublisher>(relaxed = true)
		val processor = JobPostingCreateKafkaProcessor(repo, publisher, json)
		processor.handle(ConsumerRecord("async-job.job-posting-create", 0, 0L, jobUuid.toString(), payload))
		verify(exactly = 1) { repo.insert(postingUuid, any()) }
		verify(exactly = 1) { publisher.publishSucceeded(jobUuid, jobUuid.toString(), postingUuid) }
	}

	@Test
	fun `handle insert failure publishes failed`() {
		val jobUuid = UUID.fromString("44444444-4444-4444-8444-444444444444")
		val postingUuid = UUID.fromString("55555555-5555-5555-8555-555555555555")
		val sq = UUID.fromString("66666666-6666-4666-8666-666666666666")
		val payload = """
			{
				"jobUuid": "$jobUuid",
				"entityUuid": "$postingUuid",
				"searchQueryUuid": "$sq",
				"uid": "same-uid",
				"title": "T",
				"url": "https://x",
				"company": "C",
				"content": "body",
				"publicationDate": "2026-06-02"
			}
		""".trimIndent()
		val repo = mockk<PostingRepository>()
		every { repo.insert(postingUuid, any()) } throws IllegalStateException("duplicate key")
		val publisher = mockk<JobPostingCreateResultPublisher>(relaxed = true)
		val processor = JobPostingCreateKafkaProcessor(repo, publisher, json)
		processor.handle(ConsumerRecord("async-job.job-posting-create", 0, 0L, "k", payload))
		verify(exactly = 1) { repo.insert(postingUuid, any()) }
		verify(exactly = 1) { publisher.publishFailed(jobUuid, "k", "duplicate key") }
		verify(exactly = 0) { publisher.publishSucceeded(any(), any(), any()) }
	}
}
