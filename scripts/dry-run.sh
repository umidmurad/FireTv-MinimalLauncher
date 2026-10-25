#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib.sh
source "$PROJECT_ROOT/scripts/lib.sh"

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 FIRE_TV_IP:5555 EXPECTED_SERIAL fire-os-6|fire-os-7" >&2
  exit 2
fi
require_adb
TARGET="$1"
EXPECTED_SERIAL="$2"
PROFILE="$3"
if [[ "$PROFILE" != "fire-os-6" && "$PROFILE" != "fire-os-7" ]]; then
  echo "Profile must be fire-os-6 or fire-os-7." >&2
  exit 2
fi

connect_and_verify "$TARGET" "$EXPECTED_SERIAL"

echo "Model: $(adb -s "$TARGET" shell getprop ro.product.model | tr -d '\r')"
echo "Fire OS build: $(adb -s "$TARGET" shell getprop ro.build.version.name | tr -d '\r')"
echo "Android release: $(adb -s "$TARGET" shell getprop ro.build.version.release | tr -d '\r')"
echo "Current Home: $(adb -s "$TARGET" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null | tr -d '\r')"
echo "Minimal Home installed: $(adb -s "$TARGET" shell pm path "$PACKAGE" 2>/dev/null | tr -d '\r')"
echo
echo "Planned profile: $PROFILE"
echo "The installer would install the local APK, enable $PACKAGE, configure only the"
echo "selected Home redirect, and start Minimal Home. It would not disable or uninstall"
echo "any Amazon package. No changes were made by this dry run."
