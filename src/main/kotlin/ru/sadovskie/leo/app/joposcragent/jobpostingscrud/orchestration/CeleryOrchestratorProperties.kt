package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.orchestration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "celery.orchestrator")
data class CeleryOrchestratorProperties(
	val baseUrl: String = "http://localhost:8000",
)
