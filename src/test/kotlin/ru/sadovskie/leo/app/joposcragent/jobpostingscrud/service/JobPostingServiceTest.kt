package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.tables.records.PostingsRecord
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsUidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.UuidsList
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.JobPostingListSort
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.PostingRepository
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class JobPostingServiceTest {

	private val uuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

	private val objectMapper = ObjectMapper()

	private val sampleSearchQueryUuid: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

	private val sampleItem = JobPostingsItem(
		uuid = uuid,
		searchQueryUuid = sampleSearchQueryUuid,
		uid = "131927888",
		publicationDate = "2026-01-15",
		title = "Developer",
		url = "https://example.com/v/131927888",
	)

	private fun jobPostingService(repo: PostingRepository) = JobPostingService(repo)

	@Test
	fun `get throws 404 when not found`() {
		val repo = mockk<PostingRepository>()
		every { repo.findByUuid(uuid) } returns null
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.get(uuid)
		}
		assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
	}

	@Test
	fun `get returns dto when row exists`() {
		val repo = mockk<PostingRepository>()
		val row = mockk<PostingsRecord>()
		val created = OffsetDateTime.parse("2026-01-01T12:00:00Z")
		every { row.uuid } returns uuid
		every { row.searchQueryUuid } returns sampleSearchQueryUuid
		every { row.uid } returns "131927888"
		every { row.publicationDate } returns LocalDate.of(2026, 1, 15)
		every { row.title } returns "Developer"
		every { row.url } returns "https://example.com/v/131927888"
		every { row.company } returns null
		every { row.content } returns null
		every { row.contentVector } returns null
		every { row.relevance } returns null
		every { row.evaluationStatus } returns EvaluationStatus.NEW
		every { row.responseStatus } returns null
		every { row.createdAt } returns created
		every { row.updatedAt } returns null
		every { repo.findByUuid(uuid) } returns row
		val service = jobPostingService(repo)
		val dto = service.get(uuid)
		assertEquals(uuid, dto.uuid)
		assertEquals("131927888", dto.uid)
		assertEquals("Developer", dto.title)
		assertEquals(sampleSearchQueryUuid, dto.searchQueryUuid)
	}

	@Test
	fun `create throws 409 when uuid exists`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns true
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.create(uuid, sampleItem)
		}
		assertEquals(HttpStatus.CONFLICT, ex.statusCode)
		assertEquals("Вакансия с uuid $uuid уже есть в БД", ex.reason)
		verify(exactly = 0) { repo.insert(any(), any()) }
	}

	@Test
	fun `create throws 409 when uid exists`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns false
		every { repo.existsByUid("131927888") } returns true
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.create(uuid, sampleItem)
		}
		assertEquals(HttpStatus.CONFLICT, ex.statusCode)
		assertEquals("Вакансия с uid 131927888 уже есть в БД", ex.reason)
		verify(exactly = 0) { repo.insert(any(), any()) }
	}

	@Test
	fun `create inserts when no conflict`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns false
		every { repo.existsByUid("131927888") } returns false
		every { repo.insert(uuid, sampleItem) } returns Unit
		val service = jobPostingService(repo)
		service.create(uuid, sampleItem)
		verify(exactly = 1) { repo.insert(uuid, sampleItem) }
	}

	@Test
	fun `create ignores correlation id and only inserts`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns false
		every { repo.existsByUid("131927888") } returns false
		every { repo.insert(uuid, sampleItem) } returns Unit
		val cid = UUID.randomUUID()
		val service = jobPostingService(repo)
		service.create(uuid, sampleItem, cid)
		verify(exactly = 1) { repo.insert(uuid, sampleItem) }
	}

	@Test
	fun `create on uuid conflict does not insert with correlation id`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns true
		val cid = UUID.randomUUID()
		val service = jobPostingService(repo)
		assertThrows(ResponseStatusException::class.java) {
			service.create(uuid, sampleItem, cid)
		}
		verify(exactly = 0) { repo.insert(any(), any()) }
	}

	@Test
	fun `list returns empty JobPostingsList when no rows`() {
		val repo = mockk<PostingRepository>()
		every {
			repo.countFiltered(null, null, null, null, emptyList(), false, emptyList(), false)
		} returns 0L
		every {
			repo.listFiltered(null, null, null, null, emptyList(), false, emptyList(), false, 1, 20, JobPostingListSort.UUID_ASC)
		} returns emptyList()
		val service = jobPostingService(repo)
		assertEquals(
			JobPostingsList(emptyList(), totalPages = 0),
			service.list(null, null, null, null, null, null, null, 1, 20),
		)
	}

	@Test
	fun `list sets totalPages from filtered count and page size`() {
		val repo = mockk<PostingRepository>()
		every {
			repo.countFiltered(null, null, null, null, emptyList(), false, emptyList(), false)
		} returns 21L
		every {
			repo.listFiltered(null, null, null, null, emptyList(), false, emptyList(), false, 1, 20, JobPostingListSort.UUID_ASC)
		} returns emptyList()
		val service = jobPostingService(repo)
		assertEquals(
			JobPostingsList(emptyList(), totalPages = 2),
			service.list(null, null, null, null, null, null, null, 1, 20),
		)
	}

	@Test
	fun `list throws 400 on unknown sort`() {
		val repo = mockk<PostingRepository>()
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.list(null, null, null, null, null, null, "uuid_desc", 1, 20)
		}
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		verify(exactly = 0) { repo.countFiltered(any(), any(), any(), any(), any(), any(), any(), any()) }
		verify(exactly = 0) { repo.listFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
	}

	@Test
	fun `list passes created_at_desc sort to repository`() {
		val repo = mockk<PostingRepository>()
		val sortSlot = slot<JobPostingListSort>()
		every {
			repo.countFiltered(null, null, null, null, emptyList(), false, emptyList(), false)
		} returns 0L
		every {
			repo.listFiltered(
				null, null, null, null,
				emptyList(), false, emptyList(), false,
				1, 20, capture(sortSlot),
			)
		} returns emptyList()
		val service = jobPostingService(repo)
		service.list(null, null, null, null, null, null, "created_at_desc", 1, 20)
		assertEquals(JobPostingListSort.CREATED_AT_DESC, sortSlot.captured)
	}

	@Test
	fun `listBySubstring throws 400 when substring blank`() {
		val repo = mockk<PostingRepository>()
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.listBySubstring("   ", null, null, null, 1, 20)
		}
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		verify(exactly = 0) { repo.countFilteredBySubstringUnion(any(), any(), any(), any(), any()) }
		verify(exactly = 0) { repo.listFilteredBySubstringUnion(any(), any(), any(), any(), any(), any(), any(), any()) }
	}

	@Test
	fun `listBySubstring delegates to union repository methods`() {
		val repo = mockk<PostingRepository>()
		every {
			repo.countFilteredBySubstringUnion("acme", emptyList(), false, emptyList(), false)
		} returns 0L
		every {
			repo.listFilteredBySubstringUnion(
				"acme",
				emptyList(),
				false,
				emptyList(),
				false,
				1,
				20,
				JobPostingListSort.UUID_ASC,
			)
		} returns emptyList()
		val service = jobPostingService(repo)
		assertEquals(
			JobPostingsList(emptyList(), totalPages = 0),
			service.listBySubstring("acme", null, null, null, 1, 20),
		)
	}

	@Test
	fun `findByUuids throws 404 when empty`() {
		val repo = mockk<PostingRepository>()
		every { repo.findByUuids(listOf(uuid)) } returns emptyList()
		val service = jobPostingService(repo)
		assertThrows(ResponseStatusException::class.java) {
			service.findByUuids(UuidsList(listOf(uuid)))
		}
	}

	@Test
	fun `nonExistentUids throws 404 when all exist`() {
		val repo = mockk<PostingRepository>()
		every { repo.findExistingUids(listOf("a", "b")) } returns setOf("a", "b")
		val service = jobPostingService(repo)
		assertThrows(ResponseStatusException::class.java) {
			service.nonExistentUids(JobPostingsUidsList(listOf("a", "b")))
		}
	}

	@Test
	fun `nonExistentUids returns only missing uids`() {
		val repo = mockk<PostingRepository>()
		every { repo.findExistingUids(listOf("a", "b", "c")) } returns setOf("a")
		val service = jobPostingService(repo)
		val result = service.nonExistentUids(JobPostingsUidsList(listOf("a", "b", "c")))
		assertEquals(listOf("b", "c"), result.list)
	}

	@Test
	fun `patch throws 404 when missing`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns false
		val service = jobPostingService(repo)
		assertThrows(ResponseStatusException::class.java) {
			service.patch(uuid, objectMapper.readTree("""{"title":"x"}"""))
		}
		verify(exactly = 0) { repo.patch(any(), any()) }
	}

	@Test
	fun `patch throws 400 when body empty`() {
		val repo = mockk<PostingRepository>()
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.patch(uuid, objectMapper.readTree("{}"))
		}
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		verify(exactly = 0) { repo.existsByUuid(any()) }
		verify(exactly = 0) { repo.patch(any(), any()) }
	}

	@Test
	fun `patch throws 400 on unknown field`() {
		val repo = mockk<PostingRepository>()
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.patch(uuid, objectMapper.readTree("""{"title":"ok","extra":1}"""))
		}
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		verify(exactly = 0) { repo.patch(any(), any()) }
	}

	@Test
	fun `patch throws 400 when null on non nullable column`() {
		val repo = mockk<PostingRepository>()
		val service = jobPostingService(repo)
		val ex = assertThrows(ResponseStatusException::class.java) {
			service.patch(uuid, objectMapper.readTree("""{"title":null}"""))
		}
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		verify(exactly = 0) { repo.patch(any(), any()) }
	}

	@Test
	fun `patch calls repository when valid`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns true
		every { repo.patch(uuid, any()) } returns Unit
		val service = jobPostingService(repo)
		service.patch(uuid, objectMapper.readTree("""{"title":"New title"}"""))
		verify(exactly = 1) { repo.patch(uuid, any()) }
	}

	@Test
	fun `updateEvaluationStatus throws 404 when missing`() {
		val repo = mockk<PostingRepository>()
		every { repo.existsByUuid(uuid) } returns false
		val service = jobPostingService(repo)
		assertThrows(ResponseStatusException::class.java) {
			service.updateEvaluationStatus(uuid, EvaluationStatus.RELEVANT)
		}
		verify(exactly = 0) { repo.updateEvaluationStatus(any(), any()) }
	}
}
