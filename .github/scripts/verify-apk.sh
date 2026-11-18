#!/usr/bin/env bash
set -euo pipefail

APK="${1:?Usage: verify-apk.sh <path-to-apk>}"
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }

BUILD_TOOLS_DIR=$(find "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -n1)
AAPT2="${BUILD_TOOLS_DIR}/aapt2"
APKSIGNER="${BUILD_TOOLS_DIR}/apksigner"

echo "APK size: $(du -h "$APK" | cut -f1)"

if [ -x "$AAPT2" ]; then
  echo "--- aapt2 dump badging ---"
  "$AAPT2" dump badging "$APK" | grep -E '^(package:|sdkVersion:|targetSdkVersion:|native-code:|application-label:)' || true
fi

if [ -x "$APKSIGNER" ]; then
  echo "--- apksigner verify ---"
  "$APKSIGNER" verify --print-certs "$APK"
else
  echo "apksigner not found in $BUILD_TOOLS_DIR"
  exit 1
fi
