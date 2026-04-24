---
name: spring-boot-restclient-jdk-http11-outbound
description: >-
  Outbound REST from Spring Boot 3+/4+ with RestClient and JdkClientHttpRequestFactory:
  force HTTP/1.1 to avoid h2c upgrade headers that break HTTP/1.1-only peers (empty body,
  bogus 422). Use when configuring RestClient/RestTemplate to Python/FastAPI, uvicorn,
  nginx, or any service behind plain HTTP where you see Upgrade h2c, Connection with
  HTTP2-Settings, or "request body is required" despite a valid JSON body.
---

# Spring Boot outbound RestClient: JDK client and HTTP/1.1

## Rule

Whenever you add or change **outbound HTTP** from a Spring Boot application using **`RestClient`** (or `RestTemplate`) backed by **`JdkClientHttpRequestFactory`** (explicitly or via Boot defaults), **verify protocol negotiation**.

If the downstream is **HTTP/1.1-only** or otherwise does not speak **HTTP/2 cleartext (h2c)**, the JDK `HttpClient` may still try an upgrade and send:

- `Upgrade: h2c`
- `Connection: Upgrade, HTTP2-Settings` (or similar)

Some servers then **do not read the request body correctly** and return misleading errors such as **422** with `{"detail":"request body is required"}` even though the client built a valid JSON body. Postman or curl works because they do not send that upgrade by default.

## Required fix

Build a dedicated `java.net.http.HttpClient` with an explicit protocol version and pass it into `JdkClientHttpRequestFactory`:

```kotlin
import java.net.http.HttpClient
import java.time.Duration

val httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .connectTimeout(Duration.ofSeconds(10))
    .build()
val factory = JdkClientHttpRequestFactory(httpClient)
// Often wrap with BufferingClientHttpRequestFactory when peers need Content-Length on small JSON bodies.
```

Do **not** rely on `JdkClientHttpRequestFactory()` with no arguments for calls to fragile HTTP/1.1 stacks unless you have confirmed they tolerate h2c upgrade.

## When to apply

- Spring Boot **3.x / 4.x**, Java **17+**, `RestClient` + JDK HTTP client.
- Integrations with **FastAPI/uvicorn**, **gunicorn**, older proxies, or internal services on **plain `http://`**.
- Debugging shows correct body in the debugger but wire-level or server logs show missing body; or headers include **`upgrade: h2c`**.

## Alternatives

- Use **Apache HttpClient 5** or another factory whose defaults match your downstream contract.
- If the peer truly supports **HTTPS + HTTP/2**, prefer that path instead of cleartext upgrade on port 80.

## Related pattern in this repo

`CeleryOrchestratorClientConfig` in **job-postings-crud** configures the celery-orchestrator `RestClient` with `HTTP_1_1` for the reasons above.
