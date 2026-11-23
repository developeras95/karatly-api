# Karatly API

Backend for **Karatly**: track the precious metals you own (gold, silver, platinum, palladium) in every form
you hold them, value them at today's rates, and see what you would really make if you sold today, after
making charges, tax, buyback deductions and estimated tax.

> Status: **milestone M1 (rates)**. Reference data and daily metal rates with a TTL cache. The plan, calculation rules, data model and API
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
| Purities | http://localhost:8081/api/v1/purities |
| Latest rates | http://localhost:8081/api/v1/rates/latest |
| Rate providers and quota use | http://localhost:8081/api/v1/rates/providers |

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
| `KARATLY_RATES_FETCHES_PER_DAY` | `2` | How often a day rates are fetched; TTL = 24 h / this |
| `KARATLY_RATES_CURRENCY` | `INR` | Currency rates are fetched and stored in |
| `KARATLY_RATES_CHECK_CRON` | `0 */15 * * * *` | How often the background job checks the cache |
| `KARATLY_RATES_REFRESH_ON_READ` | `true` | Reading the latest rates also fetches what is past the TTL |
| `KARATLY_RATES_RETRY_AFTER` | `30m` | Wait after a provider fails before calling it again |
| `KARATLY_GOLDAPI_IO_API_KEY` | (none) | goldapi.io key; platinum and palladium are not fetched without it |
| `KARATLY_GOLDAPI_IO_MONTHLY_BUDGET` | `140` | Most goldapi.io requests per month (free plan: 150) |
| `KARATLY_GOLDPRICE_DEV_API_KEY` | (none) | goldprice.dev key; optional |
| `KARATLY_GOLDPRICE_DEV_MONTHLY_BUDGET` | `4500` | Most goldprice.dev requests per month (free plan: 5,000) |

## How rates are fetched

| Metals | Provider | Requests per fetch |
|---|---|---|
| Gold, silver, copper | goldprice.dev (`/v1/prices`, plus `/v1/convert` for the USD rate) | 2 |
| Platinum, palladium | goldapi.io (`/api/price/{metal}/INR`) | 2 |

Rates are stored per gram of pure metal in `metal_rate`, which is also the cache. Every 15 minutes, and when the
latest rates are read, Karatly checks each provider: if a cached rate of one of its metals was updated more than
24 h / `KARATLY_RATES_FETCHES_PER_DAY` ago (or there is none), it fetches; otherwise it does nothing. A fetch that
would go over the provider's monthly budget is refused, and a provider that just failed is left alone for 30
minutes. `POST /api/v1/rates/refresh` fetches at once, ignoring the TTL but not the budget.

The stored rate is the international spot price plus the metal's premium (`PUT /api/v1/rates/premiums/{metal}`),
for example Indian import duty. A rate entered by hand (`POST /api/v1/rates`, e.g. the day's IBJA rate) wins over
fetched rates for its day.

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
