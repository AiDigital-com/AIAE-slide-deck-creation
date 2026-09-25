# Strategy & Planning

Internal toolkit for the AI Digital strategy and planning team: it drafts case
studies, category analyses, and RFP response outlines with AI and publishes each
one into the user's own Google Drive, replacing the manual copy-the-template-and
-retype-everything routine.

## Owner
- Product owner: @Sheldon Bickel (Slack)
- Tech contact:  @Azat Nabiev (Slack)

## What this is

MVP demo. NOT production-grade. See "MVP limitations" below.

A signed-in strategy or planning team member opens the Strategy Toolkit home
page and picks one of four tools:

- **Case Study Builder** — uploads client reports (or types the facts in),
  gets an AI-drafted case study, reviews and edits every field, then publishes a
  Google Slides deck copied from the master template into their own Drive.
- **Category Analysis Builder** — assembles a 3–5 slide category analysis,
  optionally auto-drafted from the client's public website plus AI-generated
  slide artwork, and publishes it to Google Slides with source hyperlinks.
- **RFP Outline Generator** — uploads an RFP, gets a drafted point of view and
  response outline, and exports it to a Google Doc.
- **Product Redirection Script** — triages growth-team requests by deal size:
  buys at or above the escalation threshold go straight to RnD, everything else
  gets a relay-ready workaround response.

Every Google write runs on the signed-in user's own Google OAuth grant obtained
through Clerk — never a service account — so the generated file lands in their
Drive and is owned by them.

MVP behaviour worth knowing: with no OpenAI key configured, each AI draft falls
back to a deterministic mapping of the entered fields, so every flow stays
usable without AI. Capability search for request triage (Slite, Asana) is
likewise optional; triage works without it.

## How to run

### On Replit (recommended for demos)
1. Fork from the Custom Template.
2. Open the workspace; the `postgresql-16` module provisions a Postgres
   database and Replit injects `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`,
   and `PGPASSWORD` as Secrets.
3. Set `CLERK_SECRET_KEY`, `VITE_CLERK_PUBLISHABLE_KEY`, and `AUTH_ISSUER_URI`
   in Secrets — Clerk SSO is REQUIRED; the backend fails fast at startup
   without it (there is no mock fallback).
4. Click **Run**. Backend on port 5000, Vite dev server on 5173.
5. To deploy: click **Deploy** → choose **Reserved VM** (not Autoscale).

### On a local developer machine

The `java` on your PATH must be 21 or newer, not just the JDK Maven uses.
`scripts/local-verify.sh` checks `java -version` from PATH and aborts first;
running the packaged jar under an older JDK fails with
`UnsupportedClassVersionError`. CI is unaffected — it pins Temurin 21.

1. `cp .env.example .env` and fill in values.
2. `docker compose --profile local up --build -d`
3. Backend at `http://localhost:8080/`, frontend at
   `http://localhost:5173/` (dev) or under the backend in built mode.
   `APP_CONTEXT_PATH` is empty by default; set it only if the app must be
   served under a sub-path.
4. `docker compose --profile local down -v` to clean up.

## Data sources

- PostgreSQL — case studies, category analyses, RFP outlines, and RnD requests
  (one table per aggregate, Liquibase-managed).
- PostgreSQL `usage_events` table — usage telemetry written by the MVP
  event-logging module.
- Google Drive / Slides / Docs — the master templates and the generated files,
  reached with the user's own OAuth grant.
- OpenAI — draft text and slide artwork. Optional: blank key means "AI not
  connected" and the deterministic fallback is used.
- The client's own public website — fetched read-only to ground a category
  analysis draft, behind a server-side-request-forgery guard.

## API

Base URLs:
- Replit / deployment: `https://<replit-host>/api/v1`
- Local backend: `http://localhost:8080/api/v1`

(Prefix both with `APP_CONTEXT_PATH` if you set one; it is empty by default.)

Documentation:
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI YAML: `/api/v1/specs/openapi.yaml`

Main endpoints:

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/v1/auth/me` | Returns the current authenticated user. | Bearer JWT |
| `GET` `POST` | `/api/v1/case-studies` | Lists saved case studies; creates one and publishes its deck. | Bearer JWT |
| `POST` | `/api/v1/case-studies/drafts` | AI-drafts a case study from uploaded source documents. | Bearer JWT |
| `GET` | `/api/v1/case-studies/{id}` | Returns one saved case study. | Bearer JWT |
| `GET` `POST` | `/api/v1/category-analyses` | Lists saved category analyses; creates one and publishes its deck. | Bearer JWT |
| `GET` | `/api/v1/category-analyses/{id}` | Returns one saved category analysis. | Bearer JWT |
| `GET` | `/api/v1/category-analyses/standard-connections` | Reports whether Google and AI are connected for the standard deck flow. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-drafts` | AI-drafts a standard category analysis deck. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-drafts/slide` | Re-drafts a single slide of a standard deck. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-decks` | Publishes a reviewed standard deck to Google Slides. | Bearer JWT |
| `GET` `POST` | `/api/v1/rfp-outlines` | Lists saved RFP outlines; creates one and exports its Google Doc. | Bearer JWT |
| `POST` | `/api/v1/rfp-outlines/drafts` | AI-drafts an RFP outline from an uploaded RFP. | Bearer JWT |
| `GET` | `/api/v1/rfp-outlines/{id}` | Returns one saved RFP outline. | Bearer JWT |
| `GET` `POST` | `/api/v1/rnd-requests` | Lists triaged requests; triages a new one. | Bearer JWT |
| `GET` | `/api/v1/rnd-requests/{id}` | Returns one triaged request. | Bearer JWT |

Keep this table synchronized with `backend/application/src/main/resources/api/v1/specs/openapi.yaml`.

## Required env vars

See `.env.example`. Real values live in Replit Secrets / local `.env` (gitignored).

## MVP limitations

- Uses a demo Clerk tenant; provision a production Clerk tenant for launch.
- Usage events are written to the app's own Postgres `usage_events` table;
  switch to a company sink when cross-service aggregation is required.
- Google master template IDs are configuration, not managed content — a
  template edited in Drive changes every future deck with no versioning.
- Deck and doc generation runs synchronously inside the request; a slow AI or
  Google call holds the HTTP connection open. There is no job queue or retry.
- No rate limiting and no per-tenant scoping; access is gated only by the
  allowed email domain.
- Coverage runs on the relaxed MVP floor (see `.template-phase`).

## What engineering must replace before production

- Demo Clerk tenant → production Clerk/Google SSO tenant + production keys.
- Replit-managed DB → company-managed Postgres (RDS / CloudSQL).
- Replit Secrets → company secret manager.
- `.replit` Reserved VM target → company infrastructure.
- Product owner and tech contact in this README.
- The RFP outline Google Doc template: `RFP_OUTLINE_TEMPLATE_DOC_ID` ships
  blank, so Doc export does nothing until the template exists.
- Synchronous generation → a background job with retries and progress.
- MVP coverage floor → the strict floor, via the `finalize-coverage` step.

## CI

`.github/workflows/ci.yml` runs the MVP safety test suite, Checkstyle, JaCoCo,
the OpenAPI generator check, frontend tests/build and secret hygiene.

Integration tests run on `workflow_dispatch` with `run_integration_tests=true`.

The docker-compose local-dev dry run is not part of CI. The local profile under
"On a local developer machine" is therefore unverified by the pipeline — run it
yourself after changing `docker-compose.yml` or either `Dockerfile`.

## Architecture

- Backend: Java 21 LTS + Spring Boot 3.x, multi-module Maven.
- Java package root: `com.aidigital.strategyplanning`.
- Frontend: React + TypeScript + Vite, typed via `openapi-typescript`.
- Auth: Clerk SSO only (required) — Clerk + Google; backend validates JWTs vs Clerk JWKS.
- Persistence: PostgreSQL (Replit SQL Database on workspace, docker-compose locally).
- Outbound HTTP: every third-party client is built by the shared pooled client
  factory in `backend/external-services`, with Micrometer metrics and Logbook
  on each one.
- Observability: JSON logs to stdout, Actuator (`health`, `prometheus`),
  Logbook HTTP request/response logs with masking, Postgres `usage_events` table.
