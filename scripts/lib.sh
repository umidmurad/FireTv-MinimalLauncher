#!/usr/bin/env bash

PACKAGE="dev.umid.firetvlauncher"
ACTIVITY="$PACKAGE/.MainActivity"
REDIRECT_SERVICE="$PACKAGE/$PACKAGE.HomeRedirectService"
AMAZON_HOME="com.amazon.tv.launcher/.ui.HomeActivity_vNext"

require_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb is required and was not found in PATH." >&2
    exit 1
  fi
}

require_identity_args() {
  if [[ $# -lt 2 || -z "$1" || -z "$2" ]]; then
    echo "Both FIRE_TV_IP:5555 and EXPECTED_SERIAL are required." >&2
    exit 2
  fi
}

connect_and_verify() {
  local target="$1"
  local expected_serial="$2"
  adb connect "$target" >/dev/null
  local actual_serial
  actual_serial="$(adb -s "$target" shell getprop ro.serialno | tr -d '\r')"
  if [[ -z "$actual_serial" || "$actual_serial" != "$expected_serial" ]]; then
    echo "Refusing to continue: expected serial '$expected_serial', found '$actual_serial'." >&2
    exit 1
  fi
  echo "Verified device serial: $actual_serial"
}
