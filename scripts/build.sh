#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT/launcher"
./gradlew assembleDebug
echo "Built: $PROJECT_ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
