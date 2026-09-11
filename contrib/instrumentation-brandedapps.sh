#!/usr/bin/env bash

#
# Kiwix Android
# Copyright (c) 2024 Kiwix <android.kiwix.org>
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
#

# Marks that this script actually started running, i.e. the emulator finished
# booting and reactivecircus/android-emulator-runner handed control to us.
# .github/actions/android-emulator-runner checks for this file to tell an
# emulator boot-time crash (e.g. kiwix/kiwix-android#5047) apart from a
# genuine test failure, and only retries the whole step for the former.
touch /tmp/emulator_script_started

# The emulator's crashpad_handler subprocess can survive `adb emu kill` and
# hang the android-emulator-runner action's teardown
# (https://github.com/ReactiveCircus/android-emulator-runner/issues/385).
# Kill it once this script exits, regardless of the test outcome.
trap 'killall -INT crashpad_handler 2>/dev/null || true' EXIT

# Play-Store-tagged images (needed for API levels with no "default" system
# image, e.g. 37) take longer to unlock user 0's credential-encrypted
# storage than plain AOSP images - the app crashes on launch before that
# (DataStore's SharedPreferences migration reads CE storage at startup).
# boot_completed doesn't imply this; wait for it explicitly.
unlock_wait=0
while [ "$(adb shell getprop sys.user.0.ce_available | tr -d '\r')" != "true" ] && [ $unlock_wait -lt 60 ]; do
  sleep 2
  unlock_wait=$(( unlock_wait + 1 ))
done

# Enable Wi-Fi on the emulator
adb shell svc wifi enable
adb logcat -c
# Check if the stylus_handwriting_enabled setting exists before disabling
if adb shell settings list secure | grep -q "stylus_handwriting_enabled"; then
  adb shell settings put secure stylus_handwriting_enabled 0
fi
# shellcheck disable=SC2035
adb logcat *:E -v color &

PACKAGE_NAME="org.kiwix.kiwixmobile.custom"
TEST_PACKAGE_NAME="${PACKAGE_NAME}.test"
TEST_SERVICES_PACKAGE="androidx.test.services"
TEST_ORCHESTRATOR_PACKAGE="androidx.test.orchestrator"
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

if is_app_installed "$TEST_SERVICES_PACKAGE"; then
  adb uninstall "${TEST_SERVICES_PACKAGE}"
fi

if is_app_installed "$TEST_ORCHESTRATOR_PACKAGE"; then
  adb uninstall "${TEST_ORCHESTRATOR_PACKAGE}"
fi
retry=0
while [ $retry -le 3 ]; do
  if ./gradlew connectedCustomexampleDebugAndroidTest; then
    echo "connectedCustomexampleDebugAndroidTest succeeded" >&2
    break
  else
    adb kill-server
    adb start-server
    # Enable Wi-Fi on the emulator
    adb shell svc wifi enable
    adb logcat -c
    # Check if the stylus_handwriting_enabled setting exists before disabling
    if adb shell settings list secure | grep -q "stylus_handwriting_enabled"; then
      adb shell settings put secure stylus_handwriting_enabled 0
    fi
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
    if is_app_installed "$TEST_SERVICES_PACKAGE"; then
      adb uninstall "${TEST_SERVICES_PACKAGE}"
    fi
    if is_app_installed "$TEST_ORCHESTRATOR_PACKAGE"; then
      adb uninstall "${TEST_ORCHESTRATOR_PACKAGE}"
    fi
    ./gradlew --stop
    retry=$(( retry + 1 ))
    if [ $retry -eq 3 ]; then
      adb exec-out screencap -p >screencap.png
      exit 1
    fi
  fi
done
