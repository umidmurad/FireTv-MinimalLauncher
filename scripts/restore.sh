#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib.sh
source "$PROJECT_ROOT/scripts/lib.sh"

require_identity_args "$@"
require_adb
TARGET="$1"
EXPECTED_SERIAL="$2"

connect_and_verify "$TARGET" "$EXPECTED_SERIAL"

adb -s "$TARGET" shell appops reset "$PACKAGE" >/dev/null 2>&1 || true

CURRENT_SERVICES="$(adb -s "$TARGET" shell settings get secure enabled_accessibility_services | tr -d '\r')"
UPDATED_SERVICES=""
if [[ "$CURRENT_SERVICES" != "null" && -n "$CURRENT_SERVICES" ]]; then
  IFS=':' read -r -a SERVICE_LIST <<< "$CURRENT_SERVICES"
  for SERVICE_NAME in "${SERVICE_LIST[@]}"; do
    if [[ -n "$SERVICE_NAME" && "$SERVICE_NAME" != "$REDIRECT_SERVICE" ]]; then
      UPDATED_SERVICES="${UPDATED_SERVICES:+$UPDATED_SERVICES:}$SERVICE_NAME"
    fi
  done
fi

if [[ -n "$UPDATED_SERVICES" ]]; then
  adb -s "$TARGET" shell settings put secure enabled_accessibility_services "$UPDATED_SERVICES"
  adb -s "$TARGET" shell settings put secure accessibility_enabled 1
else
  adb -s "$TARGET" shell settings delete secure enabled_accessibility_services >/dev/null
  adb -s "$TARGET" shell settings put secure accessibility_enabled 0
fi

adb -s "$TARGET" shell cmd package set-home-activity --user 0 "$AMAZON_HOME" >/dev/null 2>&1 || true
if adb -s "$TARGET" shell pm path "$PACKAGE" | grep -q '^package:'; then
  adb -s "$TARGET" shell pm disable-user --user 0 "$PACKAGE" >/dev/null
  adb -s "$TARGET" shell am force-stop "$PACKAGE"
fi
adb -s "$TARGET" shell input keyevent KEYCODE_HOME

echo "Stock Amazon Home restored. Minimal Home remains installed but disabled."
