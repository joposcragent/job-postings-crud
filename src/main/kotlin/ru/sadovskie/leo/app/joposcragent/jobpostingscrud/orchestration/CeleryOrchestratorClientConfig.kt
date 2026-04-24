package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(CeleryOrchestratorProperties::class)
class CeleryOrchestratorClientConfig {

	@Bean
	@Qualifier("celeryOrchestrator")
	fun celeryOrchestratorRestClient(
		properties: CeleryOrchestratorProperties,
	): RestClient = RestClient.builder()
		.baseUrl(properties.baseUrl.trimEnd('/'))
		.build()
}
