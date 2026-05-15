package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import java.util.UUID

class JobPostingCreateBeginPayloadException(
	val reason: String,
	val jobUuid: UUID?,
	val messageKey: String,
) : RuntimeException(reason)
