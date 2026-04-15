package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.dto

/**
 * Поля тела PATCH, совпадающие с JSON-ключами OpenAPI (camelCase).
 */
enum class PostingPatchField(val jsonName: String) {
	UID("uid"),
	PUBLICATION_DATE("publicationDate"),
	TITLE("title"),
	COMPANY("company"),
	URL("url"),
	CONTENT("content"),
	CONTENT_VECTOR("contentVector"),
	EVALUATION_STATUS("evaluationStatus"),
	RESPONSE_STATUS("responseStatus"),
	RELEVANCE("relevance"),
	;

	/** Поля, для которых JSON `null` недопустим (колонка NOT NULL в БД). */
	val rejectsJsonNull: Boolean
		get() = when (this) {
			COMPANY, CONTENT, CONTENT_VECTOR, RESPONSE_STATUS, RELEVANCE -> false
			else -> true
		}

	companion object {
		private val byJsonName: Map<String, PostingPatchField> =
			entries.associateBy { it.jsonName }

		fun fromJsonName(name: String): PostingPatchField? = byJsonName[name]
	}
}
