# API Documentation and Specification Strategy

## Purpose
The API Documentation module ensures that the Meer platform is discoverable, testable, and provides a clear contract for frontend developers and external consumers.

## Scope
- Automated OpenAPI (Swagger) generation.
- Static API contract definition.
- Interactive documentation (Swagger UI).

## OpenAPI Integration
The system uses `springdoc-openapi` to automatically generate API specifications from the Spring Boot controllers.

### Configuration: `OpenApiConfig`
- **Profiles**: API documentation is enabled in `local-db`, `local`, and `default` profiles.
- **Grouping**: All paths (`/**`) are grouped under the "meer" name.

### Interactive UI
When running in an enabled profile, the interactive Swagger UI is available at:
- `/swagger-ui/index.html`
- `/v3/api-docs` (JSON format)

## Static Contract: `docs/openapi.yaml`
In addition to automated generation, the project maintains a primary static OpenAPI 3.0 specification file in the root `docs/` directory.

### Invariants
- The `docs/openapi.yaml` file should be considered the primary source of truth for the API contract before implementation.
- All new endpoints must be reflected in the OpenAPI spec.
- The automated Swagger UI should be used to verify that the implementation matches the defined contract.

## Non-Goals
- Public API portal (currently restricted to internal/dashboard use).
- Multi-version API support (v1 only).
