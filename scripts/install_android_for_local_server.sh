#!/usr/bin/env bash
set -euo pipefail

HOST_NETWORK="${HOST_NETWORK:-vpn}"

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

find_adb() {
    if command -v adb >/dev/null 2>&1; then
        command -v adb
    elif [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
        printf '%s\n' "$ANDROID_HOME/platform-tools/adb"
    elif [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
        printf '%s\n' "$HOME/Library/Android/sdk/platform-tools/adb"
    else
        return 1
    fi
}

HOST_IP="${HOST_IP:-$(detect_host_ip)}"

if [ -z "$HOST_IP" ]; then
    echo "Cannot detect host IP. Run with HOST_IP=<ip> or HOST_NETWORK=lan|vpn." >&2
    exit 1
fi

ADB="$(find_adb || true)"
if [ -z "$ADB" ]; then
    echo "adb was not found. Install Android platform-tools or set ANDROID_HOME." >&2
    exit 1
fi

CONNECTED_DEVICES="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
if [ -z "$CONNECTED_DEVICES" ]; then
    echo "No connected adb devices. Connect a phone/emulator before running Gradle install." >&2
    echo "Checked with: $ADB devices" >&2
    exit 1
fi

echo "Installing Android app with API_BASE_URL=http://$HOST_IP:8080"
./gradlew :composeApp:installDebug -PapiBaseUrl="http://$HOST_IP:8080" --no-daemon
