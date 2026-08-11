#!/usr/bin/env bash

PACKAGE_NAME="org.kiwix.kiwixmobile"
TEST_PACKAGE_NAME="${PACKAGE_NAME}.test"
TEST_SERVICES_PACKAGE="androidx.test.services"
TEST_ORCHESTRATOR_PACKAGE="androidx.test.orchestrator"

MAX_RETRIES=3
retry=0
LOGCAT_PID=""

# Function to check if the application is installed.
is_app_installed() {
  adb shell pm list packages | grep -q "$1"
}

# Start logcat and print everything directly to the CI console.
start_logcat() {
  echo "Starting logcat..."

  adb logcat -c
  adb logcat -v threadtime &
  LOGCAT_PID=$!

  echo "Logcat started with PID: $LOGCAT_PID"
}

# Stop the current logcat process.
stop_logcat() {
  if [ -n "${LOGCAT_PID:-}" ] && kill -0 "$LOGCAT_PID" 2>/dev/null; then
    echo "Stopping logcat process: $LOGCAT_PID"
    kill "$LOGCAT_PID" 2>/dev/null || true
    wait "$LOGCAT_PID" 2>/dev/null || true
  fi

  LOGCAT_PID=""
}

# Cleanup when the script exits.
cleanup() {
  stop_logcat
}

trap cleanup EXIT

# Enable Wi-Fi on the emulator.
adb shell svc wifi enable

# Check if the stylus_handwriting_enabled setting exists before disabling it.
if adb shell settings list secure | grep -q "stylus_handwriting_enabled"; then
  adb shell settings put secure stylus_handwriting_enabled 0
fi

# Start full logcat output.
start_logcat

# Remove previously installed application and test packages.
if is_app_installed "$PACKAGE_NAME"; then
  adb uninstall "$PACKAGE_NAME"
fi

if is_app_installed "$TEST_PACKAGE_NAME"; then
  adb uninstall "$TEST_PACKAGE_NAME"
fi

if is_app_installed "$TEST_SERVICES_PACKAGE"; then
  adb uninstall "$TEST_SERVICES_PACKAGE"
fi

if is_app_installed "$TEST_ORCHESTRATOR_PACKAGE"; then
  adb uninstall "$TEST_ORCHESTRATOR_PACKAGE"
fi

while [ "$retry" -lt "$MAX_RETRIES" ]; do
  attempt=$((retry + 1))

  echo "=========================================="
  echo "Starting test attempt $attempt/$MAX_RETRIES"
  echo "=========================================="

  if ./gradlew connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=org.kiwix.kiwixmobile.localLibrary.OpeningFilesFromStorageTest \
    "-Dorg.gradle.jvmargs=-Xmx16G -XX:+UseParallelGC" \
    -Dfile.encoding=UTF-8; then

    echo "=========================================="
    echo "connectedDebugAndroidTest succeeded"
    echo "=========================================="

    exit 0
  fi

  echo "=========================================="
  echo "Test attempt $attempt failed"
  echo "=========================================="

  stop_logcat

  retry=$((retry + 1))

  if [ "$retry" -ge "$MAX_RETRIES" ]; then
    echo "All $MAX_RETRIES test attempts failed."

    echo "Capturing final emulator screenshot..."
    adb exec-out screencap -p > screencap.png || true

    exit 1
  fi

  echo "=========================================="
  echo "Preparing emulator for retry $((retry + 1))"
  echo "=========================================="

  # Restart ADB.
  adb kill-server || true
  adb start-server

  # Wait until the emulator is available again.
  adb wait-for-device

  # Enable Wi-Fi again after restarting ADB.
  adb shell svc wifi enable

  # Disable stylus handwriting if the setting exists.
  if adb shell settings list secure | grep -q "stylus_handwriting_enabled"; then
    adb shell settings put secure stylus_handwriting_enabled 0
  fi

  # Remove installed application and test packages.
  if is_app_installed "$PACKAGE_NAME"; then
    adb uninstall "$PACKAGE_NAME" || true
  fi

  if is_app_installed "$TEST_PACKAGE_NAME"; then
    adb uninstall "$TEST_PACKAGE_NAME" || true
  fi

  if is_app_installed "$TEST_SERVICES_PACKAGE"; then
    adb uninstall "$TEST_SERVICES_PACKAGE" || true
  fi

  if is_app_installed "$TEST_ORCHESTRATOR_PACKAGE"; then
    adb uninstall "$TEST_ORCHESTRATOR_PACKAGE" || true
  fi

  # Stop Gradle daemons and clean the project before retrying.
  ./gradlew --stop
  ./gradlew clean

  # Start a fresh logcat process for the next attempt.
  start_logcat
done

exit 1
