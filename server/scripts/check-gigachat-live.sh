#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${GIGACHAT_API_KEY:-}" ]]; then
  echo "GIGACHAT_API_KEY is required" >&2
  exit 2
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TRUSTSTORE="${GIGACHAT_TRUSTSTORE_PATH:-/tmp/productinventory-gigachat-truststore.jks}"
TRUSTSTORE_PASSWORD="${GIGACHAT_TRUSTSTORE_PASSWORD:-changeit}"

GIGACHAT_TRUSTSTORE_PASSWORD="$TRUSTSTORE_PASSWORD" \
  bash "$ROOT_DIR/server/scripts/create-gigachat-truststore.sh" "$TRUSTSTORE" >/dev/null

JAVA_TRUST_OPTIONS="-Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD"

cd "$ROOT_DIR"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }$JAVA_TRUST_OPTIONS" \
  RUN_GIGACHAT_LIVE_TEST=true \
  ./gradlew :server:test --tests '*GigaChatLiveIntegrationTest' --rerun-tasks
