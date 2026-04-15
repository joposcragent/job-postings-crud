package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.service

import tools.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.EvaluationStatus
import ru.sadovskie.leo.app.joposcragent.jobpostings.jooq.enums.ResponseStatus
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingMapper
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto.PostingPatchField
import java.time.LocalDate
import java.util.EnumMap

internal object JobPostingPatchParser {

	fun parse(body: JsonNode): EnumMap<PostingPatchField, Any?> {
		if (!body.isObject) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ожидался JSON-объект")
		}
		if (body.size() == 0) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Нет полей для обновления")
		}
		val result = EnumMap<PostingPatchField, Any?>(PostingPatchField::class.java)
		for ((name, valueNode) in body.properties()) {
			val field = PostingPatchField.fromJsonName(name)
				?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестное поле: $name")
			if (valueNode.isNull) {
				if (field.rejectsJsonNull) {
					throw ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Поле ${field.jsonName} не может быть null",
					)
				}
				result[field] = null
			} else {
				result[field] = parseValue(field, valueNode)
			}
		}
		return result
	}

	/** Пустой массив для `contentVector` — очистка (NULL в БД). */
	private fun parseValue(field: PostingPatchField, node: JsonNode): Any? =
		when (field) {
			PostingPatchField.UID -> requireText(node, field)
			PostingPatchField.TITLE -> requireText(node, field)
			PostingPatchField.URL -> requireText(node, field)
			PostingPatchField.COMPANY -> requireText(node, field)
			PostingPatchField.CONTENT ->
				if (node.isTextual) node.asText() else throw badType(field, "строка")
			PostingPatchField.PUBLICATION_DATE -> parseLocalDate(node, field)
			PostingPatchField.CONTENT_VECTOR -> parseContentVector(node, field)
			PostingPatchField.EVALUATION_STATUS -> parseEvaluationStatus(node, field)
			PostingPatchField.RESPONSE_STATUS -> parseResponseStatus(node, field)
			PostingPatchField.RELEVANCE -> parseFloat(node, field)
		}

	private fun requireText(node: JsonNode, field: PostingPatchField): String {
		if (!node.isTextual) throw badType(field, "строка")
		return node.asText()
	}

	private fun parseLocalDate(node: JsonNode, field: PostingPatchField): LocalDate =
		try {
			when {
				node.isTextual -> LocalDate.parse(node.asText())
				else -> throw badType(field, "дата в формате ISO-8601 (например 2026-01-15)")
			}
		} catch (_: Exception) {
			throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Некорректное значение ${field.jsonName}",
			)
		}

	private fun parseContentVector(node: JsonNode, field: PostingPatchField): Array<Float>? {
		if (!node.isArray) throw badType(field, "массив чисел")
		if (node.size() == 0) return null
		val doubles = mutableListOf<Double>()
		for (i in 0 until node.size()) {
			val e = node[i]
			if (!e.isNumber) throw badType(field, "массив чисел")
			doubles.add(e.asDouble())
		}
		return PostingMapper.toFloatArray(doubles)!!
	}

	private fun parseEvaluationStatus(node: JsonNode, field: PostingPatchField): EvaluationStatus {
		if (!node.isTextual) throw badType(field, "строка-enum")
		return try {
			EvaluationStatus.valueOf(node.asText())
		} catch (_: IllegalArgumentException) {
			throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Неизвестное значение ${field.jsonName}: ${node.asText()}",
			)
		}
	}

	private fun parseResponseStatus(node: JsonNode, field: PostingPatchField): ResponseStatus {
		if (!node.isTextual) throw badType(field, "строка-enum")
		return try {
			ResponseStatus.valueOf(node.asText())
		} catch (_: IllegalArgumentException) {
			throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Неизвестное значение ${field.jsonName}: ${node.asText()}",
			)
		}
	}

	private fun parseFloat(node: JsonNode, field: PostingPatchField): Float =
		try {
			when {
				node.isNumber -> node.floatValue()
				node.isTextual -> node.asText().toFloat()
				else -> throw badType(field, "число")
			}
		} catch (_: Exception) {
			throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Некорректное значение ${field.jsonName}",
			)
		}

	private fun badType(field: PostingPatchField, expected: String): Nothing =
		throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"Поле ${field.jsonName}: ожидался тип $expected",
		)
}
