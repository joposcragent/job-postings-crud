package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto

import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.tables.records.PostingsRecord
import java.time.LocalDate
import java.util.UUID

object PostingMapper {

	fun toDto(record: PostingsRecord): JobPostingsItem =
		JobPostingsItem(
			uuid = record.uuid,
			uid = record.uid,
			publicationDate = record.publicationDate.toString(),
			title = record.title,
			url = record.url,
			company = record.company,
			content = record.content,
			contentVector = record.contentVector?.toDoubleList(),
			relevance = record.relevance?.toDouble(),
			evaluationStatus = record.evaluationStatus,
			responseStatus = record.responseStatus,
			createdAt = record.createdAt,
			updatedAt = record.updatedAt,
		)

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
			publicationDate = parsePublicationDate(item.publicationDate)
			title = item.title
			url = item.url
			company = item.company
			content = item.content
			contentVector = toFloatArray(item.contentVector)
			relevance = item.relevance?.toFloat()
			evaluationStatus = item.evaluationStatus ?: EvaluationStatus.NEW
			responseStatus = item.responseStatus
		}

	fun parsePublicationDate(value: String): LocalDate =
		LocalDate.parse(value)
}

