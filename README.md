# Strategy & Planning

Internal toolkit for the AI Digital strategy and planning team: it drafts case
studies, category analyses, one-pagers, and RFP response outlines with AI and
publishes each one into the user's own Google Drive, replacing the manual
copy-the-template-and-retype-everything routine.

## Owner
- Product owner: @Sheldon Bickel (Slack)
- Tech contact:  @Azat Nabiev (Slack)

## What this is

MVP demo. NOT production-grade. See "MVP limitations" below.

A signed-in strategy or planning team member opens the Strategy Toolkit home
page and picks one of five tools:

- **Case Study Builder** — two options. The three-page deck uploads client
  reports, Word, Markdown or screenshot sources (or types the facts in), gets
  an AI-drafted case study, lets the user review every field, then publishes a
  Google Slides deck copied from the master template into their own Drive. The
  One-Page Case Study drafts into a single tokenized page, measures each field
  against its text box with the bundled fonts and revises what overflows, then
  swaps fifteen tagged icons.
- **Category Analysis Builder** — two options. The Standard deck assembles a
  3–5 slide analysis, optionally auto-drafted from the client's public website
  plus AI-generated slide artwork, and publishes it to Google Slides with source
  hyperlinks. The In-Depth deck lets the user check which of 24 slides to
  include, drafts them with live web search for fact-checking, redraws the
  donut chart and sizes the bar chart to the drafted values.
- **One Page Generator** — picks a header/footer shell and body modules, drafts
  copy that fits each module from a guidance brief, assembles the page from the
  master's exported PPTX and uploads it as a new Google Slides file. Created
  pages are listed as "Recent Pages" beside the builder.
- **RFP Outline Generator** — uploads an RFP, gets a drafted point of view and
  response outline, and exports it to a Google Doc.
- **Product Redirection Script** — an external link to the team's Slite
  assistant; it opens in a new tab and has no backend here. (The in-app R&D
  request triage, which routes buys at or above the escalation threshold to
  RnD and answers the rest with a workaround, is still built but hidden behind
  its `available` flag in the hub's tool list.)

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
- OpenAI — draft text and slide artwork; the In-Depth category analysis uses
  the Responses API with the built-in `web_search` tool. Optional: blank key
  means "AI not connected" and the deterministic fallback is used.
- Google Slides fetching from this app — the In-Depth donut PNG and the
  one-page case-study icons are swapped into a deck by URL, so Google must be
  able to reach the app's public address (see "Required env vars").
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
| `POST` | `/api/v1/case-studies/one-page-drafts` | AI-drafts a One-Page Case Study from uploaded source files. | Bearer JWT |
| `POST` | `/api/v1/case-studies/one-page-fit` | Measures reviewed one-page fields against the template's text boxes. | Bearer JWT |
| `POST` | `/api/v1/case-studies/one-page-decks` | Publishes a reviewed One-Page Case Study to Google Slides. | Bearer JWT |
| `GET` | `/api/v1/case-studies/{id}` | Returns one saved case study. | Bearer JWT |
| `GET` `POST` | `/api/v1/category-analyses` | Lists saved category analyses of every template kind (`STANDARD`, `IN_DEPTH`, `ONE_PAGER`); creates a manual one. | Bearer JWT |
| `GET` | `/api/v1/category-analyses/{id}` | Returns one saved category analysis. | Bearer JWT |
| `GET` | `/api/v1/category-analyses/standard-connections` | Reports whether Google and AI are connected for the standard deck flow. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-drafts` | AI-drafts a standard category analysis deck. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-drafts/slide` | Re-drafts a single slide of a standard deck. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/standard-decks` | Publishes a reviewed standard deck to Google Slides. | Bearer JWT |
| `GET` | `/api/v1/category-analyses/in-depth-slides` | Lists the 24 selectable In-Depth slides for the "check to include" preview. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/in-depth-drafts` | AI-drafts the checked In-Depth slides with web-search fact-checking. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/in-depth-drafts/slide` | Re-drafts a single slide of an In-Depth deck. | Bearer JWT |
| `POST` | `/api/v1/category-analyses/in-depth-decks` | Publishes a reviewed In-Depth deck to Google Slides. | Bearer JWT |
| `GET` | `/api/v1/one-pagers/connections` | Reports whether Google and AI are connected for the One Page Generator. | Bearer JWT |
| `POST` | `/api/v1/one-pagers/drafts` | AI-drafts a one-pager from a guidance brief and the chosen modules. | Bearer JWT |
| `POST` | `/api/v1/one-pagers/decks` | Assembles a reviewed one-pager and uploads it as a new Google Slides file; records it as a category analysis. | Bearer JWT |
| `GET` `POST` | `/api/v1/rfp-outlines` | Lists saved RFP outlines; creates one and exports its Google Doc. | Bearer JWT |
| `POST` | `/api/v1/rfp-outlines/drafts` | AI-drafts an RFP outline from an uploaded RFP. | Bearer JWT |
| `GET` | `/api/v1/rfp-outlines/{id}` | Returns one saved RFP outline. | Bearer JWT |
| `GET` `POST` | `/api/v1/rnd-requests` | Lists triaged requests; triages a new one. | Bearer JWT |
| `GET` | `/api/v1/rnd-requests/{id}` | Returns one triaged request. | Bearer JWT |

Keep this table synchronized with `backend/application/src/main/resources/api/v1/specs/openapi.yaml`.

## Required env vars

See `.env.example`. Real values live in Replit Secrets / local `.env` (gitignored).

Settings added with the In-Depth, One-Page Case Study and One Page Generator
tools, with their defaults from `backend/application/src/main/resources/application.yml`:

| Variable | Default | Purpose |
|---|---|---|
| `CATEGORY_ANALYSIS_IN_DEPTH_TEMPLATE_ID` | `1BovwD1sUrLHk6eTjrvKmPLF6L_-M0mOo93YGP1UJ2Ac` | Master In-Depth Category Analysis Slides template (24-slide layout library), copied per deck. |
| `CASE_STUDY_ONE_PAGE_TEMPLATE_ID` | `1S2ZPNRah7OBlPQVVdssFOCah5uc_oKfrtPjYTg38SW0` | Tokenized One-Page Case Study Slides template, copied per deck. |
| `CASE_STUDY_PUBLIC_ASSET_BASE_URL` | blank (the address the request arrived on) | Public base URL Google Slides fetches `/case-study-icons/**` from when swapping the one-page icons. |
| `ONE_PAGER_TEMPLATE_ID` | `1Ghth9tTCL685cco2c3Dm2w7Avfi9sBlWYdyFSv25jW4` | One Page Generator master ("Extended - One Pager Templates"); exported as PPTX, never copied or edited. |
| `APP_EXTERNAL_GOOGLE_TRANSFER_RESPONSE_TIMEOUT` | `120s` | Response ceiling for Drive PPTX export and upload, which move whole files. |
| `APP_EXTERNAL_GOOGLE_TRANSIENT_RETRY_ATTEMPTS` | `3` | Retries on Google's transient 5xx answers for those transfers. |

Image swaps need Google to reach this app: the In-Depth donut is served from
`/public/slide-images/{token}` and the one-page case-study icons from
`/case-study-icons/**`, and Google Slides fetches both by public URL. From
`localhost` those fetches fail and the master's placeholder images stay in the
deck, unless `CASE_STUDY_PUBLIC_ASSET_BASE_URL` points at a deployed instance
that serves the same assets.

## MVP limitations

- Uses a demo Clerk tenant; provision a production Clerk tenant for launch.
- Usage events are written to the app's own Postgres `usage_events` table;
  switch to a company sink when cross-service aggregation is required.
- Google master template IDs are configuration, not managed content — a
  template edited in Drive changes every future deck with no versioning.
- Donut and icon swaps only happen when Google can fetch the image from this
  app's public URL; on `localhost` they are skipped unless
  `CASE_STUDY_PUBLIC_ASSET_BASE_URL` is set (see "Required env vars").
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
