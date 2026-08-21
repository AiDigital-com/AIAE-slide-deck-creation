#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f scripts/lib/deploy-env.sh ]; then
  # shellcheck source=lib/deploy-env.sh
  . scripts/lib/deploy-env.sh
fi

source scripts/replit-env.sh

# Prefer the exploded layout from replit-build.sh; fall back to the fat jar.
# `.original` is the pre-repackage jar and has no main manifest.
JAR="$(ls backend/application/target/extracted/*.jar 2>/dev/null | grep -v '\.original$' | head -n1)"
if [ -z "${JAR}" ]; then
  JAR="$(ls backend/application/target/*.jar 2>/dev/null | grep -v '\.original$' | head -n1)"
fi
if [ -z "${JAR}" ]; then
  echo "ERROR: no jar in backend/application/target/. Run replit-build.sh first." >&2
  exit 1
fi

# TieredStopAtLevel=1 cuts JIT overhead during Spring init on the throttled
# Reserved VM. MaxRAMPercentage=75 replaces the ergonomic ~25% default, which is
# too small for a deck-generation request that holds whole documents in memory.
exec java -XX:TieredStopAtLevel=1 -XX:MaxRAMPercentage=75 -Dspring.jmx.enabled=false -jar "${JAR}"
