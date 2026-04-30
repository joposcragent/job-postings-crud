package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.web

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository.JobPostingListSort

object ListQueryParamParser {
	fun parseListSort(raw: String?): JobPostingListSort {
		if (raw.isNullOrBlank()) return JobPostingListSort.UUID_ASC
		return when (raw.trim().lowercase()) {
			"uuid_asc" -> JobPostingListSort.UUID_ASC
			"created_at_desc" -> JobPostingListSort.CREATED_AT_DESC
			else -> throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Unknown list sort: $raw",
			)
		}
	}

	fun parseEvaluationStatus(raw: List<String>?): Pair<List<EvaluationStatus>, Boolean> =
		parseEnumList(raw, EvaluationStatus::lookupLiteral, "evaluation status")

	fun parseResponseStatus(raw: List<String>?): Pair<List<ResponseStatus>, Boolean> =
		parseEnumList(raw, ResponseStatus::lookupLiteral, "response status")

	private fun <E> parseEnumList(
		raw: List<String>?,
		lookup: (String) -> E?,
		label: String,
	): Pair<List<E>, Boolean> {
		if (raw.isNullOrEmpty()) return emptyList<E>() to false
		val enums = mutableListOf<E>()
		var includeNull = false
		for (token in raw) {
			val t = token.trim()
			when {
				t.equals("NULL", ignoreCase = true) -> includeNull = true
				else -> {
					val v = lookup(t)
						?: throw ResponseStatusException(
							HttpStatus.BAD_REQUEST,
							"Unknown $label: $t",
						)
					enums.add(v)
				}
			}
		}
		return enums to includeNull
	}
}
