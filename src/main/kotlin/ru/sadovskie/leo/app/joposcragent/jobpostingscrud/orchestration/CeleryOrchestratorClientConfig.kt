package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(CeleryOrchestratorProperties::class)
class CeleryOrchestratorClientConfig {

	@Bean
	@Qualifier("celeryOrchestrator")
	fun celeryOrchestratorRestClient(
		properties: CeleryOrchestratorProperties,
	): RestClient {
		// JDK HttpClient defaults can negotiate HTTP/2 cleartext (h2c): Upgrade: h2c and
		// Connection: Upgrade, HTTP2-Settings. Many HTTP/1.1-only stacks then mis-handle the body
		// (e.g. FastAPI/uvicorn → 422 "request body is required"). Force HTTP/1.1 for plain outbound REST.
		val httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(10))
			.build()
		// Buffer so Content-Length is set; default RestClient streaming can arrive as empty body to uvicorn.
		val requestFactory = BufferingClientHttpRequestFactory(JdkClientHttpRequestFactory(httpClient))
		return RestClient.builder()
			.baseUrl(properties.baseUrl.trimEnd('/'))
			.requestFactory(requestFactory)
			.build()
	}
}
