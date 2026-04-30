package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL as JooqDsl
import org.springframework.stereotype.Repository
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingPatchField
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.Tables
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.tables.records.PostingsRecord
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.EnumMap
import java.util.UUID

@Repository
class PostingRepository(
	private val dsl: DSLContext,
) {

	fun findByUuid(uuid: UUID): PostingsRecord? =
		dsl.selectFrom(Tables.POSTINGS)
			.where(Tables.POSTINGS.UUID.eq(uuid))
			.fetchOne()

	fun existsByUuid(uuid: UUID): Boolean =
		dsl.fetchExists(
			dsl.selectOne()
				.from(Tables.POSTINGS)
				.where(Tables.POSTINGS.UUID.eq(uuid)),
		)

	fun existsByUid(uid: String): Boolean =
		dsl.fetchExists(
			dsl.selectOne()
				.from(Tables.POSTINGS)
				.where(Tables.POSTINGS.UID.eq(uid)),
		)

	fun insert(uuid: UUID, item: JobPostingsItem) {
		val r = PostingMapper.newRecord(uuid, item)
		dsl.insertInto(Tables.POSTINGS)
			.set(Tables.POSTINGS.UUID, r.uuid)
			.set(Tables.POSTINGS.UID, r.uid)
			.set(Tables.POSTINGS.PUBLICATION_DATE, r.publicationDate)
			.set(Tables.POSTINGS.TITLE, r.title)
			.set(Tables.POSTINGS.URL, r.url)
			.set(Tables.POSTINGS.COMPANY, r.company)
			.set(Tables.POSTINGS.CONTENT, r.content)
			.set(Tables.POSTINGS.CONTENT_VECTOR, r.contentVector)
			.set(Tables.POSTINGS.RELEVANCE, r.relevance)
			.set(Tables.POSTINGS.EVALUATION_STATUS, r.evaluationStatus)
			.set(Tables.POSTINGS.RESPONSE_STATUS, r.responseStatus)
			.execute()
	}

	fun patch(uuid: UUID, values: EnumMap<PostingPatchField, Any?>) {
		check(values.isNotEmpty())
		val q = dsl.updateQuery(Tables.POSTINGS)
		values.forEach { (field, v) ->
			when (field) {
				PostingPatchField.UID -> q.addValue(Tables.POSTINGS.UID, v as String)
				PostingPatchField.PUBLICATION_DATE -> q.addValue(Tables.POSTINGS.PUBLICATION_DATE, v as LocalDate)
				PostingPatchField.TITLE -> q.addValue(Tables.POSTINGS.TITLE, v as String)
				PostingPatchField.COMPANY -> q.addValue(Tables.POSTINGS.COMPANY, v as String?)
				PostingPatchField.URL -> q.addValue(Tables.POSTINGS.URL, v as String)
				PostingPatchField.CONTENT -> q.addValue(Tables.POSTINGS.CONTENT, v as String?)
				PostingPatchField.CONTENT_VECTOR ->
					q.addValue(Tables.POSTINGS.CONTENT_VECTOR, contentVectorFromPatchValue(v))
				PostingPatchField.EVALUATION_STATUS -> q.addValue(Tables.POSTINGS.EVALUATION_STATUS, v as EvaluationStatus)
				PostingPatchField.RESPONSE_STATUS -> q.addValue(Tables.POSTINGS.RESPONSE_STATUS, v as ResponseStatus?)
				PostingPatchField.RELEVANCE -> q.addValue(Tables.POSTINGS.RELEVANCE, v as Float?)
			}
		}
		q.addValue(Tables.POSTINGS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
		q.addConditions(Tables.POSTINGS.UUID.eq(uuid))
		q.execute()
	}

	private fun contentVectorFromPatchValue(value: Any?): Array<Float>? {
		if (value == null) return null
		check(value is Array<*>) { "contentVector must be an array or null" }
		return Array(value.size) { i -> value[i] as Float }
	}

	fun updateEvaluationStatus(uuid: UUID, status: EvaluationStatus) {
		dsl.update(Tables.POSTINGS)
			.set(Tables.POSTINGS.EVALUATION_STATUS, status)
			.set(Tables.POSTINGS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
			.where(Tables.POSTINGS.UUID.eq(uuid))
			.execute()
	}

	fun updateResponseStatus(uuid: UUID, status: ResponseStatus) {
		dsl.update(Tables.POSTINGS)
			.set(Tables.POSTINGS.RESPONSE_STATUS, status)
			.set(Tables.POSTINGS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
			.where(Tables.POSTINGS.UUID.eq(uuid))
			.execute()
	}

	fun listFiltered(
		uuid: UUID?,
		uid: String?,
		titleSubstring: String?,
		company: String?,
		evaluationStatuses: List<EvaluationStatus>,
		evaluationIncludeNull: Boolean,
		responseStatuses: List<ResponseStatus>,
		responseIncludeNull: Boolean,
		page: Int,
		size: Int,
	): List<PostingsRecord> {
		val condition = buildListFilterCondition(
			uuid,
			uid,
			titleSubstring,
			company,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
		)
		val (safePage, safeSize) = normalizePageSize(page, size)
		return dsl.selectFrom(Tables.POSTINGS)
			.where(condition)
			.orderBy(Tables.POSTINGS.UUID)
			.limit(safeSize)
			.offset((safePage - 1) * safeSize)
			.fetch()
	}

	fun countFiltered(
		uuid: UUID?,
		uid: String?,
		titleSubstring: String?,
		company: String?,
		evaluationStatuses: List<EvaluationStatus>,
		evaluationIncludeNull: Boolean,
		responseStatuses: List<ResponseStatus>,
		responseIncludeNull: Boolean,
	): Long {
		val condition = buildListFilterCondition(
			uuid,
			uid,
			titleSubstring,
			company,
			evaluationStatuses,
			evaluationIncludeNull,
			responseStatuses,
			responseIncludeNull,
		)
		return dsl.fetchCount(
			dsl.selectFrom(Tables.POSTINGS).where(condition),
		).toLong()
	}

	private fun buildListFilterCondition(
		uuid: UUID?,
		uid: String?,
		titleSubstring: String?,
		company: String?,
		evaluationStatuses: List<EvaluationStatus>,
		evaluationIncludeNull: Boolean,
		responseStatuses: List<ResponseStatus>,
		responseIncludeNull: Boolean,
	): Condition {
		var condition: Condition = JooqDsl.trueCondition()
		if (uuid != null) condition = condition.and(Tables.POSTINGS.UUID.eq(uuid))
		if (uid != null) condition = condition.and(Tables.POSTINGS.UID.eq(uid))
		if (titleSubstring != null) {
			condition = condition.and(
				Tables.POSTINGS.TITLE.like(
					JooqDsl.concat(JooqDsl.inline("%"), JooqDsl.`val`(titleSubstring), JooqDsl.inline("%")),
				),
			)
		}
		if (company != null) {
			condition = condition.and(
				Tables.POSTINGS.COMPANY.like(
					JooqDsl.concat(JooqDsl.inline("%"), JooqDsl.`val`(company), JooqDsl.inline("%")),
				),
			)
		}
		val evalIn = evaluationStatuses.takeIf { it.isNotEmpty() }
		when {
			evalIn != null && evaluationIncludeNull -> {
				condition = condition.and(
					Tables.POSTINGS.EVALUATION_STATUS.`in`(evalIn)
						.or(Tables.POSTINGS.EVALUATION_STATUS.isNull),
				)
			}
			evalIn != null -> {
				condition = condition.and(Tables.POSTINGS.EVALUATION_STATUS.`in`(evalIn))
			}
			evaluationIncludeNull -> {
				condition = condition.and(Tables.POSTINGS.EVALUATION_STATUS.isNull)
			}
		}
		val responseIn = responseStatuses.takeIf { it.isNotEmpty() }
		when {
			responseIn != null && responseIncludeNull -> {
				condition = condition.and(
					Tables.POSTINGS.RESPONSE_STATUS.`in`(responseIn)
						.or(Tables.POSTINGS.RESPONSE_STATUS.isNull),
				)
			}
			responseIn != null -> {
				condition = condition.and(Tables.POSTINGS.RESPONSE_STATUS.`in`(responseIn))
			}
			responseIncludeNull -> {
				condition = condition.and(Tables.POSTINGS.RESPONSE_STATUS.isNull)
			}
		}
		return condition
	}

	fun findByUuids(uuids: Collection<UUID>): List<PostingsRecord> {
		if (uuids.isEmpty()) return emptyList()
		return dsl.selectFrom(Tables.POSTINGS)
			.where(Tables.POSTINGS.UUID.`in`(uuids))
			.fetch()
	}

	fun findExistingUids(uids: Collection<String>): Set<String> {
		if (uids.isEmpty()) return emptySet()
		return dsl.select(Tables.POSTINGS.UID)
			.from(Tables.POSTINGS)
			.where(Tables.POSTINGS.UID.`in`(uids))
			.fetchSet(Tables.POSTINGS.UID)
	}

	companion object {
		fun normalizePageSize(page: Int, size: Int): Pair<Int, Int> =
			page.coerceAtLeast(1) to size.coerceAtLeast(1)
	}
}
