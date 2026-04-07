package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto

import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.tables.records.PostingsRecord
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object PostingMapper {

	fun toDto(record: PostingsRecord): JobPostingsItem =
		JobPostingsItem(
			uuid = record.uuid,
			uid = record.uid,
			publicationDate = record.publicationDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
			title = record.title,
			url = record.url,
			titleVector = record.titleVector?.toDoubleList(),
			contentVector = record.contentVector?.toDoubleList(),
			evaluationStatus = record.evaluationStatus,
			responseStatus = record.responseStatus,
			createdAt = record.createdAt,
			updatedAt = record.updatedAt,
		)

	fun publicationLocalDate(publicationDate: OffsetDateTime): LocalDate =
		publicationDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate()

	fun toFloatArray(values: List<Double>?): Array<Float>? {
		if (values == null) return null
		return values.map { it.toFloat() }.toTypedArray()
	}

	private fun Array<Float>?.toDoubleList(): List<Double>? =
		this?.map { it.toDouble() }

	fun newRecord(
		uuid: UUID,
		item: JobPostingsItem,
	): PostingsRecord =
		PostingsRecord().apply {
			this.uuid = uuid
			uid = item.uid
			publicationDate = publicationLocalDate(item.publicationDate)
			title = item.title
			url = item.url
			titleVector = toFloatArray(item.titleVector)
			contentVector = toFloatArray(item.contentVector)
			evaluationStatus = item.evaluationStatus ?: EvaluationStatus.NEW
			responseStatus = item.responseStatus
		}

}
