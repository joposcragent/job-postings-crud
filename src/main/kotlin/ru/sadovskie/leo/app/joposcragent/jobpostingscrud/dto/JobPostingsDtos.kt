package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import java.time.OffsetDateTime
import java.util.UUID

data class JobPostingsItem(
	val uuid: UUID,
	val uid: String,
	val publicationDate: String,
	val title: String,
	val url: String,
	val company: String? = null,
	val content: String? = null,
	val contentVector: List<Double>? = null,
	val relevance: Double? = null,
	val evaluationStatus: EvaluationStatus? = null,
	val responseStatus: ResponseStatus? = null,
	val createdAt: OffsetDateTime? = null,
	val updatedAt: OffsetDateTime? = null,
)

data class JobPostingsList(
	val list: List<JobPostingsItem>,
	@get:JsonInclude(JsonInclude.Include.NON_NULL)
	val totalPages: Int? = null,
)

data class UuidsList(
	val list: List<UUID>,
)

data class JobPostingsUidsList(
	val list: List<String>,
)
