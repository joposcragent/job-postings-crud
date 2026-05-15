package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class WebLoggingConfiguration {

	@Bean
	fun httpExchangeDebugLoggingFilter(): FilterRegistrationBean<HttpExchangeDebugLoggingFilter> =
		FilterRegistrationBean(HttpExchangeDebugLoggingFilter()).apply {
			order = Ordered.HIGHEST_PRECEDENCE + 10
			addUrlPatterns("/*")
		}
}
