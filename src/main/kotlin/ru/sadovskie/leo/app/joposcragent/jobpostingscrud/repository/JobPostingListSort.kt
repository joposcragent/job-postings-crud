package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.repository

enum class JobPostingListSort {
	/** По колонке `uuid` по возрастанию (поведение по умолчанию). */
	UUID_ASC,

	/** По `created_at` по убыванию, при равенстве — по `uuid` по убыванию. */
	CREATED_AT_DESC,
}
