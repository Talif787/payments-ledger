# Local development guide

## Prerequisites

- JDK 21 (`java -version` should report 21)
- Maven 3.9+
- Docker and the Docker Compose plugin

## Option A: everything in Docker

```
docker compose up --build
```

This starts PostgreSQL and the service. Flyway applies the schema on startup.

## Option B: Postgres in Docker, service from your IDE or Maven

```
docker compose up -d postgres
cd ledger-service
DB_URL=jdbc:postgresql://localhost:5432/ledger DB_USER=ledger DB_PASSWORD=ledger \
  mvn spring-boot:run
```

## Running tests

```
cd ledger-service
mvn test          # unit tests only (no Docker needed)
mvn verify        # includes the Testcontainers integration test (needs Docker)
```

Coverage report after `mvn verify`: `target/site/jacoco/index.html`.

## Configuration

All configuration is environment-driven (twelve-factor). See `.env.example` for
the supported variables. Defaults target the local Compose setup.

## Useful endpoints

- `GET /actuator/health` liveness and readiness
- `GET /actuator/prometheus` metrics
- `GET /swagger-ui.html` interactive API documentation

## Troubleshooting

- Port 5432 already in use: stop any local Postgres, or change the mapped port in
  `docker-compose.yml`.
- Migration checksum errors after editing a migration: never edit an applied
  migration; add a new `V2__...sql`. For local resets, `docker compose down -v`
  drops the volume.
- Serialization-conflict warnings under load are expected and are retried
  automatically by the transaction runner.
