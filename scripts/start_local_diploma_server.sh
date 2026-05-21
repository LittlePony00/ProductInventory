#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-product-inventory-db}"
MINIO_CONTAINER="${MINIO_CONTAINER:-product-inventory-minio}"
POSTGRES_VOLUME="${POSTGRES_VOLUME:-product-inventory-postgres-data}"
MINIO_VOLUME="${MINIO_VOLUME:-product-inventory-minio-data}"
POSTGRES_DB="${POSTGRES_DB:-product_inventory}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-productinventory}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-productinventory123}"
PRODUCT_IMAGES_S3_BUCKET="${PRODUCT_IMAGES_S3_BUCKET:-product-images}"
JWT_SECRET="${JWT_SECRET:-local-qa-secret-key-for-jwt-that-is-at-least-256-bits-long}"
HOST_NETWORK="${HOST_NETWORK:-vpn}"
SERVER_PORT="${SERVER_PORT:-8080}"
GIGACHAT_TRUSTSTORE_PATH="${GIGACHAT_TRUSTSTORE_PATH:-/tmp/productinventory-gigachat-truststore.jks}"
GIGACHAT_TRUSTSTORE_PASSWORD="${GIGACHAT_TRUSTSTORE_PASSWORD:-changeit}"
ENABLE_GIGACHAT_TRUSTSTORE="${ENABLE_GIGACHAT_TRUSTSTORE:-true}"
GIGACHAT_API_KEY="${GIGACHAT_API_KEY:-${GIGA_CHAT_API:-${GIGACHAT_API:-}}}"

detect_vpn_ip() {
    ifconfig | awk '
        /^[a-z0-9]+:/ {
            iface = $1
            sub(":", "", iface)
        }
        iface ~ /^utun[0-9]+$/ && $1 == "inet" && $2 != "127.0.0.1" {
            print $2
            exit
        }
    '
}

detect_lan_ip() {
    ipconfig getifaddr en0 2>/dev/null ||
        ipconfig getifaddr en1 2>/dev/null ||
        ifconfig | awk '
            /^[a-z0-9]+:/ {
                iface = $1
                sub(":", "", iface)
            }
            iface !~ /^utun[0-9]+$/ && $1 == "inet" && $2 != "127.0.0.1" {
                print $2
                exit
            }
        '
}

detect_host_ip() {
    case "$HOST_NETWORK" in
        vpn)
            detect_vpn_ip || detect_lan_ip
            ;;
        lan | wifi)
            detect_lan_ip || detect_vpn_ip
            ;;
        auto)
            detect_vpn_ip || detect_lan_ip
            ;;
        *)
            echo "Unknown HOST_NETWORK=$HOST_NETWORK. Use vpn, lan, wifi, or auto." >&2
            return 1
            ;;
    esac
}

HOST_IP="${HOST_IP:-$(detect_host_ip)}"

if [ -z "$HOST_IP" ]; then
    echo "Cannot detect host IP. Run with HOST_IP=<ip> or HOST_NETWORK=lan|vpn." >&2
    exit 1
fi

container_exists() {
    docker ps -a --format '{{.Names}}' | grep -qx "$1"
}

container_running() {
    docker ps --format '{{.Names}}' | grep -qx "$1"
}

start_postgres() {
    if container_exists "$POSTGRES_CONTAINER"; then
        if ! container_running "$POSTGRES_CONTAINER"; then
            docker start "$POSTGRES_CONTAINER" >/dev/null
        fi
        return
    fi

    docker run -d \
        --name "$POSTGRES_CONTAINER" \
        -p 5432:5432 \
        -e POSTGRES_USER="$POSTGRES_USER" \
        -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
        -e POSTGRES_DB="$POSTGRES_DB" \
        -v "$POSTGRES_VOLUME":/var/lib/postgresql/data \
        postgres:17-alpine >/dev/null
}

start_minio() {
    if container_exists "$MINIO_CONTAINER"; then
        if ! container_running "$MINIO_CONTAINER"; then
            docker start "$MINIO_CONTAINER" >/dev/null
        fi
        return
    fi

    docker run -d \
        --name "$MINIO_CONTAINER" \
        -p 9000:9000 \
        -p 9001:9001 \
        -e MINIO_ROOT_USER="$MINIO_ROOT_USER" \
        -e MINIO_ROOT_PASSWORD="$MINIO_ROOT_PASSWORD" \
        -v "$MINIO_VOLUME":/data \
        quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z \
        server /data --console-address ":9001" >/dev/null
}

wait_for_postgres() {
    until docker exec "$POSTGRES_CONTAINER" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
        sleep 1
    done
}

wait_for_minio() {
    until curl -fsS "http://127.0.0.1:9000/minio/health/live" >/dev/null 2>&1; do
        sleep 1
    done
}

configure_minio_bucket() {
    docker exec "$MINIO_CONTAINER" sh -c \
        "mc alias set local http://127.0.0.1:9000 '$MINIO_ROOT_USER' '$MINIO_ROOT_PASSWORD' >/dev/null &&
         mc mb --ignore-existing 'local/$PRODUCT_IMAGES_S3_BUCKET' >/dev/null &&
         mc anonymous set download 'local/$PRODUCT_IMAGES_S3_BUCKET' >/dev/null"
}

append_java_tool_option() {
    if [ -z "${JAVA_TOOL_OPTIONS_FOR_SERVER:-}" ]; then
        JAVA_TOOL_OPTIONS_FOR_SERVER="$1"
    else
        JAVA_TOOL_OPTIONS_FOR_SERVER="$JAVA_TOOL_OPTIONS_FOR_SERVER $1"
    fi
}

resolve_default_java_truststore() {
    if [ -n "${JAVA_CACERTS_PATH:-}" ] && [ -f "$JAVA_CACERTS_PATH" ]; then
        echo "$JAVA_CACERTS_PATH"
        return
    fi
    if [ -n "${JAVA_HOME:-}" ] && [ -f "$JAVA_HOME/lib/security/cacerts" ]; then
        echo "$JAVA_HOME/lib/security/cacerts"
        return
    fi
    java_home="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [ -n "$java_home" ] && [ -f "$java_home/lib/security/cacerts" ]; then
        echo "$java_home/lib/security/cacerts"
    fi
}

prepare_gigachat_truststore() {
    default_truststore="$(resolve_default_java_truststore)"
    if [ -z "$default_truststore" ]; then
        echo "Warning: default Java truststore was not found; GigaChat truststore will contain only imported GigaChat certificates." >&2
        return
    fi

    cp "$default_truststore" "$GIGACHAT_TRUSTSTORE_PATH"
    if [ "$GIGACHAT_TRUSTSTORE_PASSWORD" != "changeit" ]; then
        keytool -storepasswd \
            -keystore "$GIGACHAT_TRUSTSTORE_PATH" \
            -storepass changeit \
            -new "$GIGACHAT_TRUSTSTORE_PASSWORD" >/dev/null
    fi
}

configure_gigachat_truststore() {
    JAVA_TOOL_OPTIONS_FOR_SERVER="${JAVA_TOOL_OPTIONS:-}"
    if [ -z "$GIGACHAT_API_KEY" ] || [ "$ENABLE_GIGACHAT_TRUSTSTORE" != "true" ]; then
        return
    fi

    prepare_gigachat_truststore
    GIGACHAT_TRUSTSTORE_PASSWORD="$GIGACHAT_TRUSTSTORE_PASSWORD" \
        bash "$ROOT_DIR/server/scripts/create-gigachat-truststore.sh" "$GIGACHAT_TRUSTSTORE_PATH" >/dev/null
    append_java_tool_option "-Djavax.net.ssl.trustStore=$GIGACHAT_TRUSTSTORE_PATH"
    append_java_tool_option "-Djavax.net.ssl.trustStorePassword=$GIGACHAT_TRUSTSTORE_PASSWORD"
    echo "GigaChat truststore: $GIGACHAT_TRUSTSTORE_PATH"
}

start_postgres
start_minio
wait_for_postgres
wait_for_minio
configure_minio_bucket
configure_gigachat_truststore

cat <<EOF
Local diploma backend is starting.

API URL for mobile builds:
  http://$HOST_IP:$SERVER_PORT

Product image public base URL:
  http://$HOST_IP:9000/$PRODUCT_IMAGES_S3_BUCKET

Selected host network:
  $HOST_NETWORK

MinIO console:
  http://localhost:9001
  login: $MINIO_ROOT_USER
  password: $MINIO_ROOT_PASSWORD

Use HOST_NETWORK=lan ./scripts/start_local_diploma_server.sh if devices should use Wi-Fi/LAN instead of VPN.
Use HOST_IP=10.8.0.2 ./scripts/start_local_diploma_server.sh to override the address explicitly.
EOF

set +e
SPRING_PROFILES_ACTIVE=prod \
SERVER_PORT="$SERVER_PORT" \
POSTGRES_JDBC_URL="jdbc:postgresql://localhost:5432/$POSTGRES_DB" \
POSTGRES_USERNAME="$POSTGRES_USER" \
POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
JWT_SECRET="$JWT_SECRET" \
JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS_FOR_SERVER" \
GIGACHAT_API_KEY="$GIGACHAT_API_KEY" \
PRODUCT_IMAGES_S3_ENDPOINT=http://127.0.0.1:9000 \
PRODUCT_IMAGES_S3_ACCESS_KEY="$MINIO_ROOT_USER" \
PRODUCT_IMAGES_S3_SECRET_KEY="$MINIO_ROOT_PASSWORD" \
PRODUCT_IMAGES_S3_BUCKET="$PRODUCT_IMAGES_S3_BUCKET" \
PRODUCT_IMAGES_PUBLIC_BASE_URL="http://$HOST_IP:9000/$PRODUCT_IMAGES_S3_BUCKET" \
./gradlew :server:bootRun --no-daemon
BOOT_STATUS=$?
set -e

if [ "$BOOT_STATUS" -eq 143 ] || [ "$BOOT_STATUS" -eq 130 ]; then
    echo "Local diploma backend stopped."
    exit 0
fi

exit "$BOOT_STATUS"
