# Karatly API

Backend for **Karatly**: track the precious metals you own (gold, silver, platinum, palladium) in every form
you hold them, value them at today's rates, and see what you would really make if you sold today, after
making charges, tax, buyback deductions and estimated tax.

> Status: **skeleton (milestone M0)**. Reference data only. The plan, calculation rules, data model and API
> are in [docs/requirements-and-design.md](docs/requirements-and-design.md).

## Stack

Java 21 · Spring Boot 4.1 · PostgreSQL 17 · Flyway · springdoc-openapi · JUnit 5 + Testcontainers · JaCoCo

## Run it

You need Java 21 and Docker.

```bash
docker compose up -d          # PostgreSQL on localhost:5434
./mvnw spring-boot:run        # API on http://localhost:8081   (Windows: mvnw.cmd spring-boot:run)
```

Or everything in Docker: `docker compose --profile app up -d --build`.

| What | Where |
|---|---|
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI document | http://localhost:8081/v3/api-docs |
| Health | http://localhost:8081/actuator/health |
| Metals | http://localhost:8081/api/v1/metals |
| Weight units | http://localhost:8081/api/v1/units |

Ports 5434 and 8081 are chosen so Karatly runs next to Duewise (5432 / 8080) without clashes.

## Test

```bash
./mvnw verify      # unit, web-layer and integration tests (needs Docker) + coverage gate
```

The build fails below 90% line and 70% branch coverage. The report is at `target/site/jacoco/index.html`.

## Configuration

Every setting is in `src/main/resources/application.yml` as `${NAME:default}`.

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/karatly` | Database |
| `DB_USERNAME` / `DB_PASSWORD` | `karatly` / `karatly` | Database login |
| `SERVER_PORT` | `8081` | HTTP port |
| `KARATLY_TIME_ZONE` | `Asia/Kolkata` | Decides what "today" is (sale rate date, holding periods) |

## Conventions

- Packages by layer, then feature: `controller/<feature>`, `dto/<feature>`, `entity/<feature>`,
  `repository/<feature>`, `service/<feature>`.
- Errors are RFC 7807 problem details with a stable `code` and, for validation, an `errors` list.
- Money and weights are `BigDecimal` / `NUMERIC`; weights are stored in grams.
- Flyway owns the schema (`ddl-auto: validate`).

## Related

- [karatly-web](../karatly-web): the React web app.

## Licence

Proprietary. The code is public to read, not to use. See [LICENSE](LICENSE).
