package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto

import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import java.time.OffsetDateTime
import java.util.UUID

data class JobPostingsItem(
	val uuid: UUID,
	val uid: String,
	val publicationDate: OffsetDateTime,
	val title: String,
	val url: String,
	val titleVector: List<Double>? = null,
	val contentVector: List<Double>? = null,
	val evaluationStatus: EvaluationStatus? = null,
	val responseStatus: ResponseStatus? = null,
	val createdAt: OffsetDateTime? = null,
	val updatedAt: OffsetDateTime? = null,
)

data class JobPostingsList(
	val list: List<JobPostingsItem>,
)

data class UuidsList(
	val list: List<UUID>,
)

data class JobPostingsUidsList(
	val list: List<String>,
)
