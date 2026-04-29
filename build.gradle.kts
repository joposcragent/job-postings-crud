import nu.studer.gradle.jooq.JooqEdition
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target

plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
	id("nu.studer.jooq") version "9.0"
	jacoco
}

group = "ru.sadovskie.leo.app.joposcragent"
version = "1.3.0"

jacoco {
	toolVersion = "0.8.12"
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.postgresql:postgresql")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

	jooqGenerator("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("io.mockk:mockk-jvm:1.14.6")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

val jooqDbUrl = System.getenv("JOOQ_DB_URL") ?: "jdbc:postgresql://localhost:5432/joposcragent"
val jooqDbUser = System.getenv("JOOQ_DB_USER") ?: "postgres"
val jooqDbPassword = System.getenv("JOOQ_DB_PASSWORD") ?: "postgres"

jooq {
	version.set("3.19.18")
	edition.set(JooqEdition.OSS)
	configurations {
		create("main") {
			generateSchemaSourceOnCompilation.set(false)
			jooqConfiguration.apply {
				jdbc = Jdbc().apply {
					driver = "org.postgresql.Driver"
					url = jooqDbUrl
					user = jooqDbUser
					password = jooqDbPassword
				}
				generator = Generator().apply {
					database = Database().apply {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "job_postings"
						excludes = "flyway_schema_history"
						forcedTypes = listOf(
							ForcedType().apply {
								name = "LOCALDATE"
								includeExpression = ".*\\.postings\\.publication_date"
							},
						)
					}
					generate = Generate().apply {
						isDeprecated = false
						isRecords = true
						isImmutablePojos = true
						isFluentSetters = true
					}
					target = Target().apply {
						packageName = "ru.sadovskie.leo.app.joposcragent.jobpostings.jooq"
						directory = "build/generated-src/jooq/main"
					}
				}
			}
		}
	}
}

sourceSets["main"].java.srcDir("build/generated-src/jooq/main")

// Run `./gradlew generateJooq` when the DB schema changes
tasks.named("compileKotlin") {
	dependsOn("generateJooq")
}
val dockerImageRepository = System.getenv("IMAGE_NAME") ?: "joposcragent/${rootProject.name}"
val dockerImageTag = System.getenv("IMAGE_TAG") ?: project.version.toString()

tasks.bootBuildImage {
	imageName.set("$dockerImageRepository:$dockerImageTag")
	finalizedBy("bootBuildImageTagLatest")
}

tasks.register<Exec>("bootBuildImageTagLatest") {
	group = "container"
	description = "docker tag: помечает образ из bootBuildImage тегом latest"
	commandLine(
		"docker", "tag",
		"$dockerImageRepository:$dockerImageTag",
		"$dockerImageRepository:latest",
	)
}

tasks.register("buildImage") {
	group = "container"
	description = "Build OCI image via Buildpacks (bootBuildImage)."
	dependsOn("bootBuildImage")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val jacocoInstrumentedClasses = files(
	sourceSets["main"].output.classesDirs.files.map { dir ->
		fileTree(dir) { exclude("**/jobpostings/jooq/**") }
	},
)

tasks.named<JacocoReport>("jacocoTestReport") {
	dependsOn(tasks.test)
	classDirectories.setFrom(jacocoInstrumentedClasses)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
	dependsOn(tasks.test)
	classDirectories.setFrom(jacocoInstrumentedClasses)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.60".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
