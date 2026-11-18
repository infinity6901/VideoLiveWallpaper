#!/usr/bin/env bash
set -euo pipefail

APK="${1:?Usage: device-tests.sh <apk> <out-dir> [cycles]}"
OUT_DIR="${2:-device-out}"
CYCLES="${3:-5}"
PKG="com.infinity.videowallpaper.debug"

mkdir -p "$OUT_DIR/ci-logs" "$OUT_DIR/ci-status"
export CI_LOG_DIR="$OUT_DIR/ci-logs"
export CI_STATUS_DIR="$OUT_DIR/ci-status"
source .github/scripts/ci-step.sh

adb wait-for-device
adb logcat -c

ci_step install "Install debug APK on emulator" adb install -r -g "$APK"

ci_step launch-main "Cold launch MainActivity" bash -c "
  adb shell am start -W -n ${PKG}/com.infinity.videowallpaper.MainActivity
  sleep 2
  adb shell pidof ${PKG}
"

ci_step launch-settings "Launch WallpaperSettingsActivity directly" bash -c "
  adb shell am start -W -n ${PKG}/com.infinity.videowallpaper.WallpaperSettingsActivity
  sleep 2
  adb shell pidof ${PKG}
"

ci_step lifecycle-cycles "Run ${CYCLES}x background/foreground/rotate/lock lifecycle cycles" bash -c "
  adb shell settings put system accelerometer_rotation 0
  for i in \$(seq 1 ${CYCLES}); do
    echo \"Cycle \$i/${CYCLES}\"
    adb shell am start -W -n ${PKG}/com.infinity.videowallpaper.MainActivity >/dev/null
    sleep 1
    adb shell settings put system user_rotation 1
    sleep 1
    adb shell settings put system user_rotation 0
    sleep 1
    adb shell input keyevent KEYCODE_HOME
    sleep 1
    adb shell input keyevent KEYCODE_SLEEP
    sleep 1
    adb shell input keyevent KEYCODE_WAKEUP
    adb shell wm dismiss-keyguard || true
    sleep 1
    adb shell pidof ${PKG} >/dev/null
  done
"

adb shell screencap -p /sdcard/final-screen.png || true
adb pull /sdcard/final-screen.png "$OUT_DIR/final-screen.png" || true
adb logcat -d > "$OUT_DIR/app-logcat.txt" || true
adb logcat -b crash -d > "$OUT_DIR/logcat-crash.txt" || true

ci_step crash-check "Verify zero fatal exceptions or ANRs for ${PKG}" bash -c "
  if grep -E 'FATAL EXCEPTION|ANR in ${PKG}' \"$OUT_DIR/app-logcat.txt\" \"$OUT_DIR/logcat-crash.txt\" 2>/dev/null | grep -q '${PKG}'; then
    echo 'Fatal crash or ANR detected!'
    grep -n -C 5 -E 'FATAL EXCEPTION|ANR in ${PKG}' \"$OUT_DIR/app-logcat.txt\" \"$OUT_DIR/logcat-crash.txt\" || true
    exit 1
  fi
  echo 'Zero crashes or ANRs detected.'
"
