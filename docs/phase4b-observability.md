# Phase 4b: Observability

Adds distributed tracing, structured logs, and business metrics across the three
services so a single transaction can be followed end to end, in production and in
incident response.

## Distributed tracing (OpenTelemetry to Jaeger)

Each service exports spans over OTLP to Jaeger. Traces span process and transport
boundaries:

- HTTP: the inbound request to the ledger, and the ledger's outbound call to the
  fraud service, are spans in one trace. The fraud client is built from the
  auto-configured RestClient.Builder so the trace context propagates and the hop
  is instrumented.
- Database: JDBC calls appear as child spans.
- Kafka: the relay's publish and the reconciliation and fraud consumers are
  linked. Producer and consumer observation is enabled
  (spring.kafka.template.observation-enabled on the ledger,
  spring.kafka.listener.observation-enabled on the consumers), so the W3C trace
  context travels in Kafka headers. A posted transaction therefore links the
  ledger write, the relay publish, and the downstream consumers into one trace.

Configuration (injected via environment in docker-compose; the application.yml
equivalents are shown for reference):

    management.tracing.sampling.probability: 1.0        # sample everything in dev
    management.otlp.tracing.endpoint: http://jaeger:4318/v1/traces
    spring.application.name: <service>                  # the service name in Jaeger

Sampling is 1.0 for the demo. In production this is lowered (for example 0.05)
and paired with tail-based sampling at the collector.

View traces at the Jaeger UI on http://localhost:16686. Pick a service, find a
trace, and expand it to see the spans across services.

## Structured JSON logs with trace and correlation IDs

Logs are emitted as JSON using Spring Boot's native structured logging in ECS
format (logging.structured.format.console: ecs). Each line carries the trace id
and span id (from Micrometer tracing) and the correlation id (from the existing
correlation-id filter), so logs can be pivoted to the exact trace and correlated
across services. In production these ship to a log store (Elasticsearch, Loki)
where trace id joins logs to traces.

Note: with structured logging on, docker compose logs shows JSON lines rather
than plain text. Remove LOGGING_STRUCTURED_FORMAT_CONSOLE to revert to plain
console logs.

## Metrics (Prometheus)

Every service exposes /actuator/prometheus. Beyond the framework metrics
(notably http_server_requests_seconds, which already gives per-endpoint latency,
throughput, and error rate, so 401, 403, 429, and 5xx are all visible), the
project adds business metrics:

- ledger.outbox.backlog, ledger.outbox.published (the relay)
- reconciliation.discrepancies (drift)
- fraud.decisions, tagged by decision and mode (shadow vs active)

A production setup scrapes these with Prometheus and dashboards them in Grafana;
the scrape target is each service's /actuator/prometheus.

## What to look at after running

- Jaeger UI (http://localhost:16686): a POST /v1/transactions trace that fans out
  to the fraud HTTP call and, via Kafka, to the reconciliation and fraud
  consumers.
- Logs: docker compose logs ledger-service shows JSON with trace.id populated.
- Metrics: curl the three /actuator/prometheus endpoints for the custom series.
