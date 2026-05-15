package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import ru.sadovskie.leo.app.joposcragent.jobpostingscrud.logging.LogTruncate
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val CONTENT_CACHE_LIMIT = 65536

class HttpExchangeDebugLoggingFilter : OncePerRequestFilter() {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun shouldNotFilter(request: HttpServletRequest): Boolean {
		val uri = request.requestURI ?: return false
		return uri.startsWith("/actuator")
	}

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		if (!log.isDebugEnabled) {
			filterChain.doFilter(request, response)
			return
		}
		val requestWrapper = ContentCachingRequestWrapper(request, CONTENT_CACHE_LIMIT)
		val responseWrapper = ContentCachingResponseWrapper(response)
		try {
			filterChain.doFilter(requestWrapper, responseWrapper)
		} finally {
			if (log.isDebugEnabled) {
				val encoding = request.characterEncoding?.let { runCatching { Charset.forName(it) }.getOrNull() }
					?: StandardCharsets.UTF_8
				val reqBody = LogTruncate.forLog(requestWrapper.contentAsByteArray, encoding)
				val resBody = LogTruncate.forLog(responseWrapper.contentAsByteArray, encoding)
				log.debug(
					"HTTP {} {}?{} status={} reqBody={} resBody={}",
					request.method,
					request.requestURI,
					request.queryString ?: "",
					responseWrapper.status,
					reqBody,
					resBody,
				)
			}
			responseWrapper.copyBodyToResponse()
		}
	}
}
