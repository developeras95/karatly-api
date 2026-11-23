# Changelog

All notable changes to Karatly are listed here. Versions follow [Semantic Versioning](https://semver.org/):
MAJOR for breaking API changes, MINOR for new features, PATCH for fixes.

## [Unreleased]

### Added (M1: rates)

- Daily metal rates in INR per gram of pure metal, from two providers: goldprice.dev for gold, silver and copper
  (2 requests per fetch, works without a key) and goldapi.io for platinum and palladium (needs
  `KARATLY_GOLDAPI_IO_API_KEY`, 2 requests per fetch). Both sit behind a `RateProvider` interface.
- TTL cache: `metal_rate` holds one rate per metal, currency, day and source. A provider is only called when a cached
  rate of one of its metals was last updated more than 24 h / `KARATLY_RATES_FETCHES_PER_DAY` ago (default 2, so
  12 hours) or is missing. A background check runs every 15 minutes (`KARATLY_RATES_CHECK_CRON`) and reading the
  latest rates also fetches what is past the TTL (`KARATLY_RATES_REFRESH_ON_READ`).
- Guards: every outside call is logged in `rate_provider_call`; a fetch that would exceed a provider's monthly budget
  is refused (`KARATLY_GOLDPRICE_DEV_MONTHLY_BUDGET` 4500, `KARATLY_GOLDAPI_IO_MONTHLY_BUDGET` 140), and a provider
  that failed is left alone for `KARATLY_RATES_RETRY_AFTER` (30 minutes).
- Premium over spot per metal (e.g. Indian import duty), applied when a rate is fetched and stored with it.
- `GET /api/v1/rates/latest` (with the rate for each purity), `GET /api/v1/rates/history`, `POST /api/v1/rates`
  (manual rate, e.g. IBJA, which wins for its day), `POST /api/v1/rates/refresh`, `GET /api/v1/rates/providers`,
  `GET /api/v1/rates/premiums`, `PUT /api/v1/rates/premiums/{metal}`, `GET /api/v1/purities`.
- Copper (XCU) as a fifth metal; purities for gold (24K 999/995, 22K, 18K, 14K), silver (999, 925), platinum and
  palladium (999, 950).
- Migration `V3`: `purity`, `metal_rate`, `rate_premium`, `rate_provider_call`.

### Added (M0: skeleton)

- Project skeleton: Spring Boot 4.1, Java 21, PostgreSQL with Flyway, RFC 7807 error responses, OpenAPI
  (Swagger UI at `/swagger-ui.html`), health at `/actuator/health`, CI with a coverage gate.
- Reference data: `GET /api/v1/metals`, `GET /api/v1/metals/{code}`, `GET /api/v1/units` (gram, milligram,
  kilogram, troy ounce, tola).
- `docs/requirements-and-design.md`: scope, calculation rules, data model and API plan.
