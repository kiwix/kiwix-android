#!/usr/bin/env bash

# Enable Wi-Fi on the emulator
adb shell svc wifi enable
adb logcat -c
# shellcheck disable=SC2035
adb logcat *:E -v color &

PACKAGE_NAME="org.kiwix.kiwixmobile"
TEST_PACKAGE_NAME="${PACKAGE_NAME}.test"
# Function to check if the application is installed
is_app_installed() {
  adb shell pm list packages | grep -q "$1"
}

if is_app_installed "$PACKAGE_NAME"; then
  # Delete the application to properly run the test cases.
  adb uninstall "${PACKAGE_NAME}"
fi

if is_app_installed "$TEST_PACKAGE_NAME"; then
  # Delete the test application to properly run the test cases.
  adb uninstall "${TEST_PACKAGE_NAME}"
fi
retry=0
while [ $retry -le 3 ]; do
  if ./gradlew jacocoInstrumentationTestReport; then
    echo "jacocoInstrumentationTestReport succeeded" >&2
    break
  else
    adb kill-server
    adb start-server
    # Enable Wi-Fi on the emulator
    adb shell svc wifi enable
    adb logcat -c
    # shellcheck disable=SC2035
    adb logcat *:E -v color &

    if is_app_installed "$PACKAGE_NAME"; then
      # Delete the application to properly run the test cases.
      adb uninstall "${PACKAGE_NAME}"
    fi
    if is_app_installed "$TEST_PACKAGE_NAME"; then
      # Delete the test application to properly run the test cases.
      adb uninstall "${TEST_PACKAGE_NAME}"
    fi
    ./gradlew clean
    retry=$(( retry + 1 ))
    if [ $retry -eq 3 ]; then
      adb exec-out screencap -p >screencap.png
      exit 1
    fi
  fi
done
