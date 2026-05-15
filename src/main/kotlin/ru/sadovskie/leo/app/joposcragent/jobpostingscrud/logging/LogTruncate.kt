package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.logging

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal object LogTruncate {
	const val DEFAULT_MAX = 8192
	private const val SUFFIX = "...[truncated]"

	fun forLog(s: String?, max: Int = DEFAULT_MAX): String {
		if (s == null) {
			return "<null>"
		}
		if (s.length <= max) {
			return s
		}
		val take = (max - SUFFIX.length).coerceAtLeast(0)
		return s.take(take) + SUFFIX
	}

	fun forLog(bytes: ByteArray, encoding: Charset = StandardCharsets.UTF_8, max: Int = DEFAULT_MAX): String {
		if (bytes.isEmpty()) {
			return ""
		}
		return forLog(String(bytes, encoding), max)
	}
}
