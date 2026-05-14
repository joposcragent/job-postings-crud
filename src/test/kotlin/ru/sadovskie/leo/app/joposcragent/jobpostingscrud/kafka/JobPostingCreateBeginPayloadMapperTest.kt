package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class JobPostingCreateBeginPayloadMapperTest {

	private val json = JsonMapper.builder().findAndAddModules().build()

	@Test
	fun `normalizePublicationDate parses date-time`() {
		assertEquals(
			"2026-05-01",
			JobPostingCreateBeginPayloadMapper.normalizePublicationDate("2026-05-01T14:30:00Z"),
		)
	}

	@Test
	fun `normalizePublicationDate parses plain date`() {
		assertEquals("2026-05-01", JobPostingCreateBeginPayloadMapper.normalizePublicationDate("2026-05-01"))
	}

	@Test
	fun `parse builds item from full payload`() {
		val jobUuid = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
		val postingUuid = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
		val sq = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
		val jsonNode = json.readTree(
			"""
			{
				"jobUuid": "$jobUuid",
				"entityUuid": "$postingUuid",
				"jobPostingUuid": "$postingUuid",
				"searchQueryUuid": "$sq",
				"uid": "999",
				"title": "T",
				"url": "https://hh.ru/v/999",
				"company": "C",
				"content": "body",
				"publicationDate": "2026-04-20T10:00:00Z"
			}
			""".trimIndent(),
		)
		val r = JobPostingCreateBeginPayloadMapper.parse(jobUuid.toString(), jsonNode)
		assertInstanceOf(BeginPayloadParseResult.Ok::class.java, r)
		val ok = r as BeginPayloadParseResult.Ok
		assertEquals(jobUuid, ok.jobUuid)
		assertEquals(postingUuid, ok.jobPostingUuid)
		assertEquals("999", ok.item.uid)
		assertEquals("2026-04-20", ok.item.publicationDate)
	}

	@Test
	fun `parse uses entityUuid when jobPostingUuid absent`() {
		val jobUuid = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd")
		val entity = UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee")
		val sq = UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff")
		val jsonNode = json.readTree(
			"""
			{
				"jobUuid": "$jobUuid",
				"entityUuid": "$entity",
				"searchQueryUuid": "$sq",
				"uid": "1",
				"title": "T",
				"url": "https://x",
				"company": "C",
				"content": "x",
				"publicationDate": "2026-01-02"
			}
			""".trimIndent(),
		)
		val r = JobPostingCreateBeginPayloadMapper.parse(null, jsonNode)
		val ok = r as BeginPayloadParseResult.Ok
		assertEquals(entity, ok.jobPostingUuid)
	}

	@Test
	fun `parse invalid returns Invalid`() {
		val r = JobPostingCreateBeginPayloadMapper.parse(null, json.readTree("""{"jobUuid":"not-uuid"}"""))
		assertInstanceOf(BeginPayloadParseResult.Invalid::class.java, r)
		assertTrue((r as BeginPayloadParseResult.Invalid).reason.contains("jobUuid"))
	}
}
