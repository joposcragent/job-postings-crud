package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
		assertEquals(jobUuid, r.jobUuid)
		assertEquals(postingUuid, r.jobPostingUuid)
		assertEquals("999", r.item.uid)
		assertEquals("2026-04-20", r.item.publicationDate)
		assertNull(r.softWarning)
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
		assertEquals(entity, r.jobPostingUuid)
	}

	@Test
	fun `parse throws when jobUuid invalid and record key not uuid`() {
		val ex = assertThrows(JobPostingCreateBeginPayloadException::class.java) {
			JobPostingCreateBeginPayloadMapper.parse(null, json.readTree("""{"jobUuid":"not-uuid"}"""))
		}
		assertTrue(ex.reason.contains("jobUuid", ignoreCase = true))
		assertNull(ex.jobUuid)
	}

	@Test
	fun `parse resolves jobUuid from record key when payload jobUuid invalid`() {
		val keyUuid = UUID.fromString("99999999-9999-4999-8999-999999999999")
		val postingUuid = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
		val sq = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
		val jsonNode = json.readTree(
			"""
			{
				"jobUuid": "not-a-uuid",
				"jobPostingUuid": "$postingUuid",
				"searchQueryUuid": "$sq",
				"uid": "u1",
				"title": "T",
				"url": "https://x",
				"publicationDate": "2026-01-02"
			}
			""".trimIndent(),
		)
		val r = JobPostingCreateBeginPayloadMapper.parse(keyUuid.toString(), jsonNode)
		assertEquals(keyUuid, r.jobUuid)
	}

	@Test
	fun `parse allows missing company and content with softWarning`() {
		val jobUuid = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
		val postingUuid = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd")
		val sq = UUID.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee")
		val jsonNode = json.readTree(
			"""
			{
				"jobUuid": "$jobUuid",
				"jobPostingUuid": "$postingUuid",
				"searchQueryUuid": "$sq",
				"uid": "u2",
				"title": "T",
				"url": "https://x",
				"publicationDate": "2026-01-03"
			}
			""".trimIndent(),
		)
		val r = JobPostingCreateBeginPayloadMapper.parse(jobUuid.toString(), jsonNode)
		assertNull(r.item.company)
		assertNull(r.item.content)
		assertNotNull(r.softWarning)
		assertTrue(r.softWarning!!.contains("company"))
		assertTrue(r.softWarning.contains("content"))
	}
}
