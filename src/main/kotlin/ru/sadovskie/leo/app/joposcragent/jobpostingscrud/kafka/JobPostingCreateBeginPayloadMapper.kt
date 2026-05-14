package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

sealed class BeginPayloadParseResult {
	data class Ok(
		val jobUuid: UUID,
		val messageKey: String,
		val jobPostingUuid: UUID,
		val item: JobPostingsItem,
	) : BeginPayloadParseResult()

	data class Invalid(val reason: String) : BeginPayloadParseResult()
}

object JobPostingCreateBeginPayloadMapper {

	fun parse(recordKey: String?, payload: JsonNode): BeginPayloadParseResult {
		val jobUuidText = textOrNull(payload, "jobUuid")
			?: return BeginPayloadParseResult.Invalid("Отсутствует или пустой jobUuid")
		val jobUuid = parseUuid(jobUuidText)
			?: return BeginPayloadParseResult.Invalid("Некорректный jobUuid")
		val messageKey = (recordKey?.takeIf { it.isNotBlank() }) ?: jobUuid.toString()

		val entityUuid = textOrNull(payload, "entityUuid")?.let { parseUuid(it) }
		val jobPostingUuidText = textOrNull(payload, "jobPostingUuid")
		val jobPostingUuidFromField = jobPostingUuidText?.let { parseUuid(it) }
		val jobPostingUuid = jobPostingUuidFromField ?: entityUuid
			?: return BeginPayloadParseResult.Invalid("Нужен jobPostingUuid или entityUuid")

		val searchQueryUuidText = textOrNull(payload, "searchQueryUuid")
			?: return BeginPayloadParseResult.Invalid("Отсутствует searchQueryUuid")
		val searchQueryUuid = parseUuid(searchQueryUuidText)
			?: return BeginPayloadParseResult.Invalid("Некорректный searchQueryUuid")

		val uid = textOrNull(payload, "uid") ?: return BeginPayloadParseResult.Invalid("Отсутствует uid")
		val title = textOrNull(payload, "title") ?: return BeginPayloadParseResult.Invalid("Отсутствует title")
		val url = textOrNull(payload, "url") ?: return BeginPayloadParseResult.Invalid("Отсутствует url")
		val company = textOrNull(payload, "company")
			?: return BeginPayloadParseResult.Invalid("Отсутствует company")
		val content = textOrNull(payload, "content")
			?: return BeginPayloadParseResult.Invalid("Отсутствует content")
		val publicationRaw = textOrNull(payload, "publicationDate")
			?: return BeginPayloadParseResult.Invalid("Отсутствует publicationDate")
		val publicationDate = normalizePublicationDate(publicationRaw)
			?: return BeginPayloadParseResult.Invalid("Некорректная publicationDate: $publicationRaw")

		val item = JobPostingsItem(
			uuid = jobPostingUuid,
			searchQueryUuid = searchQueryUuid,
			uid = uid,
			publicationDate = publicationDate,
			title = title,
			url = url,
			company = company,
			content = content,
		)
		return BeginPayloadParseResult.Ok(jobUuid, messageKey, jobPostingUuid, item)
	}

	private fun textOrNull(node: JsonNode, field: String): String? {
		if (!node.has(field) || node.get(field).isNull) return null
		val t = node.get(field).asText().trim()
		return t.ifBlank { null }
	}

	private fun parseUuid(s: String): UUID? =
		try {
			UUID.fromString(s.trim())
		} catch (_: IllegalArgumentException) {
			null
		}

	internal fun normalizePublicationDate(raw: String): String? =
		try {
			OffsetDateTime.parse(raw).toLocalDate().toString()
		} catch (_: DateTimeParseException) {
			try {
				LocalDate.parse(raw).toString()
			} catch (_: DateTimeParseException) {
				null
			}
		}
}
