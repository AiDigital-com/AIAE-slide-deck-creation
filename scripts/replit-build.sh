#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# The SPA bakes CLERK_PUBLISHABLE_KEY into the bundle at build time
# (vite.config.ts `define`), so the published-app values must be swapped in
# before the frontend build runs — a runtime override would be too late.
if [ -f scripts/lib/deploy-env.sh ]; then
  # shellcheck source=lib/deploy-env.sh
  . scripts/lib/deploy-env.sh
fi

source scripts/replit-env.sh

bash scripts/structure-lint.sh
bash scripts/verify-gates.sh

cd frontend
if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi
npm run generate:api
test -n "${VITE_CLERK_PUBLISHABLE_KEY:-}" || {
  echo "ERROR: VITE_CLERK_PUBLISHABLE_KEY or CLERK_PUBLISHABLE_KEY must be available during frontend build." >&2
  exit 1
}
npm run build
cd ..

STATIC_DIR="backend/application/src/main/resources/static"
test -f "${STATIC_DIR}/index.html" || {
  echo "ERROR: frontend build did not produce ${STATIC_DIR}/index.html." >&2
  exit 1
}

mvn -f backend/pom.xml -B -DskipTests package

JAR="$(find backend/application/target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
if [ -z "${JAR}" ]; then
  echo "ERROR: no Spring Boot jar produced in backend/application/target." >&2
  exit 1
fi

# The Reserved VM throttles CPU during cold boot; opening the nested fat jar
# pushes first port-bind past the deployment's port check and the container
# restarts in a loop. The exploded layout loads from plain files instead.
rm -rf backend/application/target/extracted
java -Djarmode=tools -jar "${JAR}" extract --destination backend/application/target/extracted
