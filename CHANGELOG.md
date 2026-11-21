# Changelog

All notable changes to Karatly are listed here. Versions follow [Semantic Versioning](https://semver.org/):
MAJOR for breaking API changes, MINOR for new features, PATCH for fixes.

## [Unreleased]

### Added

- Project skeleton: Spring Boot 4.1, Java 21, PostgreSQL with Flyway, RFC 7807 error responses, OpenAPI
  (Swagger UI at `/swagger-ui.html`), health at `/actuator/health`, CI with a coverage gate.
- Reference data: `GET /api/v1/metals`, `GET /api/v1/metals/{code}`, `GET /api/v1/units` (gram, milligram,
  kilogram, troy ounce, tola).
- `docs/requirements-and-design.md`: scope, calculation rules, data model and API plan.
