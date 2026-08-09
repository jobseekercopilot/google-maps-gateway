# Google Maps Gateway

## Develop branch status

**Repository skeleton; not part of the runtime catalogue or Docker Compose.** No callable API, provider client, configuration, database, or local port is implemented on `develop`; the text below describes an intended boundary rather than current runtime behaviour.

Do not treat richer feature-branch commute work as released architecture. See the central [location status](https://docs.jobseekercopilot.com/journeys/location/) and [implementation status](https://docs.jobseekercopilot.com/reference/implementation-status/).

The only Job Seeker Copilot service permitted to call Google Places API (New)
and Routes API. It owns Google credentials, provider DTOs, request field masks,
session-token handling, quota/billable-event metrics and provider response
validation. It never persists provider responses.

Only `location-service` consumes this internal gateway. Google-specific DTOs,
URLs and credentials do not cross into domain services or the Angular client.

## Safe activation

Google is disabled by default. Real provider calls require all of the following:

1. approved privacy/DPIA and product consent work;
2. a restricted server-side Google key supplied as `GOOGLE_MAPS_API_KEY`;
3. explicit `GOOGLE_MAPS_ENABLED=true`;
4. deliberately configured provider quotas, budgets and operational alerts.

No key is created or purchased by this repository. Normal development and CI
set `GOOGLE_MAPS_ENABLED=false` and make no paid-provider request.

## Requirements and configuration

- Java 17 and Maven 3.9

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8105` | HTTP port |
| `GOOGLE_MAPS_GATEWAY_TOKEN` | none, required | Internal caller authentication; at least 32 bytes |
| `GOOGLE_MAPS_ENABLED` | `false` | Explicit provider enablement |
| `GOOGLE_MAPS_API_KEY` | empty | Runtime-only restricted credential |
| `GOOGLE_PLACES_BASE_URL` | `https://places.googleapis.com` | Places API origin |
| `GOOGLE_ROUTES_BASE_URL` | `https://routes.googleapis.com` | Routes API origin |
| `GOOGLE_MAPS_CONNECT_TIMEOUT` | `500ms` | Provider connection deadline |
| `GOOGLE_MAPS_READ_TIMEOUT` | `4s` | Provider response deadline |
| `GOOGLE_MAPS_SESSION_TTL` | `10m` | Google billing-session lifetime |
| `GOOGLE_MAPS_MAXIMUM_SESSIONS` | `10000` | Bounded in-memory sessions |

Credentials are inserted only into the `X-Goog-Api-Key` request header and are
never included in responses, metrics or log fields.

## API and contract

The producer-owned internal OpenAPI 1.0.0 source is
[`api/openapi.yaml`](api/openapi.yaml). `location-service` checks in the exact
reviewed snapshot and generates its client into disposable Maven build output.

- `POST /internal/v1/places/autocomplete`
- `POST /internal/v1/places/resolve`
- `POST /internal/v1/routes/matrix`
- `/actuator/health`
- `/actuator/health/readiness`

Every application endpoint requires `X-Service-Token`; health endpoints do not.
Provider responses, route results and Google address content remain transient.
Place resolution requests only `id` and `addressComponents`; Google coordinates,
formatted addresses, names and place types are deliberately not requested.

## Data retention and attribution

- A Google Place ID may cross the boundary for durable provider-reference storage.
- Autocomplete labels, address components and provider responses are transaction-only.
- Route distance, duration and response payloads are response-only and must not be
  written to profiles, saved jobs, analytics or logs.
- Any Places or Routes content shown without a Google map must be visibly attributed
  to `Google Maps` in the same content container. Public product Terms and Privacy
  notices must link to Google's applicable Terms and Privacy Policy.

## Build

```bash
GOOGLE_MAPS_GATEWAY_TOKEN=test-only-google-maps-gateway-token-32-bytes \
GOOGLE_MAPS_ENABLED=false \
mvn -B clean verify
```

Automated tests use mocked clients and cannot contact a live provider.

## Branch workflow

Use `feature/* → develop`. Do not merge application delivery directly into
`main`.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).
