#!/bin/sh
set -eu

personal_team_id="${PRODUCTINVENTORY_PERSONAL_TEAM_ID:-DZ6N62Z5PL}"
entitlements="${CODE_SIGN_ENTITLEMENTS:-}"

if [ -z "$entitlements" ]; then
  exit 0
fi

case "$entitlements" in
  /*) entitlements_path="$entitlements" ;;
  *) entitlements_path="$SRCROOT/$entitlements" ;;
esac

if [ "${DEVELOPMENT_TEAM:-}" = "$personal_team_id" ] &&
  [ -f "$entitlements_path" ] &&
  /usr/bin/grep -q "aps-environment" "$entitlements_path"; then
  echo "error: Push Notifications require a paid Apple Developer team/profile. Personal team $personal_team_id cannot sign aps-environment. Use paid-team provisioning or build without CODE_SIGN_ENTITLEMENTS." >&2
  exit 1
fi
