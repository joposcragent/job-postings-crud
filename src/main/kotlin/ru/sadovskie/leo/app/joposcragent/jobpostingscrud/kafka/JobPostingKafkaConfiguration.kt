package ru.sadovskie.leo.app.joposcragent.jobpostingscrud.kafka

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "app.kafka", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JobPostingKafkaProperties::class)
class JobPostingKafkaConfiguration
