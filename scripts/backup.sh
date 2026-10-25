#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib.sh
source "$PROJECT_ROOT/scripts/lib.sh"

require_identity_args "$@"
require_adb
TARGET="$1"
EXPECTED_SERIAL="$2"
OUTPUT_DIR="${3:-$PROJECT_ROOT/backups/$(date +%Y%m%d-%H%M%S)}"

connect_and_verify "$TARGET" "$EXPECTED_SERIAL"
mkdir -p "$OUTPUT_DIR"

adb -s "$TARGET" shell getprop > "$OUTPUT_DIR/getprop.txt"
adb -s "$TARGET" shell pm list packages -f > "$OUTPUT_DIR/packages-all.txt"
adb -s "$TARGET" shell pm list packages -d > "$OUTPUT_DIR/packages-disabled.txt"
adb -s "$TARGET" shell settings list global > "$OUTPUT_DIR/settings-global.txt"
adb -s "$TARGET" shell settings list secure > "$OUTPUT_DIR/settings-secure.txt"
adb -s "$TARGET" shell settings list system > "$OUTPUT_DIR/settings-system.txt"
adb -s "$TARGET" shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME \
  > "$OUTPUT_DIR/home-resolver.txt" 2>&1 || true
adb -s "$TARGET" exec-out screencap -p > "$OUTPUT_DIR/home-before.png"

echo "Private backup written to: $OUTPUT_DIR"
echo "Do not commit this directory to a public repository."
