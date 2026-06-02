#!/usr/bin/env bash
set -euo pipefail

HOSTS=(
  "ngw.devices.sberbank.ru:9443"
  "gigachat.devices.sberbank.ru:443"
)

OUTPUT="${1:-/tmp/productinventory-gigachat-truststore.jks}"
PASSWORD="${GIGACHAT_TRUSTSTORE_PASSWORD:-changeit}"
WORKDIR="$(mktemp -d "${TMPDIR:-/tmp}/gigachat-certs.XXXXXX")"

for host_port in "${HOSTS[@]}"; do
  host="${host_port%%:*}"
  port="${host_port##*:}"
  openssl s_client -showcerts -connect "$host:$port" -servername "$host" </dev/null 2>/dev/null |
    awk -v prefix="$WORKDIR/$host-cert-" '
      /BEGIN CERTIFICATE/ { i++ }
      i > 0 { print > (prefix i ".pem") }
    ' || true
done

shopt -s nullglob
certificates=("$WORKDIR"/*.pem)
shopt -u nullglob

if [ "${#certificates[@]}" -eq 0 ]; then
  echo "No GigaChat certificates were fetched." >&2
  exit 1
fi

index=0
for certificate in "${certificates[@]}"; do
  index=$((index + 1))
  alias="gigachat-$index"
  keytool -list \
    -storepass "$PASSWORD" \
    -keystore "$OUTPUT" \
    -alias "$alias" >/dev/null 2>&1 &&
    keytool -delete \
      -storepass "$PASSWORD" \
      -keystore "$OUTPUT" \
      -alias "$alias" >/dev/null
  keytool -importcert \
    -noprompt \
    -storepass "$PASSWORD" \
    -keystore "$OUTPUT" \
    -alias "$alias" \
    -file "$certificate" >/dev/null
done

echo "$OUTPUT"
