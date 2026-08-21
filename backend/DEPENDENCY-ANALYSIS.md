# Maven dependency analysis

`mvn verify` runs `maven-dependency-plugin:analyze-only` in every backend
module and fails on an unused compile dependency. Keep the dependency graph
small: remove a library when the gate identifies it as unused, rather than
adding a broad ignore.

The allowlist contains exactly two coordinates:

- `org.projectlombok:lombok` — required at compile time for its annotation
  processor, while the compiled classes no longer reference the Lombok JAR.
- `${project.groupId}:event-logging-to-db-feature` — the `service` module keeps
  this edge after the reference sample is stripped so the first real services
  can use `@LogUsage` and `UsageAttributes`; `application` also receives the
  runtime feature transitively through this edge. Between sample removal and
  the first real aggregate there is intentionally no service bytecode that can
  reference it.

The second allowance covers only that empty post-initialization lifecycle
window. It is not a wildcard and does not suppress analysis of any other
internal or external dependency.

Runtime and test dependencies are intentionally excluded by Maven scope
(`ignoreNonCompile=true`); they are not compile dependencies and must not be
copied into this allowlist.

If a new framework is legitimately loaded only through reflection or annotation
processing, document the exact coordinate and reason here, then add only that
coordinate under `ignoredUnusedDeclaredDependencies` in `backend/pom.xml`.

## Explicit dependencies, starters at runtime scope

Every module declares the libraries it compiles against, by coordinate. Spring
Boot starters are aggregators, so a module that compiles against
`spring-web` declares `spring-web` — not `spring-boot-starter-web`.

Starters are still declared where they are genuinely needed, but at
`runtime` scope in `backend/application`: they contribute auto-configuration and
the servlet container, which the application needs when it boots and never
references from Java. The same reasoning covers `postgresql`, `ehcache`,
`hibernate-jcache`, `logbook-spring-boot-starter`, `logstash-logback-encoder`,
`micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui`, and
`liquibase-core` in `backend/migrations`, which is a resource-only module
shipping changelogs plus the engine that reads them.

`cache-management` and `observability` are also `runtime` edges from
`application`: they are wired by component scanning, not by compiled
references. The edges stay declared because the architecture requires them and
the structure gates assert they exist.

Runtime and provided scopes are excluded from the unused-dependency check by
`ignoreNonCompile=true`. That is scope-accurate, not an allowlist: a
compile-scope dependency that nothing references still fails the build.
