#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib.sh
source "$PROJECT_ROOT/scripts/lib.sh"

if [[ $# -lt 3 || $# -gt 4 ]]; then
  echo "Usage: $0 FIRE_TV_IP:5555 EXPECTED_SERIAL fire-os-6|fire-os-7 [APK_PATH]" >&2
  exit 2
fi
require_adb
TARGET="$1"
EXPECTED_SERIAL="$2"
PROFILE="$3"
APK_PATH="${4:-$PROJECT_ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk}"

if [[ "$PROFILE" != "fire-os-6" && "$PROFILE" != "fire-os-7" ]]; then
  echo "Profile must be fire-os-6 or fire-os-7." >&2
  exit 2
fi
if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  echo "Run ./scripts/build.sh first or supply an APK path." >&2
  exit 1
fi

connect_and_verify "$TARGET" "$EXPECTED_SERIAL"
adb -s "$TARGET" install -r "$APK_PATH"
adb -s "$TARGET" shell pm enable --user 0 "$PACKAGE" >/dev/null
adb -s "$TARGET" shell cmd package set-home-activity --user 0 "$ACTIVITY" >/dev/null 2>&1 || true

if [[ "$PROFILE" == "fire-os-6" ]]; then
  adb -s "$TARGET" shell appops set "$PACKAGE" GET_USAGE_STATS allow
  adb -s "$TARGET" shell appops set "$PACKAGE" SYSTEM_ALERT_WINDOW allow
else
  CURRENT_SERVICES="$(adb -s "$TARGET" shell settings get secure enabled_accessibility_services | tr -d '\r')"
  if [[ "$CURRENT_SERVICES" == "null" || -z "$CURRENT_SERVICES" ]]; then
    UPDATED_SERVICES="$REDIRECT_SERVICE"
  elif [[ ":$CURRENT_SERVICES:" == *":$REDIRECT_SERVICE:"* ]]; then
    UPDATED_SERVICES="$CURRENT_SERVICES"
  else
    UPDATED_SERVICES="$CURRENT_SERVICES:$REDIRECT_SERVICE"
  fi
  adb -s "$TARGET" shell settings put secure accessibility_enabled 0
  adb -s "$TARGET" shell settings put secure enabled_accessibility_services "$UPDATED_SERVICES"
  adb -s "$TARGET" shell settings put secure accessibility_enabled 1
fi

adb -s "$TARGET" shell am start -n "$ACTIVITY"
echo "Minimal Home installed with profile: $PROFILE"
echo "Run restore.sh to return to stock Amazon Home without uninstalling the APK."
