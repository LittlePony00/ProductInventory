#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEVICE="${ANDROID_DEVICE:-emulator-5554}"
AVD="${ANDROID_AVD:-Pixel_9_Pro_Fold_API_35}"
ADB="${ANDROID_ADB:-"$HOME/Library/Android/sdk/platform-tools/adb"}"
APK="$ROOT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
APP_ID="com.android.rut.miit.productinventory"

layout() {
  android layout --device "$DEVICE" --pretty
}

tap() {
  "$ADB" -s "$DEVICE" shell input tap "$1" "$2"
}

text() {
  "$ADB" -s "$DEVICE" shell input text "$1"
}

assert_layout_contains() {
  local expected="$1"
  if ! layout | grep -F "$expected" >/dev/null; then
    echo "Expected layout text not found: $expected" >&2
    layout >&2
    exit 1
  fi
}

SERVER_PID=""
cleanup() {
  if [[ -n "$SERVER_PID" ]] && ps -p "$SERVER_PID" >/dev/null 2>&1; then
    kill "$SERVER_PID" || true
  fi
}
trap cleanup EXIT

cd "$ROOT_DIR"
./gradlew :composeApp:assembleDebug

SPRING_PROFILES_ACTIVE=dev ./gradlew :server:bootRun >/tmp/product-inventory-server.log 2>&1 &
SERVER_PID="$!"
for _ in {1..60}; do
  if curl -fsS http://localhost:8080/health >/dev/null 2>&1; then
    break
  fi
  if ! ps -p "$SERVER_PID" >/dev/null 2>&1; then
    echo "Server exited. Log:" >&2
    tail -100 /tmp/product-inventory-server.log >&2
    exit 1
  fi
  sleep 1
done

android emulator start "$AVD"
"$ADB" -s "$DEVICE" shell pm clear "$APP_ID" >/dev/null
android run --apks "$APK" --device "$DEVICE"
assert_layout_contains "Войдите в аккаунт"

tap 1038 1512
assert_layout_contains "Создание аккаунта"
email="user$(date +%s)@example.com"
tap 1038 841
text E2E
tap 1038 1027
text "$email"
tap 1038 1213
text Password123
tap 1038 1409
sleep 1
if layout | grep -F "Save password to Google Password Manager?" >/dev/null; then
  tap 428 1995
fi
assert_layout_contains "Нет домохозяйств"

tap 1965 1966
assert_layout_contains "Новое домохозяйство"
tap 1037 1075
text Home
tap 1564 1272
sleep 1
assert_layout_contains "Home"

tap 1897 557
assert_layout_contains "Нет продуктов"
tap 1965 1966
assert_layout_contains "Добавить продукт"
tap 1038 409
text Milk
tap 533 1417
text 2
tap 1038 1977
sleep 1
assert_layout_contains "Остаток: 2.00 / 2.00 шт"

tap 1907 723
assert_layout_contains "Списать продукт"
tap 1037 1139
"$ADB" -s "$DEVICE" shell input keyevent 67
"$ADB" -s "$DEVICE" shell input keyevent 67
"$ADB" -s "$DEVICE" shell input keyevent 67
text 0.75
tap 1282 1336
sleep 1
assert_layout_contains "Остаток: 1.25 / 2.00 шт"

tap 1907 723
assert_layout_contains "Доступно: 1.25 шт"
tap 1282 1336
sleep 1
assert_layout_contains "Остаток: 0.00 / 2.00 шт"

echo "Android Product Usage E2E passed on $DEVICE"
