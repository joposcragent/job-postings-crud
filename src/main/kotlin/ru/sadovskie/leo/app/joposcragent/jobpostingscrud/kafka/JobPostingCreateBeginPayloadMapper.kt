package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.JobPostingsItem
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

data class BeginPayloadParseResult(
	val jobUuid: UUID,
	val messageKey: String,
	val jobPostingUuid: UUID,
	val item: JobPostingsItem,
	val softWarning: String?,
)

object JobPostingCreateBeginPayloadMapper {

	fun parse(recordKey: String?, payload: JsonNode): BeginPayloadParseResult {
		val jobUuidFromPayload = textOrNull(payload, "jobUuid")?.let { parseUuid(it) }
		val jobUuidFromKey = recordKey?.trim()?.takeIf { it.isNotEmpty() }?.let { parseUuid(it) }
		val jobUuid = jobUuidFromPayload ?: jobUuidFromKey
			?: throw JobPostingCreateBeginPayloadException(
				"Отсутствует или некорректный jobUuid (и ключ записи не UUID)",
				null,
				recordKey?.takeIf { it.isNotBlank() } ?: "",
			)
		val messageKey = (recordKey?.takeIf { it.isNotBlank() }) ?: jobUuid.toString()

		fun fail(reason: String): Nothing =
			throw JobPostingCreateBeginPayloadException(reason, jobUuid, messageKey)

		val entityUuid = textOrNull(payload, "entityUuid")?.let { parseUuid(it) }
		val jobPostingUuidFromField = textOrNull(payload, "jobPostingUuid")?.let { parseUuid(it) }
		val jobPostingUuid = jobPostingUuidFromField ?: entityUuid
			?: fail("Нужен jobPostingUuid или entityUuid")

		val searchQueryUuidText = textOrNull(payload, "searchQueryUuid")
			?: fail("Отсутствует searchQueryUuid")
		val searchQueryUuid = parseUuid(searchQueryUuidText)
			?: fail("Некорректный searchQueryUuid")

		val uid = textOrNull(payload, "uid") ?: fail("Отсутствует uid")
		val title = textOrNull(payload, "title") ?: fail("Отсутствует title")
		val url = textOrNull(payload, "url") ?: fail("Отсутствует url")
		val company = textOrNull(payload, "company")
		val content = textOrNull(payload, "content")
		val publicationRaw = textOrNull(payload, "publicationDate")
			?: fail("Отсутствует publicationDate")
		val publicationDate = normalizePublicationDate(publicationRaw)
			?: fail("Некорректная publicationDate: $publicationRaw")

		val softWarning = buildSoftWarning(company, content)

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
		return BeginPayloadParseResult(jobUuid, messageKey, jobPostingUuid, item, softWarning)
	}

	private fun buildSoftWarning(company: String?, content: String?): String? {
		val parts = mutableListOf<String>()
		if (company == null) parts.add("company")
		if (content == null) parts.add("content")
		if (parts.isEmpty()) return null
		return "Отсутствуют необязательные поля: ${parts.joinToString(", ")}"
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
